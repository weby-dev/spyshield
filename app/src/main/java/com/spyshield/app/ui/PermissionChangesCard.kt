package com.spyshield.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GppMaybe
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.spyshield.app.data.PermissionChange

/** Warns about apps that picked up camera/mic access since the last scan (DESIGN.md 4.13). */
@Composable
fun PermissionChangesCard(changes: List<PermissionChange>, onOpenApp: (String) -> Unit, onDismiss: () -> Unit) {
    val gained = changes.filter { it.gained }
    if (gained.isEmpty()) return
    val c = Spy.colors
    val shown = gained.take(5)

    Callout(
        icon = Icons.Rounded.GppMaybe,
        tone = Tone.WARNING,
        title = "New camera/mic access",
        body = "These apps were allowed in since your last scan. Make sure it was you.",
    ) {
        Spacer(Modifier.height(Space.m))
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(Radius.l))
                .background(c.surface1),
        ) {
            shown.forEachIndexed { i, change ->
                if (i > 0) RowDivider(inset = 56.dp)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable(role = Role.Button) { onOpenApp(change.packageName) }
                        .heightIn(min = 52.dp)
                        .padding(horizontal = Space.m, vertical = Space.s),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AppIcon(change.packageName, 32.dp)
                    Spacer(Modifier.width(Space.m))
                    Text(
                        "${change.label} can now use your ${change.sensor.noun}",
                        color = c.textPrimary,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(Space.s))
                    Icon(change.sensor.icon, null, tint = c.warning, modifier = Modifier.size(18.dp))
                }
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(top = Space.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (gained.size > 5) {
                Text(
                    "+${gained.size - 5} more",
                    color = c.textSecondary,
                    style = MaterialTheme.typography.bodySmall.tnum(),
                )
            }
            Spacer(Modifier.weight(1f))
            GhostButton("Got it", onClick = onDismiss, color = c.onWarningContainer)
        }
    }
}
