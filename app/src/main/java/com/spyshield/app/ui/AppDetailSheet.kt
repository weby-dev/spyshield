package com.spyshield.app.ui

import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.PanTool
import androidx.compose.material.icons.rounded.RemoveCircleOutline
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.spyshield.app.MainViewModel
import com.spyshield.app.data.AppPrivacyInfo
import com.spyshield.app.data.TrustedApps
import com.spyshield.app.data.appDetailsIntent
import com.spyshield.app.data.appLabel
import com.spyshield.app.data.uninstallIntent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDetailSheet(pkg: String, vm: MainViewModel, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val apps by vm.apps.collectAsStateWithLifecycle()
    val blockedSet by vm.blocked.collectAsStateWithLifecycle()
    val events by vm.events.collectAsStateWithLifecycle()
    val info = apps.firstOrNull { it.packageName == pkg }
    val fallbackLabel = remember(pkg) { context.appLabel(pkg) }
    val label = info?.label ?: fallbackLabel
    val appEvents = events.filter { it.packageName == pkg }
    val isBlocked = pkg in blockedSet
    val trustedSet by TrustedApps.trusted.collectAsStateWithLifecycle()
    val isTrusted = pkg in trustedSet
    var showRevokeHint by remember { mutableStateOf(false) }
    val openSettings = { runCatching { context.startActivity(appDetailsIntent(pkg)) } }
    val c = Spy.colors

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        // Fully expanded, the sheet would otherwise slide under the status bar.
        modifier = Modifier.statusBarsPadding(),
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RoundedCornerShape(topStart = Radius.xxl, topEnd = Radius.xxl),
        containerColor = c.sheet,
        contentColor = c.textPrimary,
        tonalElevation = 0.dp,
        scrimColor = c.scrim,
        dragHandle = { SheetHandle() },
    ) {
        CompositionLocalProvider(LocalGroupColor provides c.surface2) {
            Column(
                Modifier
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .padding(horizontal = Space.gutter)
                    .padding(top = Space.s, bottom = Space.x4),
            ) {
                // ---- Identity header -------------------------------------------------------
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AppIcon(pkg, 64.dp)
                    Spacer(Modifier.width(Space.l))
                    Column(Modifier.weight(1f)) {
                        Text(
                            label,
                            style = MaterialTheme.typography.headlineSmall,
                            color = c.textPrimary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(Modifier.height(Space.xxs))
                        Text(
                            pkg,
                            color = c.textTertiary,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        AnimatedVisibility(
                            visible = isBlocked || isTrusted,
                            enter = fadeIn(tween(Motion.standard)) + scaleIn(tween(Motion.standard), initialScale = 0.9f),
                            exit = fadeOut(tween(Motion.fast)),
                        ) {
                            Row(
                                Modifier.padding(top = Space.s),
                                horizontalArrangement = Arrangement.spacedBy(Space.s),
                            ) {
                                if (isBlocked) StatusBadge("Blocked", Tone.DANGER, icon = Icons.Rounded.Block)
                                if (isTrusted) StatusBadge("Trusted", Tone.SAFE, icon = Icons.Rounded.VerifiedUser)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(Space.xxl))

                if (info != null) {
                    // ---- Spy risk ----------------------------------------------------------
                    SectionHeader("Spy risk")
                    Spacer(Modifier.height(Space.headerGap))
                    RiskSummary(info)

                    // ---- Access ------------------------------------------------------------
                    Spacer(Modifier.height(Space.section))
                    SectionHeader("Access")
                    Spacer(Modifier.height(Space.headerGap))
                    GroupedSection {
                        AccessRow("Camera", info.cameraRequested, info.cameraGranted)
                        RowDivider(Space.l)
                        AccessRow("Microphone", info.micRequested, info.micGranted)
                    }

                    // ---- Why this rating ---------------------------------------------------
                    if (info.reasons.isNotEmpty()) {
                        Spacer(Modifier.height(Space.section))
                        SectionHeader("Why this rating")
                        Spacer(Modifier.height(Space.headerGap))
                        GroupedSection {
                            info.reasons.forEachIndexed { i, r ->
                                if (i > 0) RowDivider(48.dp)
                                ReasonRow(r)
                            }
                        }
                    }
                    Spacer(Modifier.height(Space.section))
                }

                // ---- Recorded activity -----------------------------------------------------
                SectionHeader(
                    "Recorded activity",
                    trailingText = "${appEvents.size} uses · ${appEvents.count { it.suspicious }} suspicious",
                )
                Spacer(Modifier.height(Space.headerGap))
                if (appEvents.isEmpty()) {
                    GroupedSection {
                        EmptyState(
                            icon = Icons.Rounded.VerifiedUser,
                            title = "No camera or mic use recorded for this app yet.",
                            inline = true,
                            tone = Tone.SAFE,
                        )
                    }
                } else {
                    val shown = appEvents.take(5)
                    Column {
                        shown.forEachIndexed { i, e ->
                            EventCard(e, onClick = {}, position = groupPosition(i, shown.size))
                        }
                    }
                }

                // ---- Primary action --------------------------------------------------------
                Spacer(Modifier.height(Space.section))
                val toggleBlock: () -> Unit = {
                    vm.setBlocked(pkg, !isBlocked)
                    if (!isBlocked) showRevokeHint = true
                }
                if (isBlocked) {
                    SecondaryButton("Unblock app", onClick = toggleBlock, icon = Icons.Rounded.LockOpen)
                } else {
                    DestructiveButton("Block this app", onClick = toggleBlock, icon = Icons.Rounded.Block)
                }

                // ---- Secondary actions -----------------------------------------------------
                Spacer(Modifier.height(Space.l))
                GroupedSection {
                    ActionRow(
                        icon = Icons.Rounded.RemoveCircleOutline,
                        tile = SpyTiles.Brand,
                        title = "Revoke access",
                        trailing = { ExternalGlyph() },
                    ) { openSettings() }
                    RowDivider(64.dp)
                    ActionRow(
                        icon = Icons.Rounded.PanTool,
                        tile = SpyTiles.Warning,
                        title = "Force stop",
                        trailing = { ExternalGlyph() },
                    ) { openSettings() }
                    RowDivider(64.dp)
                    ActionRow(
                        icon = Icons.Rounded.VerifiedUser,
                        tile = SpyTiles.Safe,
                        title = if (isTrusted) "Trusted ✓ (tap to undo)" else "Mark as trusted",
                        trailing = if (isTrusted) {
                            { Icon(Icons.Rounded.CheckCircle, null, tint = c.safe, modifier = Modifier.size(20.dp)) }
                        } else {
                            null
                        },
                    ) { TrustedApps.setTrusted(pkg, !isTrusted) }
                    if (info?.isSystem != true) {
                        RowDivider(64.dp)
                        ActionRow(
                            icon = Icons.Rounded.DeleteForever,
                            tile = SpyTiles.Danger,
                            title = "Uninstall app",
                            titleColor = c.danger,
                            trailing = { ExternalGlyph() },
                        ) { runCatching { context.startActivity(uninstallIntent(pkg)) } }
                    }
                }

                Footnote(
                    "Blocked apps trigger an instant alarm the moment they touch your camera or mic, with one-tap revoke. " +
                        "Android doesn't let one app silently remove another app's permissions, so the final switch-off happens in Settings.",
                    Modifier.padding(top = Space.xs),
                )
            }
        }
    }

    if (showRevokeHint) {
        AlertDialog(
            onDismissRequest = { showRevokeHint = false },
            containerColor = c.surface2,
            shape = RoundedCornerShape(Radius.xxl),
            tonalElevation = 0.dp,
            iconContentColor = c.danger,
            titleContentColor = c.textPrimary,
            textContentColor = c.textSecondary,
            icon = { Icon(Icons.Rounded.Block, null, tint = c.danger, modifier = Modifier.size(28.dp)) },
            title = { Text("$label is blocked", style = MaterialTheme.typography.headlineSmall) },
            text = {
                Text(
                    "You'll get an alarm if it touches your camera or mic. To cut it off completely, open Permissions and set Camera and Microphone to \"Don't allow\".",
                    style = MaterialTheme.typography.bodyMedium,
                    color = c.textSecondary,
                )
            },
            confirmButton = {
                TextButton(onClick = { showRevokeHint = false; openSettings() }) {
                    Text("Open permissions", color = c.brandText, style = MaterialTheme.typography.labelLarge)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRevokeHint = false }) {
                    Text("Later", color = c.textSecondary, style = MaterialTheme.typography.labelLarge)
                }
            },
        )
    }
}

/** 36x4dp drag handle in textTertiary at 40%, 10dp vertical padding (4.16). */
@Composable
private fun SheetHandle() {
    Box(
        Modifier
            .padding(vertical = 10.dp)
            .size(width = 36.dp, height = 4.dp)
            .background(Spy.colors.textTertiary.copy(alpha = 0.4f), CircleShape)
    )
}

/** Score readout + 8dp meter inside a single group (7.3). */
@Composable
private fun RiskSummary(info: AppPrivacyInfo) {
    val c = Spy.colors
    val tone = info.level.tone
    val score = rememberCountUp(info.riskScore)
    val reduce = LocalReduceMotion.current
    val target = (info.riskScore / 100f).coerceIn(0f, 1f)
    val progress = remember { Animatable(if (reduce) target else 0f) }
    LaunchedEffect(target) {
        if (reduce) progress.snapTo(target)
        else progress.animateTo(target, tween(Motion.countDuration, easing = Motion.decelerate))
    }
    val desc = "Spy risk ${info.riskScore} of 100, ${info.level.title}"
    GroupedSection {
        Column(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 112.dp)
                .clearAndSetSemantics { contentDescription = desc }
                .padding(Space.l),
            verticalArrangement = Arrangement.Center,
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Row(Modifier.weight(1f)) {
                    Text(
                        "$score",
                        style = MaterialTheme.typography.displayMedium.tnum(),
                        color = c.textPrimary,
                        modifier = Modifier.alignByBaseline(),
                    )
                    Text(
                        "/100",
                        style = MaterialTheme.typography.titleLarge.tnum(),
                        color = c.textTertiary,
                        modifier = Modifier.alignByBaseline(),
                    )
                }
                StatusBadge(info.level.title, tone)
            }
            Spacer(Modifier.height(Space.m))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .background(c.control, CircleShape)
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(progress.value)
                        .fillMaxHeight()
                        .background(tone.fg, CircleShape)
                )
            }
        }
    }
}

/** "Camera / Allowed" style row. State colour + glyph come from the original mapping. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AccessRow(name: String, requested: Boolean, granted: Boolean) {
    val c = Spy.colors
    val (icon, color, state) = when {
        granted -> Triple(Icons.Rounded.WarningAmber, c.danger, "Allowed")
        requested -> Triple(Icons.Rounded.CheckCircle, c.safe, "Asked, but not allowed")
        else -> Triple(Icons.Rounded.CheckCircle, c.textTertiary, "Never asked")
    }
    FlowRow(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .padding(horizontal = Space.l, vertical = Space.m),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalArrangement = Arrangement.spacedBy(Space.xs),
    ) {
        Text(
            name,
            style = MaterialTheme.typography.titleMedium,
            color = c.textPrimary,
            modifier = Modifier.align(Alignment.CenterVertically).padding(end = Space.m),
        )
        Row(Modifier.align(Alignment.CenterVertically), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = color, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(state, color = color, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
        }
    }
}

@Composable
private fun ReasonRow(text: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .padding(horizontal = Space.l, vertical = Space.m),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            Icons.Rounded.WarningAmber,
            null,
            tint = Spy.colors.warning,
            modifier = Modifier.padding(top = 2.dp).size(16.dp),
        )
        Spacer(Modifier.width(Space.l))
        Text(text, color = Spy.colors.textPrimary, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun ActionRow(
    icon: ImageVector,
    tile: Color,
    title: String,
    titleColor: Color = Color.Unspecified,
    trailing: (@Composable () -> Unit)? = null,
    onClick: () -> Unit,
) {
    ListRow(
        leading = { IconTile(icon, tile, 32.dp) },
        title = title,
        trailing = trailing,
        onClick = onClick,
        titleColor = titleColor,
    )
}

/** Trailing glyph for rows that open a system screen. */
@Composable
private fun ExternalGlyph() {
    Icon(
        Icons.AutoMirrored.Rounded.OpenInNew,
        null,
        tint = Spy.colors.textTertiary,
        modifier = Modifier.size(18.dp),
    )
}
