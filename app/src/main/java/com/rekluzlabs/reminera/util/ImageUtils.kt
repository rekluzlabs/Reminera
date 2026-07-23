package com.rekluzlabs.reminera.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream

object ImageUtils {

    fun getExifOrientation(file: File): Int {
        return try {
            val exif = ExifInterface(file.absolutePath)
            exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        } catch (_: Exception) {
            ExifInterface.ORIENTATION_NORMAL
        }
    }

    fun getExifOrientation(inputStream: InputStream): Int {
        return try {
            val exif = ExifInterface(inputStream)
            exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        } catch (_: Exception) {
            ExifInterface.ORIENTATION_NORMAL
        }
    }

    fun getExifOrientation(uri: Uri, context: Context): Int {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return ExifInterface.ORIENTATION_NORMAL
        return try {
            getExifOrientation(inputStream)
        } finally {
            inputStream.close()
        }
    }

    fun rotateBitmapIfNeeded(bitmap: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.postRotate(90f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.postRotate(270f)
                matrix.postScale(-1f, 1f)
            }
            else -> return bitmap
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    fun loadBitmapWithExifOrientation(context: Context, uriStr: String, maxDimension: Int = 2048): Bitmap {
        val uri = Uri.parse(uriStr)
        val file = File(uriStr)

        return if (file.exists() && uriStr.startsWith("/")) {
            loadBitmapWithExifOrientation(file, maxDimension)
        } else {
            loadBitmapWithExifOrientation(context, uri, maxDimension)
        }
    }

    @Throws(Exception::class)
    fun loadBitmapWithExifOrientation(context: Context, uri: Uri, maxDimension: Int = 2048): Bitmap {
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: throw Exception("Unable to open input stream for $uri")

        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeStream(inputStream, null, options)
        inputStream.close()

        var inSampleSize = 1
        if (options.outHeight > maxDimension || options.outWidth > maxDimension) {
            inSampleSize = if (options.outHeight > options.outWidth) {
                options.outHeight / maxDimension
            } else {
                options.outWidth / maxDimension
            }
        }

        val inputStream2 = context.contentResolver.openInputStream(uri)
            ?: throw Exception("Unable to open input stream for $uri")

        options.inJustDecodeBounds = false
        options.inSampleSize = inSampleSize
        val bitmap = BitmapFactory.decodeStream(inputStream2, null, options)
            ?: throw Exception("Failed to decode bitmap from $uri")
        inputStream2.close()

        val orientation = getExifOrientation(uri, context)
        return rotateBitmapIfNeeded(bitmap, orientation)
    }

    @Throws(Exception::class)
    fun loadBitmapWithExifOrientation(file: File, maxDimension: Int = 2048): Bitmap {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, options)

        var inSampleSize = 1
        if (options.outHeight > maxDimension || options.outWidth > maxDimension) {
            inSampleSize = if (options.outHeight > options.outWidth) {
                options.outHeight / maxDimension
            } else {
                options.outWidth / maxDimension
            }
        }

        options.inJustDecodeBounds = false
        options.inSampleSize = inSampleSize
        val bitmap = BitmapFactory.decodeFile(file.absolutePath, options)
            ?: throw Exception("Failed to decode bitmap from ${file.absolutePath}")

        val orientation = getExifOrientation(file)
        return rotateBitmapIfNeeded(bitmap, orientation)
    }

    fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    fun cropBitmap(bitmap: Bitmap, x: Int, y: Int, width: Int, height: Int): Bitmap {
        return Bitmap.createBitmap(bitmap, x, y, width, height)
    }

    fun saveBitmapToFile(bitmap: Bitmap, file: File): Boolean {
        return try {
            val outputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, outputStream)
            outputStream.close()
            true
        } catch (_: Exception) {
            false
        }
    }

    fun saveBitmapToCache(bitmap: Bitmap, context: Context, fileName: String = "edited_${System.currentTimeMillis()}.jpg"): File? {
        return try {
            val outputFile = File(context.cacheDir, fileName)
            saveBitmapToFile(bitmap, outputFile)
            outputFile
        } catch (_: Exception) {
            null
        }
    }
}

fun Bitmap.asComposeBitmap(): ImageBitmap {
    return asImageBitmap()
}