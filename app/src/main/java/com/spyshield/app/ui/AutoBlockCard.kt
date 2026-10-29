package com.spyshield.app.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GppBad
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.NotificationManagerCompat
import com.spyshield.app.data.AutoBlockPrefs

/**
 * Opt-in switch for blocking confidently identified spies and sounding the spy alarm.
 * Renders as a group row (DESIGN.md 7.1); place it inside a [GroupedSection].
 */
@Composable
fun AutoBlockRow() {
    val context = LocalContext.current
    var on by remember { mutableStateOf(AutoBlockPrefs.isEnabled(context)) }
    val toggle = { checked: Boolean ->
        on = checked
        AutoBlockPrefs.setEnabled(context, checked)
    }
    val notifsOff = on && !NotificationManagerCompat.from(context).areNotificationsEnabled()

    ToggleRow(
        tileIcon = Icons.Rounded.GppBad,
        tileColor = SpyTiles.Danger,
        title = "Auto-block spies",
        subtitle = "When an app you are not using turns on your camera or mic, SpyShield blocks it and sounds an alarm with a one-tap Stop it now button.",
        checked = on,
        onCheckedChange = toggle,
        note = if (notifsOff) "Turn on notifications so the alarm can sound" else null,
        noteTone = Tone.WARNING,
        subtitleStyle = MaterialTheme.typography.bodySmall,
    )
}

/** Legacy entry point: the auto-block row in its own single-row group. */
@Composable
fun AutoBlockCard() {
    GroupedSection { AutoBlockRow() }
}
