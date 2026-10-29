package com.spyshield.app.monitor

import android.app.KeyguardManager
import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.PowerManager
import com.spyshield.app.data.AppScanner
import com.spyshield.app.data.Sensor
import com.spyshield.app.data.appLabel

data class Attribution(
    val packageName: String?,
    val label: String,
    val suspicious: Boolean,
    val reason: String,
    val suspects: List<String>,
    /** Camera on the lock screen is often face unlock, so short sessions are dropped. */
    val lockedCamera: Boolean,
    /** The foreground-service evidence points at a single app, so acting on it automatically is safe. */
    val confident: Boolean = false,
)

/**
 * Android hides which app holds the camera or mic from regular apps, so we infer it:
 * if the app on screen holds the permission, it's the user's own action; otherwise
 * something in the background is using the sensor and we rank the likely culprits.
 */
object Attributor {

    private const val DAY = 86_400_000L

    fun attribute(context: Context, sensor: Sensor, silenced: Boolean): Attribution {
        val pm = context.packageManager
        val screenOn = context.getSystemService(PowerManager::class.java).isInteractive
        val locked = context.getSystemService(KeyguardManager::class.java).isKeyguardLocked
        // One usage-events query serves every check below.
        val usage by lazy { ForegroundAppDetector.snapshot(context) }
        DetectionRules.duringCall(context, sensor, silenced) { usage }?.let { return it }

        if (!usage.hasAccess) {
            return Attribution(
                null, "Unknown app", false,
                "Grant Usage Access so SpyShield can name the app", emptyList(), false,
            )
        }

        val launchers = AppScanner.homePackages(context)
        val fg = if (screenOn) DetectionRules.foreground(locked, usage) else null
        val fgUsable = fg != null && fg != context.packageName && fg !in launchers &&
            fg != "com.android.systemui" &&
            pm.checkPermission(sensor.permission, fg) == PackageManager.PERMISSION_GRANTED

        // Activities shown over the keyguard (secure camera, assistant) are resumed while locked.
        if (screenOn && fgUsable && !silenced) {
            return Attribution(fg, context.appLabel(fg!!), false, "Used while the app was open on your screen", emptyList(), false)
        }

        if (screenOn && !locked && !silenced && fg != null && fg !in launchers) {
            DetectionRules.voiceTyping(context, sensor, fg)?.let { return it }
        }
        val exclude = launchers + context.packageName + listOfNotNull(fg)
        val services = usage.services
        val ranked = rankSuspects(context, sensor.permission, exclude, services)
        // Only a service that could hold this sensor in the background explains the use. A silenced
        // recorder lacked that, so the app running a service is then not the one recording. It must
        // also be the app we name.
        val capable = ranked.filter { it in services && canUseInBackground(context, it, sensor) }
        val confident = !silenced && capable.size == 1 && capable[0] == ranked.first()
        // No permission holder is running a service, but something else is: likely a privileged app.
        val unidentified = if (ranked.none { it in services }) {
            services.keys.filter { it !in exclude }.sortedByDescending { services[it] }.take(2)
        } else emptyList()
        // Keep the top permission holder in the list even then.
        val suspects = unidentified + ranked
        val reason = when {
            unidentified.isNotEmpty() -> "Couldn't tell which app — possibly a system or privileged app"
            silenced -> "A background app tried to record — Android silenced it"
            !screenOn -> "Accessed while the screen was off"
            locked && sensor == Sensor.CAMERA -> "Camera used on the lock screen (face unlock or a hidden app)"
            locked -> "Accessed while your phone was locked"
            fg == null || fg in launchers -> "Accessed in the background while you were on the home screen"
            else -> "Accessed in the background while you were using ${context.appLabel(fg)}"
        }
        val top = suspects.firstOrNull()
        return Attribution(
            packageName = top,
            label = top?.let { context.appLabel(it) } ?: "Unknown background app",
            suspicious = true,
            reason = reason,
            suspects = suspects.take(3),
            lockedCamera = locked && sensor == Sensor.CAMERA,
            confident = confident,
        )
    }

    /**
     * Apps running a foreground service first, user-installed before system ones (Play services and
     * OEM apps restart services often), then newest service, then most recently used.
     */
    private fun rankSuspects(context: Context, permission: String, exclude: Set<String>, services: Map<String, Long>): List<String> {
        val lastUsed = ForegroundAppDetector.lastUsedTimes(context, 3 * DAY)
        val candidates = AppScanner.packagesWithGranted(context, permission).filter { it !in exclude }
        // Looked up once per app, not on every comparison.
        val user = candidates.associateWith { !AppScanner.isSystemPackage(context, it) }
        return candidates.sortedWith(
            compareByDescending<String> { it in services }.thenByDescending { user[it] == true }
                .thenByDescending { services[it] ?: 0L }.thenByDescending { lastUsed[it] ?: 0L }
        )
    }

    /** Android 11+ only lets apps targeting 30+ use the sensor from the background via a service of the matching type. */
    private fun canUseInBackground(context: Context, pkg: String, sensor: Sensor): Boolean {
        if (Build.VERSION.SDK_INT < 30) return true
        val type = if (sensor == Sensor.CAMERA) ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA
        else ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
        val (targetSdk, types) = AppScanner.foregroundServiceTypes(context, pkg) ?: return false
        return targetSdk < 30 || types and type != 0
    }
}
