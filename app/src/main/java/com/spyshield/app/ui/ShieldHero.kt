package com.spyshield.app.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.PriorityHigh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.spyshield.app.data.Sensor

/** Hero states (4.1): !protection -> OFF, liveAlert -> ALERT, score >= 70 -> CALM, else CAUTION. */
enum class HeroState { OFF, CALM, CAUTION, ALERT }

fun heroState(protection: Boolean, liveAlert: Boolean, score: Int): HeroState = when {
    !protection -> HeroState.OFF
    liveAlert -> HeroState.ALERT
    score >= 70 -> HeroState.CALM
    else -> HeroState.CAUTION
}

/** Headline for a hero state (4.1 table). */
fun HeroState.headline(): String = when (this) {
    HeroState.CALM -> "You're protected"
    HeroState.CAUTION -> "Protected"
    HeroState.ALERT -> "Something is using your sensors"
    HeroState.OFF -> "Protection is off"
}

/** Support line for a hero state (4.1 table). [liveLabel] is appended for ALERT when known. */
fun HeroState.support(liveLabel: String? = null): String = when (this) {
    HeroState.CALM -> "Watching 24/7."
    HeroState.CAUTION -> "But some apps look risky"
    HeroState.ALERT -> "In the background" + (if (!liveLabel.isNullOrBlank()) " · $liveLabel" else "")
    HeroState.OFF -> "You're not being watched over"
}

/** State colour of the hero. */
val HeroState.tone: Tone
    get() = when (this) {
        HeroState.CALM -> Tone.SAFE
        HeroState.CAUTION -> Tone.WARNING
        HeroState.ALERT -> Tone.DANGER
        HeroState.OFF -> Tone.NEUTRAL
    }

@Composable
private fun HeroState.stateColor(): Color = when (this) {
    HeroState.OFF -> Spy.colors.textTertiary
    else -> tone.fg
}

@Composable
private fun HeroState.containerColor(): Color = when (this) {
    HeroState.OFF -> Spy.colors.control
    else -> tone.container
}

/**
 * The layered shield hero (4.1): aura, back plate, gradient front plate with sheen, and glyph.
 * CALM / CAUTION breathe, ALERT ripples, OFF is still. [sensor] picks the ALERT glyph.
 */
@Composable
fun ShieldHero(state: HeroState, modifier: Modifier = Modifier, size: Dp = 148.dp, sensor: Sensor? = null) {
    val c = Spy.colors
    val reduce = LocalReduceMotion.current
    val spec = tween<Color>(Motion.emphasizedDuration, easing = Motion.emphasized)
    val color by animateColorAsState(state.stateColor(), spec, label = "heroColor")
    val back by animateColorAsState(state.containerColor(), spec, label = "heroBack")
    val auraBase by animateFloatAsState(
        when (state) {
            HeroState.OFF -> 0f
            HeroState.ALERT -> 0.40f
            else -> if (c.isDark) 0.28f else 0.18f
        },
        tween(Motion.emphasizedDuration, easing = Motion.emphasized),
        label = "aura",
    )

    val animate = !reduce && state != HeroState.OFF
    val t = rememberInfiniteTransition(label = "hero")
    val breathe by t.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(4000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "breathe",
    )
    val ripple by t.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(1400, easing = LinearOutSlowInEasing), RepeatMode.Restart),
        label = "ripple",
    )
    val calmMotion = animate && state != HeroState.ALERT
    val alertMotion = animate && state == HeroState.ALERT
    val isOff = state == HeroState.OFF
    val tertiary = c.textTertiary
    val control = c.control

    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val w = this.size.width
            val h = this.size.height
            val center = Offset(w / 2f, h / 2f)

            // 1. Aura
            if (auraBase > 0f) {
                val s = if (calmMotion) 0.94f + 0.12f * breathe else 1f
                val a = (auraBase + if (calmMotion) (breathe - 0.5f) * 0.12f else 0f).coerceIn(0f, 1f)
                val r = w / 2f * s
                drawCircle(
                    Brush.radialGradient(listOf(color.copy(alpha = a), Color.Transparent), center, r),
                    radius = r,
                    center = center,
                )
            }

            // Alert ripples (shield-shaped outlines)
            if (alertMotion) {
                for (i in 0..1) {
                    val p = (ripple + i * 0.5f) % 1f
                    val sc = 1f + 0.40f * p
                    scale(sc, pivot = center) {
                        drawPath(
                            shieldPath(center, h * 0.78f),
                            color.copy(alpha = 0.5f * (1f - p)),
                            style = Stroke(2.dp.toPx() / sc),
                        )
                    }
                }
            }

            // 2. Back plate
            drawPath(shieldPath(center, h * 0.78f), back)

            // 3. Front plate
            val frontCenter = Offset(center.x, center.y - 2.dp.toPx())
            val frontH = h * 0.62f
            val front = shieldPath(frontCenter, frontH)
            val top = frontCenter.y - frontH / 2f
            if (isOff) {
                drawPath(front, control)
                drawPath(front, tertiary, style = Stroke(2.dp.toPx()))
            } else {
                drawPath(
                    front,
                    Brush.verticalGradient(listOf(lerp(color, Color.White, 0.10f), color), startY = top, endY = top + frontH),
                )
                clipPath(front) {
                    drawRect(
                        Brush.verticalGradient(
                            listOf(Color.White.copy(alpha = 0.16f), Color.White.copy(alpha = 0f)),
                            startY = top, endY = top + frontH / 2f,
                        ),
                        topLeft = Offset(0f, top),
                        size = androidx.compose.ui.geometry.Size(w, frontH / 2f),
                    )
                }
            }
        }

        // 4. Glyph
        val glyph: ImageVector = when (state) {
            HeroState.CALM -> Icons.Rounded.Check
            HeroState.CAUTION -> Icons.Rounded.PriorityHigh
            HeroState.ALERT -> (sensor ?: Sensor.CAMERA).icon
            HeroState.OFF -> Icons.Rounded.PowerSettingsNew
        }
        AnimatedContent(
            targetState = glyph to isOff,
            transitionSpec = {
                (fadeIn(tween(Motion.emphasizedDuration, easing = Motion.emphasized)) +
                    scaleIn(tween(Motion.emphasizedDuration, easing = Motion.emphasized), initialScale = 0.8f))
                    .togetherWith(fadeOut(tween(Motion.fast)))
            },
            modifier = Modifier.offset(y = (-2).dp),
            label = "heroGlyph",
        ) { (g, off) ->
            Icon(g, null, tint = if (off) tertiary else c.textOnAccent, modifier = Modifier.size(size * (40f / 148f)))
        }
    }
}

/** "Privacy score  92/100  [meter]" readout used under the hero. */
@Composable
fun ScoreReadout(score: Int, state: HeroState, modifier: Modifier = Modifier) {
    val shown = rememberCountUp(score)
    val fillColor by animateColorAsState(
        state.stateColor(),
        tween(Motion.emphasizedDuration, easing = Motion.emphasized),
        label = "meterColor",
    )
    val frac by animateFloatAsState(
        (score.coerceIn(0, 100)) / 100f,
        tween(Motion.countDuration, easing = Motion.decelerate),
        label = "meter",
    )
    val track = Spy.colors.control
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
        Text("Privacy score", style = MaterialTheme.typography.labelMedium, color = Spy.colors.textSecondary)
        Spacer(Modifier.width(Space.s))
        Text("$shown", style = MaterialTheme.typography.titleLarge.tnum(), color = Spy.colors.textPrimary)
        Text("/100", style = MaterialTheme.typography.bodySmall.tnum(), color = Spy.colors.textTertiary)
        Spacer(Modifier.width(10.dp))
        Box(
            Modifier
                .size(56.dp, 6.dp)
                .clip(CircleShape)
                .drawBehind {
                    drawRect(track)
                    drawRoundRect(
                        fillColor,
                        size = this.size.copy(width = this.size.width * frac),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(this.size.height / 2f),
                    )
                }
        )
    }
}

/**
 * Dashboard hero composite (4.1): surface1, radius 28, state wash, [ShieldHero], headline, support,
 * score readout, divider and the protection [ToggleRow]. [onProtectionChange] is called with the
 * new value from both the row and the switch, so pass the existing toggle logic unchanged.
 */
@Composable
fun HeroCard(
    state: HeroState,
    score: Int,
    protection: Boolean,
    onProtectionChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    sensor: Sensor? = null,
    liveLabel: String? = null,
    toggleNote: String? = null,
) {
    val c = Spy.colors
    val wash by animateColorAsState(
        state.stateColor().copy(alpha = if (state == HeroState.OFF) 0f else if (c.isDark) 0.10f else 0.08f),
        tween(Motion.emphasizedDuration, easing = Motion.emphasized),
        label = "wash",
    )
    val headline = state.headline()
    val support = state.support(liveLabel)
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.xxl))
            .background(c.surface1)
            .drawBehind {
                drawRect(
                    Brush.verticalGradient(listOf(wash, Color.Transparent), startY = 0f, endY = size.height * 0.6f),
                )
            },
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(start = Space.xl, end = Space.xl, top = 28.dp)
                .semantics(mergeDescendants = true) {
                    contentDescription = "$headline. $support. Privacy score $score of 100"
                },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ShieldHero(state, size = 148.dp, sensor = sensor)
            Spacer(Modifier.height(Space.m))
            Text(
                headline,
                style = MaterialTheme.typography.headlineLarge,
                color = if (state == HeroState.ALERT) c.danger else c.textPrimary,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(Space.xs))
            Text(support, style = MaterialTheme.typography.bodyLarge, color = c.textSecondary, textAlign = TextAlign.Center)
            Spacer(Modifier.height(Space.l))
            ScoreReadout(score, state)
            Spacer(Modifier.height(Space.xl))
        }
        RowDivider(0.dp)
        ToggleRow(
            tileIcon = Icons.Rounded.PowerSettingsNew,
            tileColor = if (protection) SpyTiles.Brand else SpyTiles.Neutral,
            title = "Real-time protection",
            subtitle = if (protection) "Watching camera & mic in the background" else "Tap to start watching your camera & mic",
            checked = protection,
            onCheckedChange = onProtectionChange,
            note = toggleNote,
        )
    }
}
