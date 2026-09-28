package com.velocity.editor.ui.editor

import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.VolumeOff
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.velocity.editor.R
import com.velocity.editor.data.ClipEntity
import com.velocity.editor.domain.Tracks
import com.velocity.editor.ui.library.EffectsCatalog
import com.velocity.editor.ui.theme.VelocityColors
import com.velocity.editor.util.formatMmSs
import java.util.Random

private val HeaderWidth = 76.dp
private val RulerHeight = 24.dp
private val TrackHeight = 42.dp
private val TrackGap = 4.dp
private const val DpPerSecond = 56f
private const val MinTrimMs = 500L

private fun trackColor(id: String): Color = when (id) {
    Tracks.V1 -> VelocityColors.TrackVideo
    Tracks.V2 -> VelocityColors.TrackTransition
    Tracks.A2 -> VelocityColors.TrackMusic
    else -> VelocityColors.TrackVoice
}

/** The timeline always flows left-to-right, in both languages, like every professional editor. */
@Composable
fun TimelinePanel(
    tracks: List<TrackUi>,
    positionMs: Long,
    totalMs: Long,
    isPlaying: Boolean,
    selectedClipId: Long?,
    onSeek: (Long) -> Unit,
    onSelectClip: (Long?) -> Unit,
    onToggleMute: (String) -> Unit,
    onToggleLock: (String) -> Unit,
    onTrimClip: (Long, Long, Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        val density = LocalDensity.current
        val scroll = rememberScrollState()
        var viewportPx by remember { mutableIntStateOf(0) }
        val viewportDp = with(density) { viewportPx.toDp().value }
        val contentDp = maxOf(totalMs / 1000f * DpPerSecond + 120f, viewportDp).dp
        val totalHeight = RulerHeight + (TrackHeight + TrackGap) * tracks.size
        val currentOnSeek by rememberUpdatedState(onSeek)
        val playheadDp = positionMs / 1000f * DpPerSecond

        LaunchedEffect(positionMs, isPlaying) {
            if (isPlaying) {
                val target = with(density) { playheadDp.dp.roundToPx() } - viewportPx / 2
                scroll.scrollTo(target.coerceAtLeast(0))
            }
        }

        fun pxToMs(px: Float): Long = (px / density.density / DpPerSecond * 1000f).toLong()

        Row(modifier) {
            Column(Modifier.width(HeaderWidth)) {
                Spacer(Modifier.height(RulerHeight))
                tracks.forEach { track ->
                    TrackHeader(track, onToggleMute, onToggleLock)
                    Spacer(Modifier.height(TrackGap))
                }
            }
            Box(
                Modifier.weight(1f).height(totalHeight)
                    .onSizeChanged { viewportPx = it.width }
                    .horizontalScroll(scroll),
            ) {
                Column(Modifier.width(contentDp)) {
                    Ruler(contentDp, onSeekPx = { currentOnSeek(pxToMs(it)) })
                    tracks.forEach { track ->
                        TrackLane(
                            track = track, width = contentDp, selectedClipId = selectedClipId,
                            density = density,
                            onSelect = { id -> onSelectClip(id); currentOnSeek(tracks.flatMap { it.clips }.first { c -> c.id == id }.startMs) },
                            onTapEmpty = { px -> onSelectClip(null); currentOnSeek(pxToMs(px)) },
                            onTrim = onTrimClip,
                        )
                        Spacer(Modifier.height(TrackGap))
                    }
                }
                Box(
                    Modifier.offset(x = playheadDp.dp - 1.dp).width(2.dp).height(totalHeight).background(Color.White),
                )
                Box(
                    Modifier.offset(x = playheadDp.dp - 5.dp).size(10.dp).clip(CircleShape).background(Color.White),
                )
            }
        }
    }
}

@Composable
private fun TrackHeader(track: TrackUi, onToggleMute: (String) -> Unit, onToggleLock: (String) -> Unit) {
    val isAudio = track.id == Tracks.A1 || track.id == Tracks.A2 || track.id == Tracks.V1
    Column(Modifier.height(TrackHeight).padding(end = 6.dp), verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(track.id, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, fontSize = 12.sp, color = VelocityColors.TextPrimary)
            Spacer(Modifier.width(6.dp))
            if (isAudio) {
                Icon(
                    if (track.muted) Icons.Outlined.VolumeOff else Icons.Outlined.VolumeUp,
                    contentDescription = stringResource(R.string.mute),
                    tint = if (track.muted) VelocityColors.Teal else VelocityColors.TextSecondary,
                    modifier = Modifier.size(16.dp).clickable { onToggleMute(track.id) },
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.mute), fontSize = 9.sp, color = VelocityColors.TextSecondary, maxLines = 1)
            Spacer(Modifier.width(6.dp))
            Icon(
                if (track.locked) Icons.Outlined.Lock else Icons.Outlined.LockOpen,
                contentDescription = stringResource(R.string.lock),
                tint = if (track.locked) VelocityColors.Teal else VelocityColors.TextSecondary,
                modifier = Modifier.size(12.dp).clickable { onToggleLock(track.id) },
            )
        }
    }
}

@Composable
private fun Ruler(width: Dp, onSeekPx: (Float) -> Unit) {
    val measurer = rememberTextMeasurer()
    val currentSeek by rememberUpdatedState(onSeekPx)
    val labelStyle = TextStyle(color = VelocityColors.TextSecondary, fontSize = 9.sp)
    Canvas(
        Modifier.width(width).height(RulerHeight)
            .pointerInput(Unit) { detectTapGestures { currentSeek(it.x) } }
            .pointerInput(Unit) { detectHorizontalDragGestures { change, _ -> currentSeek(change.position.x) } },
    ) {
        val pxPerSecond = DpPerSecond.dp.toPx()
        val seconds = (size.width / pxPerSecond).toInt()
        for (s in 0..seconds) {
            val x = s * pxPerSecond
            val major = s % 5 == 0
            val tick = if (major) 10.dp.toPx() else 5.dp.toPx()
            drawLine(VelocityColors.TextSecondary, Offset(x, size.height - tick), Offset(x, size.height), 1.dp.toPx())
            if (major) drawText(measurer, formatMmSs(s * 1000L), Offset(x + 3.dp.toPx(), 0f), labelStyle)
        }
    }
}

@Composable
private fun TrackLane(
    track: TrackUi,
    width: Dp,
    selectedClipId: Long?,
    density: Density,
    onSelect: (Long) -> Unit,
    onTapEmpty: (Float) -> Unit,
    onTrim: (Long, Long, Long) -> Unit,
) {
    val color = trackColor(track.id)
    val currentTapEmpty by rememberUpdatedState(onTapEmpty)
    val trimmable = track.id == Tracks.V1 || track.id == Tracks.A2
    Box(
        Modifier.width(width).height(TrackHeight).clip(RoundedCornerShape(6.dp))
            .background(Color.White.copy(alpha = 0.04f))
            .pointerInput(Unit) { detectTapGestures { currentTapEmpty(it.x) } },
    ) {
        track.clips.forEach { clip ->
            val selected = clip.id == selectedClipId
            Box(
                Modifier.offset(x = (clip.startMs / 1000f * DpPerSecond).dp)
                    .width((clip.durationMs / 1000f * DpPerSecond).dp)
                    .fillMaxHeight().padding(1.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(color.copy(alpha = 0.92f))
                    .then(if (selected) Modifier.border(2.dp, Color.White, RoundedCornerShape(6.dp)) else Modifier)
                    .clickable { onSelect(clip.id) },
            ) {
                if (track.id != Tracks.V2) {
                    Waveform(seed = clip.id, color = Color.Black.copy(alpha = 0.28f), modifier = Modifier.matchParentSize())
                }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxHeight()) {
                    if (track.id == Tracks.V1) {
                        AsyncImage(
                            model = Uri.parse(clip.uri), contentDescription = null,
                            contentScale = ContentScale.Crop, modifier = Modifier.fillMaxHeight().aspectRatio(1f),
                        )
                    }
                    val label = if (track.id == Tracks.V2) EffectsCatalog.find(clip.name)?.let { stringResource(it.label) } ?: clip.name else clip.name
                    Text(
                        label, color = Color.White, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 5.dp),
                    )
                }
                if (trimmable) {
                    TrimHandle(isStart = true, clip = clip, density = density, onTrim = onTrim, modifier = Modifier.align(Alignment.CenterStart))
                    TrimHandle(isStart = false, clip = clip, density = density, onTrim = onTrim, modifier = Modifier.align(Alignment.CenterEnd))
                }
            }
        }
    }
}

/** Drag handle on a clip's left/right edge to trim it — shortens or extends into the source media. */
@Composable
private fun TrimHandle(
    isStart: Boolean,
    clip: ClipEntity,
    density: Density,
    onTrim: (Long, Long, Long) -> Unit,
    modifier: Modifier,
) {
    var trimStart by remember(clip.id) { mutableStateOf(clip.trimStartMs) }
    var duration by remember(clip.id) { mutableStateOf(clip.durationMs) }
    Box(
        modifier
            .width(14.dp).fillMaxHeight()
            .pointerInput(clip.id) {
                detectDragGestures(
                    onDragStart = {
                        trimStart = clip.trimStartMs
                        duration = clip.durationMs
                    },
                ) { change, dragAmount ->
                    change.consume()
                    val deltaMs = (dragAmount.x / density.density / DpPerSecond * 1000f).toLong()
                    val cap = clip.sourceDurationMs.takeIf { it > 0 } ?: (trimStart + duration)
                    if (isStart) {
                        val newStart = (trimStart + deltaMs).coerceIn(0L, (cap - MinTrimMs).coerceAtLeast(0L))
                        val newDuration = (duration - (newStart - trimStart)).coerceAtLeast(MinTrimMs)
                        trimStart = newStart
                        duration = newDuration
                    } else {
                        val newDuration = (duration + deltaMs).coerceIn(MinTrimMs, (cap - trimStart).coerceAtLeast(MinTrimMs))
                        duration = newDuration
                    }
                    onTrim(clip.id, trimStart, duration)
                }
            }
            .padding(3.dp)
            .background(Color.White.copy(alpha = 0.6f), RoundedCornerShape(3.dp)),
    )
}

@Composable
private fun Waveform(seed: Long, color: Color, modifier: Modifier) {
    Canvas(modifier) {
        val random = Random(seed)
        val step = 3.dp.toPx()
        var x = 0f
        while (x < size.width) {
            val h = (0.2f + random.nextFloat() * 0.8f) * size.height * 0.7f
            drawLine(color, Offset(x, (size.height - h) / 2), Offset(x, (size.height + h) / 2), 1.5.dp.toPx())
            x += step
        }
    }
}
