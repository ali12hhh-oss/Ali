package com.velocity.editor.util

import android.text.format.DateUtils
import java.util.Locale

/** HH:MM:SS:FF timecode at 30 fps, always with Latin digits. */
fun formatTimecode(ms: Long, fps: Int = 30): String {
    val totalSeconds = ms / 1000
    val frames = ((ms % 1000) * fps / 1000).toInt()
    return String.format(
        Locale.US, "%02d:%02d:%02d:%02d",
        totalSeconds / 3600, (totalSeconds % 3600) / 60, totalSeconds % 60, frames,
    )
}

fun formatMmSs(ms: Long): String {
    val s = ms / 1000
    return String.format(Locale.US, "%02d:%02d", s / 60, s % 60)
}

fun relativeTime(timeMs: Long): String =
    DateUtils.getRelativeTimeSpanString(timeMs, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS).toString()
