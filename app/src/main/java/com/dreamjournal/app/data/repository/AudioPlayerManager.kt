package com.dreamjournal.app.data.repository

import android.media.MediaPlayer

class AudioPlayerManager {
    private var mediaPlayer: MediaPlayer? = null

    fun play(path: String, onCompleted: () -> Unit): Result<Unit> = runCatching {
        stop()
        val player = MediaPlayer().apply {
            setDataSource(path)
            prepare()
            setOnCompletionListener {
                onCompleted()
                stop()
            }
            start()
        }
        mediaPlayer = player
    }

    fun stop() {
        runCatching {
            mediaPlayer?.stop()
            mediaPlayer?.reset()
            mediaPlayer?.release()
        }
        mediaPlayer = null
    }
}
