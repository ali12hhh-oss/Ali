package com.velocity.editor.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.velocity.editor.R
import com.velocity.editor.ui.theme.VelocityColors

@Composable
fun VelocityLogo(logoSize: Dp = 32.dp) {
    Canvas(Modifier.size(logoSize)) {
        val w = this.size.width
        drawRoundRect(
            brush = Brush.linearGradient(listOf(VelocityColors.Teal, VelocityColors.TealDark)),
            cornerRadius = CornerRadius(w * 0.28f),
        )
        val path = Path().apply {
            moveTo(w * 0.28f, w * 0.30f)
            lineTo(w * 0.50f, w * 0.72f)
            lineTo(w * 0.72f, w * 0.30f)
        }
        drawPath(path, Color.White, style = Stroke(width = w * 0.12f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
fun VelocityTopBar(
    modifier: Modifier = Modifier,
    leading: @Composable () -> Unit = {},
    trailing: @Composable () -> Unit = {},
) {
    Box(modifier.fillMaxWidth().height(56.dp).padding(horizontal = 16.dp)) {
        Box(Modifier.align(Alignment.CenterStart)) { leading() }
        Text(
            text = stringResource(R.string.app_name).uppercase(),
            modifier = Modifier.align(Alignment.Center),
            color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp, letterSpacing = 1.sp,
        )
        Box(Modifier.align(Alignment.CenterEnd)) { trailing() }
    }
}

@Composable
fun ExportPill(onClick: () -> Unit) {
    Box(
        Modifier.clip(RoundedCornerShape(8.dp)).background(VelocityColors.Teal)
            .clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 7.dp),
    ) {
        Text(stringResource(R.string.export), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}

@Composable
fun BackButton(onClick: () -> Unit) {
    Row(Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onClick).padding(end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = null, tint = VelocityColors.TextSecondary)
        Text(stringResource(R.string.back), color = VelocityColors.TextSecondary, fontSize = 14.sp)
    }
}

@Composable
fun SectionHeader(@StringRes title: Int, modifier: Modifier = Modifier) {
    Text(
        text = stringResource(title),
        modifier = modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 10.dp),
        color = VelocityColors.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 16.sp,
    )
}
