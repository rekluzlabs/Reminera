package com.rekluzlabs.reminera.export

import android.content.Context
import android.os.PowerManager
import com.rekluzlabs.reminera.data.BookExportManifestDao
import com.rekluzlabs.reminera.data.FamilyMemberDao
import com.rekluzlabs.reminera.data.repository.ChapterRenderCacheRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

data class ExportProgress(
    val currentChapter: Int,
    val totalChapters: Int,
    val stage: ExportStage,
    val percentComplete: Int,
    val estimatedSecondsRemaining: Int?
)

enum class ExportStage { RENDERING, ASSEMBLING, POLISHING }

class PdfExportManager(
    private val context: Context,
    private val manifestDao: BookExportManifestDao,
    private val memberDao: FamilyMemberDao,
    private val renderCacheRepository: ChapterRenderCacheRepository
) {
    private val _progress = MutableStateFlow<ExportProgress?>(null)
    val progress: StateFlow<ExportProgress?> = _progress.asStateFlow()

    private var wakeLock: PowerManager.WakeLock? = null
    private val chapterDurations = mutableListOf<Long>()

    private val stageWeights = mapOf(
        ExportStage.RENDERING to 0.8f,
        ExportStage.ASSEMBLING to 0.2f,
        ExportStage.POLISHING to 0.0f // Not implemented yet
    )

    private fun startExportWakeLock() {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "Reminera::ExportWakeLock"
        ).apply {
            acquire(10 * 60 * 1000L /*10 min safety timeout*/)
        }
    }

    private fun releaseExportWakeLock() {
        wakeLock?.let { if (it.isHeld) it.release() }
        wakeLock = null
    }

    suspend fun exportBook(manifestId: Long): BookAssembler.AssembleResult {
        startExportWakeLock()
        chapterDurations.clear()
        
        return try {
            BookAssembler.assembleBook(
                context = context,
                manifestId = manifestId,
                manifestDao = manifestDao,
                memberDao = memberDao,
                renderCacheRepository = renderCacheRepository,
                onProgress = { current: Int, total: Int, stage: ExportStage, chapterElapsed: Long? ->
                    if (chapterElapsed != null) {
                        chapterDurations.add(chapterElapsed)
                    }
                    
                    val stageProgress = if (total > 0) current.toFloat() / total else 0f
                    
                    var cumulativePercent = 0f
                    for ((s, weight) in stageWeights) {
                        if (s == stage) {
                            cumulativePercent += stageProgress * weight
                            break
                        } else {
                            cumulativePercent += weight
                        }
                    }

                    val avgMs = if (chapterDurations.isNotEmpty()) chapterDurations.average() else null
                    val remainingSeconds = if (avgMs != null && stage == ExportStage.RENDERING) {
                        ((total - current) * avgMs / 1000).toInt()
                    } else null

                    _progress.value = ExportProgress(
                        currentChapter = current,
                        totalChapters = total,
                        stage = stage,
                        percentComplete = (cumulativePercent * 100).toInt().coerceIn(0, 100),
                        estimatedSecondsRemaining = remainingSeconds
                    )
                }
            )
        } catch (e: Exception) {
            BookAssembler.AssembleResult.Failure(e.message ?: "Unknown error")
        } finally {
            releaseExportWakeLock()
            _progress.value = null
        }
    }
}
