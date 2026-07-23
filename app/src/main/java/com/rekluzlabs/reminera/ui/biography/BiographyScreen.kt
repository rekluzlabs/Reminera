package com.rekluzlabs.reminera.ui.biography

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rekluzlabs.reminera.data.StoryEntryEntity
import com.rekluzlabs.reminera.ui.editor.ImageEditorScreen
import com.rekluzlabs.reminera.ui.home.RemineraViewModel
import com.rekluzlabs.reminera.util.AudioRecorder
import com.rekluzlabs.reminera.util.MediaSaver
import com.rekluzlabs.reminera.util.copyUriToInternal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BiographyScreen(
    personId: Long,
    memberName: String,
    viewModel: BiographyViewModel,
    remineraViewModel: RemineraViewModel? = null,
    onBack: () -> Unit,
    onSettingsClick: () -> Unit = {},
    onNavigateToStory: (biographyId: String) -> Unit = {},
    onAddMemory: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val biography = uiState.biography
    var editingSection by remember { mutableStateOf<BiographySectionUiState?>(null) }
    var showFabOptions by remember { mutableStateOf(false) }
    var fullScreenPhotoUri by remember { mutableStateOf<String?>(null) }
    var fullScreenEntryId by remember { mutableStateOf<String?>(null) }
    var showImageEditor by remember { mutableStateOf<Uri?>(null) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val mediaEntries = uiState.storyEntries.filter { it.type == "audio" || it.type == "video" || it.type == "photo" }

    var tempPhotoUri by remember { mutableStateOf<Uri?>(null) }
    val takePicture = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success -> if (success) tempPhotoUri?.let { uri ->
        scope.launch {
            val persistentUri = withContext(Dispatchers.IO) {
                try { copyUriToInternal(context, uri, "jpg") } catch (_: Exception) { uri.toString() }
            }
            remineraViewModel?.updateMemberPhoto(personId, persistentUri)
            viewModel.updatePhotoUri(persistentUri)
        }
    }}

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val persistentUri = withContext(Dispatchers.IO) {
                    try { copyUriToInternal(context, uri, "jpg") } catch (_: Exception) { uri.toString() }
                }
                remineraViewModel?.updateMemberPhoto(personId, persistentUri)
                viewModel.updatePhotoUri(persistentUri)
            }
        }
    }

    val mediaPhotoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val persistentUri = withContext(Dispatchers.IO) {
                    try {
                        copyUriToInternal(context, uri, "jpg")
                    } catch (_: Exception) { uri.toString() }
                }
                viewModel.addStoryEntry("You", "photo", persistentUri, null, System.currentTimeMillis())
            }
        }
    }

    val mediaVideoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val persistentUri = withContext(Dispatchers.IO) {
                    try {
                        copyUriToInternal(context, uri, "mp4")
                    } catch (_: Exception) { uri.toString() }
                }
                viewModel.addStoryEntry("You", "video", persistentUri, null, System.currentTimeMillis())
            }
        }
    }

    var tempVideoCaptureUri by remember { mutableStateOf<Uri?>(null) }
    val captureVideoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CaptureVideo()
    ) { success ->
        if (success && tempVideoCaptureUri != null) {
            scope.launch {
                val persistentUri = withContext(Dispatchers.IO) {
                    try {
                        copyUriToInternal(context, tempVideoCaptureUri!!, "mp4")
                    } catch (_: Exception) { tempVideoCaptureUri.toString() }
                }
                viewModel.addStoryEntry("You", "video", persistentUri, null, System.currentTimeMillis())
            }
        }
    }

    val mediaAudioPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val persistentUri = withContext(Dispatchers.IO) {
                    try {
                        copyUriToInternal(context, uri, "m4a")
                    } catch (_: Exception) { uri.toString() }
                }
                viewModel.addStoryEntry("You", "audio", persistentUri, null, System.currentTimeMillis())
            }
        }
    }

    var tempPhotoCaptureUri by remember { mutableStateOf<Uri?>(null) }
    val capturePhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && tempPhotoCaptureUri != null) {
            scope.launch {
                val persistentUri = withContext(Dispatchers.IO) {
                    try {
                        copyUriToInternal(context, tempPhotoCaptureUri!!, "jpg")
                    } catch (_: Exception) { tempPhotoCaptureUri.toString() }
                }
                remineraViewModel?.updateMemberPhoto(personId, persistentUri)
                viewModel.updatePhotoUri(persistentUri)
            }
        }
    }

    var pendingCameraAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            pendingCameraAction?.invoke()
        } else {
            android.widget.Toast.makeText(
                context,
                "Camera permission is needed to take photos",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
        pendingCameraAction = null
    }
    val startCameraIfPermitted: (() -> Unit) -> Unit = { action ->
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        if (hasPermission) {
            action()
        } else {
            pendingCameraAction = action
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    var isRecordingAudio by remember { mutableStateOf(false) }
    var recordingDuration by remember { mutableLongStateOf(0L) }

    var storyEntryToDelete by remember { mutableStateOf<StoryEntryEntity?>(null) }
    var storyEntryToDownload by remember { mutableStateOf<StoryEntryEntity?>(null) }

    val audioRecorder = remember { AudioRecorder(context) }
    var pendingRecordAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    val recordAudioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            pendingRecordAction?.invoke()
        } else {
            android.widget.Toast.makeText(
                context,
                "Microphone permission is needed to record audio",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
        pendingRecordAction = null
    }
    val startRecordingIfPermitted: (() -> Unit) -> Unit = { action ->
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (hasPermission) {
            action()
        } else {
            pendingRecordAction = action
            recordAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    DisposableEffect(Unit) {
        onDispose { if (isRecordingAudio) audioRecorder.stop() }
    }

    var showMediaOptions by remember { mutableStateOf(false) }

    val bioPhoto = remember(biography?.photoUri) {
        biography?.photoUri?.let { uriStr ->
            try {
                com.rekluzlabs.reminera.util.ImageUtils.loadBitmapWithExifOrientation(context, uriStr)
            } catch (_: Exception) { null }
        }
    }

    val sectionIcons: Map<String, ImageVector> = mapOf(
        "origins" to Icons.Default.Person,
        "milestones" to Icons.Default.CalendarMonth,
        "personality" to Icons.Default.Person,
        "legacy" to Icons.Default.MenuBook
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Box(modifier = Modifier.size(52.dp)) {
                        if (bioPhoto != null) {
                            Image(
                                bitmap = bioPhoto.asImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .align(Alignment.CenterStart)
                                .clickable {
                                    biography?.photoUri?.let {
                                        fullScreenPhotoUri = it
                                        fullScreenEntryId = null
                                    }
                                },
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .size(32.dp)
                                    .padding(start = 4.dp)
                                    .align(Alignment.CenterStart)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = biography?.fullName ?: memberName,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (biography != null) {
                            Text(
                                text = biography.relationship,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
                Row {
                    IconButton(onClick = onSettingsClick) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Birth date
            if (biography?.birthDate != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()).apply { timeZone = TimeZone.getTimeZone("UTC") }.format(Date(biography.birthDate)),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Media gallery section
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Photos & Media",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Profile photo (large)
                    if (bioPhoto != null) {
                        Image(
                            bitmap = bioPhoto.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { biography?.photoUri?.let { fullScreenPhotoUri = it } },
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // Media entries gallery
                    if (mediaEntries.isNotEmpty()) {
                        Text(
                            text = "Additional media",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            mediaEntries.forEach { entry ->
                                val dismissState = rememberSwipeToDismissBoxState(
                                    confirmValueChange = { value ->
                                        when (value) {
                                            SwipeToDismissBoxValue.StartToEnd -> {
                                                storyEntryToDelete = entry
                                                false // Don't dismiss immediately
                                            }
                                            SwipeToDismissBoxValue.EndToStart -> {
                                                storyEntryToDownload = entry
                                                false // Don't dismiss immediately
                                            }
                                            else -> false
                                        }
                                    }
                                )

                                SwipeToDismissBox(
                                    state = dismissState,
                                    backgroundContent = {
                                        val color = when (dismissState.dismissDirection) {
                                            SwipeToDismissBoxValue.StartToEnd -> MaterialTheme.colorScheme.errorContainer
                                            SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.primaryContainer
                                            else -> Color.Transparent
                                        }
                                        val alignment = when (dismissState.dismissDirection) {
                                            SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                                            SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
                                            else -> Alignment.Center
                                        }
                                        val icon = when (dismissState.dismissDirection) {
                                            SwipeToDismissBoxValue.StartToEnd -> Icons.Default.Delete
                                            SwipeToDismissBoxValue.EndToStart -> Icons.Default.Download
                                            else -> null
                                        }

                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(color)
                                                .padding(horizontal = 16.dp),
                                            contentAlignment = alignment
                                        ) {
                                            icon?.let {
                                                Icon(
                                                    imageVector = it,
                                                    contentDescription = null,
                                                    tint = if (dismissState.dismissDirection == SwipeToDismissBoxValue.StartToEnd)
                                                        MaterialTheme.colorScheme.error
                                                    else
                                                        MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    }
                                ) {
                                    MediaEntryRow(
                                        entry = entry,
                                        context = context,
                                        onMediaClick = { mediaEntry ->
                                            when (mediaEntry.type) {
                                                "photo" -> mediaEntry.mediaUri?.let {
                                                    fullScreenPhotoUri = it
                                                    fullScreenEntryId = mediaEntry.id
                                                }
                                                "video" -> {
                                                    mediaEntry.mediaUri?.let { uri ->
                                                        val contentUri = try {
                                                            val file = File(uri)
                                                            if (file.exists()) {
                                                                FileProvider.getUriForFile(context, "com.rekluzlabs.reminera.fileprovider", file)
                                                            } else {
                                                                Uri.parse(uri)
                                                            }
                                                        } catch (_: Exception) {
                                                            Uri.parse(uri)
                                                        }
                                                        val intent = Intent(Intent.ACTION_VIEW).apply {
                                                            setDataAndType(contentUri, "video/*")
                                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                        }
                                                        context.startActivity(intent)
                                                    }
                                                }
                                                "audio" -> {
                                                    mediaEntry.mediaUri?.let { uri ->
                                                        val contentUri = try {
                                                            val file = File(uri)
                                                            if (file.exists()) {
                                                                FileProvider.getUriForFile(context, "com.rekluzlabs.reminera.fileprovider", file)
                                                            } else {
                                                                Uri.parse(uri)
                                                            }
                                                        } catch (_: Exception) {
                                                            Uri.parse(uri)
                                                        }
                                                        val intent = Intent(Intent.ACTION_VIEW).apply {
                                                            setDataAndType(contentUri, "audio/*")
                                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                        }
                                                        context.startActivity(intent)
                                                    }
                                                }
                                            }
                                        }
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // Add media buttons
                    Text(
                        text = "Import media",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { mediaPhotoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Photo", fontSize = 12.sp)
                        }
                        OutlinedButton(
                            onClick = { mediaVideoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)) },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Videocam, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Video", fontSize = 12.sp)
                        }
                        OutlinedButton(
                            onClick = { mediaAudioPicker.launch(arrayOf("audio/*")) },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Audiotrack, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Audio", fontSize = 12.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Record media",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                val tempFile = File(context.cacheDir, "temp_video_${System.currentTimeMillis()}.mp4")
                                val uri = FileProvider.getUriForFile(context, "com.rekluzlabs.reminera.fileprovider", tempFile)
                                tempVideoCaptureUri = uri
                                captureVideoLauncher.launch(uri)
                            },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Videocam, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Record Video", fontSize = 12.sp)
                        }
                        OutlinedButton(
                            onClick = {
                                if (isRecordingAudio) {
                                    audioRecorder.stop()
                                    isRecordingAudio = false
                                } else {
                                    startRecordingIfPermitted {
                                        val tempFile = File(context.cacheDir, "temp_audio_${System.currentTimeMillis()}.m4a")
                                        audioRecorder.start(tempFile)
                                        isRecordingAudio = true
                                        scope.launch {
                                            val startTime = System.currentTimeMillis()
                                            while (isRecordingAudio && isActive) {
                                                recordingDuration = System.currentTimeMillis() - startTime
                                                delay(100)
                                            }
                                            if (tempFile.exists()) {
                                                val persistentUri = withContext(Dispatchers.IO) {
                                                    copyUriToInternal(context, Uri.fromFile(tempFile), "m4a")
                                                }
                                                viewModel.addStoryEntry("You", "audio", persistentUri, null, System.currentTimeMillis())
                                            }
                                        }
                                    }
                                }
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = if (isRecordingAudio) ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            ) else ButtonDefaults.outlinedButtonColors(),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = if (isRecordingAudio) Icons.Default.Pause else Icons.Default.Audiotrack,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                if (isRecordingAudio) "Stop (${recordingDuration / 1000}s)" else "Record Audio",
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Section cards
            uiState.sections.forEach { section ->
                SectionCard(
                    icon = sectionIcons[section.type] ?: Icons.Default.Edit,
                    label = section.label,
                    isPopulated = section.isPopulated,
                    fields = section.fields,
                    onClick = { editingSection = section }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Story entries card
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clickable {
                        biography?.let { onNavigateToStory(it.id) }
                    }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.MenuBook,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Their Story",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp
                        )
                        if (uiState.storyEntries.isNotEmpty()) {
                            Text(
                                text = "${uiState.storyEntries.size} entr${if (uiState.storyEntries.size == 1) "y" else "ies"}",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 13.sp
                            )
                        } else {
                            Text(
                                text = "No entries yet",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 13.sp
                            )
                        }
                    }
                    Text(">", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 18.sp)
                }
            }

            Spacer(modifier = Modifier.height(80.dp))
        }

        FloatingActionButton(
            onClick = { showFabOptions = true },
            shape = RoundedCornerShape(16.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.background,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .navigationBarsPadding()
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add")
        }


        IconButton(
            onClick = {
                startCameraIfPermitted {
                    val tempFile = File(context.cacheDir, "temp_photo_${System.currentTimeMillis()}.jpg")
                    val uri = FileProvider.getUriForFile(context, "com.rekluzlabs.reminera.fileprovider", tempFile)
                    tempPhotoCaptureUri = uri
                    capturePhotoLauncher.launch(uri)
                }
            },
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(20.dp)
                .navigationBarsPadding()
                .size(48.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
        ) {
            Icon(
                imageVector = Icons.Default.CameraAlt,
                contentDescription = "Change photo",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp)
            )
        }

        fullScreenPhotoUri?.let { fullUri ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .clickable {
                        fullScreenPhotoUri = null
                        fullScreenEntryId = null
                    }
            ) {
                val fullBitmap = remember(fullUri) {
                    try {
                        com.rekluzlabs.reminera.util.ImageUtils.loadBitmapWithExifOrientation(context, fullUri)
                    } catch (_: Exception) { null }
                }
                var rotationAngle by remember { mutableStateOf(0f) }
                var scale by remember { mutableStateOf(1f) }
                var offsetX by remember { mutableStateOf(0f) }
                var offsetY by remember { mutableStateOf(0f) }

                if (fullBitmap != null) {
                    Image(
                        bitmap = fullBitmap.asImageBitmap(),
                        contentDescription = "Full screen photo",
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                            .graphicsLayer(
                                scaleX = scale,
                                scaleY = scale,
                                translationX = offsetX,
                                translationY = offsetY,
                                rotationZ = rotationAngle
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
                IconButton(
                    onClick = {
                        fullScreenPhotoUri = null
                        fullScreenEntryId = null
                    },
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White
                    )
                }

                IconButton(
                    onClick = {
                        fullScreenPhotoUri?.let { uriStr ->
                            showImageEditor = Uri.parse(uriStr)
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Crop,
                        contentDescription = "Crop",
                        tint = Color.White
                    )
                }

                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 32.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color.White.copy(alpha = 0.2f))
                        .clickable { rotationAngle = (rotationAngle + 90f) % 360f }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Rotate Image",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }

    editingSection?.let { section ->
        SectionEditSheet(
            sectionType = section.type,
            sectionLabel = section.label,
            initialFields = section.fields,
            onSave = { fields ->
                viewModel.saveSection(section.type, fields)
                editingSection = null
            },
            onDismiss = { editingSection = null }
        )
    }

    storyEntryToDelete?.let { entry ->
        AlertDialog(
            onDismissRequest = { storyEntryToDelete = null },
            icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Delete Media?") },
            text = { Text("Are you sure you want to delete this ${entry.type}? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteStoryEntry(entry.id)
                        storyEntryToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { storyEntryToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    storyEntryToDownload?.let { entry ->
        AlertDialog(
            onDismissRequest = { storyEntryToDownload = null },
            icon = { Icon(Icons.Default.Download, contentDescription = null) },
            title = { Text("Download Media?") },
            text = { Text("Would you like to save this ${entry.type} to your device gallery?") },
            confirmButton = {
                Button(
                    onClick = {
                        MediaSaver.saveStoryEntryToDevice(context, entry)
                        storyEntryToDownload = null
                    }
                ) {
                    Text("Download")
                }
            },
            dismissButton = {
                TextButton(onClick = { storyEntryToDownload = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    showImageEditor?.let { uri ->
        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            ImageEditorScreen(
                imageUri = uri,
                onSave = { editedUri ->
                    scope.launch {
                        val persistentUri = withContext(Dispatchers.IO) {
                            try {
                                copyUriToInternal(context, editedUri, "jpg")
                            } catch (_: Exception) { editedUri.toString() }
                        }
                        if (fullScreenEntryId != null) {
                            viewModel.updateStoryEntryMedia(fullScreenEntryId!!, persistentUri)
                        } else {
                            remineraViewModel?.updateMemberPhoto(personId, persistentUri)
                            viewModel.updatePhotoUri(persistentUri)
                        }
                        fullScreenPhotoUri = persistentUri
                        showImageEditor = null
                    }
                },
                onDismiss = { showImageEditor = null }
            )
        }
    }
}

@Composable
private fun MediaEntryRow(entry: StoryEntryEntity, context: android.content.Context, onMediaClick: (StoryEntryEntity) -> Unit) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onMediaClick(entry) }
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            when (entry.type) {
                "photo" -> {
                    val thumb = remember(entry.mediaUri) {
                        entry.mediaUri?.let { uriStr ->
                            try {
                                com.rekluzlabs.reminera.util.ImageUtils.loadBitmapWithExifOrientation(context, uriStr)
                            } catch (_: Exception) { null }
                        }
                    }
                    if (thumb != null) {
                        Image(
                            bitmap = thumb.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(Icons.Default.Image, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                    }
                }
                "video" -> {
                    Icon(Icons.Default.Videocam, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                }
                "audio" -> {
                    Icon(Icons.Default.Audiotrack, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.type.replaceFirstChar { it.uppercase() },
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(entry.recordedAt)),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun SectionCard(
    icon: ImageVector,
    label: String,
    isPopulated: Boolean,
    fields: Map<String, String>,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isPopulated)
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            else
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isPopulated) MaterialTheme.colorScheme.primary
                       else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )
                if (isPopulated && fields.isNotEmpty()) {
                    val preview = fields.values.firstOrNull { it.isNotBlank() }
                    if (preview != null) {
                        Text(
                            text = preview,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                } else {
                    Text(
                        text = "Tap to add details",
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        fontSize = 13.sp
                    )
                }
            }
            Text(">", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 18.sp)
        }
    }
}
