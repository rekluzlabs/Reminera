package com.rekluzlabs.reminera.ui.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaItemMenuSheet(
    menuState: MediaMenuState,
    onDismiss: () -> Unit,
    onAction: (MediaAction) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showRenameDialog by remember { mutableStateOf(false) }
    var showMoveDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Text(
                text = menuState.entryTitle,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            val typeLabel = when (menuState.entryType) {
                "VIDEO" -> "video"
                "AUDIO" -> "audio"
                else -> "photo"
            }

            MediaMenuAction(
                icon = Icons.Default.Edit,
                label = "Rename",
                onClick = {
                    showRenameDialog = true
                }
            )

            MediaMenuAction(
                icon = Icons.Default.DriveFileMove,
                label = "Move to another member",
                onClick = {
                    showMoveDialog = true
                }
            )

            MediaMenuAction(
                icon = Icons.Default.CloudDownload,
                label = "Download to device",
                onClick = {
                    onAction(MediaAction.Download(menuState.entryId))
                    onDismiss()
                }
            )

            MediaMenuAction(
                icon = Icons.Default.Delete,
                label = "Delete",
                tint = MaterialTheme.colorScheme.error,
                onClick = {
                    showDeleteDialog = true
                }
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showRenameDialog) {
        RenameEntryDialog(
            currentTitle = menuState.entryTitle,
            onConfirm = { newTitle ->
                onAction(MediaAction.Rename(menuState.entryId, newTitle))
                showRenameDialog = false
                onDismiss()
            },
            onDismiss = { showRenameDialog = false }
        )
    }

    if (showMoveDialog) {
        MoveToMemberDialog(
            entryTitle = menuState.entryTitle,
            currentMemberName = menuState.currentMemberName,
            members = menuState.members,
            onConfirm = { member ->
                onAction(MediaAction.Move(menuState.entryId, member.id, member.name))
                showMoveDialog = false
                onDismiss()
            },
            onDismiss = { showMoveDialog = false }
        )
    }

    if (showDeleteDialog) {
        val typeLabel = when (menuState.entryType) {
            "VIDEO" -> "video"
            "AUDIO" -> "audio"
            else -> "photo"
        }
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete this $typeLabel?") },
            text = {
                Text("Delete this $typeLabel permanently? This cannot be undone.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onAction(MediaAction.Delete(menuState.entryId))
                        showDeleteDialog = false
                        onDismiss()
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun MediaMenuAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        androidx.compose.foundation.layout.Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Text(
                text = label,
                color = tint,
                fontSize = 15.sp,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun RenameEntryDialog(
    currentTitle: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf(currentTitle) }
    val isValid = title.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
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
                onClick = { onConfirm(title.trim()) },
                enabled = isValid
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun MoveToMemberDialog(
    entryTitle: String,
    currentMemberName: String?,
    members: List<com.rekluzlabs.reminera.data.FamilyMemberEntity>,
    onConfirm: (com.rekluzlabs.reminera.data.FamilyMemberEntity) -> Unit,
    onDismiss: () -> Unit
) {
    val otherMembers = remember(members, currentMemberName) {
        members.filter { it.name != currentMemberName }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Move \"$entryTitle\"") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    text = "Move to:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                if (otherMembers.isEmpty()) {
                    Text(
                        text = "No other members available.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    otherMembers.forEach { member ->
                        Surface(
                            onClick = { onConfirm(member) },
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                        ) {
                            Text(
                                text = member.name,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
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
