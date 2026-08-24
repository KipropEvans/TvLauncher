package com.tv.applelauncher

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.tv.applelauncher.adapters.ShelfAdapter
import com.tv.applelauncher.data.UpNextStore
import com.tv.applelauncher.models.Shelf
import com.tv.applelauncher.streaming.JellyfinApi
import com.tv.applelauncher.streaming.JellyfinSession
import com.tv.applelauncher.streaming.JfItem
import kotlinx.coroutines.*

class LibraryActivity : AppCompatActivity() {

    private val scope = CoroutineScope(Dispatchers.Main + Job())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_shelf_page)
        findViewById<TextView>(R.id.page_title).text = "Library"

        scope.launch {
            JellyfinSession.load(this@LibraryActivity)
            val shelves = mutableListOf<Shelf>()

            // Favorites (local)
            val favorites = UpNextStore.getAll(this@LibraryActivity,
                "favorites_${com.tv.applelauncher.data.ProfileManager.getCurrent(this@LibraryActivity).id}")
            if (favorites.isNotEmpty())
                shelves.add(Shelf("Favorites", favorites, Shelf.POSTER))

            // Jellyfin libraries
            if (JellyfinSession.isConnected()) {
                try {
                    val api = withContext(Dispatchers.IO) {
                        JellyfinApi.create(JellyfinSession.serverUrl, JellyfinSession.accessToken)
                    }
                    val uid = JellyfinSession.userId
                    val libs = withContext(Dispatchers.IO) { api.getLibraries(uid).Items }

                    libs.forEach { lib ->
                        val items = withContext(Dispatchers.IO) {
                            api.getItems(uid, lib.Id, limit = 100).Items
                        }
                        if (items.isNotEmpty()) shelves.add(
                            Shelf(lib.Name, items.map { jf -> jf.toContent() }, Shelf.POSTER))
                    }
                } catch (_: Exception) {}
            }

            findViewById<RecyclerView>(R.id.page_shelves).apply {
                layoutManager =
                    LinearLayoutManager(this@LibraryActivity, LinearLayoutManager.VERTICAL, false)
                adapter = ShelfAdapter(shelves) { }
            }
        }
    }

    private fun JfItem.toContent() = com.tv.applelauncher.models.ContentItem(
        id = Id.hashCode().toLong(),
        title = SeriesName ?: Name,
        subtitle = listOfNotNull(
            ProductionYear?.toString(), OfficialRating,
            CommunityRating?.let { "★ ${String.format("%.1f", it)}" }).joinToString(" • "),
        posterUrl = JellyfinApi.imageUrl(JellyfinSession.serverUrl, this, "Primary"),
        backdropUrl = JellyfinApi.imageUrl(JellyfinSession.serverUrl, this, "Backdrop", 1280),
        jellyfinId = Id,
        isSeries = Type == "Series"
    )

    override fun onDestroy() { super.onDestroy(); scope.cancel() }
}