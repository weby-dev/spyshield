package com.spyshield.app.monitor

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.spyshield.app.MainActivity
import com.spyshield.app.R
import com.spyshield.app.data.Store
import java.util.Calendar
/** Home-screen widget showing protection state, live camera/mic use and today's alert count. */
class StatusWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, appWidgetIds: IntArray) {
        val views = buildViews(context, state())
        appWidgetIds.forEach { manager.updateAppWidget(it, views) }
        // Older widgets may not show this yet, so the next refresh pushes to all of them.
        synchronized(Companion) { lastPushed = null }
    }

    companion object {
        private const val CYAN = 0xFF22E3C4.toInt()
        private const val GRAY = 0xFF5B6B85.toInt()
        private const val RED = 0xFFFF4D6D.toInt()

        /** Everything the widget shows. */
        private data class State(val status: String, val color: Int, val camera: String, val mic: String, val alerts: Int)

        // Guarded by the companion.
        private var lastPushed: State? = null

        /** Push the current state to every placed widget. Safe to call from any thread. */
        fun refresh(context: Context) {
            runCatching {
                val state = state()
                synchronized(this) {
                    // The same picture again would only make the launcher redraw.
                    if (state == lastPushed) return
                    val manager = AppWidgetManager.getInstance(context)
                    val component = ComponentName(context, StatusWidgetProvider::class.java)
                    // No widget placed: nothing to draw. onUpdate paints one when it's added.
                    if (manager.getAppWidgetIds(component).isEmpty()) return
                    manager.updateAppWidget(component, buildViews(context, state))
                    lastPushed = state
                }
            }
        }

        private fun state(): State {
            val live = Store.live.value
            val alert = live.camera?.suspicious == true || live.mic?.suspicious == true
            val (status, color) = when {
                alert -> "Spy alert!" to RED
                Store.protectionOn.value -> "Protected" to CYAN
                else -> "Off" to GRAY
            }
            val camera = live.camera?.label ?: "idle"
            val mic = live.mic?.label ?: "idle"

            val dayStart = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            val alerts = Store.events.value.count { it.suspicious && it.start >= dayStart }
            return State(status, color, camera, mic, alerts)
        }

        private fun buildViews(context: Context, state: State): RemoteViews {
            val open = PendingIntent.getActivity(
                context, 0,
                Intent(context, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )

            return RemoteViews(context.packageName, R.layout.widget_status).apply {
                setInt(R.id.widget_icon, "setColorFilter", state.color)
                setTextViewText(R.id.widget_status, state.status)
                setTextColor(R.id.widget_status, state.color)
                setTextViewText(R.id.widget_live, "Camera: ${state.camera} · Mic: ${state.mic}")
                setTextViewText(R.id.widget_alerts, if (state.alerts == 1) "1 alert today" else "${state.alerts} alerts today")
                setOnClickPendingIntent(R.id.widget_root, open)
            }
        }
    }
}
