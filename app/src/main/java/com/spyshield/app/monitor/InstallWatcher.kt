package com.spyshield.app.monitor

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import androidx.core.content.ContextCompat
import com.spyshield.app.data.AppScanner
import com.spyshield.app.data.appLabel

/**
 * Watches for apps being installed or updated and warns when they ask for camera/mic.
 * Android 8+ no longer delivers PACKAGE_ADDED to manifest receivers, so this is
 * registered at runtime by [SensorMonitorService].
 */
class InstallWatcher(private val context: Context) {

    private var registered = false

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            // Cached package data is stale now, removals included.
            AppScanner.invalidateAll()
            if (intent.action == Intent.ACTION_PACKAGE_REMOVED) return
            val pkg = intent.data?.schemeSpecificPart ?: return
            if (pkg == context.packageName) return
            // An update sends ADDED (replacing) followed by REPLACED; only handle the latter.
            val replacing = intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)
            if (intent.action == Intent.ACTION_PACKAGE_ADDED && replacing) return
            if (!requestsSensors(pkg)) return
            val updated = replacing || intent.action == Intent.ACTION_PACKAGE_REPLACED
            InstallAlerts.newSensorApp(context, pkg, context.appLabel(pkg), updated = updated)
        }
    }

    fun register(handler: Handler) {
        if (registered) return
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addDataScheme("package")
        }
        // Package broadcasts come from the system, so the receiver must be exported to get them.
        ContextCompat.registerReceiver(context, receiver, filter, null, handler, ContextCompat.RECEIVER_EXPORTED)
        registered = true
    }

    fun unregister() {
        if (!registered) return
        runCatching { context.unregisterReceiver(receiver) }
        registered = false
    }

    private fun requestsSensors(pkg: String): Boolean {
        val pm = context.packageManager
        val info: PackageInfo = runCatching {
            if (Build.VERSION.SDK_INT >= 33) {
                pm.getPackageInfo(pkg, PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS.toLong()))
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(pkg, PackageManager.GET_PERMISSIONS)
            }
        }.getOrNull() ?: return false
        val requested = info.requestedPermissions ?: return false
        return Manifest.permission.CAMERA in requested || Manifest.permission.RECORD_AUDIO in requested
    }
}
