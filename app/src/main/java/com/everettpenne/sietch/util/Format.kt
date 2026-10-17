package com.everettpenne.sietch.util

import java.util.Locale

/** A file size the way the rest of the system writes it: B, KB, MB, GB, TB. */
fun formatBytes(bytes: Long): String {
    if (bytes < 0) return "—"
    if (bytes < 1024) return "$bytes B"
    val units = arrayOf("KB", "MB", "GB", "TB")
    var value = bytes.toDouble()
    var i = -1
    do { value /= 1024.0; i++ } while (value >= 1024 && i < units.size - 1)
    return if (value >= 100) "%.0f %s".format(Locale.ROOT, value, units[i]) else "%.1f %s".format(Locale.ROOT, value, units[i])
}

/** A short relative date for "now-ish" times, and a short date for older ones, as Seekur and Thumper do. */
fun formatRelative(ms: Long): String {
    if (ms <= 0) return ""
    val now = System.currentTimeMillis()
    val seconds = (now - ms) / 1000
    if (seconds in 0..59) return "now"
    val minutes = seconds / 60
    if (minutes < 60) return "${minutes}m ago"
    val hours = minutes / 60
    if (hours < 24) return "${hours}h ago"
    val days = hours / 24
    if (days < 7) return "${days}d ago"
    return java.text.DateFormat.getDateInstance(java.text.DateFormat.SHORT).format(java.util.Date(ms))
}

/**
 * The one-line result of a bulk operation, honest about partial failures: "Copied 3", "Copied 2, 1 failed",
 * "Deleted failed", "Moved 0". [verb] is the past participle ("Copied", "Moved", "Deleted").
 */
fun opSummary(verb: String, ok: Int, total: Int): String {
    val failed = total - ok
    return when {
        total <= 0 -> "$verb 0"
        failed == 0 -> "$verb $ok"
        ok == 0 -> "$verb failed"
        else -> "$verb $ok, $failed failed"
    }
}
