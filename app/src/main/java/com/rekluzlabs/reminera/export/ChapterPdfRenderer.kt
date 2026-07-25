package com.rekluzlabs.reminera.export

import android.content.Context
import android.graphics.Canvas
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
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

    suspend fun renderChapter(
        context: Context,
        memberId: Long,
        html: String,
        chapterTitle: String
    ): RenderResult = withContext(Dispatchers.Main) {
        val outputDir = File(context.cacheDir, "chapter_previews")
        outputDir.mkdirs()
        val outputFile = File(outputDir, "chapter_${memberId}.pdf")

        try {
            val webView = WebView(context.applicationContext).apply {
                setLayerType(View.LAYER_TYPE_SOFTWARE, null)
            }

            suspendCancellableCoroutine { cont ->
                webView.webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        if (cont.isActive) cont.resume(Unit)
                    }
                }
                webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
            }

            val contentWidth = A4_WIDTH_PX - (MARGIN_PX * 2)

            webView.measure(
                View.MeasureSpec.makeMeasureSpec(contentWidth, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            )
            webView.layout(0, 0, webView.measuredWidth, webView.measuredHeight)

            val contentHeight = webView.measuredHeight
            val pageContentHeight = A4_HEIGHT_PX - (MARGIN_PX * 2)
            val pageCount = maxOf(1, (contentHeight + pageContentHeight - 1) / pageContentHeight)

            val pdfDocument = PdfDocument()

            for (pageIndex in 0 until pageCount) {
                val pageInfo = PdfDocument.PageInfo.Builder(
                    A4_WIDTH_PX, A4_HEIGHT_PX, pageIndex + 1
                ).create()
                val page = pdfDocument.startPage(pageInfo)

                val canvas = page.canvas
                canvas.translate(MARGIN_PX.toFloat(), MARGIN_PX.toFloat())
                canvas.save()
                canvas.translate(0f, -(pageIndex * pageContentHeight).toFloat())
                canvas.clipRect(
                    0f,
                    (pageIndex * pageContentHeight).toFloat(),
                    contentWidth.toFloat(),
                    ((pageIndex + 1) * pageContentHeight).toFloat()
                )
                webView.draw(canvas)
                canvas.restore()

                pdfDocument.finishPage(page)
            }

            outputFile.outputStream().use { out ->
                pdfDocument.writeTo(out)
            }
            pdfDocument.close()
            webView.destroy()

            if (outputFile.exists() && outputFile.length() > 0) {
                RenderResult.Success(outputFile)
            } else {
                RenderResult.Failure("PDF output file missing or empty after write")
            }
        } catch (e: Exception) {
            RenderResult.Failure(e.message ?: "Unknown rendering error")
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
