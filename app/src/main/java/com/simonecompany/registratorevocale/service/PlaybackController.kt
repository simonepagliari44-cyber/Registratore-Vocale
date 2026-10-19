package com.simonecompany.registratorevocale.service

import android.content.Context
import android.media.MediaPlayer
import com.simonecompany.registratorevocale.model.Recording
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

object PlaybackController {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var player: MediaPlayer? = null
    private var tickerJob: Job? = null

    val current = MutableStateFlow<Recording?>(null)
    val isPlaying = MutableStateFlow(false)
    val positionMs = MutableStateFlow(0L)
    val durationMs = MutableStateFlow(0L)

    fun play(context: Context, recording: Recording) {
        val app = context.applicationContext
        if (current.value?.uri == recording.uri && player != null) {
            resume(app)
            return
        }
        releasePlayer()
        val newPlayer = MediaPlayer()
        val prepared = runCatching {
            newPlayer.setDataSource(app, recording.uri)
            newPlayer.prepare()
        }.isSuccess
        if (!prepared) {
            runCatching { newPlayer.release() }
            return
        }
        newPlayer.setOnCompletionListener {
            isPlaying.value = false
            positionMs.value = durationMs.value
            PlaybackService.refresh(app)
        }
        player = newPlayer
        current.value = recording
        durationMs.value = newPlayer.duration.toLong().coerceAtLeast(recording.durationMillis)
        positionMs.value = 0L
        newPlayer.start()
        isPlaying.value = true
        startTicker()
        PlaybackService.ensureStarted(app)
    }

    fun toggle(context: Context) {
        if (isPlaying.value) pause(context) else resume(context.applicationContext)
    }

    fun pause(context: Context) {
        val wasPlaying = isPlaying.value
        runCatching { if (player?.isPlaying == true) player?.pause() }
        isPlaying.value = false
        tickerJob?.cancel()
        if (wasPlaying) PlaybackService.refresh(context.applicationContext)
    }

    fun resume(context: Context) {
        val app = context.applicationContext
        val p = player ?: return
        runCatching {
            if (durationMs.value > 0 && positionMs.value >= durationMs.value) {
                p.seekTo(0)
                positionMs.value = 0L
            }
            p.start()
        }.onSuccess {
            isPlaying.value = true
            startTicker()
            PlaybackService.refresh(app)
        }
    }

    fun stop(context: Context) {
        releasePlayer()
        isPlaying.value = false
        positionMs.value = 0L
        durationMs.value = 0L
        current.value = null
        PlaybackService.stopSelf(context.applicationContext)
    }

    fun seekTo(ms: Long) {
        val p = player ?: return
        runCatching { p.seekTo(ms.toInt()) }
        positionMs.value = ms
    }

    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = scope.launch {
            while (isPlaying.value) {
                positionMs.value = runCatching {
                    player?.currentPosition?.toLong() ?: 0L
                }.getOrDefault(0L)
                delay(250)
            }
        }
    }

    private fun releasePlayer() {
        tickerJob?.cancel()
        runCatching { player?.stop() }
        runCatching { player?.release() }
        player = null
    }
}
