package com.tv.applelauncher.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

data class UserProfile(
    val id: String,
    val name: String,
    val avatarColor: Int,
    val isKidsProfile: Boolean = false,
    val pin: String? = null
)

object ProfileManager {

    private fun prefs(ctx: Context) =
        ctx.getSharedPreferences("appletv_profiles", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun getProfiles(ctx: Context): MutableList<UserProfile> {
        val json = prefs(ctx).getString("profiles", null) ?: return createDefaults(ctx)
        return runCatching {
            gson.fromJson(json, object : TypeToken<MutableList<UserProfile>>() {}.type)
        }.getOrNull() ?: createDefaults(ctx)
    }

    private fun createDefaults(ctx: Context): MutableList<UserProfile> {
        val defaults = mutableListOf(
            UserProfile("u1", "Main", 0xFF0071E3.toInt()),
            UserProfile("u2", "Kids", 0xFFFF9F0A.toInt(), isKidsProfile = true)
        )
        save(ctx, defaults); return defaults
    }

    fun save(ctx: Context, profiles: List<UserProfile>) =
        prefs(ctx).edit().putString("profiles", gson.toJson(profiles)).apply()

    fun addProfile(ctx: Context, name: String, color: Int, isKids: Boolean): UserProfile {
        val list = getProfiles(ctx)
        val profile = UserProfile("u${System.currentTimeMillis()}", name, color, isKids)
        list.add(profile); save(ctx, list); return profile
    }

    fun getCurrent(ctx: Context): UserProfile {
        val id = prefs(ctx).getString("current", "u1")
        return getProfiles(ctx).firstOrNull { it.id == id } ?: getProfiles(ctx)[0]
    }

    fun setCurrent(ctx: Context, id: String) =
        prefs(ctx).edit().putString("current", id).apply()
}