package com.spyshield.app.data

import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.os.HandlerThread
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray

/** App-wide state shared by the monitor service and the UI. Persisted in SharedPreferences. */
object Store {
    private const val MAX_EVENTS = 400
    private const val KEY_EVENTS = "events"
    private const val KEY_BLOCKED = "blocked"
    private const val KEY_PROTECTION = "protection"
    private const val SAVE_DELAY = 1_500L

    private lateinit var prefs: SharedPreferences

    // Updates within a session (alert checks, auto-block, end time) are written together, off the caller's thread.
    private val saver by lazy { Handler(HandlerThread("spyshield-store").apply { start() }.looper) }
    private val pendingSave = Runnable { saveEvents() }

    private val _events = MutableStateFlow<List<AccessEvent>>(emptyList())
    val events: StateFlow<List<AccessEvent>> = _events.asStateFlow()

    private val _blocked = MutableStateFlow<Set<String>>(emptySet())
    val blocked: StateFlow<Set<String>> = _blocked.asStateFlow()

    private val _protectionOn = MutableStateFlow(false)
    val protectionOn: StateFlow<Boolean> = _protectionOn.asStateFlow()

    private val _live = MutableStateFlow(LiveState())
    val live: StateFlow<LiveState> = _live.asStateFlow()

    fun init(context: Context) {
        prefs = context.getSharedPreferences("spyshield", Context.MODE_PRIVATE)
        _protectionOn.value = prefs.getBoolean(KEY_PROTECTION, false)
        _blocked.value = prefs.getStringSet(KEY_BLOCKED, emptySet())!!.toSet()
        _events.value = runCatching {
            val arr = JSONArray(prefs.getString(KEY_EVENTS, "[]"))
            // Sessions left open by a killed process are closed at their start time.
            List(arr.length()) { AccessEvent.fromJson(arr.getJSONObject(it)) }
                .map { if (it.end == null) it.copy(end = it.start) else it }
        }.getOrDefault(emptyList())
    }

    /** New events are written right away so the evidence survives the process being killed. */
    @Synchronized
    fun addEvent(event: AccessEvent) {
        _events.value = (listOf(event) + _events.value).take(MAX_EVENTS)
        saveEvents()
    }

    @Synchronized
    fun updateEvent(id: Long, transform: (AccessEvent) -> AccessEvent) {
        val old = event(id) ?: return
        // Nothing changed, so there's nothing to write.
        val new = transform(old)
        if (new == old) return
        _events.value = _events.value.map { if (it.id == id) new else it }
        scheduleSave()
    }

    @Synchronized
    fun removeEvent(id: Long) {
        _events.value = _events.value.filterNot { it.id == id }
        scheduleSave()
    }

    fun event(id: Long): AccessEvent? = _events.value.firstOrNull { it.id == id }

    @Synchronized
    fun clearHistory() {
        _events.value = _events.value.filter { it.end == null }
        saveEvents()
    }

    /** Writes any pending event changes now. */
    @Synchronized
    fun flush() {
        saveEvents()
    }

    @Synchronized
    fun setBlocked(pkg: String, blocked: Boolean) {
        _blocked.value = if (blocked) _blocked.value + pkg else _blocked.value - pkg
        prefs.edit().putStringSet(KEY_BLOCKED, _blocked.value).apply()
    }

    fun isBlocked(pkg: String?) = pkg != null && pkg in _blocked.value

    fun setProtection(on: Boolean) {
        _protectionOn.value = on
        prefs.edit().putBoolean(KEY_PROTECTION, on).apply()
    }

    fun setLive(sensor: Sensor, use: LiveUse?) {
        _live.value = if (sensor == Sensor.CAMERA) _live.value.copy(camera = use) else _live.value.copy(mic = use)
    }

    private fun scheduleSave() {
        saver.removeCallbacks(pendingSave)
        saver.postDelayed(pendingSave, SAVE_DELAY)
    }

    // Synchronized with the edits so a delayed save never writes an older list over a newer one.
    @Synchronized
    private fun saveEvents() {
        saver.removeCallbacks(pendingSave)
        val arr = JSONArray()
        _events.value.forEach { arr.put(it.toJson()) }
        prefs.edit().putString(KEY_EVENTS, arr.toString()).apply()
    }
}
