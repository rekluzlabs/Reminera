package com.rekluzlabs.reminera.ui.editor

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.RectF
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.NativeCanvas
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rekluzlabs.reminera.R
import com.rekluzlabs.reminera.util.ImageUtils
import com.rekluzlabs.reminera.util.asComposeBitmap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
class ImageEditorViewModel : ViewModel() {
    var originalBitmap: Bitmap? by mutableStateOf(null)
        private set
    var currentBitmap: Bitmap? by mutableStateOf(null)
        private set
    var cropRect: RectF? by mutableStateOf(null)
        private set

    fun loadImage(uri: Uri, context: Context) {
        currentBitmap = null // Show loading
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val bitmap = ImageUtils.loadBitmapWithExifOrientation(context, uri.toString())
                val copy = bitmap.copy(Bitmap.Config.ARGB_8888, true)
                originalBitmap = copy
                currentBitmap = copy
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun rotateLeft() {
        currentBitmap?.let { bitmap ->
            val matrix = Matrix().apply { postRotate(-90f) }
            currentBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        }
    }

    fun rotateRight() {
        currentBitmap?.let { bitmap ->
            val matrix = Matrix().apply { postRotate(90f) }
            currentBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        }
    }

    fun startCrop() {
        currentBitmap?.let { bitmap ->
            cropRect = RectF(0f, 0f, bitmap.width.toFloat(), bitmap.height.toFloat())
        }
    }

    fun applyCrop(normalizedRect: RectF): Boolean {
        return try {
            currentBitmap?.let { bitmap ->
                val x = (normalizedRect.left * bitmap.width).toInt().coerceIn(0, bitmap.width - 1)
                val y = (normalizedRect.top * bitmap.height).toInt().coerceIn(0, bitmap.height - 1)
                val width = (normalizedRect.width() * bitmap.width).toInt().coerceIn(1, bitmap.width - x)
                val height = (normalizedRect.height() * bitmap.height).toInt().coerceIn(1, bitmap.height - y)

                if (width > 0 && height > 0) {
                    currentBitmap = ImageUtils.cropBitmap(bitmap, x, y, width, height)
                    cropRect = null
                    true
                } else {
                    false
                }
            } ?: false
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun resetCrop() {
        cropRect = null
    }

    fun getEditedBitmap(): Bitmap? = currentBitmap

    fun saveImage(context: Context, onResult: (Uri?) -> Unit) {
        val bitmap = currentBitmap ?: return onResult(null)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val fileName = "edited_${System.currentTimeMillis()}.jpg"
                val outputFile = File(context.cacheDir, fileName)
                FileOutputStream(outputFile).use { outStream ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 95, outStream)
                }
                val uri = androidx.core.content.FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    outputFile
                )
                withContext(Dispatchers.Main) {
                    onResult(uri)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    onResult(null)
                }
            }
        }
    }

    fun saveToFile(context: Context, file: File): Boolean {
        return currentBitmap?.let { bitmap ->
            try {
                val outputStream = FileOutputStream(file)
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, outputStream)
                outputStream.close()
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        } ?: false
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageEditorScreen(
    imageUri: Uri,
    onSave: (Uri) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val viewModel: ImageEditorViewModel = viewModel()
    var showCropOverlay by remember { mutableStateOf(false) }
    var showSaveConfirmation by remember { mutableStateOf(false) }
    var normalizedCropRect by remember { mutableStateOf(RectF(0.1f, 0.1f, 0.9f, 0.9f)) }

    LaunchedEffect(imageUri) {
        viewModel.loadImage(imageUri, context)
    }

    val bitmap = viewModel.currentBitmap

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            TopAppBar(
                title = { Text("Edit Image", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        if (showCropOverlay) {
                            if (viewModel.applyCrop(normalizedCropRect)) {
                                showCropOverlay = false
                                // Reset for next potential crop
                                normalizedCropRect = RectF(0.1f, 0.1f, 0.9f, 0.9f)
                            }
                        } else {
                            viewModel.startCrop()
                            showCropOverlay = true
                        }
                    }) {
                        Icon(
                            imageVector = if (showCropOverlay) Icons.Default.Check else Icons.Default.Crop,
                            contentDescription = if (showCropOverlay) "Done Cropping" else "Crop",
                            tint = Color.White
                        )
                    }
                    IconButton(onClick = { viewModel.rotateRight() }) {
                        Icon(Icons.Default.RotateRight, contentDescription = "Rotate", tint = Color.White)
                    }
                    IconButton(onClick = {
                        if (showCropOverlay) {
                            viewModel.applyCrop(normalizedCropRect)
                            showCropOverlay = false
                        }
                        viewModel.saveImage(context) { uri ->
                            if (uri != null) {
                                onSave(uri)
                                showSaveConfirmation = true
                            }
                        }
                    }) {
                        Icon(Icons.Default.Save, contentDescription = "Save", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black.copy(alpha = 0.8f)
                )
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                bitmap?.let { bmp ->
                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // Calculate displayed image bounds to align overlay
                        val imageAspectRatio = bmp.width.toFloat() / bmp.height.toFloat()
                        val containerAspectRatio = maxWidth.value / maxHeight.value

                        val displayedWidth: Float
                        val displayedHeight: Float

                        if (imageAspectRatio > containerAspectRatio) {
                            displayedWidth = maxWidth.value
                            displayedHeight = displayedWidth / imageAspectRatio
                        } else {
                            displayedHeight = maxHeight.value
                            displayedWidth = displayedHeight * imageAspectRatio
                        }

                        Box(
                            modifier = Modifier.size(displayedWidth.dp, displayedHeight.dp)
                        ) {
                            Image(
                                bitmap = bmp.asComposeBitmap(),
                                contentDescription = "Editing image",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )

                            if (showCropOverlay) {
                                InteractiveCropOverlay(
                                    rect = normalizedCropRect,
                                    onRectChange = { normalizedCropRect = it }
                                )
                            }
                        }
                    }
                } ?: androidx.compose.material3.CircularProgressIndicator(color = Color.White)
            }

            if (showSaveConfirmation) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f))
                        .padding(16.dp)
                        .clickable { showSaveConfirmation = false },
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .padding(bottom = 100.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Box(
                            modifier = Modifier.padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Image saved!", color = Color.White, fontSize = 16.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InteractiveCropOverlay(
    rect: RectF,
    onRectChange: (RectF) -> Unit
) {
    val currentRect by rememberUpdatedState(rect)
    var activeHandle by remember { mutableStateOf<DragHandle?>(null) }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val width = constraints.maxWidth.toFloat()
        val height = constraints.maxHeight.toFloat()

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val touchX = offset.x / width
                            val touchY = offset.y / height
                            val threshold = 0.2f // Larger threshold for handles
                            
                            activeHandle = when {
                                // Top Left
                                touchX < currentRect.left + threshold && touchY < currentRect.top + threshold -> DragHandle.TOP_LEFT
                                // Top Right
                                touchX > currentRect.right - threshold && touchY < currentRect.top + threshold -> DragHandle.TOP_RIGHT
                                // Bottom Left
                                touchX < currentRect.left + threshold && touchY > currentRect.bottom - threshold -> DragHandle.BOTTOM_LEFT
                                // Bottom Right
                                touchX > currentRect.right - threshold && touchY > currentRect.bottom - threshold -> DragHandle.BOTTOM_RIGHT
                                // Center
                                touchX > currentRect.left && touchX < currentRect.right && touchY > currentRect.top && touchY < currentRect.bottom -> DragHandle.CENTER
                                else -> null
                            }
                        },
                        onDragEnd = { activeHandle = null },
                        onDragCancel = { activeHandle = null },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            val handle = activeHandle ?: return@detectDragGestures
                            
                            val deltaX = dragAmount.x / width
                            val deltaY = dragAmount.y / height
                            
                            val newRect = RectF(currentRect)
                            
                            when (handle) {
                                DragHandle.TOP_LEFT -> {
                                    newRect.left = (newRect.left + deltaX).coerceIn(0f, currentRect.right - 0.1f)
                                    newRect.top = (newRect.top + deltaY).coerceIn(0f, currentRect.bottom - 0.1f)
                                }
                                DragHandle.TOP_RIGHT -> {
                                    newRect.right = (newRect.right + deltaX).coerceIn(currentRect.left + 0.1f, 1f)
                                    newRect.top = (newRect.top + deltaY).coerceIn(0f, currentRect.bottom - 0.1f)
                                }
                                DragHandle.BOTTOM_LEFT -> {
                                    newRect.left = (newRect.left + deltaX).coerceIn(0f, currentRect.right - 0.1f)
                                    newRect.bottom = (newRect.bottom + deltaY).coerceIn(currentRect.top + 0.1f, 1f)
                                }
                                DragHandle.BOTTOM_RIGHT -> {
                                    newRect.right = (newRect.right + deltaX).coerceIn(currentRect.left + 0.1f, 1f)
                                    newRect.bottom = (newRect.bottom + deltaY).coerceIn(currentRect.top + 0.1f, 1f)
                                }
                                DragHandle.CENTER -> {
                                    val rectWidth = currentRect.width()
                                    val rectHeight = currentRect.height()
                                    newRect.left = (newRect.left + deltaX).coerceIn(0f, 1f - rectWidth)
                                    newRect.top = (newRect.top + deltaY).coerceIn(0f, 1f - rectHeight)
                                    newRect.right = newRect.left + rectWidth
                                    newRect.bottom = newRect.top + rectHeight
                                }
                            }
                            onRectChange(newRect)
                        }
                    )
                }
        ) {
            val rectLeft = currentRect.left * size.width
            val rectTop = currentRect.top * size.height
            val rectRight = currentRect.right * size.width
            val rectBottom = currentRect.bottom * size.height

            // Background overlay
            drawIntoCanvas { canvas ->
                val paint = Paint().apply {
                    color = Color.Black.copy(alpha = 0.6f)
                }
                
                // Draw 4 rectangles around the clear center
                canvas.drawRect(Rect(0f, 0f, size.width, rectTop), paint)
                canvas.drawRect(Rect(0f, rectBottom, size.width, size.height), paint)
                canvas.drawRect(Rect(0f, rectTop, rectLeft, rectBottom), paint)
                canvas.drawRect(Rect(rectRight, rectTop, size.width, rectBottom), paint)
            }

            // Crop border
            drawRect(
                color = Color.White,
                topLeft = Offset(rectLeft, rectTop),
                size = Size(rectRight - rectLeft, rectBottom - rectTop),
                style = Stroke(width = 2.dp.toPx())
            )

            // Corners
            val handleSize = 24.dp.toPx() // Larger visual handles
            val handleThickness = 4.dp.toPx()
            val handleColor = Color.White
            
            // Top Left
            drawRect(handleColor, Offset(rectLeft - 2.dp.toPx(), rectTop - 2.dp.toPx()), Size(handleSize, handleThickness))
            drawRect(handleColor, Offset(rectLeft - 2.dp.toPx(), rectTop - 2.dp.toPx()), Size(handleThickness, handleSize))
            
            // Top Right
            drawRect(handleColor, Offset(rectRight - handleSize + 2.dp.toPx(), rectTop - 2.dp.toPx()), Size(handleSize, handleThickness))
            drawRect(handleColor, Offset(rectRight - 2.dp.toPx(), rectTop - 2.dp.toPx()), Size(handleThickness, handleSize))
            
            // Bottom Left
            drawRect(handleColor, Offset(rectLeft - 2.dp.toPx(), rectBottom - handleThickness + 2.dp.toPx()), Size(handleSize, handleThickness))
            drawRect(handleColor, Offset(rectLeft - 2.dp.toPx(), rectBottom - handleSize + 2.dp.toPx()), Size(handleThickness, handleSize))
            
            // Bottom Right
            drawRect(handleColor, Offset(rectRight - handleSize + 2.dp.toPx(), rectBottom - handleThickness + 2.dp.toPx()), Size(handleSize, handleThickness))
            drawRect(handleColor, Offset(rectRight - 2.dp.toPx(), rectBottom - handleSize + 2.dp.toPx()), Size(handleThickness, handleSize))
        }
    }
}

private enum class DragHandle {
    TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT, CENTER
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageEditorBottomSheet(
    imageUri: Uri,
    onSave: (Uri) -> Unit,
    onDismiss: () -> Unit
) {
    // For now we'll use full screen editor
    ImageEditorScreen(imageUri, onSave, onDismiss)
}