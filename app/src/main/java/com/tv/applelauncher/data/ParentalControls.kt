package com.tv.applelauncher.data

import android.content.Context

object ParentalControls {

    private fun prefs(context: Context) =
        context.getSharedPreferences("appletv_parental", Context.MODE_PRIVATE)

    /** 0=G, 1=PG, 2=PG-13/TV-14, 3=TV-MA */
    fun setMaxRating(ctx: Context, level: Int) =
        prefs(ctx).edit().putInt("max_rating", level).apply()

    fun getMaxRating(ctx: Context): Int = prefs(ctx).getInt("max_rating", 4)

    fun setPin(ctx: Context, pin: String) =
        prefs(ctx).edit().putString("pin", pin).apply()

    fun hasPin(ctx: Context) = !prefs(ctx).getString("pin", null).isNullOrBlank()

    fun verifyPin(ctx: Context, pin: String) = pin == prefs(ctx).getString("pin", null)

    fun isAllowed(ctx: Context, ratingLevel: Int): Boolean =
        ratingLevel <= getMaxRating(ctx)

    fun blockApp(ctx: Context, packageName: String) {
        val blocked = getBlockedApps(ctx).toMutableSet(); blocked.add(packageName)
        prefs(ctx).edit().putStringSet("blocked_apps", blocked).apply()
    }

    fun unblockApp(ctx: Context, packageName: String) {
        val blocked = getBlockedApps(ctx).toMutableSet(); blocked.remove(packageName)
        prefs(ctx).edit().putStringSet("blocked_apps", blocked).apply()
    }

    fun isAppBlocked(ctx: Context, packageName: String?) =
        packageName != null && packageName in getBlockedApps(ctx)

    fun getBlockedApps(ctx: Context): Set<String> =
        prefs(ctx).getStringSet("blocked_apps", emptySet()) ?: emptySet()
}