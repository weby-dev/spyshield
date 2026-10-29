package com.spyshield.app.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
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
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.MicOff
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material.icons.rounded.VideocamOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.spyshield.app.MainViewModel
import com.spyshield.app.data.AppPrivacyInfo
import com.spyshield.app.data.RiskLevel
import com.spyshield.app.data.Sensor

enum class AppFilter(val label: String) { ALL("All"), CAMERA("Camera"), MIC("Microphone"), RISKY("High risk"), BLOCKED("Blocked") }

/** Divider inset for rows with a 44dp app icon: 16 padding + 44 icon + 16 gap. */
private val AppRowInset = 76.dp

@Composable
fun AppsScreen(vm: MainViewModel, onOpenApp: (String) -> Unit) {
    val apps by vm.apps.collectAsStateWithLifecycle()
    val blocked by vm.blocked.collectAsStateWithLifecycle()
    val scanning by vm.scanning.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf(AppFilter.ALL) }
    var showSystem by rememberSaveable { mutableStateOf(false) }

    val list = remember(apps, blocked, query, filter, showSystem) {
        apps.filter { a ->
            (showSystem || !a.isSystem || a.suspiciousUses > 0 || a.packageName in blocked) &&
                (query.isBlank() || a.label.contains(query, true) || a.packageName.contains(query, true)) &&
                when (filter) {
                    AppFilter.ALL -> true
                    AppFilter.CAMERA -> a.cameraGranted
                    AppFilter.MIC -> a.micGranted
                    AppFilter.RISKY -> a.level == RiskLevel.HIGH
                    AppFilter.BLOCKED -> a.packageName in blocked
                }
        }.sortedWith(compareByDescending<AppPrivacyInfo> { it.packageName in blocked }.thenByDescending { it.riskScore })
    }
    // Purely visual split of the already-sorted list: blocked apps first, then the rest.
    val firstUnblocked = list.indexOfFirst { it.packageName !in blocked }
    val blockedApps = if (firstUnblocked == -1) list else list.subList(0, firstUnblocked)
    val otherApps = if (firstUnblocked == -1) emptyList() else list.subList(firstUnblocked, list.size)

    val reduceMotion = LocalReduceMotion.current
    val angle = if (scanning && !reduceMotion) {
        val spin = rememberInfiniteTransition(label = "spin")
        spin.animateFloat(0f, 360f, infiniteRepeatable(tween(900, easing = LinearEasing)), label = "angle").value
    } else 0f
    val firstEntry = rememberFirstEntry()
    val reachCount = apps.count { !it.isSystem && (it.cameraGranted || it.micGranted) }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader(
            title = "App scanner",
            subtitle = "$reachCount apps can reach your camera or mic",
        ) {
            CircleIconButton(
                icon = Icons.Rounded.Refresh,
                contentDescription = "Rescan",
                onClick = vm::refresh,
                iconModifier = Modifier.rotate(angle),
            )
        }

        SearchField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.padding(horizontal = Space.gutter),
        )
        Spacer(Modifier.height(Space.m))

        FilterChipRow(
            labels = AppFilter.entries.map { it.label },
            selectedIndex = filter.ordinal,
            onSelect = { filter = AppFilter.entries[it] },
        )
        Spacer(Modifier.height(Space.m))

        GroupedSection(Modifier.padding(horizontal = Space.gutter)) {
            CompactToggleRow(title = "Show system apps", checked = showSystem, onCheckedChange = { showSystem = it })
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = Space.gutter, end = Space.gutter, top = Space.xl, bottom = Space.listBottom,
            ),
        ) {
            when {
                list.isEmpty() && scanning -> {
                    items(SKELETON_ROWS) { i ->
                        SkeletonRow(Modifier.groupItem(groupPosition(i, SKELETON_ROWS), AppRowInset), iconSize = 44.dp)
                    }
                }
                list.isEmpty() -> item(key = "empty") {
                    EmptyState(
                        icon = Icons.Rounded.SearchOff,
                        title = "No apps match.",
                        modifier = Modifier.animateItem(),
                    )
                }
                else -> {
                    val split = blockedApps.isNotEmpty()
                    if (split) {
                        sectionHeader("hdr_blocked", "Blocked", topGap = false)
                        appGroup(blockedApps, blocked, firstEntry, indexOffset = 0, onOpenApp = onOpenApp)
                    }
                    if (otherApps.isNotEmpty()) {
                        if (split) sectionHeader("hdr_apps", "Apps", topGap = true)
                        appGroup(otherApps, blocked, firstEntry, indexOffset = blockedApps.size, onOpenApp = onOpenApp)
                    }
                }
            }
        }
    }
}

private const val SKELETON_ROWS = 6

private fun LazyListScope.sectionHeader(key: String, title: String, topGap: Boolean) {
    item(key = key) {
        SectionHeader(
            title,
            Modifier
                .animateItem()
                .padding(top = if (topGap) Space.section else 0.dp, bottom = Space.headerGap),
        )
    }
}

private fun LazyListScope.appGroup(
    group: List<AppPrivacyInfo>,
    blocked: Set<String>,
    firstEntry: Boolean,
    indexOffset: Int,
    onOpenApp: (String) -> Unit,
) {
    itemsIndexed(group, key = { _, a -> a.packageName }) { i, a ->
        AppRow(
            a = a,
            blocked = a.packageName in blocked,
            position = groupPosition(i, group.size),
            modifier = Modifier
                .animateItem(
                    fadeInSpec = tween(Motion.standard),
                    placementSpec = spring<IntOffset>(stiffness = Spring.StiffnessMediumLow),
                    fadeOutSpec = tween(150),
                )
                .entryAnimation(indexOffset + i, firstEntry),
        ) { onOpenApp(a.packageName) }
    }
}

@Composable
private fun SearchField(value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier) {
    val c = Spy.colors
    val src = remember { MutableInteractionSource() }
    val focused by src.collectIsFocusedAsState()
    val borderColor by animateColorAsState(
        if (focused) c.brand else Color.Transparent,
        tween(Motion.standard, easing = Motion.standardEasing),
        label = "searchBorder",
    )
    val shape = RoundedCornerShape(Radius.l)
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = c.textPrimary),
        cursorBrush = SolidColor(c.brand),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        interactionSource = src,
        decorationBox = { inner ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .clip(shape)
                    .background(c.surface1)
                    .border(1.5.dp, borderColor, shape)
                    .padding(start = Space.l, end = Space.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.Search, null, tint = c.textTertiary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(Space.m))
                Box(Modifier.weight(1f).padding(vertical = Space.m), contentAlignment = Alignment.CenterStart) {
                    if (value.isEmpty()) {
                        Text(
                            "Search apps",
                            style = MaterialTheme.typography.bodyLarge,
                            color = c.textTertiary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    inner()
                }
                if (value.isNotEmpty()) {
                    ClearButton { onValueChange("") }
                } else {
                    Spacer(Modifier.width(Space.m))
                }
            }
        },
    )
}

/** Small `control` circle with a close glyph, inside a 48dp touch target. */
@Composable
private fun ClearButton(onClick: () -> Unit) {
    CircleIconButton(
        icon = Icons.Rounded.Close,
        contentDescription = "Clear",
        onClick = onClick,
        iconModifier = Modifier.size(16.dp),
    )
}

@Composable
private fun AppRow(
    a: AppPrivacyInfo,
    blocked: Boolean,
    position: GroupPosition,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val hasSensorLine = a.cameraRequested || a.micRequested || a.preGranted
    val usageLine = if (a.lastUsed > 0) "Opened ${ago(a.lastUsed)}" else if (a.isSystem) "System app" else ""
    val meta: (@Composable () -> Unit)? = if (hasSensorLine || usageLine.isNotEmpty()) {
        @Composable {
            Column(verticalArrangement = Arrangement.spacedBy(Space.xs)) {
                if (hasSensorLine) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        SensorGlyph(Sensor.CAMERA, a.cameraRequested, a.cameraGranted)
                        SensorGlyph(Sensor.MIC, a.micRequested, a.micGranted)
                        if (a.preGranted) StatusBadge("Pre-installed", Tone.NEUTRAL)
                    }
                }
                if (usageLine.isNotEmpty()) {
                    Text(
                        usageLine,
                        color = Spy.colors.textTertiary,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    } else null

    ListRow(
        leading = { AppIcon(a.packageName, 44.dp) },
        title = a.label,
        modifier = modifier.groupItem(position, AppRowInset),
        meta = meta,
        trailing = {
            if (blocked) StatusBadge("Blocked", Tone.DANGER, icon = Icons.Rounded.Block)
            else RiskIndicator(a.level, a.riskScore)
        },
        onClick = onClick,
    )
}

@Composable
private fun SensorGlyph(sensor: Sensor, requested: Boolean, granted: Boolean) {
    if (!requested) return
    val c = Spy.colors
    val icon = when {
        granted -> sensor.icon
        sensor == Sensor.CAMERA -> Icons.Rounded.VideocamOff
        else -> Icons.Rounded.MicOff
    }
    val tint = if (granted) (if (sensor == Sensor.CAMERA) c.camera else c.mic) else c.textTertiary
    Icon(
        icon,
        if (granted) "${sensor.title} allowed" else "${sensor.title} not allowed",
        tint = tint,
        modifier = Modifier.size(16.dp),
    )
}
