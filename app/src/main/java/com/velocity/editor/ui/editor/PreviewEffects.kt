package com.velocity.editor.ui.editor

import android.graphics.Color
import android.graphics.Typeface
import android.text.SpannableString
import android.text.Spanned
import android.text.style.AbsoluteSizeSpan
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import androidx.media3.common.Effect
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.Contrast
import androidx.media3.effect.OverlayEffect
import androidx.media3.effect.OverlaySettings
import androidx.media3.effect.RgbAdjustment
import androidx.media3.effect.RgbFilter
import androidx.media3.effect.TextOverlay
import androidx.media3.effect.TextureOverlay
import com.google.common.collect.ImmutableList

/** Live preview effects for the editor's video surface — mirrors VideoExporter's filter/title logic exactly. */
@OptIn(UnstableApi::class)
fun buildPreviewEffects(filterId: String?, titleId: String?, projectName: String): List<Effect> {
    val effects = mutableListOf<Effect>()
    when (filterId) {
        "bw" -> effects += RgbFilter.createGrayscaleFilter()
        "vivid" -> effects += Contrast(0.25f)
        "warm" -> effects += RgbAdjustment.Builder().setRedScale(1.15f).setBlueScale(0.88f).build()
        "cool" -> effects += RgbAdjustment.Builder().setRedScale(0.9f).setBlueScale(1.15f).build()
        "faded" -> effects += Contrast(-0.25f)
    }
    if (titleId != null && titleId != "none") {
        effects += OverlayEffect(ImmutableList.of(titleOverlay(titleId, projectName)))
    }
    return effects
}

@OptIn(UnstableApi::class)
private fun titleOverlay(style: String, text: String): TextureOverlay {
    val span = SpannableString(text).apply {
        setSpan(ForegroundColorSpan(Color.WHITE), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        setSpan(AbsoluteSizeSpan(if (style == "bold") 72 else if (style == "minimal") 36 else 52), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        if (style == "bold") setSpan(StyleSpan(Typeface.BOLD), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    }
    val settings = when (style) {
        "bold" -> OverlaySettings.Builder().setBackgroundFrameAnchor(0f, 0.7f).setOverlayFrameAnchor(0f, 0f)
        "minimal" -> OverlaySettings.Builder().setBackgroundFrameAnchor(-0.92f, -0.88f).setOverlayFrameAnchor(-1f, -1f)
        else -> OverlaySettings.Builder().setBackgroundFrameAnchor(0f, -0.8f).setOverlayFrameAnchor(0f, 0f)
    }.build()
    return TextOverlay.createStaticTextOverlay(span, settings)
}
