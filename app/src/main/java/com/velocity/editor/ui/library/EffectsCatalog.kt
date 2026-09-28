package com.velocity.editor.ui.library

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AcUnit
import androidx.compose.material.icons.outlined.Autorenew
import androidx.compose.material.icons.outlined.BlurOn
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Contrast
import androidx.compose.material.icons.outlined.FormatBold
import androidx.compose.material.icons.outlined.Gradient
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material.icons.outlined.Title
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material.icons.outlined.ZoomIn
import androidx.compose.ui.graphics.vector.ImageVector
import com.velocity.editor.R

data class EffectItem(
    val id: String,
    @StringRes val label: Int,
    val icon: ImageVector,
    val premium: Boolean = false,
)

object EffectsCatalog {
    val transitions = listOf(
        EffectItem("fade", R.string.fx_fade, Icons.Outlined.BlurOn),
        EffectItem("slide", R.string.fx_slide, Icons.Outlined.SwapHoriz),
        EffectItem("zoom", R.string.fx_zoom, Icons.Outlined.ZoomIn, premium = true),
        EffectItem("wipe", R.string.fx_wipe, Icons.Outlined.Layers, premium = true),
        EffectItem("glitch", R.string.fx_glitch, Icons.Outlined.Bolt, premium = true),
        EffectItem("spin", R.string.fx_spin, Icons.Outlined.Autorenew, premium = true),
    )

    val filters = listOf(
        EffectItem("original", R.string.flt_original, Icons.Outlined.Gradient),
        EffectItem("bw", R.string.flt_bw, Icons.Outlined.Contrast),
        EffectItem("vivid", R.string.flt_vivid, Icons.Outlined.Palette),
        EffectItem("warm", R.string.flt_warm, Icons.Outlined.WbSunny, premium = true),
        EffectItem("cool", R.string.flt_cool, Icons.Outlined.AcUnit, premium = true),
        EffectItem("faded", R.string.flt_faded, Icons.Outlined.BlurOn, premium = true),
    )

    val titles = listOf(
        EffectItem("none", R.string.ttl_none, Icons.Outlined.TextFields),
        EffectItem("classic", R.string.ttl_classic, Icons.Outlined.Title),
        EffectItem("bold", R.string.ttl_bold, Icons.Outlined.FormatBold),
        EffectItem("minimal", R.string.ttl_minimal, Icons.Outlined.TextFields),
    )

    fun find(id: String): EffectItem? = (transitions + filters + titles).firstOrNull { it.id == id }
}
