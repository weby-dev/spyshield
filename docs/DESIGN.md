# SpyShield Design System v2

Status: binding spec for the visual/UX redesign. Behaviour, data flow, features and the meaning of every string stay the same. If something here conflicts with existing behaviour, keep the behaviour and flag it.

Target device for QA: OPPO Reno8 5G, 1080x2400, ~411dp wide, ColorOS / Android 14. Also check a 360dp-wide emulator and font scale 1.3 and 2.0.

---

## 0. What we are fixing, in one paragraph

The current UI is neon cyan and violet on navy black, with a translucent card and a 1dp border around everything, the same 22dp radius on every box, uppercase pills, a two-tone wordmark and a spinning ring "orb". Everything has the same visual weight, so nothing stands out. v2 is **calm by default and loud only when it matters**. It uses warm graphite neutrals (or warm paper in light mode), one confident brand blue, grouped inset lists in place of a card per row, a big readable status statement, and colour that is spent almost entirely on meaning (safe, warning, danger, camera, mic).

### Brand idea (one line)

**"A quiet guardian: SpyShield stays still and confident until something touches your camera or mic, and then it is unmistakable."**

### Anti-patterns (do not ship any of these)

- 1dp borders or strokes around containers (the only hairlines allowed are list dividers, the nav bar's top edge and a focused text field).
- Translucent or glass surfaces, neon glows, sweep gradients, rotating scanners, sonar rings on calm states.
- Gradients on buttons, cards, text or icons. See 1.4 for the only places gradients are allowed.
- `#22E3C4` cyan or `#8B5CF6` violet anywhere, including notifications, the widget, the overlay and the launcher.
- Two-tone wordmarks ("Spy" in a colour, "Shield" in white).
- UPPERCASE badges and labels. Everything is sentence case. Remove the `.uppercase()` in `Pill`, and use "Today", not "TODAY".
- `OutlinedButton` anywhere.
- A separate card for every list row.
- Paragraphs of grey text longer than three lines in a list context. Move long explanations into footnotes (see 4.14).

---

## 1. Colour

### 1.1 Theme mode

Follow the system with `isSystemInDarkTheme()`. Do **not** use Material You dynamic colour, because brand consistency wins. The alarm screen uses its own fixed palette in both modes (see 7.5).

### 1.2 Tokens (exact hex)

| Token | Dark | Light | Use |
|---|---|---|---|
| `bg` | `#0B0C0F` | `#F2F2EE` | Screen background, nav-bar-free areas |
| `surface1` | `#16171B` | `#FFFFFF` | Grouped list containers, stat tiles, hero, nav bar |
| `surface2` | `#1E2025` | `#FFFFFF` | Dialogs, groups that sit on a sheet |
| `sheet` | `#131418` | `#F2F2EE` | Bottom-sheet background |
| `control` | `#22242A` | `#E6E6E1` | Unselected chip, segmented track, search field, secondary button, switch track off, circular icon buttons |
| `controlThumb` | `#3A3C44` | `#FFFFFF` | Segmented-control selected thumb |
| `divider` | `#26282E` | `#E3E3DE` | Hairlines inside groups, nav top edge |
| `scrim` | `#000000` @ 60% | `#000000` @ 40% | Sheet and dialog scrim |
| `textPrimary` | `#F3F3F1` | `#121316` | Titles, values, body |
| `textSecondary` | `#A3A6AE` | `#5C5F66` | Subtitles, descriptions |
| `textTertiary` | `#858993` | `#6B6E76` | Meta (time, package name). Use only on `bg`, `surface1` or `sheet`, never on `control` |
| `textOnAccent` | `#FFFFFF` | `#FFFFFF` | Text and icons on brand, dangerStrong and solid tiles |
| `inverseSurface` | `#F3F3F1` | `#121316` | Selected filter chip, snackbar |
| `inverseText` | `#0B0C0F` | `#FFFFFF` | Text on inverseSurface |
| `brand` | `#3D5AFE` | `#3D5AFE` | Primary button fill, switch on, focused field, onboarding dots |
| `brandText` | `#8EA2FF` | `#3048E0` | Links, text buttons, "See all" |
| `brandContainer` | `#1D2550` | `#E4E8FF` | Nav indicator, brand tonal tile background |
| `onBrandContainer` | `#C7D0FF` | `#1A2A9C` | Icon and text on brandContainer |
| `safe` | `#3DD68C` | `#13804B` | Safe text and icons |
| `safeContainer` | `#143225` | `#DDF5E8` | Safe tinted fills |
| `onSafeContainer` | `#8EF0BE` | `#0B5133` | Text on safeContainer |
| `warning` | `#FFB020` | `#B25E00` | Warning text and icons, warning button fill |
| `onWarning` | `#241800` | `#FFFFFF` | Text on a warning fill |
| `warningContainer` | `#3A2A0C` | `#FFF0D6` | Callout background |
| `onWarningContainer` | `#FFD58A` | `#6B3A00` | Text on warningContainer, badge text |
| `danger` | `#FF5D5D` | `#D7263D` | Danger text and icons |
| `dangerStrong` | `#D93640` | `#D7263D` | Destructive button fill, nav badge (white text) |
| `dangerContainer` | `#3D1517` | `#FDE3E5` | Danger tinted fills (active spy tile, badge) |
| `onDangerContainer` | `#FFB3B0` | `#8A1022` | Text on dangerContainer |
| `camera` | `#5CB8FF` | `#0A6FD1` | Camera glyphs and labels |
| `cameraContainer` | `#13283D` | `#E1EEFC` | Camera tonal fill |
| `mic` | `#D18CFF` | `#8A3FD1` | Mic glyphs and labels |
| `micContainer` | `#2C1A3D` | `#F1E6FB` | Mic tonal fill |

**Solid icon tiles** (iOS-settings-style squares with a white glyph). These are the same in both themes, and each passes 3:1 against white for graphics:

| Token | Hex |
|---|---|
| `tileBrand` | `#3D5AFE` |
| `tileSafe` | `#1E9E5A` |
| `tileWarning` | `#C9741F` |
| `tileDanger` | `#D93640` |
| `tileCamera` | `#2F80ED` |
| `tileMic` | `#9B51E0` |
| `tileNeutral` | `#6B6E76` |

### 1.3 Semantic mapping

| Meaning | Foreground | Container | Solid tile |
|---|---|---|---|
| Safe / protected / granted-but-denied ("Asked, but not allowed") / LOW risk | `safe` | `safeContainer` | `tileSafe` |
| Warning / in use (not suspicious) / MEDIUM risk / setup needed / new access | `warning` | `warningContainer` | `tileWarning` |
| Danger / suspicious / blocked / auto-blocked / HIGH risk / "Allowed" camera or mic in the detail sheet | `danger` | `dangerContainer` | `tileDanger` |
| Camera (identity, not state) | `camera` | `cameraContainer` | `tileCamera` |
| Mic (identity, not state) | `mic` | `micContainer` | `tileMic` |
| Off / not watched / never asked | `textTertiary` | `control` | `tileNeutral` |

Rules:
- **State beats identity.** A suspicious camera event is danger-coloured, not camera-coloured. Camera and mic colours appear only when nothing is wrong (stat tiles, sensor glyphs in app rows, sensor badges on idle events).
- Colour never carries meaning on its own. Every state also has a word or a distinct glyph.
- `brand` never means a status. It means "you can act here" (primary button, switch, selected nav, links).

### 1.4 Gradients (only these four)

1. **Hero aura** (Dashboard and Onboarding welcome): a radial gradient of the hero state colour, from 28% alpha in dark or 18% in light to transparent, plus a top wash on the hero container (see 4.1).
2. **Hero shield front plate**: a vertical gradient from the state colour lightened 10% (`lerp(color, White, 0.10f)`) to the state colour. There is also a white sheen on the upper half, from 16% alpha to 0.
3. **Alarm screen background**: `#B3141F` to `#4A0610`, vertical.
4. **Launcher icon background**: `#4F6BFF` to `#2E46E0`, diagonal, top-left to bottom-right.

Delete the `Brush.verticalGradient(Violet...)` screen backgrounds in `AppRoot` and `OnboardingScreen`. Screens are flat `bg`.

### 1.5 Material 3 `ColorScheme` mapping

This is required so that stock M3 components (AlertDialog, ModalBottomSheet, TextField, Badge) pick up the right colours.

```
primary = brand, onPrimary = textOnAccent,
primaryContainer = brandContainer, onPrimaryContainer = onBrandContainer,
secondary = brandText, onSecondary = textOnAccent,
background = bg, onBackground = textPrimary,
surface = bg, onSurface = textPrimary,
surfaceVariant = control, onSurfaceVariant = textSecondary,
surfaceContainerLowest = bg, surfaceContainerLow = sheet,
surfaceContainer = surface1, surfaceContainerHigh = surface2, surfaceContainerHighest = control,
surfaceTint = Color.Transparent,
inverseSurface = inverseSurface, inverseOnSurface = inverseText,
error = danger, onError = textOnAccent, errorContainer = dangerContainer, onErrorContainer = onDangerContainer,
outline = divider, outlineVariant = divider, scrim = Color.Black
```

Use `darkColorScheme(...)` and `lightColorScheme(...)` respectively.

---

## 2. Typography

### 2.1 Family

**Plus Jakarta Sans** is used for everything: display, headings, body, labels and numbers. It is a geometric grotesk with open apertures and friendly but serious numerals, and it is distinctive without being "techy". Weights used: 400, 500, 600, 700, 800.

Loading:
- Add the `androidx.compose.ui:ui-text-google-fonts` dependency. It is managed by the BOM (`2024.10.01`), so no version is needed.
- Use `GoogleFont.Provider(providerAuthority = "com.google.android.gms.fonts", providerPackage = "com.google.android.gms", certificates = R.array.com_google_android_gms_fonts_certs)`.
- Add `res/values/font_certs.xml`. It contains the standard `com_google_android_gms_fonts_certs` array, which references `@array/com_google_android_gms_fonts_certs_dev` and `@array/com_google_android_gms_fonts_certs_prod`, with the standard Google Play Services certificate strings. Copy the file verbatim from a trusted AndroidX/Google sample. **Never invent certificate strings.** If you cannot source them verbatim, ship with `FontFamily.Default` and leave a `TODO(font)`. The type scale below must look good in Roboto too.
- Build the family as `FontFamily(Font(googleFont = GoogleFont("Plus Jakarta Sans"), fontProvider = provider, weight = W))` for each weight. If the download fails, Compose falls back to the system font automatically, so layouts must never depend on glyph widths.

### 2.2 Type scale (exact)

The `Typography` slots are mapped so that stock M3 components inherit sensible styles.

| Token (M3 slot) | Size / line height | Weight | Letter spacing | Used for |
|---|---|---|---|---|
| `displayLarge` | 34 / 40 sp | 800 | -0.8 sp | Alarm app name |
| `displayMedium` | 40 / 44 sp | 700 | -1.2 sp, tnum | Risk score in the detail sheet |
| `displaySmall` | 28 / 32 sp | 700 | -0.6 sp, tnum | Stat tile numbers |
| `headlineLarge` | 28 / 34 sp | 700 | -0.5 sp | Screen titles (Shield wordmark, App scanner, Activity), onboarding titles, hero state headline |
| `headlineMedium` | 24 / 30 sp | 700 | -0.4 sp | Dialog titles (M3 default slot) |
| `headlineSmall` | 22 / 28 sp | 700 | -0.3 sp | Sheet app name |
| `titleLarge` | 18 / 24 sp | 600 | -0.1 sp | Sensor tile state ("Idle"), callout title |
| `titleMedium` | 16 / 22 sp | 600 | 0 | List row title, toggle row title |
| `titleSmall` | 14 / 20 sp | 600 | 0 | Section headers |
| `bodyLarge` | 16 / 24 sp | 400 | 0 | Onboarding body, hero support line |
| `bodyMedium` | 14 / 20 sp | 400 | 0.1 sp | Row subtitles, dialog text, empty-state body |
| `bodySmall` | 13 / 18 sp | 400 | 0.1 sp | Meta lines, footnotes. **13sp is the minimum for any readable text.** |
| `labelLarge` | 16 / 20 sp | 600 | 0 | Buttons (52dp) |
| `labelMedium` | 14 / 18 sp | 600 | 0.1 sp | Chips, segmented control, compact buttons, nav labels, "See all" |
| `labelSmall` | 12 / 16 sp | 600 | 0.2 sp | Badges, sensor-tile caption ("Camera"), step indicator |

Rules:
- No other sizes. Do not set `fontSize` inline. Use `MaterialTheme.typography.x` and copy only colour, or `fontFeatureSettings` for numerals.
- Weight 800 is only used for `displayLarge` and the wordmark. Do not use `FontWeight.Black` or `ExtraBold` anywhere else.
- Body text is never bolder than 500. Emphasis inside body text uses `textPrimary` rather than bold.
- At most three text colours per component: `textPrimary`, `textSecondary` and one semantic colour.

### 2.3 Numerals

- Every number that can change or sit in a column uses tabular figures: scores, counts, durations, clock times and "+3 more". Provide `val Numeric = SpanStyle/TextStyle(fontFeatureSettings = "tnum")` and the helper `fun TextStyle.tnum() = copy(fontFeatureSettings = "tnum")`.
- A score is shown as the number in `textPrimary` followed by `/100` at the same size or smaller, in `textTertiary`. Example: **92**`/100`.
- Animated counts (hero score, stat tiles on first load) count up over 600ms with `Motion.decelerate`. Never count on every recomposition, only when the value changes.

### 2.4 Wordmark

"SpyShield" is set in `headlineLarge` at weight 800, in a single colour (`textPrimary`), preceded by an 24dp brand logomark (the shield path from 4.1, filled `brand`, with a 7dp white lens circle). Gap: 8dp.

---

## 3. Space, shape, surfaces

### 3.1 Spacing scale (4dp grid)

`object Space { xxs=2, xs=4, s=8, m=12, l=16, xl=20, xxl=24, x3=32, x4=40, x5=56 }` (all `.dp`)

- Screen horizontal gutter: **20dp** (`Space.xl`) on every screen and sheet.
- Gap between sections, from the end of a group to the next section header: **28dp** (`Space.xxl + Space.xs`).
- Section header to its group: **8dp**.
- Inside a group, row padding is **16dp horizontal and 12dp vertical**. There is no gap between rows, only dividers.
- Gap between tiles in a grid or pair: **12dp**.
- Screen title block: 16dp top padding below the status bar, 4dp between title and subtitle, 20dp below the subtitle to the first content.
- LazyColumn bottom content padding: 32dp.

### 3.2 Corner radii

`object Radius { xs=6, s=9, m=12, l=16, xl=20, xxl=28 }` (all `.dp`), plus `CircleShape` for full.

| Component | Radius |
|---|---|
| Hero container | `xxl` 28 |
| Bottom sheet (top corners), dialogs | `xxl` 28 |
| Grouped list container, stat tile, sensor tile, callout | `xl` 20 |
| Primary, secondary and destructive buttons (52dp) | `l` 16 |
| Search field | `l` 16 |
| Compact button (40dp), segmented track, risk indicator | `m` 12 |
| Segmented thumb | 9 (track radius minus 3dp inset) |
| Solid icon tile 32dp | `s` 9 |
| Large icon tile 56dp, onboarding icon tile 72dp | 18 and 22 |
| Badge | `xs` 6 |
| Chips, switches, nav indicator, dots | full |
| App icons | 26% of size (`RoundedCornerShape(size * 0.26f)`) |

### 3.3 Surface layering, not borders

Depth comes from tone steps. Back to front:

1. `bg`: the screen.
2. `surface1`: groups, tiles, hero, nav bar. In light mode these are white on warm grey, which gives the iOS grouped look.
3. `sheet`, and `surface2` for groups placed on a sheet or for dialogs.
4. `control`: interactive fills inside surfaces.

Shadows: none in dark mode. In light mode, only the segmented thumb (`shadow(1.dp, RoundedCornerShape(9.dp), ambientColor = Black.copy(0.06f), spotColor = Black.copy(0.10f))`) and the stock M3 sheet and dialog elevation. Set `tonalElevation = 0.dp` everywhere. The nav bar is separated by a 1px `divider` line on its top edge, which is the only container hairline.

Groups on a sheet: provide `val LocalGroupColor = staticCompositionLocalOf { Color.Unspecified }`. `GroupedSection` reads it and falls back to `Spy.colors.surface1`. `AppDetailSheet` wraps its content in `CompositionLocalProvider(LocalGroupColor provides Spy.colors.surface2)`.

---

## 4. Components

Every component lives in `ui/Components.kt`, except the hero, which goes in `ui/ShieldHero.kt` and replaces `ShieldOrb.kt`. The API contract is in section 10. Engineers code against those signatures in parallel.

### 4.1 Hero status header: `ShieldHero` + `HeroCard`

This replaces the ring orb. It is the one ownable, eye-catching element of the app.

**States.** `enum class HeroState { OFF, CALM, CAUTION, ALERT }`. The mapping from the existing logic is:
- `!protection` → OFF
- `liveAlert` → ALERT
- `score >= 70` → CALM
- otherwise → CAUTION

| State | State colour | Glyph in shield | Headline (`headlineLarge`) | Support (`bodyLarge`, `textSecondary`) |
|---|---|---|---|---|
| CALM | `safe` | `Icons.Rounded.Check` | "You're protected" | "Watching 24/7." |
| CAUTION | `warning` | `Icons.Rounded.PriorityHigh` | "Protected" | "But some apps look risky" |
| ALERT | `danger` | the live sensor's icon (`Videocam` or `Mic`; camera wins if both) | "Something is using your sensors" | "In the background" + (if a live app label is known) " · <label>" |
| OFF | `textTertiary` | `Icons.Rounded.PowerSettingsNew` | "Protection is off" | "You're not being watched over" |

The headline colour is `textPrimary` in every state except ALERT, where it is `danger`. The state colour lives in the shield, not in paragraphs.

**`ShieldHero(state, modifier, size = 148.dp)` drawing.** Use a Canvas inside a `size`×`size` box, layered back to front:

1. **Aura.** A circle with radius `size/2` filled with a radial gradient of the state colour (alpha 0.28 in dark, 0.18 in light) to transparent. CALM and CAUTION "breathe": scale 0.94↔1.06 with alpha ±0.06, 4000ms `FastOutSlowInEasing`, `RepeatMode.Reverse`. ALERT uses aura alpha 0.40, plus two **shield-shaped** outline ripples (same path, stroke 2dp, state colour). Each ripple scales 1.0→1.40 while alpha goes 0.5→0, over 1400ms `LinearOutSlowInEasing`, with the second ripple offset by half a period. OFF has no aura and no motion.
2. **Back plate.** The shield path scaled to 0.78 of `size` in height. Fill: the state container colour (`safeContainer`, `warningContainer` or `dangerContainer`), or `control` for OFF.
3. **Front plate.** The same path at 0.62 of `size` in height, centred and nudged 2dp up. Fill: the vertical gradient from 1.4. Upper-half sheen: white from 16% to 0%. For OFF, the fill is `control` with a 2dp `textTertiary` stroke and no sheen.
4. **Glyph.** 40dp, `textOnAccent`. For OFF it is `textTertiary`. When the state changes, use `AnimatedContent` with fade plus scale 0.8→1, 360ms, `Motion.emphasized`.

**Shield path**, normalised to a 100 × 116 box, which you scale to fit:

```
M50,0 L92,15 Q100,18 100,26 V56 C100,85 79,106 50,116 C21,106 0,85 0,56 V26 Q0,18 8,15 Z
```

Animate colours with `animateColorAsState(target, tween(360, easing = Motion.emphasized))`. If `LocalReduceMotion` is true, skip every infinite animation and draw the static frame at scale 1.

**`HeroCard`, the Dashboard composite.** It is a `surface1` container with radius 28, full width.

- Top wash: a vertical gradient of the state colour (alpha 0.10 in dark, 0.08 in light) to transparent over the top 60% of the card. This is allowed gradient 1.
- Content, centred: 28dp top padding, `ShieldHero(148dp)`, 12dp, headline, 4dp, support line (both centred, no maxLines), 16dp, **score readout**, 20dp, divider (full width, no inset), then the **protection toggle row** (4.4 `ToggleRow` with `tileBrand` and the `PowerSettingsNew` icon; `tileNeutral` when off). Title: "Real-time protection". Subtitle: "Watching camera & mic in the background" when on, "Tap to start watching your camera & mic" when off.
- Score readout: a centred Row with the text "Privacy score" (`labelMedium`, `textSecondary`), an 8dp gap, the score (`titleLarge.tnum()`, `textPrimary`) with "/100" (`bodySmall`, `textTertiary`), a 10dp gap, then a meter 56×6dp (track `control`, fill in the state colour, `CircleShape`, animated 600ms). For OFF the score is still shown; the fill colour is `textTertiary`.

### 4.2 Section header: `SectionHeader(title, action = null, onAction = null)`

- Row with 4dp horizontal padding (so it sits just inside the group edge) and a minimum height of 32dp.
- Title: `titleSmall`, `textSecondary`, sentence case.
- Action: text button, `labelMedium`, `brandText`. Use `Modifier.minimumInteractiveComponentSize()` with no extra padding beyond 8dp horizontal.
- 28dp above it (from the previous section) and 8dp below it.

### 4.3 Grouped inset list: `GroupedSection` + `GroupRow` + `RowDivider`

This replaces the "card per row" pattern everywhere.

- `GroupedSection(modifier, content: ColumnScope)`: background `LocalGroupColor` (default `surface1`), `RoundedCornerShape(20.dp)`, `clip`, no padding of its own, no border.
- Rows inside it are separated by `RowDivider(inset = 72.dp)`, a 1px `divider` line starting at the leading inset (16 padding + 40 leading + 16 gap). For rows with a 32dp tile the inset is 64dp. There is no divider after the last row.
- **For lazy lists** (Apps, Activity), each item draws its own slice. Use `Modifier.groupItem(position: GroupPosition)` with `enum class GroupPosition { Single, First, Middle, Last }`. The shape is:
  - First: top corners 20dp, bottom corners 0
  - Middle: all corners 0
  - Last: top corners 0, bottom corners 20dp
  - Single: all corners 20dp
  
  The modifier applies `clip(shape).background(LocalGroupColor or surface1)`. Draw the divider at the top of every item that is not First or Single, using `drawBehind` or a `RowDivider` composable as the first child.
- `ListRow(leading, title, subtitle = null, meta = null, trailing = null, onClick = null)`:
  - `heightIn(min = 64.dp)`, padding 16 horizontal and 12 vertical, `Arrangement` gap 16dp after leading and 12dp before trailing.
  - Title is `titleMedium`, `textPrimary`, maxLines 1 with ellipsis. Subtitle is `bodyMedium`, `textSecondary`, maxLines 2. The meta line is `bodySmall`, `textTertiary`, maxLines 1.
  - Press feedback is the default bounded ripple (`ripple()` from material3) on the full row slice. Do not scale rows.

### 4.4 Toggle row: `ToggleRow(tileIcon, tileColor, title, subtitle, checked, onCheckedChange, note = null, noteTone = Tone.WARNING)`

- Layout: `ListRow` anatomy with leading `IconTile(tileIcon, tileColor)` (32dp) and trailing `SpySwitch`.
- `heightIn(min = 64.dp)`. With a 2–3 line subtitle it simply grows, so never use a fixed height.
- The whole row is the target: `Modifier.toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)`, and the `Switch` gets `onCheckedChange = null`. **Exception:** if the existing code does extra work in the switch's callback (as `OverlayToggleCard` does), route both through the same lambda, so behaviour stays identical.
- `note` (for example "Turn on notifications so the alarm can sound", or the overlay permission hint) is shown under the subtitle as `bodySmall` in the tone colour, with a 14dp `Icons.Rounded.Info` glyph and a 4dp gap.
- `SpySwitch` colours: checked track `brand`, checked thumb white; unchecked track `control`, unchecked thumb `textTertiary`, unchecked border `Transparent`. **All switches use brand**, including auto-block. The danger meaning of auto-block is carried by its red tile.

### 4.5 Icon tiles

- `IconTile(icon, color, size = 32.dp)`: a solid `color` fill (one of the `tile*` tokens), `RoundedCornerShape(size * 0.28f)`, glyph at 56% of size, white. Use it in toggle rows, callouts, the onboarding bullet list and the action list in the sheet.
- `TonalIcon(icon, tone, size = 40.dp)`: a `tone.container` fill with a `tone.foreground` glyph at 50% of size, `RoundedCornerShape(12.dp)`. Use it in stat tiles (36dp), sensor tiles (40dp) and empty states (56dp, radius 18).
- `IconBubble` is deleted. Replace each use with one of the two above.

### 4.6 App icon and sensor badge

- `AppIcon(pkg, size)`: keep the loader. The shape becomes 26%. The fallback fill is `control` and the fallback glyph is `textTertiary`.
- `AppIconWithSensor(pkg, sensor, tone, size = 40.dp)`: the app icon with an 18dp circle at the bottom-right, offset (+4dp, +4dp). The circle is filled with the tone's tile colour (`tileDanger` if suspicious or blocked, `tileWarning` if live, else `tileCamera` or `tileMic`), has a 2dp ring in the group colour, and carries an 11dp white sensor glyph. This is the event row's signature detail. If `packageName == null`, use `TonalIcon(sensor.icon, tone, 40.dp)` instead.

### 4.7 Stat tile: `StatTile(icon, value, label, tone, onClick)`

- `surface1`, radius 20, padding 16, `heightIn(min = 112.dp)`, weight 1 in a Row with 12dp gaps.
- Top-left: `TonalIcon(icon, tone, 36.dp)`. Top-right: `Icons.AutoMirrored.Rounded.KeyboardArrowRight`, 20dp, `textTertiary`.
- Push the value to the bottom with a `Spacer(weight 1f)`. The value is `displaySmall.tnum()`, `textPrimary`. Below it, the label is `bodySmall`, `textSecondary`, maxLines 2 (no ellipsis at 1 line).
- Press: scale 0.97 (`Motion.pressSpring`) plus a ripple.
- Dashboard tones: "Apps with camera" CAMERA, "Apps with mic" MIC, "Alerts today" DANGER if > 0 else SAFE, "Blocked apps" DANGER if > 0 else NEUTRAL.

### 4.8 Sensor live tile (Dashboard)

- Same shell as `StatTile` (radius 20, padding 16, min height 132).
- Top row: `TonalIcon(sensor.icon, tone, 40.dp)` and, when active, a `LiveDot(tone color)` at the trailing edge.
- Then 14dp, a caption (`sensor.title`, `labelSmall`, `textSecondary`), 2dp, the state word (`titleLarge`): "Idle", "In use", "Spying?" or "Not watched". Then a detail line (the app label, "No app is using it" or "Turn on protection") in `bodySmall`, `textSecondary`, maxLines 1.
- Tone: suspicious → DANGER, active → WARNING, protection off → NEUTRAL, otherwise SAFE. The state-word colour is the tone foreground when active, else `textPrimary`.
- When suspicious, the **whole tile background becomes `dangerContainer`** (animated 220ms). This is the only tile that changes its fill.

### 4.9 Chips and segmented control

- **Filter chips** (Apps: All / Camera / Microphone / High risk / Blocked) are in a horizontally scrolling Row with 8dp gaps and 20dp content padding, so they bleed to the screen edge.
  - Height 36dp, `CircleShape`, horizontal padding 14dp, `labelMedium`, no border.
  - Unselected: `control` fill with `textSecondary` text. Selected: `inverseSurface` fill with `inverseText` text and a leading 16dp `Icons.Rounded.Check`.
  - Animate the fill 150ms. Add `minimumInteractiveComponentSize()`.
- **Segmented control** (Activity filters: All / Suspicious / Camera / Mic): `SegmentedControl(options: List<String>, selectedIndex, onSelect)`.
  - Full width, height 40dp, track `control` with radius 12, 3dp inner padding.
  - The thumb is `controlThumb` with radius 9 (light mode adds the shadow from 3.3) and slides with `animateDpAsState` 220ms `Motion.standard`.
  - Labels are `labelMedium`: `textPrimary` when selected, `textSecondary` otherwise, single line, `TextOverflow.Ellipsis`. Each segment is ≥ 48dp tall via the touch target.
  - Semantics: `Role.Tab` and `selected`.
- **"Show system apps"** becomes a `ToggleRow`-style compact row without a tile. Put it inside its own single-row group under the chips: title "Show system apps" (`bodyMedium`, `textPrimary`), `SpySwitch` on the right, min height 52dp.

### 4.10 Badges: `StatusBadge(text, tone, icon = null, live = false)`

This replaces `Pill`.
- Height 22dp (`heightIn(min = 22.dp)`), `RoundedCornerShape(6.dp)`, horizontal padding 8, 4dp gap.
- Fill is `tone.container`; text is `labelSmall` in `tone.onContainer`, sentence case. Optional icon 12dp in the same colour.
- If `live = true`, show a leading 6dp `LiveDot`.
- Tone mapping:
  - "Blocked" / "Auto-blocked" / "Suspicious": DANGER
  - "Live": WARNING with live dot
  - "Pre-installed": NEUTRAL (`control` fill, `textSecondary` text)
  - "Trusted": SAFE
  - Step indicators in onboarding are not badges (see 7.6).

### 4.11 Risk indicator: `RiskIndicator(level, score)`

The trailing element of app rows.
- A 44×44dp box with `RoundedCornerShape(12.dp)` and the level's container fill (`safeContainer`, `warningContainer` or `dangerContainer`).
- Centred number: `titleMedium.tnum()`, weight 700, in the level's foreground colour.
- Content description: "Risk {score} of 100, {level.title}".
- Blocked apps show `StatusBadge("Blocked", DANGER, Icons.Rounded.Block)` instead.

### 4.12 Buttons

All are `Button` or `Surface` based, with `RoundedCornerShape(16.dp)`, height 52dp, full width unless noted, text `labelLarge`, an optional 20dp leading icon with an 8dp gap, and press scale 0.98 (`Motion.pressSpring`).

| Kind | Fill | Content | Use |
|---|---|---|---|
| `PrimaryButton(tone = BRAND)` | `brand` (or `warning` with `onWarning` for tone WARNING) | white | One per screen region: Start protection, Next, Open permissions, callout CTAs |
| `SecondaryButton` | `control` | `textPrimary` | Unblock app, alternate actions |
| `DestructiveButton` | `dangerStrong` | white | Block this app |
| `GhostButton` | none | `brandText` (or `textSecondary` for "Not now", "Skip", "Later") | Tertiary actions; min height 48dp |

- Compact variant (`compact = true`): height 40dp (touch target 48 via `minimumInteractiveComponentSize`), radius 12, `labelMedium`, horizontal padding 16, wraps its content width. Use it in callouts and the onboarding "Next".
- Disabled state: `control` fill with `textTertiary` content.
- Dialog buttons stay `TextButton`. Confirm is `brandText`, destructive confirm ("Clear") is `danger`, and dismiss is `textSecondary`.

### 4.13 Callout: `Callout(icon, tone, title, body, content = {})`

Use for setup cards ("Allow Usage Access", "Turn on alerts") and the permission-changes card.
- `tone.container` fill, radius 20, padding 16. Row: `IconTile(icon, tone tile, 32.dp)`, 14dp gap, Column.
- Title is `titleMedium`, `textPrimary`. Body is `bodyMedium`, `textSecondary` (contrast verified on warningContainer in both themes). 12dp, then the compact `PrimaryButton(tone = WARNING)`.
- **Permission changes:** under the body, show a nested list on a `surface1` group (radius 14, inside the callout) of rows: `AppIcon` 32, text "{label} can now use your {noun}" (`bodyMedium`, `textPrimary`, maxLines 2), a trailing sensor glyph 18dp in `warning`, min height 52. Footer row: "+N more" (`bodySmall.tnum()`, `textSecondary`) on the left and a `GhostButton` "Got it" on the right, coloured `onWarningContainer`.

### 4.14 Footnote: `Footnote(text, icon = null)`

Use for the Kill-switch "Pro tip" and the sheet disclaimer.
- No container. Padding 4dp horizontal and 8dp top.
- Optional 16dp icon (`Icons.Rounded.TipsAndUpdates` for the tip) in `textTertiary`, with a 10dp gap.
- Text is `bodySmall`, `textSecondary`. For the tip, the leading "Pro tip:" is set in `textPrimary` weight 600 through an `AnnotatedString`, and the rest of the string is unchanged.

### 4.15 Empty state: `EmptyState(icon, title, body, action = null, inline = false)`

- **Full** (Activity with no events, Apps with no matches): a centred column with 48dp vertical padding and max width 300dp. `TonalIcon(icon, NEUTRAL, 56.dp)`, 16dp, title (`titleMedium`, `textPrimary`), 4dp, body (`bodyMedium`, `textSecondary`, centred). An optional compact `SecondaryButton` follows after 16dp.
  - Apps "No apps match." becomes title "No apps match." with no body, using the `SearchOff` icon.
  - Activity: `History` icon, "Nothing here yet" / "Camera and mic use will show up here."
- **Inline** (Dashboard "Recent alerts" when empty, sheet "Recorded activity" when empty): a single group row with leading `TonalIcon(Icons.Rounded.VerifiedUser, SAFE, 40.dp)` (`NEUTRAL` when protection is off), title and body as in `ListRow`, min height 72.

### 4.16 Bottom sheet

- `ModalBottomSheet`: `containerColor = sheet`, `shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)`, `scrimColor = scrim`, `tonalElevation = 0.dp`.
- Drag handle: 36×4dp, `textTertiary` at 40% alpha, 10dp vertical padding.
- Content: 20dp horizontal padding, 40dp bottom padding plus the navigation-bar inset (`navigationBarsPadding()`). Sections are spaced with the standard 28dp/8dp rhythm. Groups use `surface2` via `LocalGroupColor`.

### 4.17 Dialogs

`AlertDialog`: `containerColor = surface2`, `shape = RoundedCornerShape(28.dp)`, `tonalElevation = 0.dp`. Title is `headlineSmall` (22). Body is `bodyMedium`, `textSecondary`. The icon is 28dp in its tone colour (`Block` → `danger`).

### 4.18 Screen top bar: `ScreenHeader(title, subtitle, actions = {})`

- Not a Material TopAppBar. It is a Row with 20dp horizontal padding, 16dp top padding and 20dp bottom padding.
- Column: title (`headlineLarge`), 4dp, subtitle (`bodyMedium`, `textSecondary`).
- Actions are `CircleIconButton(icon, contentDescription, onClick, enabled)`: a 40dp `control` circle with a 20dp glyph in `textPrimary` (`textTertiary` when disabled), a 48dp touch target, and 8dp between buttons.
- The Apps rescan icon spins while `scanning`, as it does now.

### 4.19 Navigation bar

- `NavigationBar(containerColor = surface1, tonalElevation = 0.dp)`, height 72dp plus the inset, with a 1px `divider` line drawn on its top edge.
- Items: Shield / Apps / Activity.
  - Selected: `Icons.Rounded.*` glyph in `onBrandContainer` on an indicator pill of `brandContainer` (M3 default size 64×32), with the label in `labelMedium` and `textPrimary`.
  - Unselected: the `Icons.Outlined.*` glyph (`Outlined.Shield`, `Outlined.Apps`, `Outlined.History`) in `textSecondary`, with the label in `textSecondary`.
- `alwaysShowLabel = true`.
- Activity badge: `Badge(containerColor = dangerStrong, contentColor = White)` containing the count in `labelSmall.tnum()`. Show "99+" above 99.

### 4.20 Live dot: `LiveDot(color, size = 8.dp)`

This is `PulsingDot` renamed.
- Core dot of `size`, plus one halo that scales 1→2.4 while alpha goes 0.45→0, over 1200ms `LinearOutSlowInEasing`, restarting.
- Under reduce motion, show the static core only.

---

## 5. Motion

`object Motion`:

| Token | Value |
|---|---|
| `fast` | 120ms: press-in, chip fill |
| `standard` | 220ms: colour or fill changes, segmented thumb, tile background |
| `emphasizedDuration` | 360ms: hero state change, glyph swap |
| `countDuration` | 600ms: number count-up, score meter |
| `standardEasing` | `CubicBezierEasing(0.2f, 0f, 0f, 1f)` |
| `emphasized` | `CubicBezierEasing(0.3f, 0f, 0f, 1f)` |
| `decelerate` | `CubicBezierEasing(0f, 0f, 0f, 1f)` |
| `exit` | `CubicBezierEasing(0.3f, 0f, 1f, 1f)` |
| `pressSpring` | `spring(dampingRatio = 0.7f, stiffness = 600f)` |

What animates:
- **State changes**: hero (colour, glyph, aura behaviour), sensor tile background, switch, chips and segmented control, and badge appearance (`fadeIn` with `scaleIn(0.9f)`, 220ms).
- **Press feedback**: tiles and buttons scale to 0.97 or 0.98 with `pressSpring`, through `Modifier.pressScale(interactionSource)`. List rows use only the ripple.
- **List entry**: only on a screen's first composition (keep `remember { mutableStateOf(false) }` per screen), the first 8 items fade in and translate up from 8dp to 0, over 220ms `standardEasing`, staggered 30ms per item. After that, use `Modifier.animateItem(fadeInSpec = tween(220), placementSpec = spring(stiffness = Spring.StiffnessMediumLow), fadeOutSpec = tween(150))` on lazy items so filtering and reordering stay smooth.
- **Tab switch**: keep `AnimatedContent`: `fadeIn(tween(220, easing = standardEasing))` together with `fadeOut(tween(120))`. Do not slide.
- **Onboarding pager**: keep the existing `pageTransition` (alpha and scale), but scale by 0.06 instead of 0.12.
- **Infinite animations** are allowed only for the hero aura and ripples, `LiveDot`, the rescan spinner and the alarm ripples. Nothing else loops.

**Reduce motion.** Provide `val LocalReduceMotion = staticCompositionLocalOf { false }` in `SpyShieldTheme`, set from `Settings.Global.getFloat(contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f`. When it is true, every infinite animation renders a static frame and entry staggers are skipped.

Haptics: optional. Toggle rows may call `LocalHapticFeedback.current.performHapticFeedback(HapticFeedbackType.TextHandleMove)` on change. Add no other haptics.

---

## 6. Iconography

- Use the `Icons.Rounded.*` family (filled, rounded) for everything. Outlined is allowed only for unselected nav items (`Icons.Outlined.*`). Do not mix other styles.
- Sizes:
  - Nav: 24dp
  - Top-bar circle buttons: 20dp
  - Button leading icons: 20dp
  - Glyphs in a 32dp icon tile: 18dp
  - Tonal icons: 50% of the container
  - Inline meta glyphs (sensor in an app row, reasons list): 16dp
  - Badges: 12dp
  - Sensor badge on an app icon: 11dp
  - Hero glyph: 40dp
  - Alarm warning glyph: 28dp
- Sensor glyphs: camera `Videocam`, mic `Mic`. When a permission was requested but not granted, use `VideocamOff` or `MicOff` in `textTertiary`. The content descriptions stay "{title} allowed" and "{title} not allowed".
- Fixed icon vocabulary (use these, do not substitute):

| Meaning | Icon |
|---|---|
| Protection | `PowerSettingsNew` |
| Auto-block | `GppBad` |
| Privacy dot | `FiberManualRecord` |
| New access | `GppMaybe` |
| Usage access | `QueryStats` |
| Alerts | `NotificationsActive` |
| Battery | `BatteryChargingFull` |
| Blocked | `Block` |
| Unblock | `LockOpen` |
| Revoke | `RemoveCircleOutline` |
| Force stop | `PanTool` |
| Trust | `VerifiedUser` |
| Uninstall | `DeleteForever` |
| Share | `IosShare` |
| Clear | `DeleteSweep` |
| Rescan | `Refresh` |
| Search | `Search` |
| Reasons | `WarningAmber` |
| Tip | `TipsAndUpdates` |
| Opens a system screen (trailing) | `AutoMirrored.Rounded.OpenInNew`, 18dp, `textTertiary` |
| Row chevron | `AutoMirrored.Rounded.KeyboardArrowRight`, 20dp, `textTertiary` |

- Every icon that is the only label of a control has a content description. Decorative icons use `null`.

---

## 7. Screen blueprints

All widths assume 411dp with a 20dp gutter, so content is 371dp wide.

### 7.1 Dashboard (Shield tab)

```
[status bar]
 (logomark) SpyShield                         <- 2.4 wordmark, 16dp top
 Catch apps secretly using your camera & mic  <- bodyMedium textSecondary, 4dp
 20dp
 ┌ HeroCard (surface1, r28) ────────────────┐
 │            [ ShieldHero 148 ]            │
 │          You're protected                │ headlineLarge
 │            Watching 24/7.                │ bodyLarge secondary
 │   Privacy score  92/100  ▬▬▬▬▬░░         │
 │──────────────────────────────────────────│ divider
 │ [⏻] Real-time protection          (●  ) │ ToggleRow
 │     Watching camera & mic in the bg      │
 └──────────────────────────────────────────┘
 12dp
 [ Camera tile ]  12  [ Mic tile ]            <- 4.8, pair
 (callouts, each 12dp apart, only when needed:)
 [ Callout: Allow Usage Access   ]
 [ Callout: Turn on alerts       ]
 [ Callout: New camera/mic access] (PermissionChangesCard)
 28dp
 Recent alerts                       See all  <- SectionHeader
 ┌ group: up to 3 EventRows or inline empty ┐
 28dp
 Overview                                     <- SectionHeader (structural label)
 [Apps with camera] [Apps with mic]           <- StatTiles 2x2, 12dp gaps
 [Alerts today    ] [Blocked apps ]
 28dp
 Protection settings
 ┌ group ───────────────────────────────────┐
 │ [GppBad red tile] Auto-block spies  (  ●)│ ToggleRow, note if notifs off
 │──────────────────────────────────────────│ inset 64
 │ [● green tile] Privacy dot          (●  )│ ToggleRow, note if overlay missing
 └──────────────────────────────────────────┘
 Footnote: (tip) Pro tip: on Android 12+, ...
 32dp bottom
```

- `AutoBlockCard` and `OverlayToggleCard` keep their file names and logic, but render as group rows. Each file exposes a `@Composable fun AutoBlockRow()` and `fun OverlayToggleRow()`, and the Dashboard places both inside one `GroupedSection` with a `RowDivider(64.dp)` between them. You may keep the old names as wrappers.
- Auto-block subtitle (verbatim, but shown in `bodySmall` because it is long): "When an app you are not using turns on your camera or mic, SpyShield blocks it and sounds an alarm with a one-tap Stop it now button."
- In the Callout, the setup cards' buttons keep their labels ("Grant access", "Enable alerts").
- Section header labels "Overview" and "Protection settings" are new UI labels. They are structural and change no meaning.

### 7.2 Apps

```
 ScreenHeader: "App scanner" / "{n} apps can reach your camera or mic"   [⟳ circle]
 Search field: 48dp, r16, fill surface1, no border; focus -> 1.5dp brand border;
               leading Search 20 textTertiary; placeholder "Search apps" textTertiary;
               trailing Close circle button when non-empty. 20dp gutter.
 12dp
 Chips row (scrolls, bleeds to edge)
 12dp
 ┌ Show system apps                     (  ●)┐  single-row group, min 52
 20dp
 ┌ group (lazy slices, GroupPosition) ───────┐
 │ [icon44] Instagram                  [82] │ RiskIndicator
 │          ◉cam ◉mic [Pre-installed]       │ meta row 6dp gaps
 │          Opened 3 hours ago              │ bodySmall tertiary
 │──────────────────────────────────────────│ inset 76
 │ ...                                      │
 └──────────────────────────────────────────┘
```

- Row: `ListRow` with `AppIcon(44)`. Title is the label. Second line is a Row of sensor glyphs (16dp, `camera` or `mic` when granted, the `*Off` glyph in `textTertiary` when requested but not granted, hidden when not requested) and an optional "Pre-installed" badge. Third line is "Opened {ago}" or "System app" in `bodySmall`, `textTertiary`. Omit the third line when it is empty. This moves the text off the crowded chip line. Trailing is `RiskIndicator`, or the Blocked badge.
- Sort order stays exactly as it is. If any blocked apps are in the list, render them as their own group, with a "Blocked" `SectionHeader` above, and then the remainder as a second group with an "Apps" header. This is purely visual: the sorted list is split at the first non-blocked index.
- While scanning with an empty list, show 6 skeleton rows (an icon square and two bars in `control`, alpha pulse 0.5↔1 over 900ms) instead of a blank screen.
- The divider inset for 44dp icons is 76dp.

### 7.3 App detail sheet

```
 (handle)
 [AppIcon 64]  Instagram                   <- headlineSmall, maxLines 2
               com.instagram.android       <- bodySmall textTertiary, maxLines 1
               [Blocked] [Trusted]         <- badges, 8dp top, only when applicable
 24dp
 Spy risk                                   <- SectionHeader
 ┌ group ───────────────────────────────────┐
 │  82/100                        [High]    │ displayMedium tnum + "/100" titleLarge tertiary; level badge
 │  ▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬░░░░                  │ 8dp meter, track control, fill level colour, CircleShape
 └──────────────────────────────────────────┘  padding 16, min height 112
 Access
 ┌ Camera                     ⚠ Allowed     ┐ trailing text in tone colour + 16dp icon
 │ Microphone        ✓ Asked, but not allowed│
 └──────────────────────────────────────────┘
 Why this rating
 ┌ ⚠ reason 1 (bodyMedium textPrimary)       ┐ rows min 48, 16dp warning icon, divider inset 48
 └───────────────────────────────────────────┘
 Recorded activity                 12 uses · 3 suspicious   <- header title "Recorded activity", action-slot text in labelMedium textSecondary tnum (not clickable)
 ┌ up to 5 compact EventRows / inline empty  ┐
 28dp
 [ ⊘  Block this app ]  DestructiveButton     (or SecondaryButton "Unblock app" with LockOpen)
 16dp
 ┌ group of actions ─────────────────────────┐
 │ [tileBrand ⊖] Revoke access           ↗  │
 │ [tileWarning ✋] Force stop            ↗  │
 │ [tileSafe ✓] Mark as trusted          (✓)│ trailing CheckCircle in safe only when trusted
 │ [tileDanger ⌫] Uninstall app           ↗ │ title in danger; only if !isSystem
 └───────────────────────────────────────────┘
 Footnote: "Blocked apps trigger an instant alarm ... in Settings."
```

- The action rows reuse the existing callbacks exactly. "Mark as trusted" keeps its two labels ("Mark as trusted" and "Trusted ✓ (tap to undo)"). Its trailing element is `Icons.Rounded.CheckCircle` in `safe` when trusted, and nothing otherwise.
- The Access row state comes from the existing `PermissionRow` mapping: "Allowed" is danger with `WarningAmber`, "Asked, but not allowed" is safe with `CheckCircle`, and "Never asked" is `textTertiary` with `CheckCircle`. The trailing text uses `bodyMedium`, maxLines 1, and may wrap below the label at a large font scale (use `FlowRow` or let the Column wrap).
- The blocked hint dialog follows 4.17.

### 7.4 Activity timeline

```
 ScreenHeader "Activity" / "Every time your camera or mic switched on"  [share ○] [clear ○]
 SegmentedControl  All | Suspicious | Camera | Mic     (20dp gutter)
 16dp
 Today                                     <- sticky header (stickyHeader), titleSmall textSecondary,
                                              bg-coloured background, 8dp v-padding, so groups scroll under it
 ┌ group (lazy slices) ─────────────────────┐
 │ [app+sensor badge]  WhatsApp      3:42 PM│ title row: label (weight 1, ellipsis) + time labelMedium tnum tertiary
 │   [Live][Auto-blocked]                    │ badges row (only if any), 4dp gap
 │   Used camera while you were in X        │ reason bodyMedium secondary, maxLines 2
 │   Camera · 2m 10s                         │ meta bodySmall tertiary tnum ("in use now" when live)
 │   Possible: A, B                          │ bodySmall warning, maxLines 1 (only if >1 suspects)
 │───────────────────────────────────────────│ inset 72
 └───────────────────────────────────────────┘
 28dp before next day header
```

- `EventCard` keeps its name and signature, but renders as an `EventRow` slice. Add a parameter `position: GroupPosition = GroupPosition.Single`, so the Dashboard (3 items) and the sheet (5 items) can draw it inside a group. The badge precedence logic stays the same (Live before Auto-blocked, etc.).
- Leading element: `AppIconWithSensor(pkg, sensor, tone, 40.dp)`. The time is moved from the meta line to the top-right. The meta line becomes "{sensor.title} · {duration or 'in use now'}", which keeps the same information.
- The empty state is the full `EmptyState` centred in the remaining space.
- Clear-history dialog: 4.17, with confirm "Clear" in `danger`.

### 7.5 Spy alarm (full screen, over the lock screen)

This uses a fixed palette that ignores the theme: background gradient `#B3141F` to `#4A0610`, text white.

```
 systemBarsPadding, 24dp horizontal
 Top (16dp):  [logomark white 20] SpyShield · Spy alarm   <- labelMedium white 80%  ("Spy alarm" is a label)
 Centre (weight 1, centred):
     ripples: 2 rings, white, stroke 2dp, from 104dp circle to 1.6x, alpha 0.35→0, 1600ms, offset half
     [ AppIcon 104 ] with 4dp white 20% ring + sensor badge (28dp white circle, 16dp sensor glyph in #B3141F)
     24dp
     {label}                          <- displayLarge white, centred, maxLines 2
     is spying on your {noun}         <- titleLarge white 90%
     16dp
     Auto-block added it ... Force stop.  <- bodyLarge white 80%, max width 320
 Bottom:
     [ Stop it now ]   white fill, text #B3141F, 56dp, r16, labelLarge, icon PanTool 20
     12dp
     [ Not a spy ]     fill white 14%, text white, 56dp, r16
     8dp bottom
```

- Entry: the icon scales 0.85→1 using `spring(0.6f, 400f)` and the text fades in 220ms with a 120ms delay. Ripples respect reduce motion.
- The `WarningAmber` 48dp glyph at the top is removed, because the top label and the ripples carry the urgency.

### 7.6 Onboarding

The structure stays the same: 4 pages, and the bottom bar holds Skip, the dots and Next. Background is flat `bg`.

- **Page 0 (Welcome):**
  - The top part is centred: `ShieldHero(HeroState.CALM, size = 176.dp)`, with no score. Then 24dp, the wordmark row (2.4), 8dp, and "Catch apps secretly using your camera & mic" in `headlineLarge`, centred, max width 320.
  - 28dp, then a `GroupedSection` of three rows, each min height 64: `IconTile(Videocam, tileCamera)`, `IconTile(Mic, tileMic)` and `IconTile(Lock, tileSafe)`, with the bullet text in `bodyMedium` and `textPrimary`.
- **Permission pages (1–3):** top-aligned, 40dp top padding, left-aligned text. This is a deliberate change from the centred layout and reads as more editorial and premium.
  - `IconTile(icon, tile, 72.dp)` with radius 22: page 1 uses `tileBrand` (Usage), page 2 `tileWarning` (Alerts), page 3 `tileSafe` (Battery). Then 24dp.
  - Step indicator: "Step 1 of 3" in `labelSmall.tnum()`, `brandText` (plain text, not a pill). Then 8dp.
  - Title in `headlineLarge`, 12dp, body in `bodyLarge` and `textSecondary`, 28dp.
  - **Status block**: `AnimatedContent` keeps the granted/not-granted switch.
    - Not granted: the `PrimaryButton(button label)` at full width, then 10dp, and the hint in `bodySmall` and `textSecondary`, with an `Info` glyph of 14dp.
    - Granted: a `safeContainer` row (radius 16, padding 16) with a `CheckCircle` of 24dp in `safe`, "All set" (`titleMedium`, `onSafeContainer`) and "Permission granted" (`bodySmall`, `onSafeContainer`).
  - Page 3 footer: 24dp, `PrimaryButton("Start protection", icon = Shield)` with height 56, then the `GhostButton("Not now", textSecondary)`.
    - While page 3's permission is not yet granted, the "Keep SpyShield awake" button must not compete with "Start protection". Render it as a `SecondaryButton` instead of Primary on page 3 only, so that the page has a single primary button.
- **Bottom bar**: 20dp horizontal and 12dp vertical padding.
  - `GhostButton("Skip", textSecondary)`.
  - Dots: the active dot is 20×6dp `brand`, inactive dots are 6×6dp `textTertiary` at 50%, 6dp gaps, animating 300ms.
  - The compact `PrimaryButton("Next", trailing ArrowForward 18)`.
  - On the last page the bar fades as it does now.

### 7.7 Home-screen widget (`res/layout/widget_status.xml`, `drawable/widget_bg.xml`)

- **Background**: a solid `@color/widget_bg` with no stroke and `android:radius="@dimen/widget_radius"` set to 24dp. Add `values/colors.xml` `widget_bg #FFFFFF`, `widget_text_primary #121316`, `widget_text_secondary #5C5F66`, and a new `values-night/colors.xml` with `widget_bg #16171B`, `widget_text_primary #F3F3F1`, `widget_text_secondary #A3A6AE`. Padding is 16dp.
- **Icon**: a 40dp `ImageView` whose background is a new `drawable/widget_icon_bg.xml` (oval, solid `@color/widget_icon_bg`: `#EEF0FF` in light, `#1D2550` in night), with 9dp padding so the 22dp glyph sits centred. The glyph is `ic_stat_shield`, tinted via the existing `setColorFilter` call.
- **Text**:
  - Title: "SpyShield", 15sp, `sans-serif-medium`, `@color/widget_text_primary`.
  - Status: 13sp, `sans-serif-medium`, 8dp start margin, coloured programmatically.
  - Live and alerts lines: 12sp, `@color/widget_text_secondary`, 2dp apart.
- **`StatusWidgetProvider` colour constants** (monitor/, constants only): `CYAN` becomes `SAFE`, set to `0xFF1E9E5A` (`tileSafe`, readable on both backgrounds). `GRAY` becomes `0xFF6B6E76`. `RED` becomes `0xFFD93640`. The status strings stay the same.

### 7.8 Launcher icon (`ic_launcher_foreground.xml`, `colors.xml launcher_bg`, `mipmap-anydpi-v26`)

- **Background**: replace `@color/launcher_bg` with `@drawable/ic_launcher_background`, a vector 108×108 with a linear gradient `#4F6BFF` → `#2E46E0`, from (0,0) to (108,108). This is allowed gradient 4.
- **Foreground**: a white shield using the 4.1 path, scaled to a box 46 wide × 53 tall and centred at (54, 54), so it sits within the 66dp safe zone. It has a "lens" cutout: a circle of radius 8 at (54, 52) filled `#3D5AFE`, with a white 3-radius pupil highlight at (51, 49). No violet or cyan.
- **Monochrome**: a separate `drawable/ic_launcher_monochrome.xml` containing the shield path only, solid white, with the lens punched out using `android:fillType="evenOdd"` and both paths in one. Point `<monochrome>` at it.
- `ic_launcher_round.xml` mirrors `ic_launcher.xml`.
- **Notification small icon** (`ic_stat_shield`) stays as is. `Notifications.kt` `CYAN` becomes `0xFF3D5AFE` (brand) and `ALERT_COLOR` becomes `0xFFD7263D` in both `Notifications.kt` and `InstallAlerts.kt`.
- **`PrivacyDotOverlay`**: `GREEN 0xFF3DD68C`, `ORANGE 0xFFFFB020`, `RED 0xFFFF5D5D`, background `Color.argb(170, 11, 12, 15)`.

### 7.9 System chrome

- `res/values/themes.xml`: parent `android:Theme.Material.Light.NoActionBar`, with `windowBackground @color/bg`. The new `values-night/themes.xml` uses parent `android:Theme.Material.NoActionBar` with the same items. `colors.xml` `bg` is `#F2F2EE`, and `values-night/colors.xml` `bg` is `#0B0C0F`.
- `MainActivity.enableEdgeToEdge`: use `SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT)` for both bars, so icons flip in light mode.
- `Scaffold(containerColor = Spy.colors.bg)`.

---

## 8. Copy and tone

- Sentence case everywhere. Keep every existing string's wording, except where this document explicitly splits a sentence into a headline and support line (hero, 4.1) or moves a fragment (event time). Those are the only permitted text changes, plus the structural labels "Overview", "Protection settings", "Blocked", "Apps" and "Spy alarm".
- Remove the leading "Protection is OFF" / "Real-time protection ON" uppercase emphasis. The title is "Real-time protection" and the subtitle carries the on/off sentence. The state is also obvious from the switch and the hero.
- Use no exclamation marks in headings. In the hero ALERT headline, drop the trailing "!", which keeps the meaning.

---

## 9. Accessibility

- **Contrast** (computed against the colours each pair is actually used on, in both themes). Treat these as a gate in review:

| Pair | Dark | Light |
|---|---|---|
| `textPrimary` on `bg` / `surface1` | ~17:1 | ~17:1 |
| `textSecondary` on `surface1` | 7.1:1 | 6.5:1 |
| `textSecondary` on `control` | 6.3:1 | 5.2:1 |
| `textTertiary` on `surface1` | 5.1:1 | 5.1:1 |
| `textTertiary` on `surface2` | 4.6:1 | 5.1:1 |
| White on `brand` | 5.1:1 | 5.1:1 |
| White on `dangerStrong` | 4.6:1 | 5.0:1 |
| `brandText` on `surface1` | 7.4:1 | 6.6:1 |
| `safe` / `warning` / `danger` text on `surface1` | 9.5 / 9.7 / 5.9 | 5.0 / 4.7 / 5.0 |
| `camera` / `mic` on `surface1` | 8.3 / 7.5 | 5.0 / 5.8 |
| White on alarm `#B3141F` | 6.9:1 | same |

  - Never put `textTertiary` on `control` in light mode (about 4.0:1).
  - Never put white text on `safe` or `warning` in dark mode. Use the `tile*` colours for solid fills; they are only for glyphs, which need 3:1.
- **Touch targets** are ≥ 48×48dp. Use `Modifier.minimumInteractiveComponentSize()` on chips, compact buttons, circle icon buttons, the section "See all" action and the dots (the dots are not interactive). Toggle rows and list rows are whole-row targets.
- **Dynamic type** is tested at 1.0, 1.3 and 2.0 (Android 14 non-linear scaling).
  - Never give text containers a fixed height. Use `heightIn(min = ...)`.
  - Never set `maxLines = 1` on headlines, hero text, onboarding text, callout text or alarm text. `maxLines = 1` is allowed only for app labels in rows, package names, meta lines and chips.
  - Stat-tile labels may take 2 lines. At a font scale of 1.5 or more, the stat-tile pairs and sensor-tile pairs switch from a 2-column Row to a single column. Read `LocalDensity.current.fontScale`.
  - The segmented control at a font scale of 1.5 or more becomes the chip row (4.9).
- **Semantics**:
  - The hero gets `Modifier.semantics(mergeDescendants = true)` with the content description "{headline}. {support}. Privacy score {n} of 100".
  - The risk indicator has a description (4.11).
  - Badges are read as text. Live dots are decorative. Switch rows use `Role.Switch`, and the segmented control uses `Role.Tab`.
  - Section headers use `Modifier.semantics { heading() }`.
- **Reduce motion** is covered in section 5.
- Colour is never the only signal. Each state has a word or a glyph difference: the Off glyphs, the badge text and the state words.

---

## 10. Code contract (so five engineers produce one product)

### 10.1 `ui/Theme.kt` (owner: Engineer 1, lands first; the others code against these names)

```kotlin
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
val DarkSpyColors: SpyColors  // section 1.2 dark column
val LightSpyColors: SpyColors // section 1.2 light column
object SpyTiles { val Brand; val Safe; val Warning; val Danger; val Camera; val Mic; val Neutral } // 1.2 tile table

val LocalSpyColors = staticCompositionLocalOf { DarkSpyColors }
val LocalReduceMotion = staticCompositionLocalOf { false }
val LocalGroupColor = staticCompositionLocalOf { Color.Unspecified }

object Spy { val colors: SpyColors @Composable @ReadOnlyComposable get() = LocalSpyColors.current }

enum class Tone { NEUTRAL, BRAND, SAFE, WARNING, DANGER, CAMERA, MIC }
// @Composable extension getters on Tone: fg, container, onContainer, tile
//   NEUTRAL -> textSecondary / control / textSecondary / SpyTiles.Neutral
//   BRAND   -> brandText / brandContainer / onBrandContainer / SpyTiles.Brand
//   SAFE    -> safe / safeContainer / onSafeContainer / SpyTiles.Safe   (etc.)
//   CAMERA  -> camera / cameraContainer / camera / SpyTiles.Camera
//   MIC     -> mic / micContainer / mic / SpyTiles.Mic

object Space  { /* 3.1 */ }
object Radius { /* 3.2 */ }
object Motion { /* section 5 */ }
fun TextStyle.tnum(): TextStyle

@Composable fun SpyShieldTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit)
// provides LocalSpyColors, LocalReduceMotion, the MaterialTheme(colorScheme = 1.5 mapping, typography = 2.2)
```

- The `SC` object is **deleted**, not aliased, so that no hard-coded dark colours survive. Each engineer replaces the `SC.*` references in the files they own: `SC.Bg` becomes `Spy.colors.bg`, `SC.TextMid` becomes `textSecondary`, `SC.TextLo` becomes `textTertiary`, `SC.Cyan` becomes `brand` or `brandText` depending on whether it was used as a fill or as text, `SC.Violet` becomes `mic` (or removed), `SC.Warn` becomes `warning`, `SC.Danger` becomes `danger` or `dangerStrong`, and `SC.Safe` becomes `safe`.
- `RiskLevel.color` becomes `val RiskLevel.tone: Tone` (non-composable: HIGH→DANGER, MEDIUM→WARNING, LOW→SAFE).

### 10.2 `ui/Components.kt` (owner: Engineer 1)

Use these signatures exactly. Each one implements the referenced section.

```kotlin
@Composable fun SectionHeader(title: String, modifier: Modifier = Modifier, action: String? = null, onAction: (() -> Unit)? = null, trailingText: String? = null) // 4.2
@Composable fun GroupedSection(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit)            // 4.3
enum class GroupPosition { Single, First, Middle, Last }
fun groupPosition(index: Int, count: Int): GroupPosition
@Composable fun Modifier.groupItem(position: GroupPosition): Modifier                                                   // 4.3, includes top divider for Middle/Last via drawBehind with inset param below
@Composable fun RowDivider(inset: Dp)
@Composable fun ListRow(leading: (@Composable () -> Unit)?, title: String, modifier: Modifier = Modifier, subtitle: String? = null, meta: (@Composable () -> Unit)? = null, trailing: (@Composable () -> Unit)? = null, onClick: (() -> Unit)? = null, titleColor: Color = Color.Unspecified)
@Composable fun ToggleRow(tileIcon: ImageVector, tileColor: Color, title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit, modifier: Modifier = Modifier, note: String? = null, noteTone: Tone = Tone.WARNING) // 4.4
@Composable fun SpySwitch(checked: Boolean, onCheckedChange: ((Boolean) -> Unit)?, modifier: Modifier = Modifier)
@Composable fun IconTile(icon: ImageVector, color: Color, size: Dp = 32.dp)                                              // 4.5
@Composable fun TonalIcon(icon: ImageVector, tone: Tone, size: Dp = 40.dp)
@Composable fun AppIcon(pkg: String?, size: Dp)                                                                          // 4.6 (keep loader)
@Composable fun AppIconWithSensor(pkg: String?, sensor: Sensor, tone: Tone, size: Dp = 40.dp)
@Composable fun StatusBadge(text: String, tone: Tone, modifier: Modifier = Modifier, icon: ImageVector? = null, live: Boolean = false) // 4.10
@Composable fun RiskIndicator(level: RiskLevel, score: Int)                                                              // 4.11
@Composable fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null, trailingIcon: ImageVector? = null, tone: Tone = Tone.BRAND, compact: Boolean = false, enabled: Boolean = true, height: Dp = 52.dp) // 4.12
@Composable fun SecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null, compact: Boolean = false)
@Composable fun DestructiveButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null)
@Composable fun GhostButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, color: Color = Color.Unspecified /* defaults to brandText */, enabled: Boolean = true)
@Composable fun Callout(icon: ImageVector, tone: Tone, title: String, body: String, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit = {}) // 4.13
@Composable fun Footnote(text: AnnotatedString, modifier: Modifier = Modifier, icon: ImageVector? = null)               // 4.14
@Composable fun EmptyState(icon: ImageVector, title: String, body: String? = null, modifier: Modifier = Modifier, inline: Boolean = false, tone: Tone = Tone.NEUTRAL, action: (@Composable () -> Unit)? = null) // 4.15
@Composable fun FilterChipRow(labels: List<String>, selectedIndex: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) // 4.9
@Composable fun SegmentedControl(options: List<String>, selectedIndex: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier)
@Composable fun ScreenHeader(title: String, subtitle: String, modifier: Modifier = Modifier, actions: @Composable RowScope.() -> Unit = {}) // 4.18
@Composable fun CircleIconButton(icon: ImageVector, contentDescription: String?, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, iconModifier: Modifier = Modifier)
@Composable fun LiveDot(color: Color, size: Dp = 8.dp)                                                                   // 4.20
@Composable fun Wordmark(modifier: Modifier = Modifier, color: Color = Spy.colors.textPrimary, markColor: Color = Spy.colors.brand) // 2.4
@Composable fun Modifier.pressScale(interactionSource: MutableInteractionSource, pressed: Float = 0.97f): Modifier        // section 5
@Composable fun Modifier.entryAnimation(index: Int, enabled: Boolean): Modifier                                          // section 5
```

Keep `rememberResumeTick`, `ago`, `clock`, `dayLabel`, `duration` and `Sensor.icon` as they are. Delete `GlassCard`, `Pill`, `IconBubble`, `PulsingDot` and `SectionTitle` once nothing references them. Until Engineer 1 lands, the others may stub calls against these signatures.

### 10.3 Ownership

| Engineer | Files |
|---|---|
| 1 | `Theme.kt`, `Components.kt`, `res/values*/themes.xml`, `res/values*/colors.xml`, `font_certs.xml`, the Gradle dependency, `MainActivity` system bars |
| 2 | `ShieldHero.kt` (new; delete `ShieldOrb.kt`), `DashboardScreen.kt`, `AutoBlockCard.kt`, `OverlayToggleCard.kt`, `PermissionChangesCard.kt`, `AppRoot.kt` (nav bar 4.19) |
| 3 | `AppsScreen.kt`, `AppDetailSheet.kt` |
| 4 | `ActivityScreen.kt` (including the `EventCard` → row rendering), `SpyAlarmActivity.kt` |
| 5 | `OnboardingScreen.kt`, the widget layout and drawables, the launcher icon resources, and the colour constants in `monitor/` (`StatusWidgetProvider`, `Notifications`, `InstallAlerts`, `PrivacyDotOverlay`) |

### 10.4 Review checklist (every PR)

- [ ] No `SC.`, no `Color(0x...)` literals in `ui/` outside `Theme.kt`. The only exception is the alarm palette, which lives in `SpyAlarmActivity.kt` as private constants.
- [ ] No `.border(` on containers, no `OutlinedButton`, no `uppercase()`, and no inline `fontSize`.
- [ ] Both themes screenshotted on the Reno8: Dashboard in all 4 hero states, Apps, the sheet (blocked and unblocked), Activity (populated and empty), Alarm, and all 4 onboarding pages.
- [ ] Font scale 2.0 shows no clipped text. Every touch target is at least 48dp. Reduce motion stops all loops.
- [ ] Behaviour is unchanged: the same callbacks, the same conditions for showing each element, and the same strings (except the splits listed in section 8).
