package com.rekluzlabs.reminera.util

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.rekluzlabs.reminera.data.MemoryEntryEntity
import java.io.File

object MediaSaver {

    private const val REMINERA_DIR = "Reminera"

    fun saveToDevice(context: Context, entry: MemoryEntryEntity): Boolean {
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

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
                    put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_DCIM}/$REMINERA_DIR")
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }

                val collectionUri = when (entry.type) {
                    "PHOTO" -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                    "VIDEO" -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                    "AUDIO" -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                    else -> MediaStore.Downloads.EXTERNAL_CONTENT_URI
                }

                val uri = context.contentResolver.insert(collectionUri, contentValues)
                    ?: return false

                context.contentResolver.openOutputStream(uri)?.use { output ->
                    sourceFile.inputStream().use { input ->
                        input.copyTo(output)
                    }
                }

                contentValues.clear()
                contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                context.contentResolver.update(uri, contentValues, null, null)

                true
            } else {
                val destDir = Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DCIM
                ).resolve(REMINERA_DIR)
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
