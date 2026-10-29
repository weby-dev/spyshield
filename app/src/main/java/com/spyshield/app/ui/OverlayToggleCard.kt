package com.spyshield.app.ui

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FiberManualRecord
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.spyshield.app.data.OverlayPrefs
import com.spyshield.app.data.Store
import com.spyshield.app.monitor.PrivacyDotOverlay

/**
 * Settings row for the on-screen privacy dot. Sends the user to grant "Display over other apps" if
 * needed. Renders as a group row (DESIGN.md 7.1); place it inside a [GroupedSection].
 */
@Composable
fun OverlayToggleRow() {
    val context = LocalContext.current
    val tick = rememberResumeTick()
    var wanted by remember { mutableStateOf(OverlayPrefs.isEnabled(context)) }
    var canDraw by remember { mutableStateOf(Settings.canDrawOverlays(context)) }

    // Re-check after returning from the system permission screen, then sync the overlay.
    LaunchedEffect(tick, wanted) {
        canDraw = Settings.canDrawOverlays(context)
        if (wanted && canDraw) PrivacyDotOverlay.update(context, Store.live.value) else PrivacyDotOverlay.hide(context)
    }

    // Same logic the switch used before; the whole row and the switch both route through it.
    val onToggle: (Boolean) -> Unit = { checked ->
        wanted = checked
        OverlayPrefs.setEnabled(context, checked)
        if (checked && !Settings.canDrawOverlays(context)) {
            runCatching {
                context.startActivity(
                    Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
        }
    }

    ToggleRow(
        tileIcon = Icons.Rounded.FiberManualRecord,
        tileColor = SpyTiles.Safe,
        title = "Privacy dot",
        subtitle = "Show a green/orange dot on top of every app while the camera or mic is on",
        checked = wanted && canDraw,
        onCheckedChange = onToggle,
        note = if (wanted && !canDraw) "Allow \"Display over other apps\" so the dot can appear." else null,
        noteTone = Tone.WARNING,
    )
}

/** Legacy entry point: the privacy-dot row in its own single-row group. */
@Composable
fun OverlayToggleCard() {
    GroupedSection { OverlayToggleRow() }
}
