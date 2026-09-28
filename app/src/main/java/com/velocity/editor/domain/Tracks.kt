package com.velocity.editor.domain

object Tracks {
    const val V1 = "V1"
    const val V2 = "V2"
    const val A2 = "A2"
    const val A1 = "A1"
    val order = listOf(V1, V2, A2, A1)
}

enum class Quality(val factor: Float) { LOW(0.4f), MEDIUM(0.7f), HIGH(1f) }

enum class Addon(val key: String) {
    REMOVE_WATERMARK("watermark"),
    MUSIC_LIBRARY("music"),
    CINEMATIC_TRANSITIONS("transitions"),
    ADVANCED_FILTERS("filters"),
    EXPORT_4K("export4k"),
}

/** Estimated video bitrate in Mbps. 4K / 60fps / High = 50 Mbps. */
fun estimateBitrateMbps(height: Int, fps: Int, quality: Quality): Int {
    val base = when {
        height >= 2160 -> 25f
        height >= 1080 -> 8f
        else -> 5f
    }
    return (base * (fps / 30f) * quality.factor).toInt().coerceAtLeast(1)
}
