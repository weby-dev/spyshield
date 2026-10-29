package com.spyshield.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.spyshield.app.MainViewModel
import com.spyshield.app.data.AccessEvent
import com.spyshield.app.data.Sensor
import com.spyshield.app.data.appLabel
import com.spyshield.app.data.buildReport
import com.spyshield.app.data.share

enum class EventFilter(val label: String) { ALL("All"), SUSPICIOUS("Alerts"), CAMERA("Camera"), MIC("Mic") }

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ActivityScreen(vm: MainViewModel, onOpenApp: (String) -> Unit) {
    val events by vm.events.collectAsStateWithLifecycle()
    var filter by rememberSaveable { mutableStateOf(EventFilter.ALL) }
    var confirmClear by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val firstEntry = rememberFirstEntry()
    val c = Spy.colors

    val shown = remember(events, filter) {
        events.filter {
            when (filter) {
                EventFilter.ALL -> true
                EventFilter.SUSPICIOUS -> it.suspicious
                EventFilter.CAMERA -> it.sensor == Sensor.CAMERA
                EventFilter.MIC -> it.sensor == Sensor.MIC
            }
        }.groupBy { dayLabel(it.start) }
    }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader(
            title = "Activity",
            subtitle = "Every time your camera or mic switched on",
        ) {
            CircleIconButton(
                Icons.Rounded.IosShare, "Share report",
                onClick = { share(context, buildReport(context, vm.apps.value, events, vm.blocked.value)) },
            )
            CircleIconButton(
                Icons.Rounded.DeleteSweep, "Clear history",
                onClick = { confirmClear = true },
                enabled = events.isNotEmpty(),
            )
        }

        SegmentedControl(
            options = EventFilter.entries.map { it.label },
            selectedIndex = filter.ordinal,
            onSelect = { filter = EventFilter.entries[it] },
            modifier = Modifier.padding(horizontal = Space.gutter),
        )
        Spacer(Modifier.height(Space.l))

        if (shown.isEmpty()) {
            Box(Modifier.fillMaxWidth().weight(1f).padding(horizontal = Space.gutter), contentAlignment = Alignment.Center) {
                EmptyState(
                    icon = Icons.Rounded.History,
                    title = "Nothing here yet",
                    body = "Camera and mic use will show up here.",
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentPadding = PaddingValues(start = Space.gutter, end = Space.gutter, bottom = Space.listBottom),
            ) {
                var entryIndex = 0
                var firstDay = true
                shown.forEach { (day, list) ->
                    if (!firstDay) {
                        // 20dp + the header's 8dp top padding = the 28dp section rhythm.
                        item(key = "s-$day", contentType = "gap") { Spacer(Modifier.height(Space.xl)) }
                    }
                    firstDay = false
                    stickyHeader(key = "h-$day", contentType = "day") {
                        Text(
                            day,
                            style = MaterialTheme.typography.titleSmall,
                            color = c.textSecondary,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(c.bg)
                                .padding(horizontal = Space.xs, vertical = Space.s)
                                .semantics { heading() },
                        )
                    }
                    val startIndex = entryIndex
                    itemsIndexed(list, key = { _, e -> e.id }, contentType = { _, _ -> "event" }) { i, e ->
                        EventCard(
                            e,
                            position = groupPosition(i, list.size),
                            modifier = Modifier
                                .animateItem(
                                    fadeInSpec = tween(Motion.standard),
                                    placementSpec = spring<IntOffset>(stiffness = Spring.StiffnessMediumLow),
                                    fadeOutSpec = tween(150),
                                )
                                .entryAnimation(startIndex + i, firstEntry),
                        ) { e.packageName?.let(onOpenApp) }
                    }
                    entryIndex += list.size
                }
            }
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            containerColor = c.surface2,
            shape = RoundedCornerShape(Radius.xxl),
            tonalElevation = 0.dp,
            icon = { Icon(Icons.Rounded.DeleteSweep, null, tint = c.danger, modifier = Modifier.size(28.dp)) },
            title = { Text("Clear activity history?", style = MaterialTheme.typography.headlineSmall, color = c.textPrimary) },
            text = {
                Text(
                    "This removes all recorded camera and mic events.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = c.textSecondary,
                )
            },
            confirmButton = {
                TextButton(onClick = { vm.clearHistory(); confirmClear = false }) {
                    Text("Clear", style = MaterialTheme.typography.labelLarge, color = c.danger)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) {
                    Text("Cancel", style = MaterialTheme.typography.labelLarge, color = c.textSecondary)
                }
            },
        )
    }
}

/**
 * One camera/mic event, drawn as a slice of a grouped inset list (DESIGN.md 7.4).
 * Pass [position] when the row sits in a group with others (Dashboard, detail sheet, timeline).
 */
@Composable
fun EventCard(
    e: AccessEvent,
    position: GroupPosition = GroupPosition.Single,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val context = LocalContext.current
    val c = Spy.colors
    val live = e.end == null
    // State beats identity: suspicious/blocked is danger, live is warning, otherwise the sensor's colour.
    val tone = when {
        e.blocked || e.suspicious -> Tone.DANGER
        live -> Tone.WARNING
        else -> e.sensor.tone
    }
    val hasBadge = live || e.autoBlocked || e.blocked || e.suspicious

    Row(
        modifier
            .groupItem(position, dividerInset = 72.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .heightIn(min = 72.dp)
            .padding(horizontal = Space.l, vertical = Space.m),
        verticalAlignment = Alignment.Top,
    ) {
        Box(Modifier.padding(top = Space.xxs)) {
            AppIconWithSensor(e.packageName, e.sensor, tone, 40.dp)
        }
        Spacer(Modifier.width(Space.l))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    e.appLabel,
                    style = MaterialTheme.typography.titleMedium,
                    color = c.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(Space.s))
                Text(
                    clock(e.start),
                    style = MaterialTheme.typography.labelMedium.tnum(),
                    color = c.textTertiary,
                    maxLines = 1,
                )
            }

            AnimatedVisibility(
                visible = hasBadge,
                enter = fadeIn(tween(Motion.standard)) + scaleIn(tween(Motion.standard), initialScale = 0.9f),
                exit = fadeOut(tween(150)) + shrinkVertically(tween(Motion.standard)),
            ) {
                Row(
                    Modifier.padding(top = Space.xs),
                    horizontalArrangement = Arrangement.spacedBy(Space.xs),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Live first so an auto-blocked app still using the sensor isn't hidden.
                    if (live && e.autoBlocked) StatusBadge("Live", Tone.WARNING, live = true)
                    when {
                        e.autoBlocked -> StatusBadge("Auto-blocked", Tone.DANGER)
                        live -> StatusBadge("Live", Tone.WARNING, live = true)
                        e.blocked -> StatusBadge("Blocked", Tone.DANGER, icon = Icons.Rounded.Block)
                        e.suspicious -> StatusBadge("Suspicious", Tone.DANGER)
                    }
                }
            }

            Spacer(Modifier.height(Space.xs))
            Text(
                e.reason,
                style = MaterialTheme.typography.bodyMedium,
                color = c.textSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(Space.xxs))
            val time = if (live) "in use now" else duration((e.end ?: e.start) - e.start)
            Text(
                "${e.sensor.title} · $time",
                style = MaterialTheme.typography.bodySmall.tnum(),
                color = c.textTertiary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (e.suspects.size > 1) {
                Spacer(Modifier.height(Space.xxs))
                Text(
                    "Possible: " + e.suspects.joinToString { context.appLabel(it) },
                    style = MaterialTheme.typography.bodySmall,
                    color = c.warning,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
