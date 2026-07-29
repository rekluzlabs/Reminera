package com.rekluzlabs.reminera.export

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.view.View
import android.view.ViewGroup
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

            @Suppress("DEPRECATION")
            val w = webView
            val picture = suspendCancellableCoroutine { cont ->
                w.viewTreeObserver.addOnPreDrawListener(
                    object : android.view.ViewTreeObserver.OnPreDrawListener {
                        var attempts = 0
                        override fun onPreDraw(): Boolean {
                            attempts++
                            if (attempts >= 3) {
                                w.viewTreeObserver.removeOnPreDrawListener(this)
                                if (cont.isActive) cont.resume(w.capturePicture())
                            }
                            return true
                        }
                    }
                )
            }

            if (picture == null || picture.width <= 0 || picture.height <= 0) {
                return@withContext RenderResult.Failure("WebView produced empty picture (${picture?.width}x${picture?.height})")
            }

            val contentHeight = picture.height

            val pdfDocument = PdfDocument()

            for (pageIndex in 0 until maxOf(1, (contentHeight + pageContentHeight - 1) / pageContentHeight)) {
                val pageInfo = PdfDocument.PageInfo.Builder(
                    A4_WIDTH_PX, A4_HEIGHT_PX, pageIndex + 1
                ).create()
                val page = pdfDocument.startPage(pageInfo)

                val canvas = page.canvas
                canvas.translate(MARGIN_PX.toFloat(), MARGIN_PX.toFloat())

                val srcY = pageIndex * pageContentHeight
                val srcHeight = minOf(pageContentHeight, contentHeight - srcY)
                canvas.save()
                canvas.clipRect(0f, 0f, contentWidth.toFloat(), srcHeight.toFloat())
                canvas.translate(0f, -srcY.toFloat())
                picture.draw(canvas)
                canvas.restore()

                pdfDocument.finishPage(page)
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
