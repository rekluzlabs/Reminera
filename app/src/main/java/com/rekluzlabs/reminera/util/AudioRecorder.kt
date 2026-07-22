package com.rekluzlabs.reminera.util

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import java.io.File
import java.io.FileOutputStream

class AudioRecorder(private val context: Context) {

    private var recorder: MediaRecorder? = null
    private var isRecording = false

    fun start(outputFile: File) {
        if (recorder != null) stop()

        val r = createRecorder()
        try {
            r.setAudioSource(MediaRecorder.AudioSource.MIC)
            r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            r.setOutputFile(FileOutputStream(outputFile).fd)

            r.prepare()
            r.start()

            recorder = r
            isRecording = true
        } catch (e: Exception) {
            r.release()
            recorder = null
            isRecording = false
            throw e
        }
    }

    fun stop() {
        if (!isRecording) return
        isRecording = false
        try {
            recorder?.stop()
        } catch (_: RuntimeException) {
            // Stopped abruptly or never fully started — safe to ignore
        } finally {
            recorder?.release()
            recorder = null
        }
    }

    private fun createRecorder(): MediaRecorder {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }
    }
}
