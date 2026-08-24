package com.tv.applelauncher.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.tv.applelauncher.models.ContentItem

object UpNextStore {

    private fun prefs(ctx: Context) = ctx.getSharedPreferences("appletv", Context.MODE_PRIVATE)

    fun saveProgress(ctx: Context, item: ContentItem, positionMs: Long, durationMs: Long) {
        val list = getAll(ctx).filterNot { it.id == item.id }.toMutableList()
        list.add(0, item.copy(subtitle = formatProgress(positionMs, durationMs)))
        put(ctx, key(ctx), list.take(20))
    }

    fun addToUpNext(ctx: Context, item: ContentItem) {
        val list = getAll(ctx).filterNot { it.id == item.id }.toMutableList()
        list.add(0, item); put(ctx, key(ctx), list.take(20))
    }

    fun toggleFavorite(ctx: Context, item: ContentItem): Boolean {
        val list = getAll(ctx, favKey(ctx)).toMutableList()
        val exists = list.any { it.id == item.id }
        if (exists) list.removeAll { it.id == item.id } else list.add(0, item)
        put(ctx, favKey(ctx), list)
        return !exists
    }

    fun isFavorite(ctx: Context, item: ContentItem) =
        getAll(ctx, favKey(ctx)).any { it.id == item.id }

    fun getAll(ctx: Context, key: String? = null): List<ContentItem> {
        val json = prefs(ctx).getString(key ?: key(ctx), "[]") ?: "[]"
        return runCatching {
            Gson().fromJson<List<ContentItem>>(json, object : TypeToken<List<ContentItem>>() {}.type)
        }.getOrNull() ?: emptyList()
    }

    private fun put(ctx: Context, k: String, items: List<ContentItem>) =
        prefs(ctx).edit().putString(k, Gson().toJson(items)).apply()

    private fun key(ctx: Context) = "up_next_${ProfileManager.getCurrent(ctx).id}"
    private fun favKey(ctx: Context) = "favorites_${ProfileManager.getCurrent(ctx).id}"

    private fun formatProgress(pos: Long, dur: Long): String {
        if (dur <= 0) return ""
        val minsLeft = (dur - pos) / 60000
        return if (minsLeft > 0) "$minsLeft min left" else "Finished"
    }
}