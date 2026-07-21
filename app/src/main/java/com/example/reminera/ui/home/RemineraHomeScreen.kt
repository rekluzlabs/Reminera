package com.example.reminera.ui.home

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import android.widget.Toast
import com.example.reminera.util.MediaSaver
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.reminera.data.MemoryEntryEntity
import com.example.reminera.data.MemoryType
import com.example.reminera.util.AudioRecorder
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

// Color constants removed — use MaterialTheme.colorScheme directly

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemineraHomeScreen(
    viewModel: RemineraViewModel,
    onSettingsClick: () -> Unit = {},
    onEntryClick: (MemoryEntryEntity) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    var showBottomSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Spacer(modifier = Modifier.height(48.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Reminera",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(start = 20.dp, bottom = 2.dp)
                    )

                    Text(
                        text = "Your Family Memories",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 20.dp, bottom = 12.dp)
                    )
                }

                IconButton(onClick = onSettingsClick) {
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
                        onDeleteEntry = { viewModel.deleteEntry(it) },
                        onEntryClick = onEntryClick
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
                onSave = { title, type, notes, personTag, mediaUri ->
                    scope.launch {
                        val localPath = mediaUri?.let { uri ->
                            val ext = when (type) {
                                MemoryType.PHOTO -> "jpg"
                                MemoryType.VIDEO -> "mp4"
                                MemoryType.AUDIO -> "m4a"
                            }
                            com.example.reminera.util.copyUriToInternal(context, uri, ext)
                        } ?: ""

                        sheetState.hide()
                        showBottomSheet = false
                        
                        if (type == MemoryType.PHOTO) {
                            viewModel.addImportedPhoto(
                                title = title,
                                localFilePath = localPath,
                                dateCaptured = System.currentTimeMillis(),
                                personTag = personTag
                            )
                        } else {
                            viewModel.addRecordedMemory(
                                title = title,
                                type = type,
                                localFilePath = localPath,
                                durationMillis = 0L,
                                personTag = personTag
                            )
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun MemoryLibraryContent(
    entries: List<MemoryEntryEntity>,
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
        modifier = Modifier.fillMaxSize(),
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
    onDelete: () -> Unit,
    onClick: () -> Unit
) {
    val context = LocalContext.current
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
            .clickable(onClick = onClick)
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
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "No file to save", Toast.LENGTH_SHORT).show()
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
}

/**
 * Manual form stand-in for real capture/import UI.
 * Will be replaced once real capture/import exists in a later pass.
 */
@Composable
private fun AddMemoryBottomSheetContent(
    onSave: (title: String, type: MemoryType, notes: String?, personTag: String?, mediaUri: Uri?) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf<MemoryType?>(null) }
    var notes by remember { mutableStateOf("") }
    var personTag by remember { mutableStateOf("") }
    var mediaUri by remember { mutableStateOf<Uri?>(null) }

    val context = LocalContext.current
    
    // Launchers
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

    // Audio recording state
    var isRecording by remember { mutableStateOf(false) }
    var recordingDuration by remember { mutableStateOf(0L) }
    val audioRecorder = remember { AudioRecorder(context) }
    val scope = rememberCoroutineScope()

    val isSaveEnabled = title.isNotBlank() && selectedType != null && mediaUri != null && !isRecording

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
                            mediaUri = null // Reset media when type changes
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

        // Media Selection UI
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
                        onClick = { /* TODO: Implement audio recording */ },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Audiotrack, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Record Audio")
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

        Button(
            onClick = {
                selectedType?.let { type ->
                    val notesValue = notes.ifBlank { null }
                    val personTagValue = personTag.ifBlank { null }
                    onSave(title, type, notesValue, personTagValue, mediaUri)
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
