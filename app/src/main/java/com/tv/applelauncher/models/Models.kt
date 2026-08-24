package com.tv.applelauncher.models

import java.io.Serializable

data class ContentItem(
    val id: Long,
    val title: String,
    val subtitle: String = "",
    val posterUrl: String? = null,
    val backdropUrl: String? = null,
    val isApp: Boolean = false,
    val launchIntent: android.content.Intent? = null,
    val jellyfinId: String? = null,
    val isSeries: Boolean = false,
    val ratingLevel: Int = 4,
    val localUri: String? = null
) : Serializable

// ---- TMDB ----
data class TmdbResponse(val results: List<TmdbItem>)
data class TmdbItem(
    val id: Long,
    val title: String?,
    val name: String?,
    val overview: String?,
    val poster_path: String?,
    val backdrop_path: String?,
    val vote_average: Double?
) {
    val displayTitle get() = title ?: name ?: ""
}

// ---- Shelf model used by adapters ----
data class Shelf(val title: String, val items: List<ContentItem>, val style: Int) {
    companion object { const val WIDE = 0; const val POSTER = 1; const val SQUARE = 2 }
}