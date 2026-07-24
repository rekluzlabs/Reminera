package com.rekluzlabs.reminera.util

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private const val TAG = "PlaybackManager"

class PlaybackManager(private val context: Context) {

    private var exoPlayer: ExoPlayer? = null

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPosition = MutableStateFlow(0f)
    val currentPosition: StateFlow<Float> = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0f)
    val duration: StateFlow<Float> = _duration.asStateFlow()

    private val _isRepeat = MutableStateFlow(false)
    val isRepeat: StateFlow<Boolean> = _isRepeat.asStateFlow()

    private val _isPrepared = MutableStateFlow(false)
    val isPrepared: StateFlow<Boolean> = _isPrepared.asStateFlow()

    private val _playerError = MutableStateFlow<String?>(null)
    val playerError: StateFlow<String?> = _playerError.asStateFlow()

    private var positionUpdateJob: Job? = null

    fun getOrCreatePlayer(): ExoPlayer {
        return exoPlayer ?: ExoPlayer.Builder(context).build().also { player ->
            exoPlayer = player
            player.addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    _isPlaying.value = isPlaying
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    when (playbackState) {
                        Player.STATE_READY -> {
                            _isPrepared.value = true
                            _playerError.value = null
                            _duration.value = player.duration.toFloat()
                        }
                        Player.STATE_ENDED -> {
                            _isPlaying.value = false
                            if (_isRepeat.value) {
                                player.seekTo(0)
                                player.play()
                            } else {
                                _currentPosition.value = 0f
                            }
                        }
                        Player.STATE_IDLE -> {
                            _isPrepared.value = false
                        }
                        Player.STATE_BUFFERING -> {
                            _playerError.value = null
                        }
                    }
                }

                override fun onPlayerError(error: PlaybackException) {
                    Log.e(TAG, "Playback error: ${error.message}", error)
                    _playerError.value = error.message ?: "Playback failed"
                    _isPlaying.value = false
                    _isPrepared.value = false
                }
            })
        }
    }

    fun prepareAndPlay(uri: Uri) {
        _playerError.value = null
        val player = getOrCreatePlayer()
        val mediaItem = MediaItem.fromUri(uri)
        player.setMediaItem(mediaItem)
        player.prepare()
        player.play()
        startPositionUpdates()
    }

    fun togglePlayPause() {
        val player = exoPlayer ?: return
        if (player.isPlaying) {
            player.pause()
        } else {
            player.play()
            startPositionUpdates()
        }
    }

    fun pause() {
        exoPlayer?.pause()
    }

    fun seekTo(positionMs: Long) {
        exoPlayer?.seekTo(positionMs)
    }

    fun seekRelative(deltaMs: Long) {
        val player = exoPlayer ?: return
        val newPos = (player.currentPosition + deltaMs).coerceIn(0, player.duration)
        player.seekTo(newPos)
    }

    fun toggleRepeat() {
        _isRepeat.value = !_isRepeat.value
        exoPlayer?.repeatMode = if (_isRepeat.value) Player.REPEAT_MODE_ALL else Player.REPEAT_MODE_OFF
    }

    fun stop() {
        exoPlayer?.let { player ->
            player.seekTo(0)
            player.pause()
            _isPlaying.value = false
            _currentPosition.value = 0f
        }
    }

    fun release() {
        positionUpdateJob?.cancel()
        exoPlayer?.release()
        exoPlayer = null
        _isPlaying.value = false
        _currentPosition.value = 0f
        _duration.value = 0f
        _isPrepared.value = false
        _playerError.value = null
    }

    fun currentPositionMs(): Long = exoPlayer?.currentPosition ?: 0L
    fun durationMs(): Long = exoPlayer?.duration ?: 0L

    private fun startPositionUpdates() {
        positionUpdateJob?.cancel()
        val player = exoPlayer ?: return
        positionUpdateJob = CoroutineScope(Dispatchers.Main).launch {
            while (isActive && _isPlaying.value) {
                val dur = player.duration
                if (dur > 0) {
                    _currentPosition.value = player.currentPosition.toFloat() / dur.toFloat()
                }
                delay(250)
            }
        }
    }

    companion object {
        private val instances = mutableMapOf<String, PlaybackManager>()

        fun getInstance(context: Context, key: String = "default"): PlaybackManager {
            return instances.getOrPut(key) {
                PlaybackManager(context.applicationContext)
            }
        }

        fun release(key: String) {
            instances.remove(key)?.release()
        }

        fun releaseAll() {
            instances.values.forEach { it.release() }
            instances.clear()
        }
    }
}