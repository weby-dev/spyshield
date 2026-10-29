package com.spyshield.app.ui

import android.provider.Settings
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spyshield.app.R

// ---------------------------------------------------------------------------------------------
// Colour tokens (DESIGN.md 1.2)
// ---------------------------------------------------------------------------------------------

@Immutable
data class SpyColors(
    val isDark: Boolean,
    val bg: Color, val surface1: Color, val surface2: Color, val sheet: Color,
    val control: Color, val controlThumb: Color, val divider: Color, val scrim: Color,
    val textPrimary: Color, val textSecondary: Color, val textTertiary: Color, val textOnAccent: Color,
    val inverseSurface: Color, val inverseText: Color,
    val brand: Color, val brandText: Color, val brandContainer: Color, val onBrandContainer: Color,
    val safe: Color, val safeContainer: Color, val onSafeContainer: Color,
    val warning: Color, val onWarning: Color, val warningContainer: Color, val onWarningContainer: Color,
    val danger: Color, val dangerStrong: Color, val dangerContainer: Color, val onDangerContainer: Color,
    val camera: Color, val cameraContainer: Color, val mic: Color, val micContainer: Color,
)

val DarkSpyColors = SpyColors(
    isDark = true,
    bg = Color(0xFF0B0C0F), surface1 = Color(0xFF16171B), surface2 = Color(0xFF1E2025), sheet = Color(0xFF131418),
    control = Color(0xFF22242A), controlThumb = Color(0xFF3A3C44), divider = Color(0xFF26282E),
    scrim = Color.Black.copy(alpha = 0.60f),
    textPrimary = Color(0xFFF3F3F1), textSecondary = Color(0xFFA3A6AE), textTertiary = Color(0xFF858993),
    textOnAccent = Color.White,
    inverseSurface = Color(0xFFF3F3F1), inverseText = Color(0xFF0B0C0F),
    brand = Color(0xFF3D5AFE), brandText = Color(0xFF8EA2FF), brandContainer = Color(0xFF1D2550),
    onBrandContainer = Color(0xFFC7D0FF),
    safe = Color(0xFF3DD68C), safeContainer = Color(0xFF143225), onSafeContainer = Color(0xFF8EF0BE),
    warning = Color(0xFFFFB020), onWarning = Color(0xFF241800), warningContainer = Color(0xFF3A2A0C),
    onWarningContainer = Color(0xFFFFD58A),
    danger = Color(0xFFFF5D5D), dangerStrong = Color(0xFFD93640), dangerContainer = Color(0xFF3D1517),
    onDangerContainer = Color(0xFFFFB3B0),
    camera = Color(0xFF5CB8FF), cameraContainer = Color(0xFF13283D),
    mic = Color(0xFFD18CFF), micContainer = Color(0xFF2C1A3D),
)

val LightSpyColors = SpyColors(
    isDark = false,
    bg = Color(0xFFF2F2EE), surface1 = Color(0xFFFFFFFF), surface2 = Color(0xFFFFFFFF), sheet = Color(0xFFF2F2EE),
    control = Color(0xFFE6E6E1), controlThumb = Color(0xFFFFFFFF), divider = Color(0xFFE3E3DE),
    scrim = Color.Black.copy(alpha = 0.40f),
    textPrimary = Color(0xFF121316), textSecondary = Color(0xFF5C5F66), textTertiary = Color(0xFF6B6E76),
    textOnAccent = Color.White,
    inverseSurface = Color(0xFF121316), inverseText = Color(0xFFFFFFFF),
    brand = Color(0xFF3D5AFE), brandText = Color(0xFF3048E0), brandContainer = Color(0xFFE4E8FF),
    onBrandContainer = Color(0xFF1A2A9C),
    safe = Color(0xFF13804B), safeContainer = Color(0xFFDDF5E8), onSafeContainer = Color(0xFF0B5133),
    warning = Color(0xFFB25E00), onWarning = Color(0xFFFFFFFF), warningContainer = Color(0xFFFFF0D6),
    onWarningContainer = Color(0xFF6B3A00),
    danger = Color(0xFFD7263D), dangerStrong = Color(0xFFD7263D), dangerContainer = Color(0xFFFDE3E5),
    onDangerContainer = Color(0xFF8A1022),
    camera = Color(0xFF0A6FD1), cameraContainer = Color(0xFFE1EEFC),
    mic = Color(0xFF8A3FD1), micContainer = Color(0xFFF1E6FB),
)

/** Solid icon-tile fills (white glyph on top). Identical in both themes. */
object SpyTiles {
    val Brand = Color(0xFF3D5AFE)
    val Safe = Color(0xFF1E9E5A)
    val Warning = Color(0xFFC9741F)
    val Danger = Color(0xFFD93640)
    val Camera = Color(0xFF2F80ED)
    val Mic = Color(0xFF9B51E0)
    val Neutral = Color(0xFF6B6E76)
}

val LocalSpyColors = staticCompositionLocalOf { DarkSpyColors }
val LocalReduceMotion = staticCompositionLocalOf { false }

/** Background of the group a row sits in. Unspecified means `surface1`. Sheets provide `surface2`. */
val LocalGroupColor = staticCompositionLocalOf { Color.Unspecified }

object Spy {
    val colors: SpyColors
        @Composable @ReadOnlyComposable get() = LocalSpyColors.current
}

/**
 * Legacy colour names, kept so older call sites compile. Every property now resolves to the
 * v2 token of the active theme (light or dark). New code should use `Spy.colors.*`.
 *
 * Mapping: Bg->bg, Surface->surface1, Surface2->control, Stroke->divider, Cyan->brand,
 * Violet->mic, Danger->danger, Warn->warning, Safe->safe, TextHi->textPrimary,
 * TextMid->textSecondary, TextLo->textTertiary.
 */
object SC {
    @Volatile
    internal var active: SpyColors = DarkSpyColors

    val Bg: Color get() = active.bg
    val Surface: Color get() = active.surface1
    val Surface2: Color get() = active.control
    val Stroke: Color get() = active.divider
    val Cyan: Color get() = active.brand
    val Violet: Color get() = active.mic
    val Danger: Color get() = active.danger
    val Warn: Color get() = active.warning
    val Safe: Color get() = active.safe
    val TextHi: Color get() = active.textPrimary
    val TextMid: Color get() = active.textSecondary
    val TextLo: Color get() = active.textTertiary

    // New aliases (v2 tokens) for gradual migration.
    val Sheet: Color get() = active.sheet
    val Control: Color get() = active.control
    val Divider: Color get() = active.divider
    val Brand: Color get() = active.brand
    val BrandText: Color get() = active.brandText
    val DangerStrong: Color get() = active.dangerStrong
    val Camera: Color get() = active.camera
    val Mic: Color get() = active.mic
}

// ---------------------------------------------------------------------------------------------
// Tones (DESIGN.md 1.3)
// ---------------------------------------------------------------------------------------------

enum class Tone { NEUTRAL, BRAND, SAFE, WARNING, DANGER, CAMERA, MIC }

val Tone.fg: Color
    @Composable @ReadOnlyComposable get() = Spy.colors.let {
        when (this) {
            Tone.NEUTRAL -> it.textSecondary
            Tone.BRAND -> it.brandText
            Tone.SAFE -> it.safe
            Tone.WARNING -> it.warning
            Tone.DANGER -> it.danger
            Tone.CAMERA -> it.camera
            Tone.MIC -> it.mic
        }
    }

val Tone.container: Color
    @Composable @ReadOnlyComposable get() = Spy.colors.let {
        when (this) {
            Tone.NEUTRAL -> it.control
            Tone.BRAND -> it.brandContainer
            Tone.SAFE -> it.safeContainer
            Tone.WARNING -> it.warningContainer
            Tone.DANGER -> it.dangerContainer
            Tone.CAMERA -> it.cameraContainer
            Tone.MIC -> it.micContainer
        }
    }

val Tone.onContainer: Color
    @Composable @ReadOnlyComposable get() = Spy.colors.let {
        when (this) {
            Tone.NEUTRAL -> it.textSecondary
            Tone.BRAND -> it.onBrandContainer
            Tone.SAFE -> it.onSafeContainer
            Tone.WARNING -> it.onWarningContainer
            Tone.DANGER -> it.onDangerContainer
            Tone.CAMERA -> it.camera
            Tone.MIC -> it.mic
        }
    }

val Tone.tile: Color
    get() = when (this) {
        Tone.NEUTRAL -> SpyTiles.Neutral
        Tone.BRAND -> SpyTiles.Brand
        Tone.SAFE -> SpyTiles.Safe
        Tone.WARNING -> SpyTiles.Warning
        Tone.DANGER -> SpyTiles.Danger
        Tone.CAMERA -> SpyTiles.Camera
        Tone.MIC -> SpyTiles.Mic
    }

// ---------------------------------------------------------------------------------------------
// Space, radius, motion (DESIGN.md 3 and 5)
// ---------------------------------------------------------------------------------------------

object Space {
    val xxs = 2.dp; val xs = 4.dp; val s = 8.dp; val m = 12.dp; val l = 16.dp
    val xl = 20.dp; val xxl = 24.dp; val x3 = 32.dp; val x4 = 40.dp; val x5 = 56.dp

    /** Screen horizontal gutter. */
    val gutter = 20.dp
    /** From the end of a group to the next section header. */
    val section = 28.dp
    /** Section header to its group. */
    val headerGap = 8.dp
    /** Gap between tiles in a pair/grid and between stacked callouts. */
    val tileGap = 12.dp
    /** LazyColumn bottom content padding. */
    val listBottom = 32.dp
}

object Radius {
    val xs = 6.dp; val s = 9.dp; val m = 12.dp; val l = 16.dp; val xl = 20.dp; val xxl = 28.dp
}

object Motion {
    const val fast = 120
    const val standard = 220
    const val emphasizedDuration = 360
    const val countDuration = 600
    val standardEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    val emphasized = CubicBezierEasing(0.3f, 0f, 0f, 1f)
    val decelerate = CubicBezierEasing(0f, 0f, 0f, 1f)
    val exit = CubicBezierEasing(0.3f, 0f, 1f, 1f)
    val pressSpring: SpringSpec<Float> = spring(dampingRatio = 0.7f, stiffness = 600f)
}

// ---------------------------------------------------------------------------------------------
// Typography (DESIGN.md 2)
// ---------------------------------------------------------------------------------------------

private val fontProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs,
)

private val jakarta = GoogleFont("Plus Jakarta Sans")

/** Plus Jakarta Sans via the Play Services font provider. Falls back to the system font if unavailable. */
val SpyFontFamily: FontFamily = runCatching {
    FontFamily(
        Font(googleFont = jakarta, fontProvider = fontProvider, weight = FontWeight.Normal),
        Font(googleFont = jakarta, fontProvider = fontProvider, weight = FontWeight.Medium),
        Font(googleFont = jakarta, fontProvider = fontProvider, weight = FontWeight.SemiBold),
        Font(googleFont = jakarta, fontProvider = fontProvider, weight = FontWeight.Bold),
        Font(googleFont = jakarta, fontProvider = fontProvider, weight = FontWeight.ExtraBold),
    )
}.getOrDefault(FontFamily.Default)

/** Tabular figures for any number that changes or sits in a column. */
val Numeric = SpanStyle(fontFeatureSettings = "tnum")
fun TextStyle.tnum(): TextStyle = copy(fontFeatureSettings = "tnum")

private fun style(size: Int, line: Int, weight: FontWeight, tracking: Double, tabular: Boolean = false) = TextStyle(
    fontFamily = SpyFontFamily,
    fontSize = size.sp,
    lineHeight = line.sp,
    fontWeight = weight,
    letterSpacing = tracking.sp,
    fontFeatureSettings = if (tabular) "tnum" else null,
)

val SpyTypography = Typography(
    displayLarge = style(34, 40, FontWeight.ExtraBold, -0.8),
    displayMedium = style(40, 44, FontWeight.Bold, -1.2, tabular = true),
    displaySmall = style(28, 32, FontWeight.Bold, -0.6, tabular = true),
    headlineLarge = style(28, 34, FontWeight.Bold, -0.5),
    headlineMedium = style(24, 30, FontWeight.Bold, -0.4),
    headlineSmall = style(22, 28, FontWeight.Bold, -0.3),
    titleLarge = style(18, 24, FontWeight.SemiBold, -0.1),
    titleMedium = style(16, 22, FontWeight.SemiBold, 0.0),
    titleSmall = style(14, 20, FontWeight.SemiBold, 0.0),
    bodyLarge = style(16, 24, FontWeight.Normal, 0.0),
    bodyMedium = style(14, 20, FontWeight.Normal, 0.1),
    bodySmall = style(13, 18, FontWeight.Normal, 0.1),
    labelLarge = style(16, 20, FontWeight.SemiBold, 0.0),
    labelMedium = style(14, 18, FontWeight.SemiBold, 0.1),
    labelSmall = style(12, 16, FontWeight.SemiBold, 0.2),
)

val SpyShapes = Shapes(
    extraSmall = RoundedCornerShape(Radius.xs),
    small = RoundedCornerShape(Radius.m),
    medium = RoundedCornerShape(Radius.l),
    large = RoundedCornerShape(Radius.xl),
    extraLarge = RoundedCornerShape(Radius.xxl),
)

private fun SpyColors.toScheme(): ColorScheme {
    val base = if (isDark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = brand, onPrimary = textOnAccent,
        primaryContainer = brandContainer, onPrimaryContainer = onBrandContainer,
        inversePrimary = brandText,
        secondary = brandText, onSecondary = textOnAccent,
        secondaryContainer = brandContainer, onSecondaryContainer = onBrandContainer,
        tertiary = brandText, onTertiary = textOnAccent,
        tertiaryContainer = brandContainer, onTertiaryContainer = onBrandContainer,
        background = bg, onBackground = textPrimary,
        surface = bg, onSurface = textPrimary,
        surfaceVariant = control, onSurfaceVariant = textSecondary,
        surfaceTint = Color.Transparent,
        inverseSurface = inverseSurface, inverseOnSurface = inverseText,
        error = danger, onError = textOnAccent, errorContainer = dangerContainer, onErrorContainer = onDangerContainer,
        outline = divider, outlineVariant = divider, scrim = Color.Black,
        surfaceBright = surface2, surfaceDim = bg,
        surfaceContainerLowest = bg, surfaceContainerLow = sheet,
        surfaceContainer = surface1, surfaceContainerHigh = surface2, surfaceContainerHighest = control,
    )
}

@Composable
fun SpyShieldTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val colors = if (darkTheme) DarkSpyColors else LightSpyColors
    val context = LocalContext.current
    val reduceMotion = remember(context) {
        runCatching {
            Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
        }.getOrDefault(false)
    }
    val scheme = remember(colors) { colors.toScheme() }
    SC.active = colors
    SideEffect { SC.active = colors }
    CompositionLocalProvider(
        LocalSpyColors provides colors,
        LocalReduceMotion provides reduceMotion,
    ) {
        MaterialTheme(colorScheme = scheme, typography = SpyTypography, shapes = SpyShapes, content = content)
    }
}
