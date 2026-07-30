package com.rekluzlabs.reminera.export

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.os.Build
import android.os.ParcelFileDescriptor
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.resume

object ChapterPdfRenderer {

    sealed class RenderResult {
        data class Success(val outputFile: File) : RenderResult()
        data class Failure(val error: String) : RenderResult()
    }

    private const val A4_WIDTH_PX = 2480
    private const val A4_HEIGHT_PX = 3508
    private const val MARGIN_PX = 48

    private fun Activity.windowContentView(): ViewGroup? =
        window?.decorView?.findViewById(android.R.id.content)

    suspend fun renderChapter(
        context: Context,
        memberId: Long,
        html: String,
        chapterTitle: String
    ): RenderResult = withContext(Dispatchers.Main) {
        val outputDir = File(context.cacheDir, "chapter_previews")
        outputDir.mkdirs()
        val outputFile = File(outputDir, "chapter_${memberId}.pdf")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            WebView.enableSlowWholeDocumentDraw()
        }

        val activity = context as? Activity
        val hostView = activity?.windowContentView()

        var webView: WebView? = null
        try {
            val contentWidth = A4_WIDTH_PX - (MARGIN_PX * 2)
            val pageContentHeight = A4_HEIGHT_PX - (MARGIN_PX * 2)

            webView = WebView(context).apply {
                setBackgroundColor(Color.WHITE)
            }

            if (hostView != null) {
                webView.layoutParams = FrameLayout.LayoutParams(
                    contentWidth, ViewGroup.LayoutParams.WRAP_CONTENT
                )
                hostView.addView(webView)
            }

            suspendCancellableCoroutine { cont ->
                webView.webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        if (cont.isActive) cont.resume(Unit)
                    }
                }
                webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
            }

            val wv = webView ?: return@withContext RenderResult.Failure("WebView not created")
            val contentHeight = suspendCancellableCoroutine<Int> { cont ->
                var attempts = 0
                wv.viewTreeObserver.addOnPreDrawListener(
                    object : ViewTreeObserver.OnPreDrawListener {
                        override fun onPreDraw(): Boolean {
                            attempts++
                            if (attempts >= 3) {
                                wv.viewTreeObserver.removeOnPreDrawListener(this)
                                wv.measure(
                                    View.MeasureSpec.makeMeasureSpec(contentWidth, View.MeasureSpec.EXACTLY),
                                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
                                )
                                wv.layout(0, 0, contentWidth, wv.measuredHeight)
                                val h = maxOf(wv.measuredHeight, 1)
                                if (cont.isActive) cont.resume(h)
                            }
                            return true
                        }
                    }
                )
            }

            if (contentHeight <= 0) {
                return@withContext RenderResult.Failure("WebView produced zero-height content")
            }

            val pageCount = maxOf(1, (contentHeight + pageContentHeight - 1) / pageContentHeight)
            val pdfDocument = PdfDocument()

            for (pageIndex in 0 until pageCount) {
                val srcY = pageIndex * pageContentHeight
                val srcHeight = minOf(pageContentHeight, contentHeight - srcY)

                wv.scrollTo(0, srcY)

                val pageBitmap = Bitmap.createBitmap(A4_WIDTH_PX, A4_HEIGHT_PX, Bitmap.Config.ARGB_8888)
                val pageCanvas = Canvas(pageBitmap)
                pageCanvas.drawColor(Color.WHITE)

                val wvBitmap = Bitmap.createBitmap(contentWidth, srcHeight, Bitmap.Config.ARGB_8888)
                val wvCanvas = Canvas(wvBitmap)
                wvCanvas.drawColor(Color.WHITE)
                wv.draw(wvCanvas)
                pageCanvas.drawBitmap(wvBitmap, MARGIN_PX.toFloat(), MARGIN_PX.toFloat(), null)
                wvBitmap.recycle()

                val pageInfo = PdfDocument.PageInfo.Builder(
                    A4_WIDTH_PX, A4_HEIGHT_PX, pageIndex + 1
                ).create()
                val page = pdfDocument.startPage(pageInfo)
                page.canvas.drawBitmap(pageBitmap, 0f, 0f, null)
                pdfDocument.finishPage(page)

                pageBitmap.recycle()
            }

            outputFile.outputStream().use { out ->
                pdfDocument.writeTo(out)
            }
            pdfDocument.close()

            if (outputFile.exists() && outputFile.length() > 0) {
                RenderResult.Success(outputFile)
            } else {
                RenderResult.Failure("PDF output file missing or empty after write")
            }
        } catch (e: Exception) {
            RenderResult.Failure(e.message ?: "Unknown rendering error")
        } finally {
            webView?.let { w ->
                (w.parent as? ViewGroup)?.removeView(w)
                w.destroy()
            }
        }
    }

    fun verifyPdfDimensions(file: File): Pair<Int, Int>? {
        return try {
            val pfd = ParcelFileDescriptor.open(
                file, ParcelFileDescriptor.MODE_READ_ONLY
            )
            PdfRenderer(pfd).use { renderer ->
                val page = renderer.openPage(0)
                val width = page.width
                val height = page.height
                page.close()
                Pair(width, height)
            }
        } catch (_: Exception) {
            null
        }
    }
}
