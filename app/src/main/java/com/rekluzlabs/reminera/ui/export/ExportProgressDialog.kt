package com.rekluzlabs.reminera.ui.export

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.rekluzlabs.reminera.export.ExportProgress
import com.rekluzlabs.reminera.export.ExportStage

@Composable
fun ExportProgressDialog(
    progress: ExportProgress,
    onDismissRequest: () -> Unit = {}
) {
    Dialog(onDismissRequest = onDismissRequest) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    text = when (progress.stage) {
                        ExportStage.RENDERING -> "Rendering Chapters..."
                        ExportStage.ASSEMBLING -> "Assembling Book..."
                        ExportStage.POLISHING -> "Polishing Content..."
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                LinearProgressIndicator(
                    progress = { progress.percentComplete / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                    strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (progress.stage == ExportStage.RENDERING) {
                        Text(
                            text = "Chapter ${progress.currentChapter} of ${progress.totalChapters}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            text = "Finalizing PDF...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    Text(
                        text = "${progress.percentComplete}%",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                progress.estimatedSecondsRemaining?.let { seconds ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = formatTimeRemaining(seconds),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

private fun formatTimeRemaining(seconds: Int): String {
    return when {
        seconds < 5 -> "Almost done..."
        seconds < 60 -> "About $seconds seconds remaining"
        else -> {
            val mins = seconds / 60
            val secs = seconds % 60
            "About $mins min ${secs}s remaining"
        }
    }
}
