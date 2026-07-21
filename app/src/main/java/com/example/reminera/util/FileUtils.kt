package com.example.reminera.util

import android.content.Context
import android.net.Uri
import java.io.File

fun copyUriToInternal(context: Context, uri: Uri, extension: String = "mp4"): String {
    val inputStream = context.contentResolver.openInputStream(uri) ?: error("Cannot open input stream")
    val dir = File(context.filesDir, "media")
    dir.mkdirs()
    val destFile = File(dir, "${System.currentTimeMillis()}.$extension")
    inputStream.use { input ->
        destFile.outputStream().use { output ->
            input.copyTo(output)
        }
    }
    return destFile.absolutePath
}
