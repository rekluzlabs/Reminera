package com.rekluzlabs.reminera.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rekluzlabs.reminera.util.BackupRestoreHelper
import com.rekluzlabs.reminera.util.BackupRestoreHelper.ProgressState
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun BackupRestoreScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var backupState by remember { mutableStateOf<ProgressState>(ProgressState.Idle) }
    var restoreState by remember { mutableStateOf<ProgressState>(ProgressState.Idle) }
    var showRestoreConfirm by remember { mutableStateOf(false) }
    var pendingRestoreUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var deleteConfirmText by remember { mutableStateOf("") }
    var deleteState by remember { mutableStateOf<ProgressState>(ProgressState.Idle) }

    val lastBackupTime = remember { BackupRestoreHelper.getLastBackupTime(context) }

    val backupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                BackupRestoreHelper.createBackup(context, uri) { newState ->
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
            onDismissRequest = {
                showRestoreConfirm = false
                pendingRestoreUri = null
            },
            icon = {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = {
                Text(
                    text = "Restore Backup?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = "This will replace ALL current data with the backup contents. This action cannot be undone.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Your current family groups, members, media entries, and biographies will be overwritten.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showRestoreConfirm = false
                        val uri = pendingRestoreUri
                        pendingRestoreUri = null
                        if (uri != null) {
                            scope.launch {
                                BackupRestoreHelper.restoreBackup(context, uri) { newState ->
                                    restoreState = newState
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Restore", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showRestoreConfirm = false
                        pendingRestoreUri = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = {
                if (deleteState !is ProgressState.Running) {
                    showDeleteConfirm = false
                    deleteConfirmText = ""
                }
            },
            icon = {
                Icon(
                    Icons.Default.DeleteForever,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = {
                Text(
                    text = "Delete All Data",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = "This will permanently delete ALL your data, including family groups, members, media files, biographies, and app settings. This action CANNOT be undone.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Have you made a backup? If not, go back and export your data first.",
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Type DELETE below to confirm:",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = deleteConfirmText,
                        onValueChange = { deleteConfirmText = it },
                        placeholder = { Text("DELETE") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Characters
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            deleteState = ProgressState.Running("Deleting all data...")
                            val success = BackupRestoreHelper.clearAllData(context)
                            deleteState = if (success) ProgressState.Success else ProgressState.Error("Failed to delete all data. Please try again.")
                            if (success) {
                                showDeleteConfirm = false
                                deleteConfirmText = ""
                            }
                        }
                    },
                    enabled = deleteConfirmText == "DELETE" && deleteState !is ProgressState.Running,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete Everything", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        deleteConfirmText = ""
                    },
                    enabled = deleteState !is ProgressState.Running
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    if (deleteState is ProgressState.Error) {
        AlertDialog(
            onDismissRequest = { deleteState = ProgressState.Idle },
            icon = {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = {
                Text("Error", fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    text = (deleteState as ProgressState.Error).message,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                TextButton(onClick = { deleteState = ProgressState.Idle }) {
                    Text("OK")
                }
            }
        )
    }

    if (deleteState is ProgressState.Running) {
        AlertDialog(
            onDismissRequest = {},
            icon = {
                CircularProgressIndicator(modifier = Modifier.size(32.dp))
            },
            title = {
                Text("Deleting All Data...", fontWeight = FontWeight.Bold)
            },
            text = {
                Text("Please wait while your data is being permanently removed.")
            },
            confirmButton = {}
        )
    }

    if (backupState is ProgressState.Running) {
        val state = backupState as ProgressState.Running
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
            title = {
                Text("Backup in progress", fontWeight = FontWeight.Bold)
            },
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

    if (restoreState is ProgressState.Running) {
        val state = restoreState as ProgressState.Running
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
            title = {
                Text("Restore in progress", fontWeight = FontWeight.Bold)
            },
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
                text = "Backup & Restore",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Export all your data to a backup file, or restore from a previous backup.",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }

        item {
            Text(
                text = "Last backup: ${
                    if (lastBackupTime > 0) {
                        SimpleDateFormat("MMM d, yyyy 'at' h:mm a", Locale.getDefault()).format(Date(lastBackupTime))
                    } else {
                        "Never"
                    }
                }",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }

        item {
            Text(
                text = "Export Data",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }

        item {
            Text(
                text = "Creates a ZIP file containing your database and all media files (photos, videos, audio recordings. This may take some time to perform depending on the amount of media being exported.).",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }

        item { Spacer(modifier = Modifier.height(12.dp)) }

        item {
            val isRunning = backupState is ProgressState.Running
            Button(
                onClick = { backupLauncher.launch("reminera_backup.zip") },
                enabled = !isRunning && restoreState !is ProgressState.Running,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                if (isRunning) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Backing up...")
                } else {
                    Text("Backup and Export ALL Data", fontWeight = FontWeight.Bold)
                }
            }
        }

        item {
            AnimatedVisibility(
                visible = backupState is ProgressState.Success,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Text(
                    text = "Backup created successfully.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 8.dp).padding(top = 8.dp)
                )
            }
        }

        item {
            AnimatedVisibility(
                visible = backupState is ProgressState.Error,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Text(
                    text = (backupState as? ProgressState.Error)?.message ?: "",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 8.dp).padding(top = 8.dp)
                )
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }

        item {
            Text(
                text = "Restore Data",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }

        item {
            Text(
                text = "Import a previously exported backup. This will overwrite all current data. This may take some time depending on backup file size.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }

        item { Spacer(modifier = Modifier.height(12.dp)) }

        item {
            val isRunning = restoreState is ProgressState.Running
            Button(
                onClick = { restoreLauncher.launch(arrayOf("application/zip", "application/octet-stream")) },
                enabled = !isRunning && backupState !is ProgressState.Running,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                if (isRunning) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Restoring...")
                } else {
                    Text("Restore from Backup", fontWeight = FontWeight.Bold)
                }
            }
        }

        item {
            AnimatedVisibility(
                visible = restoreState is ProgressState.Success,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Text(
                    text = "Backup restored successfully. Restart the app to see changes.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 8.dp).padding(top = 8.dp)
                )
            }
        }

        item {
            AnimatedVisibility(
                visible = restoreState is ProgressState.Error,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Text(
                    text = (restoreState as? ProgressState.Error)?.message ?: "",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 8.dp).padding(top = 8.dp)
                )
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }

        item {
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }

        item {
            Text(
                text = "Danger Zone",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }

        item {
            Text(
                text = "Permanently erase all app data and reset to factory state.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }

        item { Spacer(modifier = Modifier.height(12.dp)) }

        item {
            val isRunning = deleteState is ProgressState.Running
            Button(
                onClick = {
                    deleteConfirmText = ""
                    showDeleteConfirm = true
                },
                enabled = !isRunning && backupState !is ProgressState.Running && restoreState !is ProgressState.Running,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Icon(
                    Icons.Default.DeleteForever,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Delete All Data", fontWeight = FontWeight.Bold)
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }

        item {
            Text(
                text = "Note: Backups include your database, media files, and app preferences. " +
                    "The API key stored in the secure keystore is not included in backups for security reasons.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp),
                textAlign = TextAlign.Start
            )
        }
    }
}
