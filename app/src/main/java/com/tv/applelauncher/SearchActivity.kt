package com.tv.applelauncher

import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.tv.applelauncher.adapters.ShelfAdapter
import com.tv.applelauncher.api.TmdbService
import com.tv.applelauncher.models.ContentItem
import com.tv.applelauncher.models.Shelf
import kotlinx.coroutines.*

class SearchActivity : AppCompatActivity() {

    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var searchJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_shelf_page)

        val searchBox = findViewById<EditText>(R.id.search_box)
        searchBox.visibility = EditText.VISIBLE
        findViewById<TextView>(R.id.page_title).visibility = TextView.GONE
        searchBox.requestFocus()

        searchBox.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch(searchBox.text.toString()); true
            } else false
        }
        searchBox.setOnKeyListener { _, keyCode, event ->
            if (event.action == android.view.KeyEvent.ACTION_DOWN &&
                keyCode == android.view.KeyEvent.KEYCODE_DPAD_CENTER) {
                performSearch(searchBox.text.toString()); true
            } else false
        }
    }

    private fun performSearch(query: String) {
        if (query.isBlank()) return
        searchJob?.cancel()
        searchJob = scope.launch {
            try {
                val results = withContext(Dispatchers.IO) {
                    TmdbService.create().search(TmdbService.API_KEY, query).results
                }.map {
                    ContentItem(
                        id = it.id, title = it.displayTitle,
                        subtitle = String.format(java.util.Locale.US, "★ %.1f", it.vote_average ?: 0.0),
                        posterUrl = it.poster_path?.let { p -> TmdbService.IMG_BASE + "w500" + p },
                        backdropUrl = it.backdrop_path?.let { b -> TmdbService.IMG_BASE + "w1280" + b })
                }

                findViewById<RecyclerView>(R.id.page_shelves).apply {
                    layoutManager =
                        LinearLayoutManager(this@SearchActivity, LinearLayoutManager.VERTICAL, false)
                    adapter = ShelfAdapter(listOf(
                        Shelf("Results for \"$query\"", results, Shelf.POSTER))) { }
                }
            } catch (_: Exception) {}
        }
    }

    override fun onDestroy() { super.onDestroy(); scope.cancel() }
}