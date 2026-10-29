package com.spyshield.app.ui

import android.content.Context
import android.text.format.DateUtils
import android.util.LruCache
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.spyshield.app.data.RiskLevel
import com.spyshield.app.data.Sensor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// =============================================================================================
// Data helpers
// =============================================================================================

val Sensor.icon: ImageVector get() = if (this == Sensor.CAMERA) Icons.Rounded.Videocam else Icons.Rounded.Mic

/** Identity tone of a sensor (camera blue, mic purple). State tones win over this. */
val Sensor.tone: Tone get() = if (this == Sensor.CAMERA) Tone.CAMERA else Tone.MIC

/** Semantic tone of a risk level: HIGH -> DANGER, MEDIUM -> WARNING, LOW -> SAFE. */
val RiskLevel.tone: Tone
    get() = when (this) {
        RiskLevel.HIGH -> Tone.DANGER
        RiskLevel.MEDIUM -> Tone.WARNING
        RiskLevel.LOW -> Tone.SAFE
    }

/** Foreground colour of a risk level in the active theme. Prefer [RiskLevel.tone]. */
val RiskLevel.color: Color
    get() = when (this) {
        RiskLevel.HIGH -> SC.Danger
        RiskLevel.MEDIUM -> SC.Warn
        RiskLevel.LOW -> SC.Safe
    }

private val Color.orSurface1: Color
    @Composable get() = if (this == Color.Unspecified) Spy.colors.surface1 else this

/** The colour of the group the caller is drawn in (sheet groups are `surface2`). */
val currentGroupColor: Color
    @Composable get() = LocalGroupColor.current.orSurface1

// =============================================================================================
// Motion modifiers
// =============================================================================================

/** Scales the element down while [interactionSource] is pressed (tiles 0.97, buttons 0.98). */
@Composable
fun Modifier.pressScale(interactionSource: MutableInteractionSource, pressed: Float = 0.97f): Modifier {
    val isPressed by interactionSource.collectIsPressedAsState()
    val s by animateFloatAsState(if (isPressed) pressed else 1f, Motion.pressSpring, label = "press")
    return this.graphicsLayer { scaleX = s; scaleY = s }
}

/**
 * First-composition entry: the first 8 items fade in and rise 8dp, staggered 30ms.
 * Pass `enabled = false` after the screen's first composition, and under reduce motion it is skipped.
 */
@Composable
fun Modifier.entryAnimation(index: Int, enabled: Boolean): Modifier {
    val reduce = LocalReduceMotion.current
    val active = enabled && !reduce && index < 8
    var shown by remember { mutableStateOf(!active) }
    LaunchedEffect(active) {
        if (active) {
            delay(index * 30L)
            shown = true
        } else shown = true
    }
    val p by animateFloatAsState(
        if (shown) 1f else 0f,
        tween(Motion.standard, easing = Motion.standardEasing),
        label = "entry",
    )
    if (!active) return this
    return this.graphicsLayer {
        alpha = p
        translationY = (1f - p) * 8.dp.toPx()
    }
}

/** Counts from 0 to [target] on first show (600ms, decelerate), then animates only when it changes. */
@Composable
fun rememberCountUp(target: Int): Int {
    val reduce = LocalReduceMotion.current
    val anim = remember { Animatable(if (reduce) target.toFloat() else 0f) }
    LaunchedEffect(target) {
        if (reduce) anim.snapTo(target.toFloat())
        else anim.animateTo(target.toFloat(), tween(Motion.countDuration, easing = Motion.decelerate))
    }
    return anim.value.toInt()
}

// =============================================================================================
// Shield path + wordmark
// =============================================================================================

/**
 * The SpyShield shield silhouette (normalised 100 x 116 box), scaled to [height] and centred on
 * [center]. `M50,0 L92,15 Q100,18 100,26 V56 C100,85 79,106 50,116 C21,106 0,85 0,56 V26 Q0,18 8,15 Z`
 */
fun shieldPath(center: Offset, height: Float): Path {
    val k = height / 116f
    val left = center.x - 50f * k
    val top = center.y - 58f * k
    fun x(v: Float) = left + v * k
    fun y(v: Float) = top + v * k
    return Path().apply {
        moveTo(x(50f), y(0f))
        lineTo(x(92f), y(15f))
        quadraticTo(x(100f), y(18f), x(100f), y(26f))
        lineTo(x(100f), y(56f))
        cubicTo(x(100f), y(85f), x(79f), y(106f), x(50f), y(116f))
        cubicTo(x(21f), y(106f), x(0f), y(85f), x(0f), y(56f))
        lineTo(x(0f), y(26f))
        quadraticTo(x(0f), y(18f), x(8f), y(15f))
        close()
    }
}

/** The shield logomark: a filled shield with a white lens. */
@Composable
fun Logomark(size: Dp = 24.dp, color: Color = Spy.colors.brand, lensColor: Color = Color.White) {
    Canvas(Modifier.size(size)) {
        val c = Offset(this.size.width / 2f, this.size.height / 2f)
        drawPath(shieldPath(c, this.size.height), color)
        drawCircle(lensColor, radius = this.size.height * (3.5f / 24f), center = Offset(c.x, c.y - this.size.height * 0.03f))
    }
}

/** "SpyShield" wordmark: 24dp logomark, 8dp gap, headlineLarge at weight 800, one colour. */
@Composable
fun Wordmark(modifier: Modifier = Modifier, color: Color = Spy.colors.textPrimary, markColor: Color = Spy.colors.brand) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Logomark(24.dp, markColor)
        Spacer(Modifier.width(Space.s))
        Text(
            "SpyShield",
            style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.ExtraBold),
            color = color,
        )
    }
}

// =============================================================================================
// Screen header, section header
// =============================================================================================

/** Screen title block (4.18): 20dp sides, 16dp top, 20dp bottom. Actions are [CircleIconButton]s. */
@Composable
fun ScreenHeader(title: String, subtitle: String, modifier: Modifier = Modifier, actions: @Composable RowScope.() -> Unit = {}) {
    Row(
        modifier.fillMaxWidth().padding(start = Space.gutter, end = Space.gutter, top = Space.l, bottom = Space.xl),
        verticalAlignment = Alignment.Top,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.headlineLarge, color = Spy.colors.textPrimary, modifier = Modifier.semantics { heading() })
            Spacer(Modifier.height(Space.xs))
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = Spy.colors.textSecondary)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Space.s), verticalAlignment = Alignment.CenterVertically, content = actions)
    }
}

/** 40dp `control` circle with a 20dp glyph and a 48dp touch target. */
@Composable
fun CircleIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    iconModifier: Modifier = Modifier,
) {
    val src = remember { MutableInteractionSource() }
    Box(
        modifier
            .minimumInteractiveComponentSize()
            .pressScale(src, 0.94f)
            .size(40.dp)
            .clip(CircleShape)
            .background(Spy.colors.control)
            .clickable(src, LocalIndication.current, enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon, contentDescription,
            tint = if (enabled) Spy.colors.textPrimary else Spy.colors.textTertiary,
            modifier = iconModifier.size(20.dp),
        )
    }
}

/**
 * Section header (4.2). Callers add 28dp above (after a previous group) and 8dp below.
 * [action] + [onAction] render a "See all" style link; [trailingText] renders non-clickable meta.
 */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    action: String? = null,
    onAction: (() -> Unit)? = null,
    trailingText: String? = null,
) {
    Row(
        modifier.fillMaxWidth().heightIn(min = 32.dp).padding(horizontal = Space.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            color = Spy.colors.textSecondary,
            modifier = Modifier.weight(1f).semantics { heading() },
        )
        if (trailingText != null) {
            Text(trailingText, style = MaterialTheme.typography.labelMedium.tnum(), color = Spy.colors.textSecondary, maxLines = 1)
        }
        if (action != null && onAction != null) {
            Box(
                Modifier
                    .minimumInteractiveComponentSize()
                    .clip(RoundedCornerShape(Radius.s))
                    .clickable(role = Role.Button, onClick = onAction)
                    .padding(horizontal = Space.s),
                contentAlignment = Alignment.Center,
            ) {
                Text(action, style = MaterialTheme.typography.labelMedium, color = Spy.colors.brandText)
            }
        }
    }
}

// =============================================================================================
// Grouped inset lists
// =============================================================================================

enum class GroupPosition { Single, First, Middle, Last }

fun groupPosition(index: Int, count: Int): GroupPosition = when {
    count <= 1 -> GroupPosition.Single
    index == 0 -> GroupPosition.First
    index == count - 1 -> GroupPosition.Last
    else -> GroupPosition.Middle
}

fun groupShape(position: GroupPosition): RoundedCornerShape = when (position) {
    GroupPosition.Single -> RoundedCornerShape(Radius.xl)
    GroupPosition.First -> RoundedCornerShape(topStart = Radius.xl, topEnd = Radius.xl)
    GroupPosition.Middle -> RoundedCornerShape(0.dp)
    GroupPosition.Last -> RoundedCornerShape(bottomStart = Radius.xl, bottomEnd = Radius.xl)
}

/** Rounded inset group container (4.3). No padding, no border. Separate rows with [RowDivider]. */
@Composable
fun GroupedSection(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier.fillMaxWidth().clip(RoundedCornerShape(Radius.xl)).background(currentGroupColor),
        content = content,
    )
}

/**
 * One slice of a group drawn by a lazy list item. Middle/Last slices draw a 1px divider on their
 * top edge, starting at [dividerInset] (72dp for 40dp leading, 76dp for 44dp, 64dp for 32dp tiles).
 */
@Composable
fun Modifier.groupItem(position: GroupPosition, dividerInset: Dp = 72.dp): Modifier {
    val bg = currentGroupColor
    val divider = Spy.colors.divider
    val drawTop = position == GroupPosition.Middle || position == GroupPosition.Last
    return this
        .fillMaxWidth()
        .clip(groupShape(position))
        .background(bg)
        .then(
            if (drawTop) Modifier.drawWithContent {
                drawContent()
                val px = 1f
                val start = dividerInset.toPx()
                drawRect(divider, topLeft = Offset(start, 0f), size = Size((size.width - start).coerceAtLeast(0f), px))
            } else Modifier
        )
}

/** 1px hairline inside a group, starting at [inset]. */
@Composable
fun RowDivider(inset: Dp) {
    val density = LocalDensity.current
    HorizontalDivider(
        modifier = Modifier.padding(start = inset),
        thickness = with(density) { 1f.toDp() },
        color = Spy.colors.divider,
    )
}

/**
 * Standard group row (4.3): min 64dp, 16/12 padding, 16dp after leading, 12dp before trailing.
 * Title is single-line; subtitle up to 2 lines; [meta] is a free slot under the subtitle.
 */
@Composable
fun ListRow(
    leading: (@Composable () -> Unit)?,
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    meta: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    titleColor: Color = Color.Unspecified,
) {
    Row(
        modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .heightIn(min = 64.dp)
            .padding(horizontal = Space.l, vertical = Space.m),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leading != null) {
            leading()
            Spacer(Modifier.width(Space.l))
        }
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                color = if (titleColor == Color.Unspecified) Spy.colors.textPrimary else titleColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle != null) {
                Spacer(Modifier.height(Space.xxs))
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Spy.colors.textSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (meta != null) {
                Spacer(Modifier.height(Space.xs))
                meta()
            }
        }
        if (trailing != null) {
            Spacer(Modifier.width(Space.m))
            trailing()
        }
    }
}

/** Trailing chevron for navigational rows. */
@Composable
fun RowChevron() {
    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = Spy.colors.textTertiary, modifier = Modifier.size(20.dp))
}

// =============================================================================================
// Switches and toggle rows
// =============================================================================================

/** Brand switch. Pass `onCheckedChange = null` when the whole row is the toggle target. */
@Composable
fun SpySwitch(checked: Boolean, onCheckedChange: ((Boolean) -> Unit)?, modifier: Modifier = Modifier) {
    val c = Spy.colors
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        colors = SwitchDefaults.colors(
            checkedTrackColor = c.brand,
            checkedThumbColor = Color.White,
            checkedBorderColor = Color.Transparent,
            checkedIconColor = c.brand,
            uncheckedTrackColor = c.control,
            uncheckedThumbColor = c.textTertiary,
            uncheckedBorderColor = Color.Transparent,
            uncheckedIconColor = c.control,
        ),
    )
}

/**
 * Settings-style toggle row (4.4): 32dp solid tile, title + subtitle, optional tone [note], switch.
 * The whole row toggles (Role.Switch) and routes through [onCheckedChange].
 * [subtitleStyle] lets long explanations use bodySmall.
 */
@Composable
fun ToggleRow(
    tileIcon: ImageVector,
    tileColor: Color,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    note: String? = null,
    noteTone: Tone = Tone.WARNING,
    subtitleStyle: TextStyle = MaterialTheme.typography.bodyMedium,
    enabled: Boolean = true,
) {
    val haptics = LocalHapticFeedback.current
    Row(
        modifier
            .fillMaxWidth()
            .toggleable(value = checked, enabled = enabled, role = Role.Switch) {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onCheckedChange(it)
            }
            .heightIn(min = 64.dp)
            .padding(horizontal = Space.l, vertical = Space.m),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconTile(tileIcon, tileColor, 32.dp)
        Spacer(Modifier.width(Space.l))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = Spy.colors.textPrimary)
            Spacer(Modifier.height(Space.xxs))
            Text(subtitle, style = subtitleStyle, color = Spy.colors.textSecondary)
            if (note != null) {
                Spacer(Modifier.height(Space.xs + Space.xxs))
                Row(verticalAlignment = Alignment.Top) {
                    Icon(Icons.Rounded.Info, null, tint = noteTone.fg, modifier = Modifier.padding(top = 2.dp).size(14.dp))
                    Spacer(Modifier.width(Space.xs))
                    Text(note, style = MaterialTheme.typography.bodySmall, color = noteTone.fg)
                }
            }
        }
        Spacer(Modifier.width(Space.m))
        SpySwitch(checked, null)
    }
}

/** Tile-less compact switch row (e.g. "Show system apps"), min 52dp, bodyMedium title. */
@Composable
fun CompactToggleRow(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
            .heightIn(min = 52.dp)
            .padding(horizontal = Space.l, vertical = Space.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.bodyMedium, color = Spy.colors.textPrimary, modifier = Modifier.weight(1f))
        Spacer(Modifier.width(Space.m))
        SpySwitch(checked, null)
    }
}

// =============================================================================================
// Icon tiles
// =============================================================================================

/** Solid iOS-settings-style square with a white glyph (4.5). Use a `SpyTiles.*` colour. */
@Composable
fun IconTile(icon: ImageVector, color: Color, size: Dp = 32.dp) {
    val radius = when {
        size >= 72.dp -> 22.dp
        size >= 56.dp -> 18.dp
        else -> size * 0.28f
    }
    Box(
        Modifier.size(size).clip(RoundedCornerShape(radius)).background(color),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(size * 0.56f))
    }
}

/** Tonal square: tone container fill with a tone foreground glyph at 50% (4.5). */
@Composable
fun TonalIcon(icon: ImageVector, tone: Tone, size: Dp = 40.dp) {
    val radius = if (size >= 56.dp) 18.dp else size * 0.3f
    Box(
        Modifier.size(size).clip(RoundedCornerShape(radius)).background(tone.container),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, tint = tone.fg, modifier = Modifier.size(size * 0.5f))
    }
}

// =============================================================================================
// App icons
// =============================================================================================

private object IconCache {
    private val cache = LruCache<String, ImageBitmap>(200)
    fun get(pkg: String?) = pkg?.let { cache.get(it) }
    fun load(context: Context, pkg: String): ImageBitmap? = runCatching {
        context.packageManager.getApplicationIcon(pkg).toBitmap(128, 128).asImageBitmap().also { cache.put(pkg, it) }
    }.getOrNull()
}

@Composable
fun AppIcon(pkg: String?, size: Dp) {
    val context = LocalContext.current
    val bitmap by produceState(IconCache.get(pkg), pkg) {
        if (value == null && pkg != null) value = withContext(Dispatchers.IO) { IconCache.load(context, pkg) }
    }
    val shape = RoundedCornerShape(size * 0.26f)
    val bmp = bitmap
    if (bmp != null) {
        Image(bmp, null, Modifier.size(size).clip(shape))
    } else {
        Box(Modifier.size(size).clip(shape).background(Spy.colors.control), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.Android, null, tint = Spy.colors.textTertiary, modifier = Modifier.size(size * 0.55f))
        }
    }
}

/**
 * App icon with an 18dp sensor badge at the bottom-right (4.6). [tone] picks the badge fill:
 * DANGER -> tileDanger, WARNING -> tileWarning, anything else -> the sensor's identity tile.
 * With a null [pkg] it renders a [TonalIcon] of the sensor.
 */
@Composable
fun AppIconWithSensor(pkg: String?, sensor: Sensor, tone: Tone, size: Dp = 40.dp) {
    if (pkg == null) {
        TonalIcon(sensor.icon, tone, size)
        return
    }
    val badge = when (tone) {
        Tone.DANGER -> SpyTiles.Danger
        Tone.WARNING -> SpyTiles.Warning
        else -> sensor.tone.tile
    }
    Box(Modifier.size(size)) {
        AppIcon(pkg, size)
        Box(
            Modifier
                .align(Alignment.BottomEnd)
                .offset(6.dp, 6.dp)
                .size(22.dp)
                .background(currentGroupColor, CircleShape)
                .padding(2.dp)
                .background(badge, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(sensor.icon, null, tint = Color.White, modifier = Modifier.size(11.dp))
        }
    }
}

// =============================================================================================
// Badges, risk indicator, live dot
// =============================================================================================

/** Sentence-case status badge (4.10): 22dp, radius 6, tone container fill. */
@Composable
fun StatusBadge(text: String, tone: Tone, modifier: Modifier = Modifier, icon: ImageVector? = null, live: Boolean = false) {
    val fg = if (tone == Tone.NEUTRAL) Spy.colors.textSecondary else tone.onContainer
    Row(
        modifier
            .heightIn(min = 22.dp)
            .background(tone.container, RoundedCornerShape(Radius.xs))
            .padding(horizontal = Space.s, vertical = Space.xxs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.xs),
    ) {
        if (live) LiveDot(tone.fg, 6.dp)
        if (icon != null) Icon(icon, null, tint = fg, modifier = Modifier.size(12.dp))
        Text(text, style = MaterialTheme.typography.labelSmall, color = fg, maxLines = 1)
    }
}

/** 44dp risk square used as the trailing element of app rows (4.11). */
@Composable
fun RiskIndicator(level: RiskLevel, score: Int) {
    val tone = level.tone
    val desc = "Risk $score of 100, ${level.title}"
    Box(
        Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(Radius.m))
            .background(tone.container)
            .semantics(mergeDescendants = true) { contentDescription = desc },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            "$score",
            style = MaterialTheme.typography.titleMedium.tnum().copy(fontWeight = FontWeight.Bold),
            color = tone.fg,
        )
    }
}

/** Live indicator (4.20): solid core + one expanding halo. Static under reduce motion. */
@Composable
fun LiveDot(color: Color, size: Dp = 8.dp) {
    val reduce = LocalReduceMotion.current
    if (reduce) {
        Box(Modifier.size(size).background(color, CircleShape))
        return
    }
    val t = rememberInfiniteTransition(label = "live")
    val p by t.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(1200, easing = LinearOutSlowInEasing), RepeatMode.Restart),
        label = "halo",
    )
    Box(
        Modifier.size(size).drawBehind {
            val r = this.size.minDimension / 2f
            drawCircle(color.copy(alpha = 0.45f * (1f - p)), radius = r * (1f + 1.4f * p))
            drawCircle(color, radius = r)
        }
    )
}

// =============================================================================================
// Tiles
// =============================================================================================

/** Shared tile shell: radius 20, padding 16, press scale 0.97 + ripple. */
@Composable
private fun TileShell(
    modifier: Modifier,
    minHeight: Dp,
    color: Color,
    onClick: (() -> Unit)?,
    content: @Composable ColumnScope.() -> Unit,
) {
    val src = remember { MutableInteractionSource() }
    Column(
        modifier
            .then(if (onClick != null) Modifier.pressScale(src) else Modifier)
            .clip(RoundedCornerShape(Radius.xl))
            .background(color)
            .then(
                if (onClick != null) Modifier.clickable(src, LocalIndication.current, role = Role.Button, onClick = onClick)
                else Modifier
            )
            .heightIn(min = minHeight)
            .padding(Space.l),
        content = content,
    )
}

/**
 * Overview stat tile (4.7). Put two in a Row with `Modifier.weight(1f)` and 12dp gaps.
 * [value] counts up on first show. Use [valueText] to override the formatted number.
 */
@Composable
fun StatTile(
    icon: ImageVector,
    value: Int,
    label: String,
    tone: Tone,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    valueText: String? = null,
) {
    val shown = rememberCountUp(value)
    TileShell(modifier, 112.dp, Spy.colors.surface1, onClick) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            TonalIcon(icon, tone, 36.dp)
            Spacer(Modifier.weight(1f))
            if (onClick != null) RowChevron()
        }
        Spacer(Modifier.weight(1f).heightIn(min = Space.m))
        Text(
            valueText ?: "$shown",
            style = MaterialTheme.typography.displaySmall.tnum(),
            color = Spy.colors.textPrimary,
        )
        Text(label, style = MaterialTheme.typography.bodySmall, color = Spy.colors.textSecondary, maxLines = 2)
    }
}

/**
 * Dashboard sensor tile (4.8). [stateWord] is "Idle" / "In use" / "Spying?" / "Not watched";
 * [detail] is the app label, "No app is using it" or "Turn on protection".
 * [tone]: suspicious DANGER, active WARNING, protection off NEUTRAL, else SAFE.
 * When [suspicious] the whole tile turns `dangerContainer`.
 */
@Composable
fun SensorTile(
    sensor: Sensor,
    stateWord: String,
    detail: String,
    tone: Tone,
    active: Boolean,
    suspicious: Boolean,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val bg by animateColorAsState(
        if (suspicious) Spy.colors.dangerContainer else Spy.colors.surface1,
        tween(Motion.standard, easing = Motion.standardEasing),
        label = "sensorBg",
    )
    TileShell(modifier, 132.dp, bg, onClick) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TonalIcon(sensor.icon, tone, 40.dp)
            Spacer(Modifier.weight(1f))
            if (active) LiveDot(tone.fg)
        }
        Spacer(Modifier.height(14.dp))
        Text(sensor.title, style = MaterialTheme.typography.labelSmall, color = Spy.colors.textSecondary)
        Spacer(Modifier.height(Space.xxs))
        Text(
            stateWord,
            style = MaterialTheme.typography.titleLarge,
            color = if (active) tone.fg else Spy.colors.textPrimary,
        )
        Text(
            detail,
            style = MaterialTheme.typography.bodySmall,
            color = Spy.colors.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** True when tile pairs should stack into one column (font scale >= 1.5). */
@Composable
fun useSingleColumnTiles(): Boolean = LocalDensity.current.fontScale >= 1.5f

// =============================================================================================
// Buttons
// =============================================================================================

@Composable
private fun BaseButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier,
    container: Color,
    content: Color,
    icon: ImageVector?,
    trailingIcon: ImageVector?,
    compact: Boolean,
    enabled: Boolean,
    height: Dp,
) {
    val src = remember { MutableInteractionSource() }
    val fill = if (enabled) container else Spy.colors.control
    val fg = if (enabled) content else Spy.colors.textTertiary
    val shape = RoundedCornerShape(if (compact) Radius.m else Radius.l)
    Row(
        modifier
            .then(if (compact) Modifier.minimumInteractiveComponentSize() else Modifier.fillMaxWidth())
            .pressScale(src, 0.98f)
            .heightIn(min = if (compact) 40.dp else height)
            .clip(shape)
            .background(fill)
            .clickable(src, LocalIndication.current, enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = if (compact) Space.l else Space.xl, vertical = Space.s),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, null, tint = fg, modifier = Modifier.size(if (compact) 18.dp else 20.dp))
            Spacer(Modifier.width(Space.s))
        }
        Text(
            text,
            style = if (compact) MaterialTheme.typography.labelMedium else MaterialTheme.typography.labelLarge,
            color = fg,
            textAlign = TextAlign.Center,
        )
        if (trailingIcon != null) {
            Spacer(Modifier.width(Space.s))
            Icon(trailingIcon, null, tint = fg, modifier = Modifier.size(18.dp))
        }
    }
}

/** Brand (or WARNING) filled button (4.12). Full width at 52dp unless [compact] (40dp, wraps). */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    tone: Tone = Tone.BRAND,
    compact: Boolean = false,
    enabled: Boolean = true,
    height: Dp = 52.dp,
) {
    val c = Spy.colors
    val (bg, fg) = when (tone) {
        Tone.WARNING -> c.warning to c.onWarning
        Tone.DANGER -> c.dangerStrong to Color.White
        else -> c.brand to c.textOnAccent
    }
    BaseButton(text, onClick, modifier, bg, fg, icon, trailingIcon, compact, enabled, height)
}

/** `control` filled button with `textPrimary` content. */
@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    compact: Boolean = false,
) {
    BaseButton(text, onClick, modifier, Spy.colors.control, Spy.colors.textPrimary, icon, null, compact, true, 52.dp)
}

/** `dangerStrong` filled button with white content. */
@Composable
fun DestructiveButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null) {
    BaseButton(text, onClick, modifier, Spy.colors.dangerStrong, Color.White, icon, null, false, true, 52.dp)
}

/** Text-only tertiary action, min 48dp. [color] defaults to `brandText`. */
@Composable
fun GhostButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    enabled: Boolean = true,
) {
    val fg = when {
        !enabled -> Spy.colors.textTertiary
        color == Color.Unspecified -> Spy.colors.brandText
        else -> color
    }
    Box(
        modifier
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(Radius.m))
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = Space.m),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = MaterialTheme.typography.labelMedium, color = fg)
    }
}

// =============================================================================================
// Callout, footnote, empty state
// =============================================================================================

/**
 * Tinted setup/notice card (4.13). [content] goes under the body (typically 12dp spacer + a
 * compact `PrimaryButton(tone = Tone.WARNING)`, or a nested list).
 */
@Composable
fun Callout(
    icon: ImageVector,
    tone: Tone,
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit = {},
) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.xl))
            .background(tone.container)
            .padding(Space.l),
        verticalAlignment = Alignment.Top,
    ) {
        IconTile(icon, tone.tile, 32.dp)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = Spy.colors.textPrimary)
            Spacer(Modifier.height(Space.xxs))
            Text(body, style = MaterialTheme.typography.bodyMedium, color = Spy.colors.textSecondary)
            content()
        }
    }
}

/** Container-less fine print (4.14). Build tips with [proTip]. */
@Composable
fun Footnote(text: AnnotatedString, modifier: Modifier = Modifier, icon: ImageVector? = null) {
    Row(modifier.fillMaxWidth().padding(start = Space.xs, end = Space.xs, top = Space.s), verticalAlignment = Alignment.Top) {
        if (icon != null) {
            Icon(icon, null, tint = Spy.colors.textTertiary, modifier = Modifier.padding(top = 1.dp).size(16.dp))
            Spacer(Modifier.width(10.dp))
        }
        Text(text, style = MaterialTheme.typography.bodySmall, color = Spy.colors.textSecondary)
    }
}

@Composable
fun Footnote(text: String, modifier: Modifier = Modifier, icon: ImageVector? = null) =
    Footnote(AnnotatedString(text), modifier, icon)

/**
 * Sets a leading "Pro tip:" (or any [prefix]) in textPrimary weight 600; the string is unchanged.
 */
@Composable
fun proTip(text: String, prefix: String = "Pro tip:"): AnnotatedString {
    val strong = Spy.colors.textPrimary
    return buildAnnotatedString {
        if (text.startsWith(prefix)) {
            withStyle(SpanStyle(color = strong, fontWeight = FontWeight.SemiBold)) { append(prefix) }
            append(text.substring(prefix.length))
        } else append(text)
    }
}

/**
 * Empty state (4.15). Full: centred column (max 300dp). Inline: a single group row with a 40dp
 * tonal icon (use [tone] SAFE, or NEUTRAL when protection is off) — wrap it in a [GroupedSection].
 */
@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    body: String? = null,
    modifier: Modifier = Modifier,
    inline: Boolean = false,
    tone: Tone = Tone.NEUTRAL,
    action: (@Composable () -> Unit)? = null,
) {
    if (inline) {
        Row(
            modifier.fillMaxWidth().heightIn(min = 72.dp).padding(horizontal = Space.l, vertical = Space.m),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TonalIcon(icon, tone, 40.dp)
            Spacer(Modifier.width(Space.l))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = Spy.colors.textPrimary)
                if (body != null) {
                    Spacer(Modifier.height(Space.xxs))
                    Text(body, style = MaterialTheme.typography.bodyMedium, color = Spy.colors.textSecondary)
                }
            }
            if (action != null) {
                Spacer(Modifier.width(Space.m))
                action()
            }
        }
        return
    }
    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Column(
            Modifier.widthIn(max = 300.dp).padding(vertical = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            TonalIcon(icon, tone, 56.dp)
            Spacer(Modifier.height(Space.l))
            Text(title, style = MaterialTheme.typography.titleMedium, color = Spy.colors.textPrimary, textAlign = TextAlign.Center)
            if (body != null) {
                Spacer(Modifier.height(Space.xs))
                Text(body, style = MaterialTheme.typography.bodyMedium, color = Spy.colors.textSecondary, textAlign = TextAlign.Center)
            }
            if (action != null) {
                Spacer(Modifier.height(Space.l))
                action()
            }
        }
    }
}

/** Loading placeholder row (icon square + two bars) with a 0.5 to 1 alpha pulse. */
@Composable
fun SkeletonRow(modifier: Modifier = Modifier, iconSize: Dp = 44.dp) {
    val reduce = LocalReduceMotion.current
    val a = if (reduce) 0.75f else {
        val t = rememberInfiniteTransition(label = "skeleton")
        t.animateFloat(0.5f, 1f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "a").value
    }
    val fill = Spy.colors.control
    Row(
        modifier.fillMaxWidth().heightIn(min = 72.dp).padding(horizontal = Space.l, vertical = Space.m).graphicsLayer { alpha = a },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(iconSize).background(fill, RoundedCornerShape(iconSize * 0.26f)))
        Spacer(Modifier.width(Space.l))
        Column(Modifier.weight(1f)) {
            Box(Modifier.fillMaxWidth(0.55f).height(14.dp).background(fill, RoundedCornerShape(Radius.xs)))
            Spacer(Modifier.height(Space.s))
            Box(Modifier.fillMaxWidth(0.35f).height(10.dp).background(fill, RoundedCornerShape(Radius.xs)))
        }
    }
}

// =============================================================================================
// Chips and segmented control
// =============================================================================================

/**
 * Horizontally scrolling filter chips (4.9). Bleeds to the screen edge: do not add the gutter on
 * the caller, it is in [contentPadding].
 */
@Composable
fun FilterChipRow(
    labels: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = Space.gutter),
) {
    Row(
        modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(contentPadding),
        horizontalArrangement = Arrangement.spacedBy(Space.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        labels.forEachIndexed { i, label -> SpyChip(label, i == selectedIndex) { onSelect(i) } }
    }
}

/** A single filter chip (36dp, full radius). Selected uses inverseSurface with a check. */
@Composable
fun SpyChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val c = Spy.colors
    val bg by animateColorAsState(if (selected) c.inverseSurface else c.control, tween(150), label = "chipBg")
    val fg by animateColorAsState(if (selected) c.inverseText else c.textSecondary, tween(150), label = "chipFg")
    Row(
        Modifier
            .minimumInteractiveComponentSize()
            .heightIn(min = 36.dp)
            .clip(CircleShape)
            .background(bg)
            .selectable(selected = selected, role = Role.Tab, onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (selected) {
            Icon(Icons.Rounded.Check, null, tint = fg, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
        }
        Text(label, style = MaterialTheme.typography.labelMedium, color = fg, maxLines = 1)
    }
}

/**
 * Full-width segmented control (4.9): 40dp `control` track, sliding `controlThumb`.
 * At font scale >= 1.5 it renders as a [FilterChipRow] instead.
 */
@Composable
fun SegmentedControl(options: List<String>, selectedIndex: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    if (LocalDensity.current.fontScale >= 1.5f) {
        FilterChipRow(options, selectedIndex, onSelect, modifier, PaddingValues(0.dp))
        return
    }
    val c = Spy.colors
    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .minimumInteractiveComponentSize()
            .height(40.dp)
            .clip(RoundedCornerShape(Radius.m))
            .background(c.control)
            .padding(3.dp)
    ) {
        val n = options.size.coerceAtLeast(1)
        val segW = maxWidth / n
        val x by animateDpAsState(
            segW * selectedIndex.coerceIn(0, n - 1),
            tween(Motion.standard, easing = Motion.standardEasing),
            label = "thumb",
        )
        val thumbShape = RoundedCornerShape(Radius.s)
        Box(
            Modifier
                .offset(x = x)
                .width(segW)
                .fillMaxHeight()
                .then(
                    if (!c.isDark) Modifier.shadow(1.dp, thumbShape, ambientColor = Color.Black.copy(0.06f), spotColor = Color.Black.copy(0.10f))
                    else Modifier
                )
                .background(c.controlThumb, thumbShape)
        )
        Row(Modifier.fillMaxWidth().fillMaxHeight()) {
            options.forEachIndexed { i, label ->
                val sel = i == selectedIndex
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(thumbShape)
                        .selectable(selected = sel, role = Role.Tab) { onSelect(i) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        label,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (sel) c.textPrimary else c.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = Space.xs),
                    )
                }
            }
        }
    }
}

// =============================================================================================
// Legacy names (kept so existing screens compile; they now render the v2 look)
// =============================================================================================

/**
 * Legacy container. Now a flat `surface1` (or group colour) block, radius 20, no border, no glass.
 * A non-null [accent] tints the fill with that colour (12%) for emphasis.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    accent: Color? = null,
    onClick: (() -> Unit)? = null,
    padding: Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val base = currentGroupColor
    val fill = accent?.copy(alpha = 0.12f)?.compositeOver(base) ?: base
    val src = remember { MutableInteractionSource() }
    Column(
        modifier
            .then(if (onClick != null) Modifier.pressScale(src, 0.98f) else Modifier)
            .clip(RoundedCornerShape(Radius.xl))
            .background(fill)
            .then(
                if (onClick != null) Modifier.clickable(src, LocalIndication.current, role = Role.Button, onClick = onClick)
                else Modifier
            )
            .padding(padding),
        content = content,
    )
}

/** Legacy badge. Now a sentence-case [StatusBadge]-style label tinted with [color]. */
@Composable
fun Pill(text: String, color: Color, modifier: Modifier = Modifier, icon: ImageVector? = null) {
    Row(
        modifier
            .heightIn(min = 22.dp)
            .background(color.copy(alpha = 0.16f).compositeOver(currentGroupColor), RoundedCornerShape(Radius.xs))
            .padding(horizontal = Space.s, vertical = Space.xxs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, null, tint = color, modifier = Modifier.size(12.dp))
            Spacer(Modifier.width(Space.xs))
        }
        Text(text, color = color, style = MaterialTheme.typography.labelSmall, maxLines = 1)
    }
}

/** Legacy tonal icon. Now a rounded square tinted with [color]. Prefer [TonalIcon] / [IconTile]. */
@Composable
fun IconBubble(icon: ImageVector, color: Color, size: Dp = 40.dp) {
    Box(
        Modifier
            .size(size)
            .clip(RoundedCornerShape(if (size >= 56.dp) 18.dp else size * 0.3f))
            .background(color.copy(alpha = 0.16f).compositeOver(currentGroupColor)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(size * 0.5f))
    }
}

/** Legacy name for [LiveDot]. */
@Composable
fun PulsingDot(color: Color, size: Dp = 10.dp) = LiveDot(color, size * 0.8f)

/** Legacy name for [SectionHeader]. */
@Composable
fun SectionTitle(title: String, action: String? = null, onAction: (() -> Unit)? = null) =
    SectionHeader(title, Modifier.padding(top = Space.s), action = action, onAction = onAction)

// =============================================================================================
// Lifecycle + formatting helpers (unchanged)
// =============================================================================================

/** Increments every time the screen resumes, so permission checks refresh after visiting Settings. */
@Composable
fun rememberResumeTick(): Int {
    val owner = LocalLifecycleOwner.current
    var tick by remember { mutableIntStateOf(0) }
    DisposableEffect(owner) {
        val obs = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_RESUME) tick++ }
        owner.lifecycle.addObserver(obs)
        onDispose { owner.lifecycle.removeObserver(obs) }
    }
    return tick
}

/** True only during a screen's first composition (survives config change); use for [entryAnimation]. */
@Composable
fun rememberFirstEntry(): Boolean {
    var first by rememberSaveable { mutableStateOf(true) }
    LaunchedEffect(Unit) { delay(600); first = false }
    return first
}

fun ago(ts: Long): String =
    if (ts <= 0) "Never" else DateUtils.getRelativeTimeSpanString(
        ts, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS, DateUtils.FORMAT_ABBREV_RELATIVE,
    ).toString()

fun clock(ts: Long): String = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(ts))

fun dayLabel(ts: Long): String = when {
    DateUtils.isToday(ts) -> "Today"
    DateUtils.isToday(ts + DateUtils.DAY_IN_MILLIS) -> "Yesterday"
    else -> SimpleDateFormat("EEE, d MMM", Locale.getDefault()).format(Date(ts))
}

fun duration(ms: Long): String {
    val s = ms / 1000
    return when {
        s < 1 -> "<1s"
        s < 60 -> "${s}s"
        s < 3600 -> "${s / 60}m ${s % 60}s"
        else -> "${s / 3600}h ${(s % 3600) / 60}m"
    }
}
