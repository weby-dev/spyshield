package com.spyshield.app.data

import android.Manifest
import org.json.JSONArray
import org.json.JSONObject

enum class Sensor(val permission: String, val noun: String, val title: String) {
    CAMERA(Manifest.permission.CAMERA, "camera", "Camera"),
    MIC(Manifest.permission.RECORD_AUDIO, "microphone", "Microphone"),
}

/** One period during which the camera or microphone was busy. `end == null` means still in use. */
data class AccessEvent(
    val id: Long,
    val sensor: Sensor,
    val packageName: String?,
    val appLabel: String,
    val start: Long,
    val end: Long?,
    val suspicious: Boolean,
    val reason: String,
    val suspects: List<String> = emptyList(),
    val blocked: Boolean = false,
    /** The attribution named one app with confidence (see [com.spyshield.app.monitor.Attribution.confident]). */
    val confident: Boolean = false,
    /** Auto-block put this app on the blocklist when the alert fired. */
    val autoBlocked: Boolean = false,
) {
    fun toJson(): JSONObject = JSONObject()
        .put("id", id)
        .put("sensor", sensor.name)
        .put("pkg", packageName ?: "")
        .put("label", appLabel)
        .put("start", start)
        .put("end", end ?: -1L)
        .put("suspicious", suspicious)
        .put("reason", reason)
        .put("suspects", JSONArray(suspects))
        .put("blocked", blocked)
        .put("confident", confident)
        .put("autoBlocked", autoBlocked)

    companion object {
        fun fromJson(o: JSONObject): AccessEvent {
            val suspects = o.optJSONArray("suspects")
            return AccessEvent(
                id = o.getLong("id"),
                sensor = Sensor.valueOf(o.getString("sensor")),
                packageName = o.optString("pkg").ifEmpty { null },
                appLabel = o.optString("label"),
                start = o.getLong("start"),
                end = o.optLong("end", -1L).takeIf { it >= 0 },
                suspicious = o.optBoolean("suspicious"),
                reason = o.optString("reason"),
                suspects = List(suspects?.length() ?: 0) { suspects!!.getString(it) },
                blocked = o.optBoolean("blocked"),
                confident = o.optBoolean("confident"),
                autoBlocked = o.optBoolean("autoBlocked"),
            )
        }
    }
}

data class LiveUse(
    val eventId: Long,
    val label: String,
    val packageName: String?,
    val suspicious: Boolean,
    val since: Long,
)

data class LiveState(val camera: LiveUse? = null, val mic: LiveUse? = null) {
    fun of(sensor: Sensor) = if (sensor == Sensor.CAMERA) camera else mic
}

enum class RiskLevel(val title: String) { LOW("Low"), MEDIUM("Medium"), HIGH("High") }

data class AppPrivacyInfo(
    val packageName: String,
    val label: String,
    val isSystem: Boolean,
    val cameraRequested: Boolean,
    val cameraGranted: Boolean,
    val micRequested: Boolean,
    val micGranted: Boolean,
    val sideloaded: Boolean,
    val lastUsed: Long,
    val riskScore: Int,
    val reasons: List<String>,
    val uses: Int,
    val suspiciousUses: Int,
    /** System app that holds camera or mic access the user never granted. */
    val preGranted: Boolean = false,
) {
    val level: RiskLevel
        get() = when {
            riskScore >= 60 -> RiskLevel.HIGH
            riskScore >= 35 -> RiskLevel.MEDIUM
            else -> RiskLevel.LOW
        }
}
