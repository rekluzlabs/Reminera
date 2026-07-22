package com.rekluzlabs.reminera.ui.detail

import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.net.Uri
import android.widget.VideoView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.rekluzlabs.reminera.data.MemoryEntryEntity
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.TimeUnit

@Composable
fun FullScreenMediaViewer(
    entry: MemoryEntryEntity,
    onBack: () -> Unit
) {
    val file = remember(entry.localFilePath) { File(entry.localFilePath) }
    if (!file.exists()) {
        onBack()
        return
    }

    val uri = Uri.fromFile(file)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        when (entry.type) {
            "PHOTO" -> FullScreenPhoto(uri, entry)
            "VIDEO" -> FullScreenVideo(uri)
            "AUDIO" -> FullScreenAudio(uri, entry.title)
        }

        IconButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp)
                .statusBarsPadding()
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = Color.White
            )
        }
    }
}

@Composable
private fun FullScreenPhoto(uri: Uri, entry: MemoryEntryEntity) {
    val context = LocalContext.current
    val bitmap = remember(uri) {
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                BitmapFactory.decodeStream(input)
            }
        } catch (_: Exception) {
            null
        }
    }

    val secondaryPath = entry.secondaryMediaPath
    val secondaryType = entry.secondaryMediaType
    var showingSecondaryVideo by remember(entry.id) { mutableStateOf(false) }

    if (showingSecondaryVideo && secondaryType == "VIDEO" && !secondaryPath.isNullOrBlank()) {
        val secondaryFile = remember(secondaryPath) { File(secondaryPath) }
        if (secondaryFile.exists()) {
            FullScreenVideo(Uri.fromFile(secondaryFile))
            return
        } else {
            showingSecondaryVideo = false
        }
    }

    val hasVideoSecondary = secondaryType == "VIDEO" && !secondaryPath.isNullOrBlank()
    val hasAudioSecondary = secondaryType == "AUDIO" && !secondaryPath.isNullOrBlank()

    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    Box(modifier = Modifier.fillMaxSize()) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Full screen photo",
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offsetX,
                        translationY = offsetY
                    )
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onDoubleTap = {
                                if (scale > 1.5f) {
                                    scale = 1f
                                    offsetX = 0f
                                    offsetY = 0f
                                } else {
                                    scale = 2.5f
                                }
                            }
                        )
                    }
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(1f, 5f)
                            if (scale > 1f) {
                                offsetX += pan.x
                                offsetY += pan.y
                            } else {
                                offsetX = 0f
                                offsetY = 0f
                            }
                        }
                    },
                contentScale = ContentScale.Fit
            )
        }

        if (hasVideoSecondary) {
            if (!showingSecondaryVideo) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(enabled = scale <= 1f) { showingSecondaryVideo = true },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.25f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Play video",
                            tint = Color.White,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }
            }
        }

        if (hasAudioSecondary && scale <= 1f) {
            FullScreenPhotoAudioChip(secondaryPath!!)
        }
    }
}

@Composable
private fun FullScreenPhotoAudioChip(secondaryAudioPath: String) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isPlaying by remember(secondaryAudioPath) { mutableStateOf(false) }
    var isRepeat by remember(secondaryAudioPath) { mutableStateOf(false) }
    var mediaPlayer by remember(secondaryAudioPath) { mutableStateOf<MediaPlayer?>(null) }
    var currentPosition by remember(secondaryAudioPath) { mutableFloatStateOf(0f) }
    var duration by remember(secondaryAudioPath) { mutableFloatStateOf(1f) }

    DisposableEffect(secondaryAudioPath) {
        onDispose {
            mediaPlayer?.release()
            mediaPlayer = null
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 32.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        ) {
            LinearProgressIndicator(
                progress = { currentPosition },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = Color.White,
                trackColor = Color.White.copy(alpha = 0.3f),
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "${formatTime((currentPosition * duration).toLong())} / ${formatTime(duration.toLong())}",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { isRepeat = !isRepeat }) {
                    Icon(
                        imageVector = Icons.Default.Repeat,
                        contentDescription = "Repeat",
                        tint = if (isRepeat) Color(0xFF00BCD4) else Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(24.dp)
                    )
                }

                IconButton(
                    onClick = {
                        mediaPlayer?.let { mp ->
                            val newPos = maxOf(0, mp.currentPosition - 10000)
                            mp.seekTo(newPos)
                            currentPosition = newPos.toFloat() / mp.duration.toFloat()
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.FastRewind,
                        contentDescription = "Rewind 10s",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f))
                ) {
                    IconButton(
                        onClick = {
                            val file = File(secondaryAudioPath)
                            if (!file.exists()) return@IconButton
                            val player = mediaPlayer
                            if (player == null) {
                                val newPlayer = MediaPlayer().apply {
                                    setDataSource(context, Uri.fromFile(file))
                                    setOnPreparedListener {
                                        start()
                                        isPlaying = true
                                        duration = it.duration.toFloat()
                                        scope.launch {
                                            while (isActive && isPlaying) {
                                                currentPosition = currentPosition.coerceAtMost(1f)
                                                delay(250)
                                            }
                                        }
                                    }
                                    setOnCompletionListener {
                                        isPlaying = false
                                        if (isRepeat) {
                                            seekTo(0)
                                            start()
                                            isPlaying = true
                                        } else {
                                            currentPosition = 0f
                                        }
                                    }
                                    prepareAsync()
                                }
                                mediaPlayer = newPlayer
                            } else if (player.isPlaying) {
                                player.pause()
                                isPlaying = false
                            } else {
                                player.start()
                                isPlaying = true
                                scope.launch {
                                    while (isActive && isPlaying) {
                                        currentPosition = player.currentPosition.toFloat() / player.duration.toFloat()
                                        delay(250)
                                    }
                                }
                            }
                        },
                        modifier = Modifier.size(56.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                            Text(
                                text = if (isPlaying) "PAUSE" else "PLAY",
                                color = Color.White,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                IconButton(
                    onClick = {
                        mediaPlayer?.let { mp ->
                            val newPos = minOf(mp.duration, mp.currentPosition + 10000)
                            mp.seekTo(newPos)
                            currentPosition = newPos.toFloat() / mp.duration.toFloat()
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.FastForward,
                        contentDescription = "Forward 10s",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }

                IconButton(
                    onClick = {
                        mediaPlayer?.let { mp ->
                            mp.seekTo(0)
                            mp.pause()
                            isPlaying = false
                            currentPosition = 0f
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = "Stop",
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun FullScreenVideo(uri: Uri) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isPlaying by remember(uri) { mutableStateOf(false) }
    var isRepeat by remember(uri) { mutableStateOf(false) }
    var isPrepared by remember(uri) { mutableStateOf(false) }
    var hasStarted by remember(uri) { mutableStateOf(false) }
    var videoView by remember(uri) { mutableStateOf<VideoView?>(null) }
    var mediaPlayer by remember(uri) { mutableStateOf<MediaPlayer?>(null) }
    var currentPosition by remember(uri) { mutableFloatStateOf(0f) }
    var duration by remember(uri) { mutableFloatStateOf(1f) }

    AndroidView(
        factory = { ctx ->
            VideoView(ctx).apply {
                setVideoURI(uri)
                setOnPreparedListener { mp ->
                    isPrepared = true
                    duration = mp.duration.toFloat()
                    mediaPlayer = mp
                }
                setOnCompletionListener {
                    isPlaying = false
                    if (isRepeat) {
                        seekTo(0)
                        start()
                        isPlaying = true
                    } else {
                        hasStarted = false
                        currentPosition = 0f
                    }
                }
                videoView = this
            }
        },
        modifier = Modifier.fillMaxSize()
    )

    val placeholderBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.72f)
                .align(Alignment.TopCenter)
                .padding(top = 72.dp)
                .padding(horizontal = 24.dp)
        ) {
            if (!hasStarted) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .border(2.dp, placeholderBorderColor, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayCircle,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.6f),
                                modifier = Modifier.size(40.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Tap Play to start video",
                            color = Color.White.copy(alpha = 0.4f),
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LinearProgressIndicator(
                progress = { currentPosition },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = Color.White,
                trackColor = Color.White.copy(alpha = 0.3f),
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "${formatTime((currentPosition * duration).toLong())} / ${formatTime(duration.toLong())}",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    isRepeat = !isRepeat
                    mediaPlayer?.isLooping = isRepeat
                }) {
                    Icon(
                        imageVector = Icons.Default.Repeat,
                        contentDescription = "Repeat",
                        tint = if (isRepeat) Color(0xFF00BCD4) else Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(24.dp)
                    )
                }

                IconButton(
                    onClick = {
                        videoView?.let { vv ->
                            if (!isPrepared) return@let
                            val newPos = maxOf(0, vv.currentPosition - 10000)
                            vv.seekTo(newPos)
                            currentPosition = newPos.toFloat() / vv.duration.toFloat()
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.FastRewind,
                        contentDescription = "Rewind 10s",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f))
                ) {
                    IconButton(
                        onClick = {
                            videoView?.let { vv ->
                                if (!isPrepared) return@let
                                if (vv.isPlaying) {
                                    vv.pause()
                                    isPlaying = false
                                } else {
                                    vv.start()
                                    isPlaying = true
                                    hasStarted = true
                                    scope.launch {
                                        while (isActive && isPlaying) {
                                            currentPosition = vv.currentPosition.toFloat() / vv.duration.toFloat()
                                            delay(250)
                                        }
                                    }
                                }
                            }
                        },
                        modifier = Modifier.size(56.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                            Text(
                                text = if (isPlaying) "PAUSE" else "PLAY",
                                color = Color.White,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                IconButton(
                    onClick = {
                        videoView?.let { vv ->
                            if (!isPrepared) return@let
                            val newPos = minOf(vv.duration, vv.currentPosition + 10000)
                            vv.seekTo(newPos)
                            currentPosition = newPos.toFloat() / vv.duration.toFloat()
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.FastForward,
                        contentDescription = "Forward 10s",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }

                IconButton(
                    onClick = {
                        videoView?.let { vv ->
                            if (!isPrepared) return@let
                            vv.seekTo(0)
                            vv.pause()
                            isPlaying = false
                            hasStarted = false
                            currentPosition = 0f
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = "Stop",
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun FullScreenAudio(uri: Uri, title: String) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isPlaying by remember { mutableStateOf(false) }
    var isRepeat by remember { mutableStateOf(false) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var currentPosition by remember { mutableFloatStateOf(0f) }
    var duration by remember { mutableFloatStateOf(1f) }

    DisposableEffect(uri) {
        val player = MediaPlayer().apply {
            setDataSource(context, uri)
            setOnPreparedListener {
                mediaPlayer = this
                duration = it.duration.toFloat()
            }
            setOnCompletionListener {
                isPlaying = false
                if (isRepeat) {
                    seekTo(0)
                    start()
                    isPlaying = true
                } else {
                    currentPosition = 0f
                }
            }
            prepareAsync()
        }

        onDispose {
            player.release()
            mediaPlayer = null
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.Audiotrack,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(72.dp)
            )

            Spacer(modifier = Modifier.size(16.dp))

            Text(
                text = title,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            LinearProgressIndicator(
                progress = { currentPosition },
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = Color.White,
                trackColor = Color.White.copy(alpha = 0.3f),
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "${formatTime((currentPosition * duration).toLong())} / ${formatTime(duration.toLong())}",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 13.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { isRepeat = !isRepeat }) {
                    Icon(
                        imageVector = Icons.Default.Repeat,
                        contentDescription = "Repeat",
                        tint = if (isRepeat) Color(0xFF00BCD4) else Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(28.dp)
                    )
                }

                IconButton(
                    onClick = {
                        mediaPlayer?.let { mp ->
                            val newPos = maxOf(0, mp.currentPosition - 10000)
                            mp.seekTo(newPos)
                            currentPosition = newPos.toFloat() / mp.duration.toFloat()
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.FastRewind,
                        contentDescription = "Rewind 10s",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f))
                ) {
                    IconButton(
                        onClick = {
                            mediaPlayer?.let { mp ->
                                if (mp.isPlaying) {
                                    mp.pause()
                                    isPlaying = false
                                } else {
                                    mp.start()
                                    isPlaying = true
                                    scope.launch {
                                        while (isActive && isPlaying) {
                                            currentPosition = mp.currentPosition.toFloat() / mp.duration.toFloat()
                                            delay(250)
                                        }
                                    }
                                }
                            }
                        },
                        modifier = Modifier.size(64.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                            Text(
                                text = if (isPlaying) "PAUSE" else "PLAY",
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                IconButton(
                    onClick = {
                        mediaPlayer?.let { mp ->
                            val newPos = minOf(mp.duration, mp.currentPosition + 10000)
                            mp.seekTo(newPos)
                            currentPosition = newPos.toFloat() / mp.duration.toFloat()
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.FastForward,
                        contentDescription = "Forward 10s",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }

                IconButton(
                    onClick = {
                        mediaPlayer?.let { mp ->
                            mp.seekTo(0)
                            mp.pause()
                            isPlaying = false
                            currentPosition = 0f
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = "Stop",
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}

private fun formatTime(millis: Long): String {
    val minutes = TimeUnit.MILLISECONDS.toMinutes(millis)
    val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) - TimeUnit.MINUTES.toSeconds(minutes)
    return String.format("%d:%02d", minutes, seconds)
}
