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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rekluzlabs.reminera.data.StoryEntryEntity
import com.rekluzlabs.reminera.export.ChapterPdfRenderer
import com.rekluzlabs.reminera.export.GeminiBiographyProvider
import com.rekluzlabs.reminera.ui.editor.ImageEditorScreen
import com.rekluzlabs.reminera.ui.home.MediaAction
import com.rekluzlabs.reminera.ui.home.MediaActionResult
import com.rekluzlabs.reminera.ui.home.MediaItemMenuSheet
import com.rekluzlabs.reminera.ui.home.MediaMenuState
import com.rekluzlabs.reminera.ui.home.RemineraViewModel
import com.rekluzlabs.reminera.util.AudioRecorder
import com.rekluzlabs.reminera.util.MediaSaver
import com.rekluzlabs.reminera.util.ThumbnailHelper
import com.rekluzlabs.reminera.util.PlaybackManager
import com.rekluzlabs.reminera.util.SecureApiKeyStore
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
    onNavigateToAiSettings: () -> Unit = {},
    onNavigateToStory: (biographyId: String) -> Unit = {},
    onAddMemory: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val biography = uiState.biography
    var editingSection by remember { mutableStateOf<BiographySectionUiState?>(null) }
    var showFabOptions by remember { mutableStateOf(false) }
    var fullScreenPhotoUri by remember { mutableStateOf<String?>(null) }
    var fullScreenEntryId by remember { mutableStateOf<String?>(null) }
    var fullScreenVideoUri by remember { mutableStateOf<String?>(null) }
    var fullScreenAudioUri by remember { mutableStateOf<String?>(null) }
    // Removed fullScreenYouTubeId as we now open YouTube directly via Intent

    var showImageEditor by remember { mutableStateOf<Uri?>(null) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val chapterExport by viewModel.chapterExport.collectAsState()
    val isAiPolishing by viewModel.isAiPolishing.collectAsState()
    val aiPolishResult by viewModel.aiPolishResult.collectAsState()
    var showAiConsentDialog by remember { mutableStateOf(false) }
    var showAiAccessDialog by remember { mutableStateOf(false) }
    var showApiKeyDialog by remember { mutableStateOf(false) }
    var showChapterTextPreview by remember { mutableStateOf(false) }
    var isExportingPdf by remember { mutableStateOf(false) }
    var isExportingText by remember { mutableStateOf(false) }

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
                viewModel.addStoryEntry("You", "photo", persistentUri, null, System.currentTimeMillis(), context)
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
                viewModel.addStoryEntry("You", "video", persistentUri, null, System.currentTimeMillis(), context)
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
                viewModel.addStoryEntry("You", "video", persistentUri, null, System.currentTimeMillis(), context)
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
                viewModel.addStoryEntry("You", "audio", persistentUri, null, System.currentTimeMillis(), context)
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
    var showRenameDialog by remember { mutableStateOf(false) }
    var renameEntryId by remember { mutableStateOf<String?>(null) }
    var renameCurrentTitle by remember { mutableStateOf("") }
    var mediaMenuState by remember { mutableStateOf<MediaMenuState?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(aiPolishResult) {
        when (val result = aiPolishResult) {
            is MediaActionResult.Success -> {
                snackbarHostState.showSnackbar("AI biography generated successfully.")
                viewModel.clearAiPolishResult()
            }
            is MediaActionResult.Error -> {
                snackbarHostState.showSnackbar(result.message)
                viewModel.clearAiPolishResult()
            }
            null -> {}
        }
    }

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
                                                "video" -> mediaEntry.mediaUri?.let { uri ->
                                                    val isUrl = uri.startsWith("http://") || uri.startsWith("https://")
                                                    if (isUrl) {
                                                        if (uri.contains("youtube.com") || uri.contains("youtu.be")) {
                                                            val videoId = ThumbnailHelper.extractYouTubeVideoId(uri)
                                                            if (videoId != null) {
                                                                val appIntent = Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube:$videoId"))
                                                                    .setPackage("com.google.android.youtube")

                                                                val webIntent = Intent(Intent.ACTION_VIEW,
                                                                    Uri.parse("https://www.youtube.com/watch?v=$videoId"))

                                                                try {
                                                                    context.startActivity(appIntent)
                                                                } catch (e: Exception) {
                                                                    context.startActivity(webIntent)
                                                                }
                                                            }
                                                        } else if (uri.contains("vimeo.com")) {
                                                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(uri)).apply {
                                                                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_DOCUMENT)
                                                                addFlags(android.content.Intent.FLAG_ACTIVITY_NO_HISTORY)
                                                            }
                                                            context.startActivity(intent)
                                                        } else {
                                                            fullScreenVideoUri = uri
                                                        }
                                                    } else {
                                                        val file = File(uri)
                                                        if (file.exists()) {
                                                            fullScreenVideoUri = uri
                                                        }
                                                    }
                                                }
                                                "audio" -> mediaEntry.mediaUri?.let { uri ->
                                                    val isUrl = uri.startsWith("http://") || uri.startsWith("https://")
                                                    if (isUrl) {
                                                        fullScreenAudioUri = uri
                                                    } else {
                                                        val file = File(uri)
                                                        if (file.exists()) {
                                                            fullScreenAudioUri = uri
                                                        }
                                                    }
                                                }
                                            }
                                        },
                                        onRename = { mediaEntry ->
                                            renameEntryId = mediaEntry.id
                                            renameCurrentTitle = mediaEntry.textContent ?: mediaEntry.type.replaceFirstChar { it.uppercase() }
                                            showRenameDialog = true
                                        },
                                        onMenuClick = { mediaEntry ->
                                            val memberName = biography?.fullName ?: memberName
                                            val entryUri = mediaEntry.mediaUri
                                            mediaMenuState = MediaMenuState(
                                                entryId = mediaEntry.id,
                                                entryTitle = mediaEntry.textContent ?: mediaEntry.type.replaceFirstChar { it.uppercase() },
                                                entryType = mediaEntry.type,
                                                currentMemberName = memberName,
                                                members = emptyList(),
                                                linkUrl = if (entryUri != null && (entryUri.startsWith("http://") || entryUri.startsWith("https://"))) entryUri else null
                                            )
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
                                                viewModel.addStoryEntry("You", "audio", persistentUri, null, System.currentTimeMillis(), context)
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

            // AI biography source indicator
            val hasChapterText = chapterExport?.generatedBioText?.isNotBlank() == true
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                if (hasChapterText) Modifier.clickable { showChapterTextPreview = true }
                                else Modifier
                            )
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Chapter Text",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            val source = chapterExport?.biographySource
                            Text(
                                text = when {
                                    hasChapterText && source == "AI_POLISHED" -> "AI-polished — tap to preview"
                                    hasChapterText -> "Raw — tap to preview"
                                    else -> "No text yet"
                                },
                                fontSize = 13.sp,
                                color = if (hasChapterText)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (isAiPolishing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            val keyStore = remember { SecureApiKeyStore(context) }
                            FilledTonalButton(
                                onClick = {
                                    if (!keyStore.isAiAccessEnabled()) {
                                        showAiAccessDialog = true
                                    } else if (!keyStore.hasApiKey() || !keyStore.isVerified()) {
                                        showApiKeyDialog = true
                                    } else if (!hasConsent(context)) {
                                        showAiConsentDialog = true
                                    } else {
                                        viewModel.requestAiPolish(
                                            GeminiBiographyProvider(keyStore.getApiKey()!!, keyStore.getSelectedModel())
                                        )
                                    }
                                },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = if (chapterExport?.biographySource == "AI_POLISHED")
                                        "Regenerate"
                                    else
                                        "Generate AI",
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                    if (hasChapterText) {
                        Spacer(modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            TextButton(
                                onClick = {
                                    val text = chapterExport?.generatedBioText ?: return@TextButton
                                    isExportingPdf = true
                                    scope.launch {
                                        try {
                                            val html = """<!DOCTYPE html>
<html><head><meta charset="utf-8"><style>
body { padding: 40px; font-family: Georgia, serif; line-height: 1.8; font-size: 12pt; color: #000; }
h1 { font-size: 22pt; margin-bottom: 20px; }
p { margin-bottom: 10px; }
</style></head><body>
<h1>${biography?.fullName?.replace("&", "&amp;")?.replace("<", "&lt;")?.replace(">", "&gt;") ?: memberName}</h1>
${text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\n", "<br>")}
</body></html>"""
                                            val result = withContext(Dispatchers.Main) {
                                                ChapterPdfRenderer.renderChapter(
                                                    context = context,
                                                    memberId = personId,
                                                    html = html,
                                                    chapterTitle = "Chapter: ${biography?.fullName ?: memberName}"
                                                )
                                            }
                                            if (result is ChapterPdfRenderer.RenderResult.Success) {
                                                val uri = FileProvider.getUriForFile(
                                                    context,
                                                    "com.rekluzlabs.reminera.fileprovider",
                                                    result.outputFile
                                                )
                                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                                    setDataAndType(uri, "application/pdf")
                                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                }
                                                context.startActivity(intent)
                                            } else {
                                                val msg = (result as? ChapterPdfRenderer.RenderResult.Failure)?.error ?: "PDF generation failed"
                                                snackbarHostState.showSnackbar(msg)
                                            }
                                        } catch (e: Exception) {
                                            snackbarHostState.showSnackbar("PDF export failed: ${e.message}")
                                        }
                                        isExportingPdf = false
                                    }
                                },
                                enabled = !isExportingPdf
                            ) {
                                if (isExportingPdf) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(14.dp),
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                }
                                Icon(
                                    imageVector = Icons.Default.PictureAsPdf,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Export PDF", fontSize = 12.sp)
                            }
                            TextButton(
                                onClick = {
                                    val text = chapterExport?.generatedBioText ?: return@TextButton
                                    isExportingText = true
                                    scope.launch {
                                        try {
                                            val file = withContext(Dispatchers.IO) {
                                                val outputDir = File(context.cacheDir, "chapter_exports")
                                                outputDir.mkdirs()
                                                val f = File(outputDir, "${biography?.fullName ?: memberName}_chapter.txt")
                                                f.writeText(text)
                                                f
                                            }
                                            val uri = FileProvider.getUriForFile(
                                                context,
                                                "com.rekluzlabs.reminera.fileprovider",
                                                file
                                            )
                                            val intent = Intent(Intent.ACTION_SEND).apply {
                                                setDataAndType(uri, "text/plain")
                                                putExtra(Intent.EXTRA_STREAM, uri)
                                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            }
                                            context.startActivity(Intent.createChooser(intent, "Share Chapter Text"))
                                        } catch (e: Exception) {
                                            snackbarHostState.showSnackbar("Text export failed: ${e.message}")
                                        }
                                        isExportingText = false
                                    }
                                },
                                enabled = !isExportingText
                            ) {
                                if (isExportingText) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(14.dp),
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                }
                                Icon(
                                    imageVector = Icons.Default.Description,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Export Text", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            if (showAiConsentDialog) {
                AiConsentDialog(
                    onAccept = {
                        showAiConsentDialog = false
                        val keyStore = SecureApiKeyStore(context)
                        viewModel.requestAiPolish(
                            GeminiBiographyProvider(keyStore.getApiKey()!!, keyStore.getSelectedModel())
                        )
                    },
                    onDismiss = { showAiConsentDialog = false }
                )
            }

            if (showAiAccessDialog) {
                AlertDialog(
                    onDismissRequest = { showAiAccessDialog = false },
                    title = {
                        Text(
                            text = "AI Biography Access",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    },
                    text = {
                        Text(
                            text = "AI biography generation is currently disabled. " +
                                "Enable it in Settings to use this feature.\n\n" +
                                "Your API key and data remain secure and are never used without your permission.",
                            fontSize = 14.sp,
                            lineHeight = 20.sp
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                showAiAccessDialog = false
                                onNavigateToAiSettings()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text("Go to Settings")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showAiAccessDialog = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }

            if (showApiKeyDialog) {
                var apiKeyInput by remember { mutableStateOf("") }
                var isVerifying by remember { mutableStateOf(false) }
                var verifyError by remember { mutableStateOf<String?>(null) }
                val scope = rememberCoroutineScope()

                AlertDialog(
                    onDismissRequest = { showApiKeyDialog = false },
                    title = {
                        Text(
                            text = "API Key Required",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    },
                    text = {
                        Column {
                            Text(
                                text = "AI biography generation requires a verified Gemini API key. " +
                                    "Enter your key below or go to Settings to configure it.",
                                fontSize = 14.sp,
                                lineHeight = 20.sp
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedTextField(
                                value = apiKeyInput,
                                onValueChange = {
                                    apiKeyInput = it
                                    verifyError = null
                                },
                                placeholder = { Text("Enter your API key...") },
                                visualTransformation = PasswordVisualTransformation(),
                                singleLine = true,
                                isError = verifyError != null,
                                supportingText = verifyError?.let { err ->
                                    { Text(err, color = MaterialTheme.colorScheme.error, fontSize = 12.sp) }
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    cursorColor = MaterialTheme.colorScheme.primary,
                                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                            if (isVerifying) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Verifying key...",
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (apiKeyInput.isNotBlank()) {
                                    isVerifying = true
                                    verifyError = null
                                    val ks = SecureApiKeyStore(context)
                                    scope.launch {
                                        val result = ks.verifyApiKey(apiKeyInput.trim())
                                        isVerifying = false
                                        if (result.isSuccess) {
                                            ks.saveApiKey(apiKeyInput.trim())
                                            ks.saveVerified(true)
                                            showApiKeyDialog = false
                                            if (!hasConsent(context)) {
                                                showAiConsentDialog = true
                                            } else {
                                                viewModel.requestAiPolish(
                                                    GeminiBiographyProvider(ks.getApiKey()!!, ks.getSelectedModel())
                                                )
                                            }
                                        } else {
                                            verifyError = result.exceptionOrNull()?.message
                                                ?: "Key verification failed. Please check your key and try again."
                                        }
                                    }
                                }
                            },
                            enabled = apiKeyInput.isNotBlank() && !isVerifying,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text("Save & Continue")
                        }
                    },
                    dismissButton = {
                        Row {
                            TextButton(onClick = {
                                showApiKeyDialog = false
                                onNavigateToAiSettings()
                            }) {
                                Text("Go to Settings")
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            TextButton(onClick = { showApiKeyDialog = false }) {
                                Text("Cancel")
                            }
                        }
                    }
                )
            }

            if (showChapterTextPreview) {
                val text = chapterExport?.generatedBioText ?: ""
                val source = chapterExport?.biographySource
                AlertDialog(
                    onDismissRequest = { showChapterTextPreview = false },
                    title = {
                        Text(
                            text = when (source) {
                                "AI_POLISHED" -> "Chapter Text (AI-polished)"
                                else -> "Chapter Text (Raw)"
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    },
                    text = {
                        Box(
                            modifier = Modifier
                                .heightIn(max = 400.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            Text(
                                text = text,
                                fontSize = 14.sp,
                                lineHeight = 22.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showChapterTextPreview = false }) {
                            Text("Close")
                        }
                    }
                )
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

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 88.dp)
                .navigationBarsPadding()
        )

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

    fullScreenVideoUri?.let { videoUri ->
        val videoFile = remember(videoUri) { File(videoUri) }
        if (videoFile.exists()) {
            BiographyFullScreenVideo(
                videoUri = videoUri,
                onClose = { fullScreenVideoUri = null }
            )
        } else {
            fullScreenVideoUri = null
        }
    }

    fullScreenAudioUri?.let { audioUri ->
        val audioFile = remember(audioUri) { File(audioUri) }
        if (audioFile.exists()) {
            BiographyFullScreenAudio(
                audioUri = audioUri,
                onClose = { fullScreenAudioUri = null }
            )
        } else {
            fullScreenAudioUri = null
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

    if (showFabOptions) {
        AddStoryEntryDialog(
            onDismiss = { showFabOptions = false },
            onSave = { type, textContent, mediaUri ->
                viewModel.addStoryEntry(
                    "You", type, mediaUri, textContent,
                    System.currentTimeMillis(), context
                )
                showFabOptions = false
            }
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

    val bioActionResult by viewModel.mediaActionResult.collectAsState()
    LaunchedEffect(bioActionResult) {
        bioActionResult?.let { result ->
            val message = when (result) {
                is com.rekluzlabs.reminera.ui.home.MediaActionResult.Success -> "Done"
                is com.rekluzlabs.reminera.ui.home.MediaActionResult.Error -> result.message
            }
            snackbarHostState.showSnackbar(message)
            viewModel.clearActionResult()
        }
    }

    if (showRenameDialog && renameEntryId != null) {
        var title by remember { mutableStateOf(renameCurrentTitle) }
        val isValid = title.isNotBlank()
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Rename") },
            text = {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        cursorColor = MaterialTheme.colorScheme.primary,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.handleMediaAction(MediaAction.Rename(renameEntryId!!, title.trim()), context)
                        showRenameDialog = false
                    },
                    enabled = isValid
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) { Text("Cancel") }
            }
        )
    }

    mediaMenuState?.let { menuState ->
        MediaItemMenuSheet(
            menuState = menuState,
            onDismiss = { mediaMenuState = null },
            onAction = { action ->
                viewModel.handleMediaAction(action, context)
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
private fun MediaEntryRow(
    entry: StoryEntryEntity,
    context: android.content.Context,
    onMediaClick: (StoryEntryEntity) -> Unit,
    onRename: (StoryEntryEntity) -> Unit = {},
    onMenuClick: (StoryEntryEntity) -> Unit = {}
) {
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
                    val isLinkUri = entry.mediaUri?.let { it.startsWith("http://") || it.startsWith("https://") } == true

                    val thumb = remember(entry.thumbnailUri, entry.mediaUri) {
                        entry.thumbnailUri?.let { path ->
                            try {
                                android.graphics.BitmapFactory.decodeFile(path)
                            } catch (_: Exception) { null }
                        } ?: if (!isLinkUri) entry.mediaUri?.let { uriStr ->
                            try {
                                val retriever = android.media.MediaMetadataRetriever()
                                val file = File(uriStr)
                                if (file.exists()) {
                                    retriever.setDataSource(uriStr)
                                } else {
                                    retriever.setDataSource(context, Uri.parse(uriStr))
                                }
                                val frame = retriever.frameAtTime
                                retriever.release()
                                frame
                            } catch (_: Exception) { null }
                        } else null
                    }

                    if (thumb != null) {
                        Box {
                            Image(
                                bitmap = thumb.asImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier
                                    .size(24.dp)
                                    .align(Alignment.Center)
                                    .background(Color.Black.copy(alpha = 0.3f), CircleShape)
                            )
                        }
                    } else if (isLinkUri) {
                        Icon(
                            imageVector = Icons.Default.Link,
                            contentDescription = "External link",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Videocam,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
                "audio" -> {
                    Icon(Icons.Default.Audiotrack, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = entry.textContent?.takeIf { it.isNotBlank() }
                            ?: entry.type.replaceFirstChar { it.uppercase() },
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    val mediaUri = entry.mediaUri
                    if (mediaUri != null && (mediaUri.startsWith("http://") || mediaUri.startsWith("https://"))) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.Link,
                            contentDescription = "External link",
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Rename",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier
                            .size(14.dp)
                            .clickable { onRename(entry) }
                    )
                }
                Text(
                    text = SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(entry.recordedAt)),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
            }
            IconButton(
                onClick = { onMenuClick(entry) },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Options",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
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

@Composable
private fun BiographyFullScreenVideo(
    videoUri: String,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val uri = remember(videoUri) {
        if (videoUri.startsWith("http://") || videoUri.startsWith("https://"))
            Uri.parse(videoUri)
        else
            Uri.fromFile(File(videoUri))
    }
    val key = remember(videoUri) { "bio_video_$videoUri" }
    val playbackManager = remember(videoUri) {
        PlaybackManager.getInstance(context, key)
    }

    val isPlaying by playbackManager.isPlaying.collectAsState()
    val currentPosition by playbackManager.currentPosition.collectAsState()
    val duration by playbackManager.duration.collectAsState()
    val isRepeat by playbackManager.isRepeat.collectAsState()
    val playerError by playbackManager.playerError.collectAsState()

    DisposableEffect(videoUri) {
        playbackManager.prepareAndPlay(uri)
        onDispose {
            PlaybackManager.release(key)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        androidx.compose.ui.viewinterop.AndroidView(
            factory = { ctx ->
                androidx.media3.ui.PlayerView(ctx).apply {
                    player = playbackManager.getOrCreatePlayer()
                    useController = false
                }
            },
            update = { playerView ->
                val player = playbackManager.getOrCreatePlayer()
                if (playerView.player != player) {
                    playerView.player = player
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        IconButton(
            onClick = onClose,
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

        if (playerError != null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Videocam,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = playerError ?: "Playback failed",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
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
                text = "${formatBioTime(playbackManager.currentPositionMs())} / ${formatBioTime(playbackManager.durationMs())}",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { playbackManager.toggleRepeat() }) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Repeat",
                        tint = if (isRepeat) Color(0xFF00BCD4) else Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(24.dp)
                    )
                }

                IconButton(onClick = { playbackManager.seekRelative(-10000) }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
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
                        onClick = { playbackManager.togglePlayPause() },
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

                IconButton(onClick = { playbackManager.seekRelative(10000) }) {
                    Icon(
                        imageVector = Icons.Default.Videocam,
                        contentDescription = "Forward 10s",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }

                IconButton(onClick = { playbackManager.stop() }) {
                    Icon(
                        imageVector = Icons.Default.Close,
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
private fun BiographyFullScreenAudio(
    audioUri: String,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val uri = remember(audioUri) {
        if (audioUri.startsWith("http://") || audioUri.startsWith("https://"))
            Uri.parse(audioUri)
        else
            Uri.fromFile(File(audioUri))
    }
    val key = remember(audioUri) { "bio_audio_$audioUri" }
    val playbackManager = remember(audioUri) {
        PlaybackManager.getInstance(context, key)
    }

    val isPlaying by playbackManager.isPlaying.collectAsState()
    val currentPosition by playbackManager.currentPosition.collectAsState()
    val duration by playbackManager.duration.collectAsState()
    val isRepeat by playbackManager.isRepeat.collectAsState()
    val playerError by playbackManager.playerError.collectAsState()

    DisposableEffect(audioUri) {
        playbackManager.prepareAndPlay(uri)
        onDispose {
            PlaybackManager.release(key)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (playerError != null) {
                Icon(
                    imageVector = Icons.Default.Audiotrack,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
                    modifier = Modifier.size(72.dp)
                )

                Spacer(modifier = Modifier.size(16.dp))

                Text(
                    text = playerError ?: "Playback failed",
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Audiotrack,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(72.dp)
                )

                Spacer(modifier = Modifier.size(16.dp))

                Text(
                    text = "Audio Recording",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }

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
                text = "${formatBioTime(playbackManager.currentPositionMs())} / ${formatBioTime(playbackManager.durationMs())}",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 13.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { playbackManager.toggleRepeat() }) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Repeat",
                        tint = if (isRepeat) Color(0xFF00BCD4) else Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(28.dp)
                    )
                }

                IconButton(onClick = { playbackManager.seekRelative(-10000) }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
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
                        onClick = { playbackManager.togglePlayPause() },
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

                IconButton(onClick = { playbackManager.seekRelative(10000) }) {
                    Icon(
                        imageVector = Icons.Default.Videocam,
                        contentDescription = "Forward 10s",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }

                IconButton(onClick = { playbackManager.stop() }) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Stop",
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }

        IconButton(
            onClick = onClose,
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
    }
}


private fun formatBioTime(millis: Long): String {
    val minutes = java.util.concurrent.TimeUnit.MILLISECONDS.toMinutes(millis)
    val seconds = java.util.concurrent.TimeUnit.MILLISECONDS.toSeconds(millis) - java.util.concurrent.TimeUnit.MINUTES.toSeconds(minutes)
    return String.format("%d:%02d", minutes, seconds)
}
