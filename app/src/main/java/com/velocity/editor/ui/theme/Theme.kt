package com.velocity.editor.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object VelocityColors {
    val Background = Color(0xFF101214)
    val Surface = Color(0xFF1A1D1F)
    val SurfaceHigh = Color(0xFF25292C)
    val Outline = Color(0xFF34393D)
    val Teal = Color(0xFF22B39A)
    val TealDark = Color(0xFF127F6E)
    val TextPrimary = Color(0xFFF2F4F5)
    val TextSecondary = Color(0xFF9AA3A8)
    val TrackVideo = Color(0xFF17B890)
    val TrackTransition = Color(0xFF3F63A6)
    val TrackMusic = Color(0xFF7C5FD0)
    val TrackVoice = Color(0xFF26A793)
}

private val VelocityScheme = darkColorScheme(
    primary = VelocityColors.Teal,
    onPrimary = Color.White,
    secondary = VelocityColors.Teal,
    background = VelocityColors.Background,
    onBackground = VelocityColors.TextPrimary,
    surface = VelocityColors.Surface,
    onSurface = VelocityColors.TextPrimary,
    surfaceVariant = VelocityColors.SurfaceHigh,
    onSurfaceVariant = VelocityColors.TextSecondary,
    outline = VelocityColors.Outline,
)

@Composable
fun VelocityTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = VelocityScheme, typography = Typography(), content = content)
}
