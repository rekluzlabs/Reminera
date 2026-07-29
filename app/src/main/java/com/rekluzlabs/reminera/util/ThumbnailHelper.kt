package com.rekluzlabs.reminera.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.util.UUID

object ThumbnailHelper {
    private const val TAG = "ThumbnailHelper"

    /**
     * Generates a thumbnail for a video and saves it to internal storage.
     * Returns the absolute path to the generated thumbnail file, or null if it fails.
     */
    fun generateVideoThumbnail(context: Context, videoPath: String): String? {
        val retriever = MediaMetadataRetriever()
        return try {
            val file = File(videoPath)
            if (!file.exists()) return null

            retriever.setDataSource(videoPath)
            val bitmap = retriever.frameAtTime ?: return null

            saveThumbnail(context, bitmap)
        } catch (e: Exception) {
            Log.e(TAG, "Error generating thumbnail for $videoPath", e)
            null
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {}
        }
    }

    /**
     * Generates a thumbnail for a video from a Uri and saves it to internal storage.
     */
    fun generateVideoThumbnail(context: Context, videoUri: Uri): String? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, videoUri)
            val bitmap = retriever.frameAtTime ?: return null

            saveThumbnail(context, bitmap)
        } catch (e: Exception) {
            Log.e(TAG, "Error generating thumbnail for $videoUri", e)
            null
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {}
        }
    }

    fun generateLinkThumbnail(context: Context, url: String): String? {
        val videoId = extractYouTubeVideoId(url) ?: return null
        return try {
            val thumbnailUrl = URL("https://img.youtube.com/vi/$videoId/hqdefault.jpg")
            val connection = thumbnailUrl.openConnection()
            connection.connectTimeout = 5000
            connection.readTimeout = 10000
            val inputStream = connection.getInputStream()
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()
            if (bitmap != null) saveThumbnail(context, bitmap) else null
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading YouTube thumbnail for $url", e)
            null
        }
    }

    fun extractYouTubeVideoId(url: String): String? {
        val regex = Regex("""(?:youtube\.com/watch\?v=|youtu\.be/|youtube\.com/shorts/)([a-zA-Z0-9_-]{11})""")
        return regex.find(url)?.groupValues?.getOrNull(1)
    }

    private fun saveThumbnail(context: Context, bitmap: Bitmap): String? {
        val thumbDir = File(context.filesDir, "thumbnails").also { it.mkdirs() }
        val thumbFile = File(thumbDir, "thumb_${UUID.randomUUID()}.jpg")

        return try {
            FileOutputStream(thumbFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
            }
            thumbFile.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "Error saving thumbnail", e)
            null
        }
    }
}
