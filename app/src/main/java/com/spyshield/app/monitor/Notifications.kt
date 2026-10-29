package com.spyshield.app.monitor

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.spyshield.app.MainActivity
import com.spyshield.app.R
import com.spyshield.app.data.AccessEvent
import com.spyshield.app.data.appDetailsIntent
import com.spyshield.app.data.appLabel
import com.spyshield.app.ui.SpyAlarmActivity

object Notifications {
    const val CH_MONITOR = "monitor"
    const val CH_ALERTS = "alerts"
    const val CH_ALARM = "spy_alarm"
    const val ID_MONITOR = 1
    const val EXTRA_TAB = "open_tab"

    private const val ALERT_COLOR = 0xFFFF4D6D.toInt()
    private const val CYAN = 0xFF22E3C4.toInt()

    fun createChannels(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CH_MONITOR, "Protection status", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Shows that SpyShield is watching your camera and mic"
                setShowBadge(false)
            }
        )
        nm.createNotificationChannel(
            NotificationChannel(CH_ALERTS, "Spy alerts", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Alerts when an app uses your camera or mic in the background"
                enableVibration(true)
            }
        )
        nm.createNotificationChannel(
            NotificationChannel(CH_ALARM, "Spy alarm", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Sounds when auto-block catches an app spying on you"
                enableVibration(true)
            }
        )
    }

    private fun openApp(context: Context, tab: String?): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        if (tab != null) intent.putExtra(EXTRA_TAB, tab)
        return PendingIntent.getActivity(
            context, tab.hashCode(), intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    fun monitor(context: Context, text: String): Notification =
        NotificationCompat.Builder(context, CH_MONITOR)
            .setSmallIcon(R.drawable.ic_stat_shield)
            .setContentTitle("SpyShield is protecting you")
            .setContentText(text)
            .setColor(CYAN)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(openApp(context, null))
            .build()

    fun alert(context: Context, e: AccessEvent) {
        val nm = context.getSystemService(NotificationManager::class.java)
        if (!nm.areNotificationsEnabled()) return
        val notifId = (e.id % Int.MAX_VALUE).toInt()

        val title = if (e.blocked) "Blocked app is using your ${e.sensor.noun}!"
        else "${e.sensor.title} used in the background"
        val suspectsLine = e.suspects.takeIf { it.size > 1 }
            ?.joinToString(prefix = "\nPossible apps: ") { context.appLabel(it) }
            .orEmpty()
        val body = "${e.appLabel}: ${e.reason}$suspectsLine"

        val builder = NotificationCompat.Builder(context, CH_ALERTS)
            .setSmallIcon(R.drawable.ic_stat_shield)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setColor(ALERT_COLOR)
            .setAutoCancel(true)
            .setContentIntent(openApp(context, "activity"))

        e.packageName?.let { pkg ->
            builder.addAction(
                0, "Revoke access",
                PendingIntent.getActivity(context, pkg.hashCode(), appDetailsIntent(pkg), PendingIntent.FLAG_IMMUTABLE),
            )
            if (!e.blocked) {
                val block = Intent(context, BlockActionReceiver::class.java)
                    .putExtra(BlockActionReceiver.EXTRA_PKG, pkg)
                    .putExtra(BlockActionReceiver.EXTRA_NOTIF, notifId)
                builder.addAction(
                    0, "Block app",
                    PendingIntent.getBroadcast(
                        context, notifId, block,
                        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                    ),
                )
            }
        }
        runCatching { nm.notify(notifId, builder.build()) }
    }

    /** Loud alarm after auto-block, with a one-tap route to the app's settings page to force stop it. */
    fun spyAlarm(context: Context, e: AccessEvent) {
        val pkg = e.packageName ?: return
        val nm = context.getSystemService(NotificationManager::class.java)
        // A muted alarm channel must not make the auto-block silent; fall back to a regular alert.
        if (!nm.areNotificationsEnabled() || nm.getNotificationChannel(CH_ALARM)?.importance == NotificationManager.IMPORTANCE_NONE) {
            alert(context, e)
            return
        }
        val notifId = (e.id % Int.MAX_VALUE).toInt()
        val text = "Auto-block added it to your blocklist. Tap Stop it now, then Force stop."
        val notSpy = Intent(context, NotSpyReceiver::class.java)
            .putExtra(BlockActionReceiver.EXTRA_PKG, pkg)
            .putExtra(BlockActionReceiver.EXTRA_NOTIF, notifId)
            .putExtra(NotSpyReceiver.EXTRA_EVENT, e.id)
        // Trusting the spy must need an unlock, or whoever holds the locked phone could do it.
        val notSpyAction = NotificationCompat.Action.Builder(
            0, "Not a spy",
            PendingIntent.getBroadcast(context, notifId, notSpy, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT),
        ).setAuthenticationRequired(true).build()

        val builder = NotificationCompat.Builder(context, CH_ALARM)
            .setSmallIcon(R.drawable.ic_stat_shield)
            .setContentTitle("${e.appLabel} is spying on your ${e.sensor.noun}")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setColor(ALERT_COLOR)
            .setAutoCancel(true)
            .setContentIntent(openApp(context, "activity"))
            .addAction(
                0, "Stop it now",
                SpyAlarmActivity.pendingIntent(context, e, notifId, stopNow = true),
            )
            .addAction(notSpyAction)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
        // USE_FULL_SCREEN_INTENT is declared, so only Android 14+ can take it away.
        val fullScreen = Build.VERSION.SDK_INT < 34 || nm.canUseFullScreenIntent()
        if (fullScreen) builder.setFullScreenIntent(SpyAlarmActivity.pendingIntent(context, e, notifId), true)
        runCatching { nm.notify(notifId, builder.build()) }
    }
}
