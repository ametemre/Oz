package com.can.oz

import android.content.Context
import android.media.MediaPlayer
import java.io.File

class Player(private val context: Context) {
    private var mediaPlayer: MediaPlayer? = null

    fun play(file: File) {
        mediaPlayer?.release()
        mediaPlayer = MediaPlayer().apply {
            setDataSource(file.absolutePath)
            prepare()
            start()
        }
    }

    fun stop() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
    }
}
