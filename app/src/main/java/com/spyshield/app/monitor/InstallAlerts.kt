package com.spyshield.app.monitor

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.spyshield.app.MainActivity
import com.spyshield.app.R
import com.spyshield.app.data.appDetailsIntent

/** Notification raised when a freshly installed or updated app asks for camera/mic access. */
object InstallAlerts {
    private const val ALERT_COLOR = 0xFFFF4D6D.toInt()

    fun newSensorApp(context: Context, pkg: String, label: String, updated: Boolean) {
        val nm = context.getSystemService(NotificationManager::class.java)
        if (!nm.areNotificationsEnabled()) return
        // Stable per package so an install followed by an update doesn't stack two alerts.
        val notifId = ("install:$pkg").hashCode()

        val body = if (updated) "$label was updated — review its access" else "$label was installed — review its access"
        val open = PendingIntent.getActivity(
            context, notifId,
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val review = PendingIntent.getActivity(
            context, notifId, appDetailsIntent(pkg),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val notification = NotificationCompat.Builder(context, Notifications.CH_ALERTS)
            .setSmallIcon(R.drawable.ic_stat_shield)
            .setContentTitle("New app can use your camera/mic")
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setColor(ALERT_COLOR)
            .setAutoCancel(true)
            .setContentIntent(open)
            .addAction(0, "Review", review)
            .build()
        runCatching { nm.notify(notifId, notification) }
    }
}
