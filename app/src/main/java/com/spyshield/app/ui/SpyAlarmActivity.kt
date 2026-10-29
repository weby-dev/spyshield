package com.spyshield.app.ui

import android.app.KeyguardManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PanTool
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.spyshield.app.data.AccessEvent
import com.spyshield.app.data.Sensor
import com.spyshield.app.data.appDetailsIntent
import com.spyshield.app.monitor.NotSpyReceiver

/** Fixed alarm palette (DESIGN.md 7.5): ignores the light/dark theme on purpose. */
private val AlarmTop = Color(0xFFB3141F)
private val AlarmBottom = Color(0xFF4A0610)
private val AlarmIconSize = 104.dp
private val AlarmRing = 4.dp

/** Full-screen spy alarm shown over the lock screen after auto-block catches an app. */
class SpyAlarmActivity : ComponentActivity() {

    private class Alarm(val pkg: String, val label: String, val sensor: Sensor, val notifId: Int, val eventId: Long)

    private val alarm = mutableStateOf<Alarm?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
        }
        if (!read(intent)) {
            finish()
            return
        }
        // The alarm is always deep red, so the system bar icons are always light.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        setContent { SpyShieldTheme { alarm.value?.let { AlarmScreen(it) } } }
        if (intent.getBooleanExtra(EXTRA_STOP_NOW, false)) alarm.value?.let(::stopNow)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (read(intent) && intent.getBooleanExtra(EXTRA_STOP_NOW, false)) alarm.value?.let(::stopNow)
    }

    private fun read(intent: Intent?): Boolean {
        val pkg = intent?.getStringExtra(EXTRA_PKG) ?: return false
        val sensor = runCatching { Sensor.valueOf(intent.getStringExtra(EXTRA_SENSOR)!!) }.getOrNull() ?: return false
        alarm.value = Alarm(
            pkg, intent.getStringExtra(EXTRA_LABEL) ?: pkg, sensor,
            intent.getIntExtra(EXTRA_NOTIF, 0), intent.getLongExtra(EXTRA_EVENT, 0L),
        )
        return true
    }

    private fun stopNow(a: Alarm) {
        // The settings page can't show over the lock screen, so ask the user to unlock first.
        afterUnlock(cancelFinishes = true) {
            // The user is acting on it, so the banner shouldn't keep covering App info.
            getSystemService(android.app.NotificationManager::class.java).cancel(a.notifId)
            runCatching { startActivity(appDetailsIntent(a.pkg)) }
            finish()
        }
    }

    private fun notSpy(a: Alarm) {
        // Trusting the app needs an unlock, so whoever holds the locked phone can't whitelist the spy.
        afterUnlock(cancelFinishes = false) {
            NotSpyReceiver.markNotSpy(this, a.pkg, a.notifId, a.eventId)
            finish()
        }
    }

    /** Runs [action] now, or once the user unlocks; finishing early would cancel the unlock prompt. */
    private fun afterUnlock(cancelFinishes: Boolean, action: () -> Unit) {
        val km = getSystemService(KeyguardManager::class.java)
        if (!km.isKeyguardLocked) {
            action()
            return
        }
        km.requestDismissKeyguard(this, object : KeyguardManager.KeyguardDismissCallback() {
            override fun onDismissSucceeded() = action()
            override fun onDismissCancelled() { if (cancelFinishes) finish() }
            override fun onDismissError() { if (cancelFinishes) finish() }
        })
    }

    @Composable
    private fun AlarmScreen(a: Alarm) {
        val reduce = LocalReduceMotion.current
        val iconScale = remember { Animatable(if (reduce) 1f else 0.85f) }
        val textAlpha = remember { Animatable(if (reduce) 1f else 0f) }
        LaunchedEffect(Unit) {
            if (!reduce) iconScale.animateTo(1f, spring(dampingRatio = 0.6f, stiffness = 400f))
        }
        LaunchedEffect(Unit) {
            if (!reduce) textAlpha.animateTo(1f, tween(Motion.standard, delayMillis = 120, easing = Motion.standardEasing))
        }
        val framed = AlarmIconSize + AlarmRing * 2

        Column(
            Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(AlarmTop, AlarmBottom)))
                .systemBarsPadding()
                .padding(horizontal = Space.xxl),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Top label
            Row(
                Modifier.padding(top = Space.l),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Logomark(20.dp, color = Color.White, lensColor = AlarmTop)
                Spacer(Modifier.width(Space.s))
                Text(
                    "SpyShield · Spy alarm",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.80f),
                )
            }

            // Centre: the culprit
            Column(
                Modifier.fillMaxWidth().weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Box(
                    Modifier.graphicsLayer {
                        scaleX = iconScale.value
                        scaleY = iconScale.value
                    },
                ) {
                    AlarmRipples(framed, reduce)
                    Box(
                        Modifier
                            .size(framed)
                            .background(Color.White.copy(alpha = 0.20f), RoundedCornerShape(framed * 0.26f))
                            .padding(AlarmRing),
                        contentAlignment = Alignment.Center,
                    ) {
                        AppIcon(a.pkg, AlarmIconSize)
                    }
                    Box(
                        Modifier
                            .align(Alignment.BottomEnd)
                            .offset(x = 6.dp, y = 6.dp)
                            .size(28.dp)
                            .background(Color.White, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(a.sensor.icon, null, tint = AlarmTop, modifier = Modifier.size(16.dp))
                    }
                }

                Spacer(Modifier.height(Space.xxl))

                Column(
                    Modifier.graphicsLayer { alpha = textAlpha.value },
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        a.label,
                        color = Color.White,
                        style = MaterialTheme.typography.displayLarge,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.semantics { heading() },
                    )
                    Spacer(Modifier.height(Space.xs))
                    Text(
                        "is spying on your ${a.sensor.noun}",
                        color = Color.White.copy(alpha = 0.90f),
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(Space.l))
                    Text(
                        "Auto-block added it to your blocklist. Tap Stop it now, then Force stop.",
                        color = Color.White.copy(alpha = 0.80f),
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.widthIn(max = 320.dp),
                    )
                }
            }

            // Bottom actions
            AlarmButton(
                text = "Stop it now",
                container = Color.White,
                content = AlarmTop,
                onClick = { stopNow(a) },
                showIcon = true,
            )
            Spacer(Modifier.height(Space.m))
            AlarmButton(
                text = "Not a spy",
                container = Color.White.copy(alpha = 0.14f),
                content = Color.White,
                onClick = { notSpy(a) },
            )
            Spacer(Modifier.height(Space.s))
        }
    }

    /** Two white rings expanding from the icon (1.0x to 1.6x, alpha 0.35 to 0), offset by half a period. */
    @Composable
    private fun AlarmRipples(base: Dp, reduce: Boolean) {
        if (reduce) {
            Spacer(Modifier.size(base))
            return
        }
        val t = rememberInfiniteTransition(label = "alarmRipples")
        val p by t.animateFloat(
            0f, 1f,
            infiniteRepeatable(tween(1600, easing = LinearOutSlowInEasing), RepeatMode.Restart),
            label = "ripple",
        )
        Spacer(
            Modifier.size(base).drawBehind {
                val r = size.minDimension / 2f
                val stroke = Stroke(width = 2.dp.toPx())
                for (offset in floatArrayOf(0f, 0.5f)) {
                    val q = (p + offset) % 1f
                    drawCircle(
                        Color.White.copy(alpha = 0.35f * (1f - q)),
                        radius = r * (1f + 0.6f * q),
                        style = stroke,
                    )
                }
            }
        )
    }

    @Composable
    private fun AlarmButton(
        text: String,
        container: Color,
        content: Color,
        onClick: () -> Unit,
        showIcon: Boolean = false,
    ) {
        val src = remember { MutableInteractionSource() }
        Button(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).pressScale(src, 0.98f),
            shape = RoundedCornerShape(Radius.l),
            colors = ButtonDefaults.buttonColors(containerColor = container, contentColor = content),
            elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp, 0.dp, 0.dp, 0.dp),
            contentPadding = PaddingValues(horizontal = Space.xl, vertical = Space.m),
            interactionSource = src,
        ) {
            if (showIcon) {
                Icon(Icons.Rounded.PanTool, null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(Space.s))
            }
            Text(text, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
        }
    }

    companion object {
        private const val EXTRA_PKG = "pkg"
        private const val EXTRA_LABEL = "label"
        private const val EXTRA_SENSOR = "sensor"
        private const val EXTRA_NOTIF = "notif"
        private const val EXTRA_EVENT = "event"
        private const val EXTRA_STOP_NOW = "stop_now"

        /** [stopNow] goes straight to App info (the notification's "Stop it now" action). */
        fun pendingIntent(context: Context, e: AccessEvent, notifId: Int, stopNow: Boolean = false): PendingIntent {
            val intent = Intent(context, SpyAlarmActivity::class.java)
                .putExtra(EXTRA_PKG, e.packageName)
                .putExtra(EXTRA_LABEL, e.appLabel)
                .putExtra(EXTRA_SENSOR, e.sensor.name)
                .putExtra(EXTRA_NOTIF, notifId)
                .putExtra(EXTRA_EVENT, e.id)
                .putExtra(EXTRA_STOP_NOW, stopNow)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            return PendingIntent.getActivity(
                context, if (stopNow) notifId xor 0x40000000 else notifId, intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
        }
    }
}
