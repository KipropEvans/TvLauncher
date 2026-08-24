package com.tv.applelauncher.streaming

import android.content.Context

object JellyfinSession {
    var serverUrl: String = ""
    var accessToken: String = ""
    var userId: String = ""

    private fun prefs(ctx: Context) =
        ctx.getSharedPreferences("jellyfin", Context.MODE_PRIVATE)

    fun save(ctx: Context) = prefs(ctx).edit()
        .putString("url", serverUrl)
        .putString("token", accessToken)
        .putString("user", userId).apply()

    fun load(ctx: Context) {
        serverUrl = prefs(ctx).getString("url", "") ?: ""
        accessToken = prefs(ctx).getString("token", "") ?: ""
        userId = prefs(ctx).getString("user", "") ?: ""
    }

    fun isConnected() = serverUrl.isNotBlank() && accessToken.isNotBlank()
}