package com.example.sayobotdownloader.ui.detail

import android.media.MediaPlayer

class AudioPreviewPlayer {
    private var player: MediaPlayer? = null

    fun play(sid: Int, onReady: () -> Unit = {}, onError: (String) -> Unit = {}) {
        release()
        player = MediaPlayer().apply {
            setDataSource("https://cdnx.sayobot.cn:25225/preview/$sid.mp3")
            setOnPreparedListener { start(); onReady() }
            setOnErrorListener { _, what, extra -> onError("Audio error: $what/$extra"); true }
            prepareAsync()
        }
    }

    fun toggle(): Boolean {
        player?.let {
            if (it.isPlaying) { it.pause(); return false }
            else { it.start(); return true }
        }
        return false
    }

    val isPlaying get() = player?.isPlaying == true
    val currentPosition get() = player?.currentPosition ?: 0
    val duration get() = player?.duration ?: 0

    fun seekTo(positionMs: Int) { player?.seekTo(positionMs) }
    fun release() { player?.release(); player = null }
}