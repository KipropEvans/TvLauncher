package com.tv.applelauncher

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.tv.applelauncher.adapters.JellyfinEpisodeAdapter
import com.tv.applelauncher.data.UpNextStore
import com.tv.applelauncher.models.ContentItem
import com.tv.applelauncher.streaming.JellyfinApi
import com.tv.applelauncher.streaming.JellyfinSession
import kotlinx.coroutines.*

class DetailActivity : AppCompatActivity() {

    private val scope = CoroutineScope(Dispatchers.Main + Job())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail)
        val item = intent.getSerializableExtra("item") as? ContentItem ?: return

        renderBasic(item)

        if (item.jellyfinId != null && JellyfinSession.isConnected()) loadFromJellyfin(item)
    }

    private fun renderBasic(item: ContentItem) {
        Glide.with(this).load(item.backdropUrl ?: item.posterUrl)
            .centerCrop().into(findViewById(R.id.detail_backdrop))
        Glide.with(this).load(item.posterUrl ?: item.backdropUrl)
            .centerCrop().into(findViewById(R.id.detail_poster))

        findViewById<TextView>(R.id.detail_title).text = item.title
        findViewById<TextView>(R.id.detail_meta).text = item.subtitle
        findViewById<TextView>(R.id.detail_description).text = item.subtitle

        val playBtn = findViewById<Button>(R.id.btn_play)
        val addBtn = findViewById<Button>(R.id.btn_add)

        addBtn.setOnClickListener {
            val added = UpNextStore.toggleFavorite(this, item)
            addBtn.text = if (added) "✓ In Up Next" else "+ Add to Up Next"
        }

        // Default play (local URI or fallback); replaced by Jellyfin flow below
        playBtn.setOnClickListener {
            startActivity(Intent(this, PlayerActivity::class.java).apply {
                putExtra("title", item.title)
                putExtra("stream_url", item.localUri ?: item.backdropUrl)
            })
        }
    }

    private fun loadFromJellyfin(item: ContentItem) {
        scope.launch {
            try {
                val api = withContext(Dispatchers.IO) {
                    JellyfinApi.create(JellyfinSession.serverUrl, JellyfinSession.accessToken)
                }
                val uid = JellyfinSession.userId

                if (item.isSeries) {
                    val episodes = withContext(Dispatchers.IO) {
                        api.getEpisodes(item.jellyfinId!!, uid).Items
                    }
                    findViewById<RecyclerView>(R.id.episodes_list).apply {
                        layoutManager = LinearLayoutManager(this@DetailActivity)
                        adapter = JellyfinEpisodeAdapter(episodes)
                    }
                } else {
                    findViewById<Button>(R.id.btn_play).setOnClickListener {
                        launchPlayer(item.jellyfinId!!, item.title, 0L)
                    }
                }
            } catch (_: Exception) {}
        }
    }

    private fun launchPlayer(itemId: String, title: String, resumeTicks: Long) {
        startActivity(Intent(this, PlayerActivity::class.java).apply {
            putExtra("title", title)
            putExtra("jellyfin_item_id", itemId)
            putExtra("stream_url", JellyfinApi.streamUrl(
                JellyfinSession.serverUrl, itemId, JellyfinSession.accessToken))
            putExtra("resume_ticks", resumeTicks)
        })
    }

    override fun onDestroy() { super.onDestroy(); scope.cancel() }
}