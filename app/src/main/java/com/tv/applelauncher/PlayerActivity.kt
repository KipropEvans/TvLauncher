package com.tv.applelauncher

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import com.tv.applelauncher.streaming.JellyfinApi
import com.tv.applelauncher.streaming.JellyfinSession
import kotlinx.coroutines.*
import java.util.concurrent.TimeUnit

class PlayerActivity : AppCompatActivity() {

    private var player: ExoPlayer? = null
    private var jellyfinItemId: String? = null
    private var lastReported = 0L
    private val scope = CoroutineScope(Dispatchers.Main + Job())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)

        jellyfinItemId = intent.getStringExtra("jellyfin_item_id")
        val url = intent.getStringExtra("stream_url") ?: return
        val resumeTicks = intent.getLongExtra("resume_ticks", 0L)

        player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(DefaultMediaSourceFactory(this))
            .build()
        findViewById<PlayerView>(R.id.player_view).player = player

        player?.setMediaItem(MediaItem.fromUri(Uri.parse(url)), resumeTicks / 10_000)
        player?.prepare()
        player?.playWhenReady = true

        // Auto-fallback to transcoding if direct play fails
        player?.addListener(object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                jellyfinItemId?.let { id ->
                    val hls = JellyfinApi.hlsUrl(
                        JellyfinSession.serverUrl, id, JellyfinSession.accessToken)
                    player?.setMediaItem(MediaItem.fromUri(Uri.parse(hls)))
                    player?.prepare()
                    player?.playWhenReady = true
                    Toast.makeText(this@PlayerActivity,
                        "Switched to transcoding…", Toast.LENGTH_SHORT).show()
                }
            }
        })

        // Periodic progress sync back to Jellyfin
        scope.launch {
            while (isActive) {
                delay(TimeUnit.SECONDS.toMillis(10))
                player?.let { p ->
                    if (p.isPlaying && jellyfinItemId != null &&
                        p.currentPosition - lastReported > 5_000) {
                        lastReported = p.currentPosition
                        withContext(Dispatchers.IO) {
                            runCatching {
                                JellyfinApi.create(JellyfinSession.serverUrl,
                                    JellyfinSession.accessToken)
                                    .reportProgress(jellyfinItemId!!,
                                        p.currentPosition * 10_000)
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onStop() {
        player?.let { p ->
            jellyfinItemId?.let { id ->
                scope.launch(Dispatchers.IO) {
                    runCatching {
                        JellyfinApi.create(JellyfinSession.serverUrl, JellyfinSession.accessToken)
                            .reportProgress(id, p.currentPosition * 10_000)
                    }
                }
            }
        }
        player?.release(); player = null
        super.onStop()
    }

    override fun onDestroy() { super.onDestroy(); scope.cancel() }
}