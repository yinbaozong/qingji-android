package com.dreamjournal.app.data.repository

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class AudioRecorderManager(private val context: Context) {
    private var recorder: MediaRecorder? = null
    private var currentFile: File? = null
    private var startedAtMillis: Long? = null

    fun start(): Result<Unit> = runCatching {
        stopInternal()
        val file = createOutputFile(context)
        val mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }
        mediaRecorder.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.AMR_NB)
            setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
            setOutputFile(file.absolutePath)
            prepare()
            start()
        }
        recorder = mediaRecorder
        currentFile = file
        startedAtMillis = System.currentTimeMillis()
    }

    fun stop(): Result<String?> = runCatching {
        val filePath = currentFile?.absolutePath
        recorder?.apply {
            stop()
            reset()
            release()
        }
        recorder = null
        currentFile = null
        startedAtMillis = null
        filePath
    }

    fun cancel() {
        stopInternal()
    }

    fun currentAmplitude(): Int {
        return runCatching { recorder?.maxAmplitude ?: 0 }.getOrDefault(0)
    }

    fun currentDurationSeconds(): Int {
        val start = startedAtMillis ?: return 0
        return ((System.currentTimeMillis() - start) / 1000L).toInt()
    }

    private fun stopInternal() {
        runCatching {
            recorder?.apply {
                stop()
                reset()
                release()
            }
        }
        recorder = null
        currentFile = null
        startedAtMillis = null
    }

    private fun createOutputFile(context: Context): File {
        val dir = File(context.filesDir, "recordings")
        if (!dir.exists()) dir.mkdirs()
        val time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
        return File(dir, "dream_$time.amr")
    }
}
