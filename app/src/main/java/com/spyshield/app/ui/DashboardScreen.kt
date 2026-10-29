package com.spyshield.app.ui

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.QueryStats
import androidx.compose.material.icons.rounded.TipsAndUpdates
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.spyshield.app.MainViewModel
import com.spyshield.app.data.AppPrivacyInfo
import com.spyshield.app.data.LiveUse
import com.spyshield.app.data.RiskLevel
import com.spyshield.app.data.Sensor
import com.spyshield.app.monitor.ForegroundAppDetector

fun privacyScore(protection: Boolean, hasUsage: Boolean, apps: List<AppPrivacyInfo>, alerts24h: Int): Int {
    var s = 100
    if (!protection) s -= 20
    if (!hasUsage) s -= 10
    s -= apps.filter { !it.isSystem }.sumOf {
        when (it.level) {
            RiskLevel.HIGH -> 6
            RiskLevel.MEDIUM -> 2
            RiskLevel.LOW -> 0
        }.toInt()
    }.coerceAtMost(40)
    s -= (alerts24h * 10).coerceAtMost(30)
    return s.coerceIn(0, 100)
}

@Composable
fun DashboardScreen(vm: MainViewModel, onOpenTab: (Tab) -> Unit, onOpenApp: (String) -> Unit) {
    val context = LocalContext.current
    val apps by vm.apps.collectAsStateWithLifecycle()
    val events by vm.events.collectAsStateWithLifecycle()
    val live by vm.live.collectAsStateWithLifecycle()
    val protection by vm.protection.collectAsStateWithLifecycle()
    val blocked by vm.blocked.collectAsStateWithLifecycle()
    val permissionChanges by vm.permissionChanges.collectAsStateWithLifecycle()

    val tick = rememberResumeTick()
    val hasUsage = remember(tick) { ForegroundAppDetector.hasUsageAccess(context) }
    val notifOk = remember(tick) { NotificationManagerCompat.from(context).areNotificationsEnabled() }
    // Once denied twice the system stops showing the dialog, so fall back to the settings page.
    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted) {
            runCatching {
                context.startActivity(
                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
        }
    }

    val dayAgo = System.currentTimeMillis() - 86_400_000L
    val alerts = events.filter { it.suspicious }
    val alerts24 = alerts.count { it.start > dayAgo }
    val score = privacyScore(protection, hasUsage, apps, alerts24)
    val liveAlert = live.camera?.suspicious == true || live.mic?.suspicious == true
    val hero = heroState(protection, liveAlert, score)
    // ALERT glyph and support line come from the suspicious live use; camera wins if both.
    val alertSensor = when {
        live.camera?.suspicious == true -> Sensor.CAMERA
        live.mic?.suspicious == true -> Sensor.MIC
        else -> null
    }
    val alertLabel = alertSensor?.let { live.of(it)?.label }
    val showPermissionChanges = permissionChanges.any { it.gained }
    val recent = alerts.take(3)
    val userApps = apps.filter { !it.isSystem }
    val singleColumn = useSingleColumnTiles()
    val firstEntry = rememberFirstEntry()

    LazyColumn(
        contentPadding = PaddingValues(start = Space.gutter, end = Space.gutter, bottom = Space.listBottom),
    ) {
        // Wordmark + tagline
        item(key = "header") {
            Column(
                Modifier
                    .fillMaxWidth()
                    .entryAnimation(0, firstEntry)
                    .padding(top = Space.l, bottom = Space.xl),
            ) {
                Wordmark()
                Spacer(Modifier.height(Space.xs))
                Text(
                    "Catch apps secretly using your camera & mic",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Spy.colors.textSecondary,
                )
            }
        }

        // Hero status + protection toggle
        item(key = "hero") {
            HeroCard(
                state = hero,
                score = score,
                protection = protection,
                onProtectionChange = { vm.setProtection(it) },
                modifier = Modifier.entryAnimation(1, firstEntry),
                sensor = alertSensor,
                liveLabel = alertLabel,
            )
        }

        // Live sensor tiles
        item(key = "sensors") {
            TilePair(
                singleColumn = singleColumn,
                modifier = Modifier.padding(top = Space.tileGap).entryAnimation(2, firstEntry),
                first = { m -> SensorLiveTile(Sensor.CAMERA, live.camera, protection, m) { it?.let(onOpenApp) } },
                second = { m -> SensorLiveTile(Sensor.MIC, live.mic, protection, m) { it?.let(onOpenApp) } },
            )
        }

        // Callouts, only when needed
        if (!hasUsage) item(key = "usage") {
            SetupCallout(
                icon = Icons.Rounded.QueryStats,
                title = "Allow Usage Access",
                text = "Needed to name the app that turns on your camera or mic. Find SpyShield in the list and switch it on.",
                button = "Grant access",
                modifier = Modifier.animateItem().padding(top = Space.tileGap),
            ) { context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
        }
        if (!notifOk) item(key = "notifs") {
            SetupCallout(
                icon = Icons.Rounded.NotificationsActive,
                title = "Turn on alerts",
                text = "So SpyShield can warn you the second a hidden app starts spying.",
                button = "Enable alerts",
                modifier = Modifier.animateItem().padding(top = Space.tileGap),
            ) {
                if (Build.VERSION.SDK_INT >= 33) {
                    notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    context.startActivity(
                        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                }
            }
        }
        if (showPermissionChanges) item(key = "permChanges") {
            Box(Modifier.animateItem().padding(top = Space.tileGap)) {
                PermissionChangesCard(permissionChanges, onOpenApp) { vm.dismissPermissionChanges() }
            }
        }

        // Recent alerts
        item(key = "recentHeader") {
            SectionHeader(
                "Recent alerts",
                Modifier.padding(top = Space.section, bottom = Space.headerGap).entryAnimation(3, firstEntry),
                action = "See all",
                onAction = { onOpenTab(Tab.ACTIVITY) },
            )
        }
        if (recent.isEmpty()) item(key = "recentEmpty") {
            GroupedSection(Modifier.animateItem()) {
                EmptyState(
                    icon = Icons.Rounded.VerifiedUser,
                    title = "No suspicious activity yet",
                    body = if (protection) "We'll alert you instantly if an app uses your camera or mic behind your back."
                    else "Turn on protection to start catching spy apps.",
                    inline = true,
                    tone = if (protection) Tone.SAFE else Tone.NEUTRAL,
                )
            }
        }
        itemsIndexed(recent, key = { _, e -> e.id }) { i, e ->
            Box(
                Modifier
                    .animateItem(
                        fadeInSpec = tween(Motion.standard),
                        placementSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        fadeOutSpec = tween(150),
                    )
                    .groupItem(groupPosition(i, recent.size), dividerInset = 72.dp),
            ) {
                EventCard(e, onClick = { e.packageName?.let(onOpenApp) })
            }
        }

        // Overview stats
        item(key = "overviewHeader") {
            SectionHeader("Overview", Modifier.padding(top = Space.section, bottom = Space.headerGap))
        }
        item(key = "overview") {
            Column(verticalArrangement = Arrangement.spacedBy(Space.tileGap)) {
                TilePair(
                    singleColumn = singleColumn,
                    first = { m ->
                        StatTile(Sensor.CAMERA.icon, userApps.count { it.cameraGranted }, "Apps with camera", Tone.CAMERA, { onOpenTab(Tab.APPS) }, m)
                    },
                    second = { m ->
                        StatTile(Sensor.MIC.icon, userApps.count { it.micGranted }, "Apps with mic", Tone.MIC, { onOpenTab(Tab.APPS) }, m)
                    },
                )
                TilePair(
                    singleColumn = singleColumn,
                    first = { m ->
                        StatTile(
                            Icons.Rounded.WarningAmber, alerts24, "Alerts today",
                            if (alerts24 > 0) Tone.DANGER else Tone.SAFE, { onOpenTab(Tab.ACTIVITY) }, m,
                        )
                    },
                    second = { m ->
                        StatTile(
                            Icons.Rounded.Block, blocked.size, "Blocked apps",
                            if (blocked.isNotEmpty()) Tone.DANGER else Tone.NEUTRAL, { onOpenTab(Tab.APPS) }, m,
                        )
                    },
                )
            }
        }

        // Protection settings
        item(key = "settingsHeader") {
            SectionHeader("Protection settings", Modifier.padding(top = Space.section, bottom = Space.headerGap))
        }
        item(key = "settings") {
            GroupedSection {
                AutoBlockRow()
                RowDivider(64.dp)
                OverlayToggleRow()
            }
        }
        item(key = "tip") {
            Footnote(
                proTip("Pro tip: on Android 12+, swipe down Quick Settings and use the Camera access / Microphone access tiles to cut every app off instantly."),
                icon = Icons.Rounded.TipsAndUpdates,
            )
        }
    }
}

/** Two tiles side by side with 12dp gaps, or stacked when the font scale is large. */
@Composable
private fun TilePair(
    singleColumn: Boolean,
    modifier: Modifier = Modifier,
    first: @Composable (Modifier) -> Unit,
    second: @Composable (Modifier) -> Unit,
) {
    if (singleColumn) {
        Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Space.tileGap)) {
            first(Modifier.fillMaxWidth())
            second(Modifier.fillMaxWidth())
        }
    } else {
        Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Space.tileGap)) {
            first(Modifier.weight(1f))
            second(Modifier.weight(1f))
        }
    }
}

@Composable
private fun SensorLiveTile(sensor: Sensor, use: LiveUse?, protection: Boolean, modifier: Modifier, onOpen: (String?) -> Unit) {
    val active = use != null
    val suspicious = use?.suspicious == true
    val tone = when {
        suspicious -> Tone.DANGER
        active -> Tone.WARNING
        !protection -> Tone.NEUTRAL
        else -> Tone.SAFE
    }
    SensorTile(
        sensor = sensor,
        stateWord = when {
            !protection -> "Not watched"
            active -> if (suspicious) "Spying?" else "In use"
            else -> "Idle"
        },
        detail = use?.label ?: if (protection) "No app is using it" else "Turn on protection",
        tone = tone,
        active = active,
        suspicious = suspicious,
        modifier = modifier,
        onClick = { onOpen(use?.packageName) },
    )
}

@Composable
private fun SetupCallout(
    icon: ImageVector,
    title: String,
    text: String,
    button: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Callout(icon = icon, tone = Tone.WARNING, title = title, body = text, modifier = modifier) {
        Spacer(Modifier.height(Space.m))
        PrimaryButton(button, onClick = onClick, tone = Tone.WARNING, compact = true)
    }
}
