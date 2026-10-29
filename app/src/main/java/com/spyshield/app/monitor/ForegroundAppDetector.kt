package com.spyshield.app.monitor

import android.Manifest
import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process

/** Uses Usage Access to work out which app is on screen and which apps ran recently. */
object ForegroundAppDetector {

    fun hasUsageAccess(context: Context): Boolean {
        val ops = context.getSystemService(AppOpsManager::class.java)
        val mode = if (Build.VERSION.SDK_INT >= 29) {
            ops.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        } else {
            @Suppress("DEPRECATION")
            ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        }
        // MODE_DEFAULT defers to the permission grant; ColorOS and others store "Allow" that way.
        return when (mode) {
            AppOpsManager.MODE_ALLOWED -> true
            AppOpsManager.MODE_DEFAULT ->
                context.checkSelfPermission(Manifest.permission.PACKAGE_USAGE_STATS) == PackageManager.PERMISSION_GRANTED
            else -> false
        }
    }

    /** Package whose activity is currently resumed, or null if unknown. */
    @Suppress("DEPRECATION")
    fun currentForeground(context: Context, windowMs: Long = 30 * 60_000L): String? {
        if (!hasUsageAccess(context)) return null
        val usm = context.getSystemService(UsageStatsManager::class.java)
        val end = System.currentTimeMillis()
        val events = usm.queryEvents(end - windowMs, end + 1_000L) ?: return null
        val event = UsageEvents.Event()
        // Track per activity, since one app's activities resume/pause around each other.
        val resumed = HashMap<String, Pair<String, Long>>()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            val key = event.packageName + "/" + event.className
            when (event.eventType) {
                UsageEvents.Event.MOVE_TO_FOREGROUND -> resumed[key] = event.packageName to event.timeStamp
                UsageEvents.Event.MOVE_TO_BACKGROUND -> resumed.remove(key)
            }
        }
        return resumed.values.maxByOrNull { it.second }?.first
    }

    /**
     * Package -> latest start time of its foreground services that are still running. On Android 11+
     * an app needs one to use the camera or mic from the background, so this is the strongest culprit signal.
     */
    fun activeForegroundServices(context: Context, windowMs: Long = 6 * 3_600_000L): Map<String, Long> {
        if (Build.VERSION.SDK_INT < 29 || !hasUsageAccess(context)) return emptyMap()
        val usm = context.getSystemService(UsageStatsManager::class.java)
        val end = System.currentTimeMillis()
        val events = usm.queryEvents(end - windowMs, end + 1_000L) ?: return emptyMap()
        val event = UsageEvents.Event()
        // Track per service class, since one app can run several.
        val running = HashMap<String, Pair<String, Long>>()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            val key = event.packageName + "/" + event.className
            when (event.eventType) {
                UsageEvents.Event.FOREGROUND_SERVICE_START -> running[key] = event.packageName to event.timeStamp
                UsageEvents.Event.FOREGROUND_SERVICE_STOP -> running.remove(key)
                // Services running at shutdown never log a stop.
                UsageEvents.Event.DEVICE_SHUTDOWN, UsageEvents.Event.DEVICE_STARTUP -> running.clear()
            }
        }
        return running.values.groupBy({ it.first }, { it.second }).mapValues { it.value.max() }
    }

    /**
     * What [currentForeground] (30 min and 6 h) and [activeForegroundServices] would return, read
     * from one usage-events query instead of three.
     */
    class Snapshot(val hasAccess: Boolean, val foreground: String?, val foregroundLong: String?, val services: Map<String, Long>)

    @Suppress("DEPRECATION")
    fun snapshot(context: Context): Snapshot {
        if (!hasUsageAccess(context)) return Snapshot(false, null, null, emptyMap())
        val usm = context.getSystemService(UsageStatsManager::class.java)
        val end = System.currentTimeMillis()
        val shortStart = end - 30 * 60_000L
        val events = usm.queryEvents(end - 6 * 3_600_000L, end + 1_000L) ?: return Snapshot(true, null, null, emptyMap())
        val event = UsageEvents.Event()
        val resumed = HashMap<String, Pair<String, Long>>()
        // Only activities that resumed inside the last 30 min, as the short query would see them.
        val resumedShort = HashMap<String, Pair<String, Long>>()
        val running = HashMap<String, Pair<String, Long>>()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            val key = event.packageName + "/" + event.className
            when (event.eventType) {
                UsageEvents.Event.MOVE_TO_FOREGROUND -> {
                    resumed[key] = event.packageName to event.timeStamp
                    if (event.timeStamp >= shortStart) resumedShort[key] = event.packageName to event.timeStamp
                }
                UsageEvents.Event.MOVE_TO_BACKGROUND -> {
                    resumed.remove(key)
                    resumedShort.remove(key)
                }
                UsageEvents.Event.FOREGROUND_SERVICE_START -> running[key] = event.packageName to event.timeStamp
                UsageEvents.Event.FOREGROUND_SERVICE_STOP -> running.remove(key)
                UsageEvents.Event.DEVICE_SHUTDOWN, UsageEvents.Event.DEVICE_STARTUP -> running.clear()
            }
        }
        return Snapshot(
            hasAccess = true,
            foreground = resumedShort.values.maxByOrNull { it.second }?.first,
            foregroundLong = resumed.values.maxByOrNull { it.second }?.first,
            services = if (Build.VERSION.SDK_INT < 29) emptyMap()
            else running.values.groupBy({ it.first }, { it.second }).mapValues { it.value.max() },
        )
    }

    fun lastUsedTimes(context: Context, windowMs: Long): Map<String, Long> {
        if (!hasUsageAccess(context)) return emptyMap()
        val usm = context.getSystemService(UsageStatsManager::class.java)
        val end = System.currentTimeMillis()
        return runCatching {
            usm.queryAndAggregateUsageStats(end - windowMs, end).mapValues { it.value.lastTimeUsed }
        }.getOrDefault(emptyMap())
    }
}
