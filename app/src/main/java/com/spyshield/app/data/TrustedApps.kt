package com.spyshield.app.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Apps the user vouched for: their camera/mic use is still logged, but never raises an alert. */
object TrustedApps {
    private const val KEY_TRUSTED = "trusted"

    private var prefs: SharedPreferences? = null

    private val _trusted = MutableStateFlow<Set<String>>(emptySet())
    val trusted: StateFlow<Set<String>> = _trusted.asStateFlow()

    fun init(context: Context) {
        val p = context.getSharedPreferences("spyshield_trusted", Context.MODE_PRIVATE)
        prefs = p
        _trusted.value = p.getStringSet(KEY_TRUSTED, emptySet())?.toSet() ?: emptySet()
    }

    fun isTrusted(pkg: String?) = pkg != null && pkg in _trusted.value

    @Synchronized
    fun setTrusted(pkg: String, trusted: Boolean) {
        _trusted.value = if (trusted) _trusted.value + pkg else _trusted.value - pkg
        prefs?.edit()?.putStringSet(KEY_TRUSTED, _trusted.value)?.apply()
    }
}
