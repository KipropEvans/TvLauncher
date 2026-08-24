package com.tv.applelauncher

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.tv.applelauncher.adapters.ShelfAdapter
import com.tv.applelauncher.api.TmdbService
import com.tv.applelauncher.models.ContentItem
import com.tv.applelauncher.models.Shelf
import kotlinx.coroutines.*
import java.util.Locale

class StoreActivity : AppCompatActivity() {

    companion object {
        fun openDetail(ctx: android.content.Context, item: ContentItem) {
            ctx.startActivity(Intent(ctx, DetailActivity::class.java).putExtra("item", item))
        }
    }

    private val scope = CoroutineScope(Dispatchers.Main + Job())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_shelf_page)
        findViewById<TextView>(R.id.page_title).text = "Store"

        scope.launch {
            try {
                val api = TmdbService.create()
                val trending = withContext(Dispatchers.IO) { api.trending(TmdbService.API_KEY).results }
                val topRated = withContext(Dispatchers.IO) { api.topRated(TmdbService.API_KEY).results }

                fun toContent(list: List<com.tv.applelauncher.models.TmdbItem>) =
                    list.map {
                        ContentItem(
                            id = it.id,
                            title = it.displayTitle,
                            subtitle = String.format(Locale.US, "★ %.1f", it.vote_average ?: 0.0),
                            posterUrl = it.poster_path?.let { p -> TmdbService.IMG_BASE + "w500" + p },
                            backdropUrl = it.backdrop_path?.let { b -> TmdbService.IMG_BASE + "w1280" + b }
                        )
                    }

                findViewById<RecyclerView>(R.id.page_shelves).apply {
                    layoutManager =
                        LinearLayoutManager(this@StoreActivity, LinearLayoutManager.VERTICAL, false)
                    adapter = ShelfAdapter(listOf(
                        Shelf("Trending Today", toContent(trending), Shelf.POSTER),
                        Shelf("Top Rated", toContent(topRated), Shelf.POSTER)
                    )) { }
                }
            } catch (_: Exception) {}
        }
    }

    override fun onDestroy() { super.onDestroy(); scope.cancel() }
}