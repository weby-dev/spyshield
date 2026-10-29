package com.spyshield.app.data

import android.content.Context

/** Whether the user wants the on-screen privacy dot. Off by default; it also needs the overlay permission. */
object OverlayPrefs {
    private const val PREFS = "spyshield_overlay"
    private const val KEY_ENABLED = "enabled"

    fun isEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_ENABLED, false)

    fun setEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY_ENABLED, enabled).apply()
    }
}
