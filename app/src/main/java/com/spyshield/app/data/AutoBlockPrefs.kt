package com.spyshield.app.data

import android.content.Context

/** Whether confident spy alerts should block the app and sound the alarm. Off by default. */
object AutoBlockPrefs {
    private const val PREFS = "spyshield_autoblock"
    private const val KEY_ENABLED = "enabled"

    fun isEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_ENABLED, false)

    fun setEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY_ENABLED, enabled).apply()
    }
}
