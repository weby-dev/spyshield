package com.spyshield.app.data

import android.content.Context
import android.content.Intent
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Max events listed in a shared report, newest first. */
private const val REPORT_EVENT_LIMIT = 50

private const val RULE = "────────────────────────────────"

/**
 * Builds a plain-text privacy report suitable for sharing via any messaging/mail app.
 * Pure string building — safe to call on the main thread for the sizes Store keeps.
 */
fun buildReport(
    context: Context,
    apps: List<AppPrivacyInfo>,
    events: List<AccessEvent>,
    blocked: Set<String>,
): String {
    val stamp = SimpleDateFormat("EEE, d MMM yyyy · h:mm a", Locale.getDefault())
    val time = SimpleDateFormat("d MMM, h:mm a", Locale.getDefault())
    val high = apps.filter { it.level == RiskLevel.HIGH }.sortedByDescending { it.riskScore }
    val medium = apps.filter { it.level == RiskLevel.MEDIUM }.sortedByDescending { it.riskScore }

    return buildString {
        appendLine("SPYSHIELD PRIVACY REPORT")
        appendLine("Generated ${stamp.format(Date())}")
        appendLine(RULE)
        appendLine()

        // ── Summary counts ──
        appendLine("SUMMARY")
        appendLine("  Apps scanned:            ${apps.size}")
        appendLine("  Camera access granted:   ${apps.count { it.cameraGranted }}")
        appendLine("  Mic access granted:      ${apps.count { it.micGranted }}")
        appendLine("  High risk apps:          ${high.size}")
        appendLine("  Medium risk apps:        ${medium.size}")
        appendLine("  Blocked apps:            ${blocked.size}")
        appendLine("  Recorded events:         ${events.size}")
        appendLine("  Suspicious events:       ${events.count { it.suspicious }}")
        appendLine("  Blocked attempts:        ${events.count { it.blocked }}")
        appendLine()

        // ── Risky apps ──
        appendRiskSection("HIGH RISK APPS", high)
        appendRiskSection("MEDIUM RISK APPS", medium)

        // ── Blocked apps ──
        appendLine("BLOCKED APPS (${blocked.size})")
        if (blocked.isEmpty()) {
            appendLine("  None")
        } else {
            val labels = apps.associate { it.packageName to it.label }
            blocked.map { pkg -> (labels[pkg] ?: context.appLabel(pkg)) to pkg }
                .sortedBy { it.first.lowercase() }
                .forEach { (label, pkg) -> appendLine("  • $label ($pkg)") }
        }
        appendLine()

        // ── Recent events ──
        val recent = events.sortedByDescending { it.start }.take(REPORT_EVENT_LIMIT)
        appendLine("RECENT ACTIVITY (last ${recent.size} of ${events.size})")
        if (recent.isEmpty()) appendLine("  No camera or mic use recorded")
        recent.forEach { e ->
            val dur = e.end?.let { formatDuration(it - e.start) } ?: "in use"
            val flags = buildList {
                if (e.suspicious) add("SUSPICIOUS")
                if (e.blocked) add("BLOCKED")
            }
            val flagText = if (flags.isEmpty()) "" else "  [${flags.joinToString(", ")}]"
            appendLine("  ${time.format(Date(e.start))}  ${e.sensor.title} · ${e.appLabel} · $dur$flagText")
            if (e.reason.isNotBlank()) appendLine("      ${e.reason}")
        }
        appendLine()
        appendLine(RULE)
        append("Generated on-device by SpyShield. No data left your phone unless you shared this report.")
    }
}

private fun StringBuilder.appendRiskSection(title: String, list: List<AppPrivacyInfo>) {
    appendLine("$title (${list.size})")
    if (list.isEmpty()) appendLine("  None")
    list.forEach { a ->
        val perms = buildList {
            if (a.cameraGranted) add("camera") else if (a.cameraRequested) add("camera (requested)")
            if (a.micGranted) add("mic") else if (a.micRequested) add("mic (requested)")
        }.joinToString(", ").ifEmpty { "no sensor permissions" }
        appendLine("  • ${a.label} — score ${a.riskScore}/100")
        appendLine("      ${a.packageName} · $perms")
        if (a.uses > 0) appendLine("      ${a.uses} uses, ${a.suspiciousUses} suspicious")
        a.reasons.forEach { appendLine("      - $it") }
    }
    appendLine()
}

private fun formatDuration(ms: Long): String {
    val s = (ms / 1000).coerceAtLeast(0)
    return when {
        s < 60 -> "${s}s"
        s < 3600 -> "${s / 60}m ${s % 60}s"
        else -> "${s / 3600}h ${(s % 3600) / 60}m"
    }
}

/** Opens the system share sheet with the report as plain text. */
fun share(context: Context, report: String) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "SpyShield privacy report")
        putExtra(Intent.EXTRA_TEXT, report)
    }
    val chooser = Intent.createChooser(send, "Share privacy report").apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(chooser)
}
