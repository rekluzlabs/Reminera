package com.rekluzlabs.reminera.ui.home

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.rekluzlabs.reminera.data.FamilyGroupEntity
import com.rekluzlabs.reminera.data.GroupType
import com.rekluzlabs.reminera.data.MemoryEntryEntity
import com.rekluzlabs.reminera.data.MemoryType
import com.rekluzlabs.reminera.data.RemineraDatabase
import com.rekluzlabs.reminera.ui.detail.MemoryDetailScreen
import com.rekluzlabs.reminera.ui.detail.MemoryEditScreen
import com.rekluzlabs.reminera.ui.settings.SettingsScreen
import com.rekluzlabs.reminera.ui.settings.ThemeManager
import com.rekluzlabs.reminera.ui.settings.ThemeMode
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
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemineraHomeScreen(
    groupId: Long,
    viewModel: RemineraViewModel,
    themeManager: ThemeManager? = null,
    onBack: () -> Unit = {},
    onSettingsClick: () -> Unit = {}
) {
    var selectedEntry by rememberSaveable { mutableStateOf<MemoryEntryEntity?>(null) }
    var editingEntry by rememberSaveable { mutableStateOf<MemoryEntryEntity?>(null) }
    var showSettings by remember { mutableStateOf(false) }
    var themeMode by rememberSaveable { mutableStateOf(themeManager?.getThemeMode() ?: ThemeMode.LIGHT) }

    DisposableEffect(groupId) {
        viewModel.setGroupId(groupId)
        onDispose { }
    }

    val uiState by viewModel.uiState.collectAsState()

    var showBottomSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    val context = LocalContext.current
    val db = remember { RemineraDatabase.getInstance(context) }
    val groupsState = db.familyGroupDao().getAllOrderedBySortOrder()
        .collectAsState(initial = emptyList())
    val allGroups = groupsState.value

    when {
        showSettings -> {
            BackHandler { showSettings = false }
            SettingsScreen(
                currentTheme = themeMode,
                onThemeSelected = { mode ->
                    themeMode = mode
                    themeManager?.setThemeMode(mode)
                },
                onBack = { showSettings = false }
            )
        }
        editingEntry != null -> {
            val entry = editingEntry!!
            BackHandler { editingEntry = null }
            MemoryEditScreen(
                entry = entry,
                onBack = { editingEntry = null },
                onSave = { id, title, notes, personTag, type, localFilePath ->
                    viewModel.updateEntryDetails(id, title, notes, personTag, type, localFilePath)
                    selectedEntry = selectedEntry?.copy(
                        title = title,
                        notes = notes,
                        personTag = personTag,
                        type = type ?: selectedEntry?.type ?: "PHOTO",
                        localFilePath = localFilePath ?: selectedEntry?.localFilePath ?: ""
                    )
                    editingEntry = null
                },
                onDelete = { id ->
                    viewModel.deleteEntry(id)
                    selectedEntry = null
                    editingEntry = null
                }
            )
        }
        selectedEntry != null -> {
            val entry = selectedEntry!!
            BackHandler { selectedEntry = null }
            MemoryDetailScreen(
                entry = entry,
                onBack = { selectedEntry = null },
                onEdit = { editingEntry = selectedEntry },
                onDelete = {
                    viewModel.deleteEntry(entry.id)
                    selectedEntry = null
                },
                onMoveToGroup = { id, newGroupId -> viewModel.moveToGroup(id, newGroupId) }
            )
        }
        else -> {
            BackHandler { onBack() }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {

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
                                    contentDescription = "Back to groups",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Column {
                                Text(
                                    text = "Reminera",
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
                                )
                                Text(
                                    text = "Your Family Memories",
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(start = 4.dp, bottom = 12.dp)
                                )
                            }
                        }

                        IconButton(onClick = {
                            showSettings = true
                            onSettingsClick()
                        }) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    when (val state = uiState) {
                        is MemoryLibraryUiState.Loading -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            }
                        }

                        is MemoryLibraryUiState.Empty -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No memories yet — tap + to record your first one.",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 16.sp,
                                    modifier = Modifier.padding(32.dp)
                                )
                            }
                        }

                        is MemoryLibraryUiState.Success -> {
                            MemoryLibraryContent(
                                entries = state.entries,
                                groupId = groupId,
                                allGroups = allGroups,
                                viewModel = viewModel,
                                db = db,
                                onDeleteEntry = { viewModel.deleteEntry(it) },
                                onEntryClick = { entry -> selectedEntry = entry }
                            )
                        }
                    }
                }

                FloatingActionButton(
                    onClick = { showBottomSheet = true },
                    shape = RoundedCornerShape(16.dp),
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.background,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(20.dp)
                        .navigationBarsPadding()
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add memory")
                }
            }

            if (showBottomSheet) {
                ModalBottomSheet(
                    onDismissRequest = { showBottomSheet = false },
                    sheetState = sheetState,
                    containerColor = MaterialTheme.colorScheme.background
                ) {
                    val context = LocalContext.current
                    AddMemoryBottomSheetContent(
                        onSave = { title, type, notes, personTag, mediaUri, secondaryMediaType, secondaryMediaUri ->
                            scope.launch(Dispatchers.IO) {
                                val localPath = mediaUri?.let { uri ->
                                    val ext = when (type) {
                                        MemoryType.PHOTO -> "jpg"
                                        MemoryType.VIDEO -> "mp4"
                                        MemoryType.AUDIO -> "m4a"
                                    }
                                    copyUriToInternal(context, uri, ext)
                                } ?: ""

                                val secondaryLocalPath = if (secondaryMediaType != null && secondaryMediaUri != null) {
                                    val ext = if (secondaryMediaType == MemoryType.VIDEO) "mp4" else "m4a"
                                    copyUriToInternal(context, secondaryMediaUri, ext)
                                } else null

                                withContext(Dispatchers.Main) {
                                    sheetState.hide()
                                    showBottomSheet = false
                                }

                                if (type == MemoryType.PHOTO) {
                                    viewModel.addImportedPhoto(
                                        title = title,
                                        localFilePath = localPath,
                                        dateCaptured = System.currentTimeMillis(),
                                        personTag = personTag,
                                        groupId = groupId,
                                        secondaryMediaPath = secondaryLocalPath,
                                        secondaryMediaType = secondaryMediaType?.name
                                    )
                                } else {
                                    viewModel.addRecordedMemory(
                                        title = title,
                                        type = type,
                                        localFilePath = localPath,
                                        durationMillis = 0L,
                                        personTag = personTag,
                                        groupId = groupId
                                    )
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun MemoryLibraryContent(
    entries: List<MemoryEntryEntity>,
    groupId: Long,
    allGroups: List<FamilyGroupEntity>,
    viewModel: RemineraViewModel,
    db: RemineraDatabase,
    onDeleteEntry: (String) -> Unit,
    onEntryClick: (MemoryEntryEntity) -> Unit
) {
    val grouped = entries.groupBy { entry ->
        val cal = Calendar.getInstance().apply { timeInMillis = entry.dateCaptured }
        val month = cal.get(Calendar.MONTH)
        val year = cal.get(Calendar.YEAR)
        Pair(month, year)
    }

    val sortedGroups = grouped.entries.sortedByDescending { (key, _) ->
        val (month, year) = key
        year * 12L + month
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().navigationBarsPadding(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp)
    ) {
        sortedGroups.forEach { (key, groupEntries) ->
            val (month, year) = key
            val monthName = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
                .format(
                    Calendar.getInstance().apply {
                        set(Calendar.YEAR, year)
                        set(Calendar.MONTH, month)
                        set(Calendar.DAY_OF_MONTH, 1)
                    }.time
                )

            item {
                Text(
                    text = monthName,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
                )
            }

            items(groupEntries, key = { it.id }) { entry ->
                MemoryEntryCard(
                    entry = entry,
                    groupId = groupId,
                    allGroups = allGroups,
                    viewModel = viewModel,
                    db = db,
                    onDelete = { onDeleteEntry(entry.id) },
                    onClick = { onEntryClick(entry) }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun MemoryEntryCard(
    entry: MemoryEntryEntity,
    groupId: Long,
    allGroups: List<FamilyGroupEntity>,
    viewModel: RemineraViewModel,
    db: RemineraDatabase,
    onDelete: () -> Unit,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    var showMoveDialog by remember { mutableStateOf(false) }
    val file = remember(entry.localFilePath) { File(entry.localFilePath) }

    val thumbBitmap = remember(entry.localFilePath, entry.type) {
        if (!file.exists()) null
        else when (entry.type) {
            "PHOTO" -> {
                val opts = BitmapFactory.Options().apply { inSampleSize = 4 }
                try { BitmapFactory.decodeFile(file.absolutePath, opts) } catch (_: Exception) { null }
            }
            "VIDEO" -> {
                try {
                    val retriever = MediaMetadataRetriever()
                    retriever.setDataSource(file.absolutePath)
                    val frame = retriever.frameAtTime
                    retriever.release()
                    frame
                } catch (_: Exception) { null }
            }
            else -> null
        }
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = { showMoveDialog = true }
            )
    ) {
        Column {
            Row(
                modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    when (entry.type) {
                        "PHOTO" -> {
                            Box(modifier = Modifier.size(48.dp)) {
                                if (thumbBitmap != null) {
                                    Image(
                                        bitmap = thumbBitmap.asImageBitmap(),
                                        contentDescription = "Thumbnail",
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Image,
                                        contentDescription = "Photo",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                                if (!entry.secondaryMediaType.isNullOrBlank()) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .size(18.dp)
                                            .clip(androidx.compose.foundation.shape.CircleShape)
                                            .background(MaterialTheme.colorScheme.primary),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (entry.secondaryMediaType == "VIDEO") Icons.Default.Videocam else Icons.Default.Audiotrack,
                                            contentDescription = if (entry.secondaryMediaType == "VIDEO") "Has attached video" else "Has attached audio",
                                            tint = Color.White,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }
                            }
                        }
                        "VIDEO" -> {
                            if (thumbBitmap != null) {
                                Box {
                                    Image(
                                        bitmap = thumbBitmap.asImageBitmap(),
                                        contentDescription = "Video thumbnail",
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                    Icon(
                                        imageVector = Icons.Default.PlayCircle,
                                        contentDescription = null,
                                        tint = Color.White.copy(alpha = 0.8f),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            } else {
                                Icon(
                                    imageVector = Icons.Default.PlayCircle,
                                    contentDescription = "Video",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                        "AUDIO" -> {
                            var isAudioPlaying by remember(entry.id) { mutableStateOf(false) }
                            var audioPlayer by remember(entry.id) { mutableStateOf<MediaPlayer?>(null) }

                            DisposableEffect(entry.id) {
                                onDispose {
                                    audioPlayer?.release()
                                    audioPlayer = null
                                }
                            }

                            IconButton(
                                onClick = {
                                    if (file.exists()) {
                                        if (isAudioPlaying) {
                                            audioPlayer?.pause()
                                            isAudioPlaying = false
                                        } else {
                                            val player = audioPlayer ?: MediaPlayer().apply {
                                                setDataSource(file.absolutePath)
                                                setOnCompletionListener {
                                                    isAudioPlaying = false
                                                }
                                                setOnPreparedListener {
                                                    audioPlayer = this
                                                    start()
                                                    isAudioPlaying = true
                                                }
                                                prepareAsync()
                                            }.also { audioPlayer = it }
                                            if (player.isPlaying) {
                                                player.pause()
                                                isAudioPlaying = false
                                            } else {
                                                player.start()
                                                isAudioPlaying = true
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = if (isAudioPlaying) Icons.Default.Pause else Icons.Default.Audiotrack,
                                    contentDescription = if (isAudioPlaying) "Pause" else "Play audio",
                                    tint = if (isAudioPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = entry.title,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )

                    val dateFormat = remember {
                        java.text.SimpleDateFormat("MMM dd, yyyy · hh:mm a", java.util.Locale.getDefault())
                    }
                    Text(
                        text = dateFormat.format(java.util.Date(entry.dateCaptured)),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 2.dp, bottom = 2.dp)
                    )

                    if (!entry.personTag.isNullOrBlank()) {
                        Text(
                            text = entry.personTag,
                            color = MaterialTheme.colorScheme.secondary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    if (!entry.notes.isNullOrBlank()) {
                        Text(
                            text = entry.notes,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 4.dp, end = 4.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = {
                    if (entry.localFilePath.isNotBlank()) {
                        val saved = MediaSaver.saveToDevice(context, entry)
                        val msg = if (saved) "Saved to device" else "Failed to save"
                        android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
                    } else {
                        android.widget.Toast.makeText(context, "No file to save", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }) {
                    Icon(
                        imageVector = Icons.Default.SaveAlt,
                        contentDescription = "Save to device",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete memory",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    if (showMoveDialog) {
        MoveMemoryDialog(
            entry = entry,
            allGroups = allGroups,
            currentGroupId = groupId,
            viewModel = viewModel,
            db = db,
            onDismiss = { showMoveDialog = false }
        )
    }
}

@Composable
private fun AddMemoryBottomSheetContent(
    onSave: (
        title: String,
        type: MemoryType,
        notes: String?,
        personTag: String?,
        mediaUri: Uri?,
        secondaryMediaType: MemoryType?,
        secondaryMediaUri: Uri?
    ) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf<MemoryType?>(null) }
    var notes by remember { mutableStateOf("") }
    var personTag by remember { mutableStateOf("") }
    var mediaUri by remember { mutableStateOf<Uri?>(null) }

    var secondaryMediaType by remember { mutableStateOf<MemoryType?>(null) }
    var secondaryMediaUri by remember { mutableStateOf<Uri?>(null) }
    var isRecordingSecondaryAudio by remember { mutableStateOf(false) }
    var secondaryRecordingDuration by remember { mutableStateOf(0L) }

    val context = LocalContext.current

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri -> mediaUri = uri }

    val videoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri -> mediaUri = uri }

    var tempPhotoUri by remember { mutableStateOf<Uri?>(null) }
    val takePicture = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success -> if (success) mediaUri = tempPhotoUri }

    var tempVideoUri by remember { mutableStateOf<Uri?>(null) }
    val captureVideo = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CaptureVideo()
    ) { success -> if (success) mediaUri = tempVideoUri }

    val audioPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri -> mediaUri = uri }

    var isRecording by remember { mutableStateOf(false) }
    var recordingDuration by remember { mutableStateOf(0L) }
    val audioRecorder = remember { AudioRecorder(context) }
    val scope = rememberCoroutineScope()

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

    val secondaryVideoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri -> secondaryMediaUri = uri }

    var tempSecondaryVideoUri by remember { mutableStateOf<Uri?>(null) }
    val captureSecondaryVideo = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CaptureVideo()
    ) { success -> if (success) secondaryMediaUri = tempSecondaryVideoUri }

    val secondaryAudioPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri -> secondaryMediaUri = uri }

    val secondaryAudioRecorder = remember { AudioRecorder(context) }
    DisposableEffect(Unit) {
        onDispose { if (isRecordingSecondaryAudio) secondaryAudioRecorder.stop() }
    }

    val isSaveEnabled = title.isNotBlank() && selectedType != null && mediaUri != null && !isRecording && !isRecordingSecondaryAudio

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .verticalScroll(scrollState)
    ) {
        Text(
            text = "Add Memory",
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Title") },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                focusedLabelColor = MaterialTheme.colorScheme.primary,
                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                cursorColor = MaterialTheme.colorScheme.primary,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MemoryType.entries.forEach { type ->
                val isSelected = selectedType == type
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        if (selectedType != type) {
                            selectedType = type
                            mediaUri = null
                            secondaryMediaType = null
                            secondaryMediaUri = null
                        }
                    },
                    label = { Text(type.name) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.secondary,
                        selectedLabelColor = Color.White
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (selectedType != null) {
            Text(
                text = if (mediaUri == null) "Choose media source:" else "Media selected!",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (selectedType == MemoryType.PHOTO || selectedType == MemoryType.VIDEO) {
                    Button(
                        onClick = {
                            if (selectedType == MemoryType.PHOTO) {
                                val tempFile = File(context.cacheDir, "temp_photo_${System.currentTimeMillis()}.jpg")
                                val uri = FileProvider.getUriForFile(context, "com.rekluzlabs.reminera.fileprovider", tempFile)
                                tempPhotoUri = uri
                                takePicture.launch(uri)
                            } else {
                                val tempFile = File(context.cacheDir, "temp_video_${System.currentTimeMillis()}.mp4")
                                val uri = FileProvider.getUriForFile(context, "com.rekluzlabs.reminera.fileprovider", tempFile)
                                tempVideoUri = uri
                                captureVideo.launch(uri)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Capture")
                    }

                    Button(
                        onClick = {
                            if (selectedType == MemoryType.PHOTO) {
                                photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            } else {
                                videoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface, contentColor = MaterialTheme.colorScheme.onSurface),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Import")
                    }
                } else if (selectedType == MemoryType.AUDIO) {
                    Button(
                        onClick = {
                            if (isRecording) {
                                audioRecorder.stop()
                                isRecording = false
                            } else {
                                startRecordingIfPermitted {
                                    val tempFile = File(context.cacheDir, "temp_audio_${System.currentTimeMillis()}.m4a")
                                    mediaUri = Uri.fromFile(tempFile)
                                    audioRecorder.start(tempFile)
                                    isRecording = true
                                    scope.launch {
                                        val startTime = System.currentTimeMillis()
                                        while (isRecording && isActive) {
                                            recordingDuration = System.currentTimeMillis() - startTime
                                            delay(100)
                                        }
                                    }
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = if (isRecording) Icons.Default.Pause else Icons.Default.Audiotrack,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isRecording) "Stop (${recordingDuration / 1000}s)" else "Record Audio")
                    }

                    Button(
                        onClick = { audioPicker.launch(arrayOf("audio/*")) },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface, contentColor = MaterialTheme.colorScheme.onSurface),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Import Audio")
                    }
                }
            }
            if (mediaUri != null) {
                Text(
                    text = "File: ${mediaUri?.lastPathSegment}",
                    color = MaterialTheme.colorScheme.secondary,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text("Notes") },
            maxLines = 3,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                focusedLabelColor = MaterialTheme.colorScheme.primary,
                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                cursorColor = MaterialTheme.colorScheme.primary,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = personTag,
            onValueChange = { personTag = it },
            label = { Text("Who is this about? (optional)") },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                focusedLabelColor = MaterialTheme.colorScheme.primary,
                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                cursorColor = MaterialTheme.colorScheme.primary,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        val currentMediaUri = mediaUri
        val currentType = selectedType
        if (currentMediaUri != null && currentType != null) {
            val previewBitmap = remember(currentMediaUri, currentType) {
                try {
                    if (currentType == MemoryType.PHOTO) {
                        context.contentResolver.openInputStream(currentMediaUri)?.use { input ->
                            BitmapFactory.decodeStream(input)
                        }
                    } else null
                } catch (_: Exception) { null }
            }

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                when (currentType) {
                    MemoryType.PHOTO -> {
                        if (previewBitmap != null) {
                            Image(
                                bitmap = previewBitmap.asImageBitmap(),
                                contentDescription = "Preview",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp),
                                contentScale = ContentScale.Fit
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Image,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                        }
                    }
                    MemoryType.VIDEO -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                text = "Video selected",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(top = 56.dp)
                            )
                        }
                    }
                    MemoryType.AUDIO -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(80.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Audiotrack,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Audio selected",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        if (currentType == MemoryType.PHOTO && currentMediaUri != null) {
            Text(
                text = "Add audio or video (optional)",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(null, MemoryType.AUDIO, MemoryType.VIDEO).forEach { option ->
                    val label = when (option) {
                        null -> "None"
                        MemoryType.AUDIO -> "Audio"
                        MemoryType.VIDEO -> "Video"
                        else -> ""
                    }
                    FilterChip(
                        selected = secondaryMediaType == option,
                        onClick = {
                            secondaryMediaType = option
                            secondaryMediaUri = null
                        },
                        label = { Text(label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.secondary,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            when (secondaryMediaType) {
                MemoryType.AUDIO -> {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = {
                                if (isRecordingSecondaryAudio) {
                                    secondaryAudioRecorder.stop()
                                    isRecordingSecondaryAudio = false
                                } else {
                                    startRecordingIfPermitted {
                                        val tempFile = File(context.cacheDir, "temp_secondary_audio_${System.currentTimeMillis()}.m4a")
                                        secondaryMediaUri = Uri.fromFile(tempFile)
                                        secondaryAudioRecorder.start(tempFile)
                                        isRecordingSecondaryAudio = true
                                        scope.launch {
                                            val startTime = System.currentTimeMillis()
                                            while (isRecordingSecondaryAudio && isActive) {
                                                secondaryRecordingDuration = System.currentTimeMillis() - startTime
                                                delay(100)
                                            }
                                        }
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isRecordingSecondaryAudio) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                imageVector = if (isRecordingSecondaryAudio) Icons.Default.Pause else Icons.Default.Audiotrack,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isRecordingSecondaryAudio) "Stop (${secondaryRecordingDuration / 1000}s)" else "Record Audio")
                        }

                        Button(
                            onClick = { secondaryAudioPicker.launch(arrayOf("audio/*")) },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface, contentColor = MaterialTheme.colorScheme.onSurface),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Import Audio")
                        }
                    }
                }
                MemoryType.VIDEO -> {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = {
                                val tempFile = File(context.cacheDir, "temp_secondary_video_${System.currentTimeMillis()}.mp4")
                                val uri = FileProvider.getUriForFile(context, "com.rekluzlabs.reminera.fileprovider", tempFile)
                                tempSecondaryVideoUri = uri
                                captureSecondaryVideo.launch(uri)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Capture")
                        }

                        Button(
                            onClick = {
                                secondaryVideoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface, contentColor = MaterialTheme.colorScheme.onSurface),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Import")
                        }
                    }
                }
                else -> {}
            }

            if (secondaryMediaUri != null) {
                Text(
                    text = "Attached: ${secondaryMediaUri?.lastPathSegment}",
                    color = MaterialTheme.colorScheme.secondary,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
        }

        Button(
            onClick = {
                selectedType?.let { type ->
                    val notesValue = notes.ifBlank { null }
                    val personTagValue = personTag.ifBlank { null }
                    val effectiveSecondaryType = if (secondaryMediaUri != null) secondaryMediaType else null
                    onSave(title, type, notesValue, personTagValue, mediaUri, effectiveSecondaryType, secondaryMediaUri)
                }
            },
            enabled = isSaveEnabled,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Text("Save Memory", color = Color.White, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun MoveMemoryDialog(
    entry: MemoryEntryEntity,
    allGroups: List<FamilyGroupEntity>,
    currentGroupId: Long,
    viewModel: RemineraViewModel,
    db: RemineraDatabase,
    onDismiss: () -> Unit
) {
    var showCreateGroup by remember { mutableStateOf(false) }
    var newGroupName by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val otherGroups = allGroups.filter { it.id != currentGroupId }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Move Memory") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    text = "Move \"${entry.title}\" to:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                if (showCreateGroup) {
                    OutlinedTextField(
                        value = newGroupName,
                        onValueChange = { newGroupName = it },
                        label = { Text("New group name") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            cursorColor = MaterialTheme.colorScheme.primary,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    FilledTonalButton(
                        onClick = {
                            if (newGroupName.isNotBlank()) {
                                scope.launch {
                                    val newGroup = FamilyGroupEntity(
                                        name = newGroupName,
                                        groupType = GroupType.CUSTOM.name,
                                        sortOrder = (allGroups.maxOfOrNull { it.sortOrder } ?: 0) + 1,
                                        createdAt = System.currentTimeMillis()
                                    )
                                    val newId = db.familyGroupDao().insert(newGroup)
                                    viewModel.moveToGroup(entry.id, newId)
                                    onDismiss()
                                }
                            }
                        },
                        enabled = newGroupName.isNotBlank(),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Create & Move")
                    }
                } else if (otherGroups.isEmpty()) {
                    Text(
                        text = "No other groups available.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    FilledTonalButton(
                        onClick = { showCreateGroup = true },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Create New Group")
                    }
                } else {
                    otherGroups.forEach { group ->
                        Surface(
                            onClick = {
                                viewModel.moveToGroup(entry.id, group.id)
                                onDismiss()
                            },
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
                        ) {
                            Text(
                                text = group.name,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
