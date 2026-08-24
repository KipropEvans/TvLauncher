package com.tv.applelauncher

import android.os.Handler
import android.os.Looper
import android.service.dreams.DreamService
import android.widget.FrameLayout
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.util.concurrent.Executors

/**
 * Apple-TV-style aerial screensaver.
 * Pulls real aerial video URLs from a community-maintained mirror
 * of Apple's own aerial screensaver manifest and plays them with ExoPlayer.
 */
class AerialDreamService : DreamService() {

    private val handler = Handler(Looper.getMainLooper())
    private val executor = Executors.newSingleThreadExecutor()
    private var player: ExoPlayer? = null
    private var playerView: PlayerView? = null
    private var videoUrls: List<String> = emptyList()
    private var index = 0

    // Community mirror of Apple's aerial screensaver manifest (kopiro/xscreensaver-apple-aerial)
    private val manifestUrl =
        "https://raw.githubusercontent.com/kopiro/xscreensaver-apple-aerial/main/entries.json"

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        isInteractive = false
        fullscreen = true

        playerView = PlayerView(this).apply {
            useController = false
        }
        setContentView(FrameLayout(this).apply { addView(playerView) })

        fetchManifestAndStart()
    }

    private fun fetchManifestAndStart() {
        executor.execute {
            try {
                val client = OkHttpClient()
                val request = Request.Builder().url(manifestUrl).build()
                val response = client.newCall(request).execute()
                val body = response.body?.string() ?: return@execute
                val entries = JSONArray(body)
                val urls = mutableListOf<String>()

                for (i in 0 until entries.length()) {
                    val entry = entries.getJSONObject(i)
                    // Prefer 1080p H264 for compatibility, fall back to any available key
                    val url = entry.optString("url-1080-H264").ifBlank {
                        entry.optString("url-1080-SDR")
                    }
                    if (url.isNotBlank()) urls.add(url)
                }

                videoUrls = urls.shuffled()
                handler.post { startPlayback() }
            } catch (e: Exception) {
                // Network unavailable or manifest changed shape — fail silently, screensaver stays black
            }
        }
    }

    private fun startPlayback() {
        if (videoUrls.isEmpty()) return

        player = ExoPlayer.Builder(this).build().also { exo ->
            playerView?.player = exo
            exo.addListener(object : androidx.media3.common.Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    if (state == androidx.media3.common.Player.STATE_ENDED) {
                        nextVideo()
                    }
                }
            })
        }
        playCurrent()
    }

    private fun playCurrent() {
        val url = videoUrls[index % videoUrls.size]
        player?.setMediaItem(MediaItem.fromUri(url))
        player?.prepare()
        player?.playWhenReady = true
    }

    private fun nextVideo() {
        index++
        playCurrent()
    }

    override fun onDetachedFromWindow() {
        handler.removeCallbacksAndMessages(null)
        player?.release()
        player = null
        super.onDetachedFromWindow()
    }
}