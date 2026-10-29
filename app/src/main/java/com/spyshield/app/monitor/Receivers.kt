package com.spyshield.app.monitor

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.spyshield.app.data.Store
import com.spyshield.app.data.TrustedApps

/** Restarts protection after a reboot or app update if the user had it on. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val ok = intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == Intent.ACTION_MY_PACKAGE_REPLACED
        if (ok && Store.protectionOn.value) SensorMonitorService.start(context)
    }
}

/** "Block app" button on an alert notification. */
class BlockActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pkg = intent.getStringExtra(EXTRA_PKG) ?: return
        Store.setBlocked(pkg, true)
        context.getSystemService(NotificationManager::class.java).cancel(intent.getIntExtra(EXTRA_NOTIF, 0))
    }

    companion object {
        const val EXTRA_PKG = "pkg"
        const val EXTRA_NOTIF = "notif"
    }
}

/** "Not a spy" on the spy alarm: undoes the auto-block and trusts the app so it won't alarm again. */
class NotSpyReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pkg = intent.getStringExtra(BlockActionReceiver.EXTRA_PKG) ?: return
        markNotSpy(context, pkg, intent.getIntExtra(BlockActionReceiver.EXTRA_NOTIF, 0), intent.getLongExtra(EXTRA_EVENT, 0L))
    }

    companion object {
        const val EXTRA_EVENT = "event"

        fun markNotSpy(context: Context, pkg: String, notifId: Int, eventId: Long) {
            Store.setBlocked(pkg, false)
            TrustedApps.setTrusted(pkg, true)
            Store.updateEvent(eventId) { it.copy(blocked = false, autoBlocked = false) }
            context.getSystemService(NotificationManager::class.java).cancel(notifId)
        }
    }
}
