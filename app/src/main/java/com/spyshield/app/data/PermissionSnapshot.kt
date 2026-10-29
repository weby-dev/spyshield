package com.spyshield.app.data

import android.content.Context
import org.json.JSONObject

/** An app whose camera or mic access changed since the previous scan. */
data class PermissionChange(
    val packageName: String,
    val label: String,
    val sensor: Sensor,
    val gained: Boolean,
)

/**
 * Remembers which apps held camera/mic access at the last scan, so we can tell the user
 * when an app quietly picks up a new permission. The first run only records a baseline.
 */
object PermissionSnapshot {
    private const val PREFS = "spyshield_perm_snapshot"
    private const val KEY_SNAPSHOT = "snapshot"

    private data class Grants(val cam: Boolean, val mic: Boolean)

    @Synchronized
    fun diff(context: Context, apps: List<AppPrivacyInfo>): List<PermissionChange> {
        // An empty list means the scan failed; don't overwrite a good snapshot with it.
        if (apps.isEmpty()) return emptyList()
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val previous = prefs.getString(KEY_SNAPSHOT, null)?.let(::parse)
        val current = apps.associate { it.packageName to Grants(it.cameraGranted, it.micGranted) }
        // Usually nothing changed, so skip rewriting the same snapshot.
        if (previous != current) prefs.edit().putString(KEY_SNAPSHOT, toJson(current)).apply()
        if (previous == null) return emptyList()

        val changes = mutableListOf<PermissionChange>()
        for (app in apps) {
            // Apps installed since the last scan count as gaining whatever they now hold.
            val before = previous[app.packageName] ?: Grants(cam = false, mic = false)
            if (app.cameraGranted != before.cam) changes += PermissionChange(app.packageName, app.label, Sensor.CAMERA, app.cameraGranted)
            if (app.micGranted != before.mic) changes += PermissionChange(app.packageName, app.label, Sensor.MIC, app.micGranted)
        }
        return changes
    }

    private fun toJson(map: Map<String, Grants>): String {
        val o = JSONObject()
        map.forEach { (pkg, g) -> o.put(pkg, JSONObject().put("cam", g.cam).put("mic", g.mic)) }
        return o.toString()
    }

    private fun parse(json: String): Map<String, Grants>? = runCatching {
        val o = JSONObject(json)
        o.keys().asSequence().associateWith { pkg ->
            val g = o.getJSONObject(pkg)
            Grants(g.optBoolean("cam"), g.optBoolean("mic"))
        }
    }.getOrNull()
}
