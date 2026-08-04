package com.rekluzlabs.reminera.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rekluzlabs.reminera.data.FamilyGroupEntity
import com.rekluzlabs.reminera.data.FamilyMemberEntity
import com.rekluzlabs.reminera.data.RemineraDatabase
import com.rekluzlabs.reminera.util.BackupRestoreHelper
import com.rekluzlabs.reminera.util.ImageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private data class MemberBackupInfo(
    val member: FamilyMemberEntity,
    val group: FamilyGroupEntity,
    val memoryCount: Int,
    val storyCount: Int,
    val totalMediaSize: Long,
    val photoBitmap: android.graphics.Bitmap? = null
)

@Composable
fun MemberBackupScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var members by remember { mutableStateOf<List<MemberBackupInfo>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    var backupTargetMemberId by remember { mutableStateOf<Long?>(null) }
    var backupState by remember { mutableStateOf<BackupRestoreHelper.ProgressState>(BackupRestoreHelper.ProgressState.Idle) }

    var restoreTargetMemberId by remember { mutableStateOf<Long?>(null) }
    var restoreState by remember { mutableStateOf<BackupRestoreHelper.ProgressState>(BackupRestoreHelper.ProgressState.Idle) }
    var showRestoreConfirm by remember { mutableStateOf(false) }
    var pendingRestoreUri by remember { mutableStateOf<android.net.Uri?>(null) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val db = RemineraDatabase.getInstance(context)
            val groups = db.familyGroupDao().getAllOrderedBySortOrderList()
            val allMembers = db.familyMemberDao().getAllMembersList()
            val groupMap = groups.associateBy { it.id }

            members = allMembers.mapNotNull { member ->
                val group = groupMap[member.groupId] ?: return@mapNotNull null
                val memoryCount = db.memoryEntryDao()
                    .getEntriesByGroupIdAndPersonTagList(member.groupId, member.name).size
                val bio = db.biographyDao().getByPersonIdOnce(member.id)
                val stories = if (bio != null) db.storyEntryDao().getByBiographyIdOnce(bio.id) else emptyList()

                var totalSize = 0L
                member.photoUri?.let { totalSize += File(it).length() }
                bio?.photoUri?.let { totalSize += File(it).length() }
                stories.forEach { s ->
                    s.mediaUri?.let { totalSize += File(it).length() }
                    s.thumbnailUri?.let { totalSize += File(it).length() }
                }

                val photoBitmap = member.photoUri?.let {
                    try { ImageUtils.loadBitmapWithExifOrientation(context, it, 120) } catch (_: Exception) { null }
                }
                MemberBackupInfo(
                    member = member,
                    group = group,
                    memoryCount = memoryCount,
                    storyCount = stories.size,
                    totalMediaSize = totalSize,
                    photoBitmap = photoBitmap
                )
            }
            isLoading = false
        }
    }

    val backupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        val memberId = backupTargetMemberId
        if (uri != null && memberId != null) {
            backupTargetMemberId = null
            scope.launch {
                BackupRestoreHelper.createMemberBackup(context, memberId, uri) { newState ->
                    backupState = newState
                }
            }
        }
    }

    val restoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            pendingRestoreUri = uri
            showRestoreConfirm = true
        }
    }

    if (showRestoreConfirm && pendingRestoreUri != null) {
        AlertDialog(
            onDismissRequest = { showRestoreConfirm = false; pendingRestoreUri = null },
            icon = {
                Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            },
            title = { Text("Restore Member Backup?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "This will restore or update this member's data from the backup. Records with matching IDs will be updated; new records will be added. Your other members' data will not be affected.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showRestoreConfirm = false
                    val uri = pendingRestoreUri
                    pendingRestoreUri = null
                    if (uri != null) {
                        scope.launch {
                            BackupRestoreHelper.restoreMemberBackup(context, uri) { newState ->
                                restoreState = newState
                            }
                        }
                    }
                }) {
                    Text("Restore", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreConfirm = false; pendingRestoreUri = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (backupState is BackupRestoreHelper.ProgressState.Running) {
        val state = backupState as BackupRestoreHelper.ProgressState.Running
        AlertDialog(
            onDismissRequest = {},
            icon = {
                if (state.progress >= 0f) {
                    LinearProgressIndicator(
                        progress = { state.progress },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                    )
                } else {
                    CircularProgressIndicator(modifier = Modifier.size(32.dp))
                }
            },
            title = { Text("Backup in progress", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(state.message)
                    if (state.detail != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = state.detail,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            confirmButton = {}
        )
    }

    if (backupState is BackupRestoreHelper.ProgressState.Success) {
        AlertDialog(
            onDismissRequest = { backupState = BackupRestoreHelper.ProgressState.Idle },
            title = { Text("Backup Complete", fontWeight = FontWeight.Bold) },
            text = { Text("Member backup created successfully.") },
            confirmButton = { TextButton(onClick = { backupState = BackupRestoreHelper.ProgressState.Idle }) { Text("OK") } }
        )
    }

    if (backupState is BackupRestoreHelper.ProgressState.Error) {
        AlertDialog(
            onDismissRequest = { backupState = BackupRestoreHelper.ProgressState.Idle },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Backup Failed", fontWeight = FontWeight.Bold) },
            text = { Text((backupState as BackupRestoreHelper.ProgressState.Error).message) },
            confirmButton = { TextButton(onClick = { backupState = BackupRestoreHelper.ProgressState.Idle }) { Text("OK") } }
        )
    }

    if (restoreState is BackupRestoreHelper.ProgressState.Running) {
        val state = restoreState as BackupRestoreHelper.ProgressState.Running
        AlertDialog(
            onDismissRequest = {},
            icon = {
                if (state.progress >= 0f) {
                    LinearProgressIndicator(
                        progress = { state.progress },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                    )
                } else {
                    CircularProgressIndicator(modifier = Modifier.size(32.dp))
                }
            },
            title = { Text("Restore in progress", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(state.message)
                    if (state.detail != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = state.detail,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            confirmButton = {}
        )
    }

    if (restoreState is BackupRestoreHelper.ProgressState.Success) {
        AlertDialog(
            onDismissRequest = { restoreState = BackupRestoreHelper.ProgressState.Idle },
            title = { Text("Restore Complete", fontWeight = FontWeight.Bold) },
            text = { Text("Member data restored successfully.") },
            confirmButton = { TextButton(onClick = { restoreState = BackupRestoreHelper.ProgressState.Idle }) { Text("OK") } }
        )
    }

    if (restoreState is BackupRestoreHelper.ProgressState.Error) {
        AlertDialog(
            onDismissRequest = { restoreState = BackupRestoreHelper.ProgressState.Idle },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Restore Failed", fontWeight = FontWeight.Bold) },
            text = { Text((restoreState as BackupRestoreHelper.ProgressState.Error).message) },
            confirmButton = { TextButton(onClick = { restoreState = BackupRestoreHelper.ProgressState.Idle }) { Text("OK") } }
        )
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                Text(
                    text = "Individual Backups",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        }

        item {
            Text(
                text = "Back up or restore a single family member's data, including their biography, stories, and tagged media.",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }

        if (isLoading) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        } else if (members.isEmpty()) {
            item {
                Text(
                    text = "No family members found. Add family members first.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp)
                )
            }
        } else {
            items(members, key = { it.member.id }) { info ->
                MemberBackupCard(
                    info = info,
                    onBackup = {
                        backupTargetMemberId = info.member.id
                        backupLauncher.launch("member_${info.member.name.replace(" ", "_")}_backup.zip")
                    },
                    onRestore = {
                        restoreTargetMemberId = info.member.id
                        restoreLauncher.launch(arrayOf("application/zip", "application/octet-stream"))
                    },
                    isBusy = backupState is BackupRestoreHelper.ProgressState.Running ||
                             restoreState is BackupRestoreHelper.ProgressState.Running
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun MemberBackupCard(
    info: MemberBackupInfo,
    onBackup: () -> Unit,
    onRestore: () -> Unit,
    isBusy: Boolean
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (info.photoBitmap != null) {
                Image(
                    bitmap = info.photoBitmap.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = info.member.name,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = info.group.name,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = buildString {
                        val parts = mutableListOf<String>()
                        if (info.memoryCount > 0) parts.add("${info.memoryCount} media")
                        if (info.storyCount > 0) parts.add("${info.storyCount} stories")
                        if (parts.isEmpty()) parts.add("No data")
                        append(parts.joinToString(", "))
                        if (info.totalMediaSize > 0) {
                            val mb = info.totalMediaSize / (1024 * 1024)
                            if (mb > 0) append(" · ${mb}MB")
                            else append(" · <1MB")
                        }
                    },
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Button(
                    onClick = onBackup,
                    enabled = !isBusy,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Icon(
                        Icons.Default.Restore,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Backup", fontSize = 12.sp)
                }
                OutlinedButton(
                    onClick = onRestore,
                    enabled = !isBusy,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Icon(
                        Icons.Default.Restore,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Restore", fontSize = 12.sp)
                }
            }
        }
    }
}
