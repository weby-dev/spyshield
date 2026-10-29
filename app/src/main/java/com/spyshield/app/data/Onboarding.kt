package com.spyshield.app.data

import android.content.Context

/** Remembers whether the first-run walkthrough has been completed. Kept apart from Store on purpose. */
object Onboarding {
    private const val PREFS = "spyshield_onboarding"
    private const val KEY_DONE = "done"

    fun isDone(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_DONE, false)

    fun markDone(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY_DONE, true).apply()
    }
}
