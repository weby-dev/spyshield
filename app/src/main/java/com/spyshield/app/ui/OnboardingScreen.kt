package com.spyshield.app.ui

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.QueryStats
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import com.spyshield.app.monitor.ForegroundAppDetector
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

private const val PAGE_COUNT = 4
private const val LAST_PAGE = PAGE_COUNT - 1

/** First-run walkthrough: pitch, then the three permissions SpyShield needs to work properly. */
@Composable
fun OnboardingScreen(onFinish: (startProtection: Boolean) -> Unit) {
    val pager = rememberPagerState(pageCount = { PAGE_COUNT })
    val scope = rememberCoroutineScope()
    val isLast = pager.currentPage == LAST_PAGE

    // System back steps through the pages instead of leaving the app mid-setup.
    BackHandler(enabled = pager.currentPage > 0) {
        scope.launch { pager.animateScrollToPage(pager.currentPage - 1) }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Spy.colors.bg)
            .systemBarsPadding()
    ) {
        Column(Modifier.fillMaxSize()) {
            HorizontalPager(
                state = pager,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) { page ->
                Box(Modifier.fillMaxSize().pageTransition(pager, page)) {
                    when (page) {
                        0 -> WelcomeStep()
                        1 -> UsageStep()
                        2 -> AlertsStep()
                        else -> StayRunningStep(onFinish)
                    }
                }
            }

            // Bottom bar: Skip · dots · Next. On the last page the page itself carries the CTA.
            val barAlpha by animateFloatAsState(if (isLast) 0f else 1f, tween(250), label = "barAlpha")
            Row(
                Modifier
                    .fillMaxWidth()
                    .alpha(barAlpha)
                    .padding(horizontal = Space.xl, vertical = Space.m),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    GhostButton(
                        text = "Skip",
                        onClick = { scope.launch { pager.animateScrollToPage(LAST_PAGE) } },
                        color = Spy.colors.textSecondary,
                        enabled = !isLast,
                    )
                }
                PageDots(pager.currentPage)
                Box(Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
                    PrimaryButton(
                        text = "Next",
                        onClick = { scope.launch { pager.animateScrollToPage((pager.currentPage + 1).coerceAtMost(LAST_PAGE)) } },
                        trailingIcon = Icons.AutoMirrored.Rounded.ArrowForward,
                        compact = true,
                        enabled = !isLast,
                    )
                }
            }
        }
    }
}

/** Fades and gently shrinks pages as they slide away from the centre. */
private fun Modifier.pageTransition(pager: PagerState, page: Int): Modifier = graphicsLayer {
    val offset = ((pager.currentPage - page) + pager.currentPageOffsetFraction).absoluteValue.coerceIn(0f, 1f)
    alpha = 1f - offset * 0.6f
    val s = 1f - offset * 0.06f
    scaleX = s
    scaleY = s
}

@Composable
private fun PageDots(current: Int) {
    val c = Spy.colors
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(PAGE_COUNT) { i ->
            val selected = i == current
            val w by animateDpAsState(if (selected) 20.dp else 6.dp, tween(300), label = "dotW")
            val col by animateColorAsState(
                if (selected) c.brand else c.textTertiary.copy(alpha = 0.5f),
                tween(300),
                label = "dotC",
            )
            Box(
                Modifier
                    .height(6.dp)
                    .width(w)
                    .clip(CircleShape)
                    .background(col)
            )
        }
    }
}

// ---------- Steps ----------

@Composable
private fun WelcomeStep() {
    val first = rememberFirstEntry()
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val minH = maxHeight
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .heightIn(min = minH)
                .padding(horizontal = Space.gutter, vertical = Space.xxl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            ShieldHero(HeroState.CALM, size = 176.dp, modifier = Modifier.entryAnimation(0, first))
            Spacer(Modifier.height(Space.xxl))
            Wordmark(Modifier.entryAnimation(1, first))
            Spacer(Modifier.height(Space.s))
            Text(
                "Catch apps secretly using your camera & mic",
                color = Spy.colors.textPrimary,
                style = MaterialTheme.typography.headlineLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .widthIn(max = 320.dp)
                    .entryAnimation(2, first)
                    .semantics { heading() },
            )
            Spacer(Modifier.height(Space.section))
            GroupedSection(Modifier.entryAnimation(3, first)) {
                BulletRow(Icons.Rounded.Videocam, SpyTiles.Camera, "Instant alert the moment a background app turns on your camera")
                RowDivider(64.dp)
                BulletRow(Icons.Rounded.Mic, SpyTiles.Mic, "Names the app that's listening through your microphone")
                RowDivider(64.dp)
                BulletRow(Icons.Rounded.Lock, SpyTiles.Safe, "Everything stays on your phone. No accounts, no uploads.")
            }
        }
    }
}

@Composable
private fun UsageStep() {
    val context = LocalContext.current
    val tick = rememberResumeTick()
    val granted = remember(tick) { ForegroundAppDetector.hasUsageAccess(context) }
    PermissionStep(
        icon = Icons.Rounded.QueryStats,
        tile = SpyTiles.Brand,
        step = 1,
        title = "Allow Usage Access",
        body = "Android tells SpyShield when the camera or mic switches on, but not who switched it on. " +
            "Usage Access lets us see which app was running at that moment so we can name it.",
        hint = "Find SpyShield in the list and switch it on, then come back.",
        granted = granted,
        button = "Open Usage Access",
        onAction = { context.safeStart(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) },
    )
}

@Composable
private fun AlertsStep() {
    val context = LocalContext.current
    val tick = rememberResumeTick()
    var refresh by remember { mutableIntStateOf(0) }
    val granted = remember(tick, refresh) { NotificationManagerCompat.from(context).areNotificationsEnabled() }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { refresh++ }
    PermissionStep(
        icon = Icons.Rounded.NotificationsActive,
        tile = SpyTiles.Warning,
        step = 2,
        title = "Turn on alerts",
        body = "SpyShield warns you the second a hidden app starts using your camera or mic, " +
            "and shows a quiet status notification while it's watching.",
        hint = "You can mute individual alert types later in system settings.",
        granted = granted,
        button = "Enable alerts",
        onAction = {
            if (Build.VERSION.SDK_INT >= 33) {
                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                context.safeStart(
                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                )
            }
        },
    )
}

@Composable
private fun StayRunningStep(onFinish: (Boolean) -> Unit) {
    val context = LocalContext.current
    val tick = rememberResumeTick()
    val granted = remember(tick) { isIgnoringBatteryOptimizations(context) }
    PermissionStep(
        icon = Icons.Rounded.BatteryChargingFull,
        tile = SpyTiles.Safe,
        step = 3,
        title = "Stay running",
        body = "Some phones put background apps to sleep to save battery, which would stop SpyShield from watching. " +
            "Exempting it keeps protection on around the clock. The impact on battery is tiny.",
        hint = "Choose \"Allow\" when Android asks.",
        granted = granted,
        button = "Keep SpyShield awake",
        onAction = { requestBatteryExemption(context) },
        // This page's single primary action is "Start protection", so the permission button steps back.
        secondaryAction = true,
    ) {
        Spacer(Modifier.height(Space.xxl))
        PrimaryButton(
            text = "Start protection",
            onClick = { onFinish(true) },
            icon = Icons.Rounded.Shield,
            height = 56.dp,
        )
        Spacer(Modifier.height(Space.xs))
        GhostButton(
            text = "Not now",
            onClick = { onFinish(false) },
            color = Spy.colors.textSecondary,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
    }
}

// ---------- Building blocks ----------

@Composable
private fun PermissionStep(
    icon: ImageVector,
    tile: Color,
    step: Int,
    title: String,
    body: String,
    hint: String,
    granted: Boolean,
    button: String,
    onAction: () -> Unit,
    secondaryAction: Boolean = false,
    footer: @Composable ColumnScope.() -> Unit = {},
) {
    val c = Spy.colors
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = Space.gutter, end = Space.gutter, top = Space.x4, bottom = Space.l),
        horizontalAlignment = Alignment.Start,
    ) {
        IconTile(icon, tile, 72.dp)
        Spacer(Modifier.height(Space.xxl))
        Text(
            "Step $step of ${PAGE_COUNT - 1}",
            style = MaterialTheme.typography.labelSmall.tnum(),
            color = c.brandText,
        )
        Spacer(Modifier.height(Space.s))
        Text(
            title,
            style = MaterialTheme.typography.headlineLarge,
            color = c.textPrimary,
            modifier = Modifier.semantics { heading() },
        )
        Spacer(Modifier.height(Space.m))
        Text(body, style = MaterialTheme.typography.bodyLarge, color = c.textSecondary)
        Spacer(Modifier.height(Space.section))
        AnimatedContent(
            targetState = granted,
            transitionSpec = {
                (fadeIn(tween(Motion.standard)) + scaleIn(tween(Motion.standard, easing = Motion.standardEasing), initialScale = 0.92f)) togetherWith
                    fadeOut(tween(150))
            },
            label = "granted",
            modifier = Modifier.fillMaxWidth(),
        ) { ok ->
            if (ok) {
                GrantedBlock()
            } else {
                Column(Modifier.fillMaxWidth()) {
                    if (secondaryAction) {
                        SecondaryButton(text = button, onClick = onAction)
                    } else {
                        PrimaryButton(text = button, onClick = onAction)
                    }
                    Spacer(Modifier.height(Space.m))
                    Row(verticalAlignment = Alignment.Top, modifier = Modifier.padding(horizontal = Space.xs)) {
                        Icon(
                            Icons.Rounded.Info, null,
                            tint = c.textSecondary,
                            modifier = Modifier.padding(top = 2.dp).size(14.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(hint, style = MaterialTheme.typography.bodySmall, color = c.textSecondary)
                    }
                }
            }
        }
        footer()
    }
}

@Composable
private fun GrantedBlock() {
    val c = Spy.colors
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.l))
            .background(c.safeContainer)
            .padding(Space.l),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.CheckCircle, null, tint = c.safe, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(Space.m))
        Column(Modifier.weight(1f)) {
            Text("All set", style = MaterialTheme.typography.titleMedium, color = c.onSafeContainer)
            Text("Permission granted", style = MaterialTheme.typography.bodySmall, color = c.onSafeContainer)
        }
    }
}

@Composable
private fun BulletRow(icon: ImageVector, tile: Color, text: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .padding(horizontal = Space.l, vertical = Space.m),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconTile(icon, tile, 32.dp)
        Spacer(Modifier.width(Space.l))
        Text(text, color = Spy.colors.textPrimary, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
    }
}

// ---------- System helpers ----------

private fun Context.safeStart(intent: Intent): Boolean = try {
    startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    true
} catch (e: ActivityNotFoundException) {
    false
} catch (e: SecurityException) {
    false
}

private fun isIgnoringBatteryOptimizations(context: Context): Boolean =
    context.getSystemService(PowerManager::class.java)?.isIgnoringBatteryOptimizations(context.packageName) == true

/** Direct "Allow" prompt first; some OEM builds drop it, so fall back to the full exemption list. */
private fun requestBatteryExemption(context: Context) {
    val direct = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:${context.packageName}"))
    if (!context.safeStart(direct)) context.safeStart(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
}
