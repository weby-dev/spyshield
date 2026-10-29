package com.spyshield.app.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.spyshield.app.MainViewModel
import com.spyshield.app.data.Onboarding

/** Bottom-nav destinations. [icon] is the selected (rounded) glyph, [outlinedIcon] the unselected one. */
enum class Tab(val label: String, val icon: ImageVector, val outlinedIcon: ImageVector) {
    SHIELD("Shield", Icons.Rounded.Shield, Icons.Outlined.Shield),
    APPS("Apps", Icons.Rounded.Apps, Icons.Outlined.Apps),
    ACTIVITY("Activity", Icons.Rounded.History, Icons.Outlined.History),
}

@Composable
fun AppRoot(requestedTab: Tab?, onTabConsumed: () -> Unit, vm: MainViewModel = viewModel()) {
    val context = LocalContext.current
    var tab by rememberSaveable { mutableStateOf(Tab.SHIELD) }
    var selectedPkg by rememberSaveable { mutableStateOf<String?>(null) }
    val events by vm.events.collectAsStateWithLifecycle()
    val tick = rememberResumeTick()

    LaunchedEffect(tick) { vm.refresh() }
    LaunchedEffect(requestedTab) {
        if (requestedTab != null) {
            tab = requestedTab
            onTabConsumed()
        }
    }

    var onboarded by remember { mutableStateOf(Onboarding.isDone(context)) }
    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    // Onboarding asks for notifications itself; only returning users get this prompt.
    LaunchedEffect(Unit) {
        if (onboarded && Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    if (!onboarded) {
        OnboardingScreen { start ->
            Onboarding.markDone(context)
            onboarded = true
            if (start) vm.setProtection(true)
        }
        return
    }

    val dayAgo = System.currentTimeMillis() - 86_400_000L
    val unseenAlerts = events.count { it.suspicious && it.start > dayAgo }

    Scaffold(
        containerColor = Spy.colors.bg,
        bottomBar = { SpyNavigationBar(tab, unseenAlerts) { tab = it } },
    ) { padding ->
        Box(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .background(Spy.colors.bg)
        ) {
            AnimatedContent(
                targetState = tab,
                transitionSpec = {
                    fadeIn(tween(Motion.standard, easing = Motion.standardEasing)) togetherWith fadeOut(tween(Motion.fast))
                },
                label = "tab",
            ) { t ->
                when (t) {
                    Tab.SHIELD -> DashboardScreen(vm, onOpenTab = { tab = it }, onOpenApp = { selectedPkg = it })
                    Tab.APPS -> AppsScreen(vm) { selectedPkg = it }
                    Tab.ACTIVITY -> ActivityScreen(vm) { selectedPkg = it }
                }
            }
        }
    }

    selectedPkg?.let { pkg -> AppDetailSheet(pkg, vm) { selectedPkg = null } }
}

/** Navigation bar (4.19): surface1, 1px divider on the top edge, brand-container indicator. */
@Composable
private fun SpyNavigationBar(current: Tab, unseenAlerts: Int, onSelect: (Tab) -> Unit) {
    val c = Spy.colors
    val divider = c.divider
    NavigationBar(
        containerColor = c.surface1,
        contentColor = c.textSecondary,
        tonalElevation = 0.dp,
        modifier = Modifier.drawWithContent {
            drawContent()
            drawRect(divider, topLeft = Offset.Zero, size = Size(size.width, 1f))
        },
    ) {
        Tab.entries.forEach { t ->
            val selected = current == t
            NavigationBarItem(
                selected = selected,
                onClick = { onSelect(t) },
                icon = {
                    val glyph = if (selected) t.icon else t.outlinedIcon
                    if (t == Tab.ACTIVITY && unseenAlerts > 0) {
                        BadgedBox(
                            badge = {
                                Badge(containerColor = c.dangerStrong, contentColor = Color.White) {
                                    Text(
                                        if (unseenAlerts > 99) "99+" else "$unseenAlerts",
                                        style = MaterialTheme.typography.labelSmall.tnum(),
                                    )
                                }
                            },
                        ) { Icon(glyph, t.label, modifier = Modifier.size(24.dp)) }
                    } else {
                        Icon(glyph, t.label, modifier = Modifier.size(24.dp))
                    }
                },
                label = { Text(t.label, style = MaterialTheme.typography.labelMedium, maxLines = 1) },
                alwaysShowLabel = true,
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = c.onBrandContainer,
                    selectedTextColor = c.textPrimary,
                    indicatorColor = c.brandContainer,
                    unselectedIconColor = c.textSecondary,
                    unselectedTextColor = c.textSecondary,
                ),
            )
        }
    }
}
