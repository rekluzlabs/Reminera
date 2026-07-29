package com.rekluzlabs.reminera.ui.settings

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rekluzlabs.reminera.data.RemineraDatabase
import com.rekluzlabs.reminera.data.FamilyGroupEntity
import com.rekluzlabs.reminera.data.MemoryEntryEntity
import com.rekluzlabs.reminera.data.FamilyMemberEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

private data class StorageCategory(
    val label: String,
    val sizeBytes: Long,
    val fileCount: Int,
    val color: Color
)

private data class GroupStorage(
    val group: FamilyGroupEntity,
    val sizeBytes: Long,
    val photoSize: Long,
    val videoSize: Long,
    val audioSize: Long,
    val photoCount: Int,
    val videoCount: Int,
    val audioCount: Int
)

private data class StorageStats(
    val totalSize: Long,
    val categories: List<StorageCategory>,
    val groups: List<GroupStorage>,
    val dbSizeBytes: Long,
    val mediaDirSize: Long,
    val recordingsDirSize: Long,
    val exportDirSize: Long
)

private val PhotoColor = Color(0xFF4CAF50)
private val VideoColor = Color(0xFF2196F3)
private val AudioColor = Color(0xFFFF9800)
private val DatabaseColor = Color(0xFF9C27B0)
private val ExportColor = Color(0xFF607D8B)
private val OtherColor = Color(0xFF795548)

@Composable
fun StorageUsageScreen(
    onBack: () -> Unit,
    onNavigateToBackup: () -> Unit = {}
) {
    val context = LocalContext.current
    var stats by remember { mutableStateOf<StorageStats?>(null) }
    var expandedGroup by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(Unit) {
        stats = withContext(Dispatchers.IO) {
            computeStorageStats(context)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        item {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        item {
            Text(
                text = "Storage Usage",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "See how much space your media and data occupy.",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }

        if (stats == null) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(32.dp),
                        strokeWidth = 3.dp
                    )
                }
            }
        } else {
            val s = stats!!

            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "Total App Data",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = formatSize(s.totalSize),
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(20.dp)) }

            item {
                Text(
                    text = "By Media Type",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }

            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        if (s.totalSize > 0) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(12.dp)
                                    .clip(RoundedCornerShape(6.dp))
                            ) {
                                s.categories.filter { it.sizeBytes > 0 }.forEach { cat ->
                                    val fraction = cat.sizeBytes.toFloat() / s.totalSize.toFloat()
                                    Box(
                                        modifier = Modifier
                                            .weight(fraction.coerceAtLeast(0.01f))
                                            .height(12.dp)
                                            .background(cat.color)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        s.categories.forEach { cat ->
                            StorageBreakdownRow(
                                label = cat.label,
                                sizeBytes = cat.sizeBytes,
                                fileCount = cat.fileCount,
                                color = cat.color
                            )
                            if (cat != s.categories.last()) {
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }
                }
            }

            if (s.groups.isNotEmpty()) {
                item { Spacer(modifier = Modifier.height(20.dp)) }

                item {
                    Text(
                        text = "By Family Group",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }

                item { Spacer(modifier = Modifier.height(12.dp)) }

                items(s.groups, key = { it.group.id }) { groupStorage ->
                    val isExpanded = expandedGroup == groupStorage.group.id
                    GroupStorageCard(
                        groupStorage = groupStorage,
                        totalSize = s.totalSize,
                        isExpanded = isExpanded,
                        onClick = {
                            expandedGroup = if (isExpanded) null else groupStorage.group.id
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }

            item {
                Text(
                    text = "Tip: Use Backup & Restore to export data you no longer need on-device.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }

            item {
                Text(
                    text = "Go to Backup & Restore",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .clickable(onClick = onNavigateToBackup)
                        .padding(vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun StorageBreakdownRow(
    label: String,
    sizeBytes: Long,
    fileCount: Int,
    color: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(color)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "$fileCount file${if (fileCount != 1) "s" else ""}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = formatSize(sizeBytes),
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun GroupStorageCard(
    groupStorage: GroupStorage,
    totalSize: Long,
    isExpanded: Boolean,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onClick)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = groupStorage.group.name,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    val totalFiles = groupStorage.photoCount + groupStorage.videoCount + groupStorage.audioCount
                    Text(
                        text = "${totalFiles} file${if (totalFiles != 1) "s" else ""} \u00b7 ${formatSize(groupStorage.sizeBytes)}",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (totalSize > 0 && groupStorage.sizeBytes > 0) {
                    val fraction = groupStorage.sizeBytes.toFloat() / totalSize.toFloat()
                    LinearProgressIndicator(
                        progress = { fraction.coerceIn(0f, 1f) },
                        modifier = Modifier
                            .width(64.dp)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }

                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    if (groupStorage.photoCount > 0) {
                        StorageBreakdownRow(
                            label = "Photos",
                            sizeBytes = groupStorage.photoSize,
                            fileCount = groupStorage.photoCount,
                            color = PhotoColor
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    if (groupStorage.videoCount > 0) {
                        StorageBreakdownRow(
                            label = "Videos",
                            sizeBytes = groupStorage.videoSize,
                            fileCount = groupStorage.videoCount,
                            color = VideoColor
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    if (groupStorage.audioCount > 0) {
                        StorageBreakdownRow(
                            label = "Audio",
                            sizeBytes = groupStorage.audioSize,
                            fileCount = groupStorage.audioCount,
                            color = AudioColor
                        )
                    }

                    if (groupStorage.photoCount == 0 && groupStorage.videoCount == 0 && groupStorage.audioCount == 0) {
                        Text(
                            text = "No media files in this group.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

private suspend fun computeStorageStats(context: Context): StorageStats {
    val database = RemineraDatabase.getInstance(context)

    val entries = database.memoryEntryDao().getAllEntriesList()
    val groups = database.familyGroupDao().getAllOrderedBySortOrderList()
    val members = database.familyMemberDao().getAllMembersList()

    val mediaDir = File(context.filesDir, "media")
    val recordingsDir = File(context.filesDir, "recordings")
    val exportsDir = File(context.filesDir, "exports")
    val dbFile = context.getDatabasePath("reminera.db")

    var photoBytes = 0L; var photoCount = 0
    var videoBytes = 0L; var videoCount = 0
    var audioBytes = 0L; var audioCount = 0
    var otherBytes = 0L; var otherCount = 0

    entries.forEach { entry ->
        val filePaths = mutableListOf(entry.localFilePath)
        entry.thumbnailPath?.let { filePaths.add(it) }
        entry.secondaryMediaPath?.let { filePaths.add(it) }

        filePaths.forEach { path ->
            val file = File(path)
            if (file.exists()) {
                val size = file.length()
                when (entry.type) {
                    "PHOTO" -> { photoBytes += size; photoCount++ }
                    "VIDEO" -> { videoBytes += size; videoCount++ }
                    "AUDIO" -> { audioBytes += size; audioCount++ }
                    else -> { otherBytes += size; otherCount++ }
                }
            }
        }
    }

    members.forEach { member ->
        member.photoUri?.let { path ->
            val file = File(path)
            if (file.exists()) {
                photoBytes += file.length()
                photoCount++
            }
        }
    }

    val dbSize = if (dbFile.exists()) dbFile.length() else 0L
    val mediaDirSize = dirSize(mediaDir)
    val recordingsDirSize = dirSize(recordingsDir)
    val exportDirSize = dirSize(exportsDir)

    val categories = listOf(
        StorageCategory("Photos", photoBytes, photoCount, PhotoColor),
        StorageCategory("Videos", videoBytes, videoCount, VideoColor),
        StorageCategory("Audio", audioBytes, audioCount, AudioColor),
        StorageCategory("Database", dbSize, 1, DatabaseColor),
        StorageCategory("Exports (PDFs)", exportDirSize, countFiles(exportsDir), ExportColor),
        StorageCategory("Other", otherBytes, otherCount, OtherColor)
    ).filter { it.sizeBytes > 0 || it.fileCount > 0 }

    val groupMap = groups.associateBy { it.id }
    val groupEntries = entries.groupBy { it.groupId }

    val groupStorages = groupMap.map { (groupId, group) ->
        val groupEntryList = groupEntries[groupId] ?: emptyList()

        var gPhoto = 0L; var gVideo = 0L; var gAudio = 0L
        var gPhotoCount = 0; var gVideoCount = 0; var gAudioCount = 0

        groupEntryList.forEach { entry ->
            val filePaths = mutableListOf(entry.localFilePath)
            entry.thumbnailPath?.let { filePaths.add(it) }
            entry.secondaryMediaPath?.let { filePaths.add(it) }

            val totalEntrySize = filePaths.sumOf { File(it).let { f -> if (f.exists()) f.length() else 0L } }

            when (entry.type) {
                "PHOTO" -> { gPhoto += totalEntrySize; gPhotoCount++ }
                "VIDEO" -> { gVideo += totalEntrySize; gVideoCount++ }
                "AUDIO" -> { gAudio += totalEntrySize; gAudioCount++ }
            }
        }

        GroupStorage(
            group = group,
            sizeBytes = gPhoto + gVideo + gAudio,
            photoSize = gPhoto, videoSize = gVideo, audioSize = gAudio,
            photoCount = gPhotoCount, videoCount = gVideoCount, audioCount = gAudioCount
        )
    }.filter { it.sizeBytes > 0 }
        .sortedByDescending { it.sizeBytes }

    val totalSize = categories.sumOf { it.sizeBytes }

    return StorageStats(
        totalSize = totalSize,
        categories = categories,
        groups = groupStorages,
        dbSizeBytes = dbSize,
        mediaDirSize = mediaDirSize,
        recordingsDirSize = recordingsDirSize,
        exportDirSize = exportDirSize
    )
}

private fun dirSize(dir: File): Long {
    if (!dir.exists()) return 0L
    return dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
}

private fun countFiles(dir: File): Int {
    if (!dir.exists()) return 0
    return dir.walkTopDown().filter { it.isFile }.count()
}

private fun formatSize(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return "%.1f KB".format(kb)
    val mb = kb / 1024.0
    if (mb < 1024) return "%.1f MB".format(mb)
    val gb = mb / 1024.0
    return "%.2f GB".format(gb)
}
