package com.rekluzlabs.reminera.util

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.rekluzlabs.reminera.data.MemoryEntryEntity
import com.rekluzlabs.reminera.data.StoryEntryEntity
import java.io.File

object DownloadHelper {

    fun downloadToDownloads(context: Context, entry: MemoryEntryEntity): Boolean {
        val sourceFile = File(entry.localFilePath)
        if (!sourceFile.exists()) return false

        return try {
            val mimeType = when (entry.type) {
                "PHOTO" -> "image/jpeg"
                "VIDEO" -> "video/mp4"
                "AUDIO" -> "audio/mp4"
                else -> "application/octet-stream"
            }

            val extension = sourceFile.extension.ifBlank {
                when (entry.type) {
                    "PHOTO" -> "jpg"
                    "VIDEO" -> "mp4"
                    "AUDIO" -> "mp4"
                    else -> "bin"
                }
            }

            val displayName = "${entry.title}_${entry.id.take(8)}.$extension"

            saveFileToDownloads(context, sourceFile, displayName, mimeType)
        } catch (_: Exception) {
            false
        }
    }

    fun downloadStoryEntryToDownloads(context: Context, entry: StoryEntryEntity): Boolean {
        val filePath = entry.mediaUri ?: return false
        val sourceFile = File(filePath)
        if (!sourceFile.exists()) return false

        return try {
            val mimeType = when (entry.type) {
                "photo" -> "image/jpeg"
                "video" -> "video/mp4"
                "audio" -> "audio/mp4"
                else -> "application/octet-stream"
            }

            val extension = sourceFile.extension.ifBlank {
                when (entry.type) {
                    "photo" -> "jpg"
                    "video" -> "mp4"
                    "audio" -> "m4a"
                    else -> "bin"
                }
            }

            val name = entry.textContent?.take(30) ?: entry.type.replaceFirstChar { it.uppercase() }
            val displayName = "${name}_${entry.id.take(8)}.$extension"

            saveFileToDownloads(context, sourceFile, displayName, mimeType)
        } catch (_: Exception) {
            false
        }
    }

    private fun saveFileToDownloads(context: Context, sourceFile: File, displayName: String, mimeType: String): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, displayName)
                    put(MediaStore.Downloads.MIME_TYPE, mimeType)
                    put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                    put(MediaStore.Downloads.IS_PENDING, 1)
                }

                val collectionUri = MediaStore.Downloads.EXTERNAL_CONTENT_URI

                val uri = context.contentResolver.insert(collectionUri, contentValues)
                    ?: return false

                context.contentResolver.openOutputStream(uri)?.use { output ->
                    sourceFile.inputStream().use { input ->
                        input.copyTo(output)
                    }
                }

                contentValues.clear()
                contentValues.put(MediaStore.Downloads.IS_PENDING, 0)
                context.contentResolver.update(uri, contentValues, null, null)

                true
            } else {
                @Suppress("DEPRECATION")
                val destDir = Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOWNLOADS
                )
                destDir.mkdirs()
                val destFile = File(destDir, displayName)
                sourceFile.copyTo(destFile, overwrite = true)
                true
            }
        } catch (_: Exception) {
            false
        }
    }
}
