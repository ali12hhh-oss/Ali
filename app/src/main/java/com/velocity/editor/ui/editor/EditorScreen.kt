package com.velocity.editor.ui.editor

import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.ContentCut
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.velocity.editor.R
import com.velocity.editor.data.ClipEntity
import com.velocity.editor.domain.Addon
import com.velocity.editor.domain.Tracks
import com.velocity.editor.ui.components.BackButton
import com.velocity.editor.ui.components.ExportPill
import com.velocity.editor.ui.components.VelocityTopBar
import com.velocity.editor.ui.library.EffectSection
import com.velocity.editor.ui.library.EffectsCatalog
import com.velocity.editor.ui.theme.VelocityColors
import com.velocity.editor.util.formatTimecode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

private enum class EditorTool(@StringRes val label: Int, val icon: ImageVector) {
    Media(R.string.tool_media, Icons.Outlined.VideoLibrary),
    Audio(R.string.tool_audio, Icons.Outlined.MusicNote),
    Split(R.string.tool_edit, Icons.Outlined.ContentCut),
    Effects(R.string.tool_effects, Icons.Outlined.AutoAwesome),
    Text(R.string.tool_text, Icons.Outlined.TextFields),
    Speed(R.string.speed, Icons.Outlined.Speed),
}

private enum class EffectsSubTab(@StringRes val label: Int) {
    Filters(R.string.section_filters), Titles(R.string.section_titles), Transitions(R.string.section_transitions)
}

private fun mediaItemFor(clip: ClipEntity): MediaItem =
    MediaItem.Builder().setUri(clip.uri)
        .setClippingConfiguration(
            MediaItem.ClippingConfiguration.Builder()
                .setStartPositionMs(clip.trimStartMs)
                .setEndPositionMs(clip.trimStartMs + clip.durationMs)
                .build(),
        ).build()

private fun seekTrack(player: ExoPlayer, clips: List<ClipEntity>, targetMs: Long) {
    if (clips.isEmpty()) return
    val index = clips.indexOfLast { it.startMs <= targetMs }.coerceAtLeast(0)
    val clip = clips[index]
    player.seekTo(index, (targetMs - clip.startMs).coerceIn(0L, clip.durationMs))
}

/** Mirrors VideoExporter's/PreviewEffects' filter look, but as an android.graphics.ColorMatrix for a static thumbnail. */
private fun filterColorMatrix(id: String?): ColorMatrix? = when (id) {
    "bw" -> ColorMatrix().apply { setSaturation(0f) }
    "vivid" -> contrastMatrix(1.25f)
    "warm" -> ColorMatrix(
        floatArrayOf(
            1.15f, 0f, 0f, 0f, 0f,
            0f, 1f, 0f, 0f, 0f,
            0f, 0f, 0.85f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f,
        ),
    )
    "cool" -> ColorMatrix(
        floatArrayOf(
            0.9f, 0f, 0f, 0f, 0f,
            0f, 1f, 0f, 0f, 0f,
            0f, 0f, 1.15f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f,
        ),
    )
    "faded" -> contrastMatrix(0.75f)
    else -> null
}

private fun contrastMatrix(scale: Float): ColorMatrix {
    val t = (1 - scale) * 128f
    return ColorMatrix(
        floatArrayOf(
            scale, 0f, 0f, 0f, t,
            0f, scale, 0f, 0f, t,
            0f, 0f, scale, 0f, t,
            0f, 0f, 0f, 1f, 0f,
        ),
    )
}

/** Real filter preview: takes one actual frame from the current video and applies the filter to it. */
@Composable
private fun FilterPreviewTile(id: String, sourceFrame: Bitmap?, modifier: Modifier) {
    val bitmap = remember(sourceFrame, id) {
        sourceFrame?.let { src ->
            val matrix = filterColorMatrix(id)
            if (matrix == null) {
                src
            } else {
                val out = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
                val canvas = AndroidCanvas(out)
                val paint = Paint().apply { colorFilter = ColorMatrixColorFilter(matrix) }
                canvas.drawBitmap(src, 0f, 0f, paint)
                out
            }
        }
    }
    if (bitmap != null) {
        Image(bitmap.asImageBitmap(), null, modifier, contentScale = ContentScale.Crop)
    } else {
        Box(modifier.background(VelocityColors.SurfaceHigh))
    }
}

/** Real title preview: an "Aa" sample rendered in the exact weight/size the overlay will use. */
@Composable
private fun TitlePreviewTile(id: String, modifier: Modifier) {
    Box(modifier.background(Color(0xFF14171A)), contentAlignment = Alignment.Center) {
        when (id) {
            "classic" -> Text("Aa", color = Color.White, fontSize = 20.sp)
            "bold" -> Text("Aa", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black)
            "minimal" -> Text("Aa", color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
            else -> Text("Aa", color = VelocityColors.TextSecondary, fontSize = 20.sp)
        }
    }
}

/** Real transition preview: a small looping animation matching fade/slide/zoom; other styles get a pulsing demo. */
@Composable
private fun TransitionPreviewTile(id: String, modifier: Modifier) {
    val infinite = rememberInfiniteTransition(label = "transition_preview")
    val phase by infinite.animateFloat(0f, 1f, infiniteRepeatable(tween(1100), RepeatMode.Restart), label = "phase")
    Box(modifier.background(Color(0xFF12323A))) {
        when (id) {
            "fade" -> Box(Modifier.matchParentSize().background(VelocityColors.TrackVideo.copy(alpha = phase)))
            "slide" -> Box(Modifier.matchParentSize().offset(x = ((phase - 0.5f) * 50).dp).background(VelocityColors.TrackVideo))
            "zoom" -> Box(Modifier.matchParentSize().scale(0.55f + phase * 0.45f).background(VelocityColors.TrackVideo))
            else -> Box(Modifier.matchParentSize().background(VelocityColors.TrackVideo.copy(alpha = 0.3f + phase * 0.5f)))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, UnstableApi::class)
@Composable
fun EditorScreen(
    onBack: () -> Unit,
    onExport: (Long) -> Unit,
    onOpenLibrary: (Long, Int) -> Unit,
    viewModel: EditorViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val exoPlayer = remember { ExoPlayer.Builder(context).build() }
    val musicPlayer = remember { ExoPlayer.Builder(context).build() }
    DisposableEffect(exoPlayer, musicPlayer) { onDispose { exoPlayer.release(); musicPlayer.release() } }

    val v1 = state.tracks.firstOrNull { it.id == Tracks.V1 }
    val videoClips = v1?.clips.orEmpty()
    val v1Muted = v1?.muted == true
    val a2 = state.tracks.firstOrNull { it.id == Tracks.A2 }
    val musicClips = a2?.clips.orEmpty()
    val a2Muted = a2?.muted == true

    var isPlaying by remember { mutableStateOf(false) }
    var positionMs by remember { mutableLongStateOf(0L) }
    var speed by remember { mutableFloatStateOf(1f) }
    var volume by remember { mutableFloatStateOf(1f) }
    var selectedClip by remember { mutableStateOf<Long?>(null) }
    var activeTool by remember { mutableStateOf<EditorTool?>(null) }
    var effectsSubTab by remember { mutableStateOf(EffectsSubTab.Filters) }
    var zoom by remember { mutableFloatStateOf(1f) }

    // A real frame from the current video, used to render actual filtered thumbnails below.
    var sourceFrame by remember { mutableStateOf<Bitmap?>(null) }
    val previewClipUri = videoClips.firstOrNull()?.uri
    LaunchedEffect(previewClipUri) {
        sourceFrame = previewClipUri?.let { uriStr ->
            withContext(Dispatchers.IO) {
                val retriever = MediaMetadataRetriever()
                val frame = runCatching {
                    retriever.setDataSource(context, Uri.parse(uriStr))
                    retriever.getFrameAtTime(0)
                }.getOrNull()
                runCatching { retriever.release() }
                frame
            }
        }
    }

    val hint = stringResource(R.string.locked_hint)
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNotEmpty()) viewModel.addVideo(uris)
    }

    fun toggleTool(tool: EditorTool) { activeTool = if (activeTool == tool) null else tool }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) { isPlaying = playing }
        }
        exoPlayer.addListener(listener)
        onDispose { exoPlayer.removeListener(listener) }
    }
    LaunchedEffect(videoClips) {
        exoPlayer.setMediaItems(videoClips.map(::mediaItemFor))
        exoPlayer.prepare()
    }
    LaunchedEffect(musicClips) {
        musicPlayer.setMediaItems(musicClips.map(::mediaItemFor))
        musicPlayer.prepare()
    }
    LaunchedEffect(v1Muted, volume) { exoPlayer.volume = if (v1Muted) 0f else volume }
    LaunchedEffect(a2Muted, volume) { musicPlayer.volume = if (a2Muted) 0f else volume }
    LaunchedEffect(speed) { exoPlayer.setPlaybackSpeed(speed); musicPlayer.setPlaybackSpeed(speed) }
    LaunchedEffect(state.filterId, state.titleId, state.projectName) {
        exoPlayer.setVideoEffects(buildPreviewEffects(state.filterId, state.titleId, state.projectName))
    }
    LaunchedEffect(videoClips) {
        while (true) {
            val base = videoClips.getOrNull(exoPlayer.currentMediaItemIndex)?.startMs ?: 0L
            positionMs = base + exoPlayer.currentPosition.coerceAtLeast(0L)
            delay(40)
        }
    }

    fun seekTo(ms: Long) {
        if (videoClips.isEmpty()) return
        val target = ms.coerceIn(0L, state.totalMs)
        seekTrack(exoPlayer, videoClips, target)
        seekTrack(musicPlayer, musicClips, target)
        positionMs = target
    }

    fun togglePlay() {
        if (isPlaying) {
            exoPlayer.pause()
            musicPlayer.pause()
        } else {
            if (exoPlayer.playbackState == Player.STATE_ENDED || positionMs >= state.totalMs) {
                seekTo(0)
            }
            exoPlayer.playWhenReady = true
            exoPlayer.play()
            if (musicClips.isNotEmpty()) {
                musicPlayer.playWhenReady = true
                musicPlayer.play()
            }
        }
    }

    Column(Modifier.fillMaxSize().background(VelocityColors.Background).statusBarsPadding()) {
        VelocityTopBar(
            leading = { BackButton(onBack) },
            trailing = { ExportPill { onExport(state.projectId) } },
        )
        Text(
            stringResource(R.string.editor_project_name, state.projectName),
            Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
            color = VelocityColors.TextSecondary, fontSize = 11.sp,
        )

        Box(
            Modifier.padding(horizontal = 16.dp, vertical = 6.dp).fillMaxWidth().aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(12.dp)).background(Color.Black),
            contentAlignment = Alignment.Center,
        ) {
            if (videoClips.isEmpty()) {
                Text(stringResource(R.string.preview_empty), color = VelocityColors.TextSecondary, fontSize = 13.sp)
            } else {
                AndroidView(
                    factory = { ctx -> PlayerView(ctx).apply { useController = false; this.player = exoPlayer } },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = ::togglePlay) {
                    Icon(if (isPlaying) Icons.Outlined.Pause else Icons.Outlined.PlayArrow, stringResource(if (isPlaying) R.string.pause else R.string.play), tint = VelocityColors.Teal)
                }
                Slider(
                    value = positionMs.toFloat().coerceAtMost(state.totalMs.toFloat().coerceAtLeast(1f)),
                    onValueChange = { seekTo(it.toLong()) },
                    valueRange = 0f..state.totalMs.toFloat().coerceAtLeast(1f),
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White, activeTrackColor = VelocityColors.Teal,
                        inactiveTrackColor = VelocityColors.Outline,
                    ),
                    modifier = Modifier.weight(1f),
                )
                Text(formatTimecode(positionMs), fontSize = 11.sp, color = VelocityColors.TextPrimary)
                Icon(Icons.AutoMirrored.Outlined.VolumeUp, null, tint = VelocityColors.TextSecondary, modifier = Modifier.padding(start = 8.dp, end = 4.dp).size(18.dp))
            }
        }

        Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.timeline), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = VelocityColors.TextPrimary)
            Spacer(Modifier.weight(1f))
            IconButton(onClick = { picker.launch(arrayOf("video/*")) }) {
                Icon(Icons.Outlined.Add, stringResource(R.string.add_clip), tint = VelocityColors.Teal)
            }
            IconButton(
                onClick = { selectedClip?.let { viewModel.deleteClip(it); selectedClip = null } },
                enabled = selectedClip != null,
            ) {
                Icon(
                    Icons.Outlined.Delete, stringResource(R.string.delete),
                    tint = if (selectedClip != null) Color.White else VelocityColors.Outline,
                )
            }
        }

        TimelinePanel(
            tracks = state.tracks,
            positionMs = positionMs,
            totalMs = state.totalMs,
            isPlaying = isPlaying,
            selectedClipId = selectedClip,
            zoom = zoom,
            onZoomChange = { zoom = (zoom * it).coerceIn(MinZoom, MaxZoom) },
            onSeek = ::seekTo,
            onSelectClip = { selectedClip = it },
            onToggleMute = viewModel::toggleMute,
            onToggleLock = viewModel::toggleLock,
            onTrimClip = viewModel::trimClip,
            modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
        )

        if (activeTool == EditorTool.Effects) {
            Column(Modifier.background(VelocityColors.Surface)) {
                Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 6.dp)) {
                    EffectsSubTab.values().forEach { sub ->
                        Text(
                            stringResource(sub.label), fontSize = 13.sp,
                            color = if (effectsSubTab == sub) VelocityColors.Teal else VelocityColors.TextSecondary,
                            fontWeight = if (effectsSubTab == sub) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.clickable { effectsSubTab = sub }.padding(end = 20.dp),
                        )
                    }
                }
                when (effectsSubTab) {
                    EffectsSubTab.Filters -> {
                        val unlocked = state.addons[Addon.ADVANCED_FILTERS] != false
                        EffectSection(
                            R.string.section_filters, EffectsCatalog.filters, unlocked,
                            { (state.filterId ?: "original") == it.id }, showHeader = false,
                            tileContent = { item, _, m -> FilterPreviewTile(item.id, sourceFrame, m) },
                        ) {
                            if (it.premium && !unlocked) Toast.makeText(context, hint, Toast.LENGTH_SHORT).show()
                            else viewModel.setFilter(if (it.id == "original") null else it.id)
                        }
                    }
                    EffectsSubTab.Titles -> EffectSection(
                        R.string.section_titles, EffectsCatalog.titles, true,
                        { (state.titleId ?: "none") == it.id }, showHeader = false,
                        tileContent = { item, _, m -> TitlePreviewTile(item.id, m) },
                    ) {
                        viewModel.setTitle(if (it.id == "none") null else it.id)
                    }
                    EffectsSubTab.Transitions -> {
                        val unlocked = state.addons[Addon.CINEMATIC_TRANSITIONS] != false
                        EffectSection(
                            R.string.section_transitions, EffectsCatalog.transitions, unlocked,
                            { state.transitionId == it.id }, showHeader = false,
                            tileContent = { item, _, m -> TransitionPreviewTile(item.id, m) },
                        ) {
                            if (it.premium && !unlocked) Toast.makeText(context, hint, Toast.LENGTH_SHORT).show()
                            else viewModel.setTransition(if (state.transitionId == it.id) null else it.id)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        } else if (activeTool == EditorTool.Text) {
            Column(Modifier.background(VelocityColors.Surface).padding(bottom = 8.dp)) {
                EffectSection(
                    R.string.section_titles, EffectsCatalog.titles, true,
                    { (state.titleId ?: "none") == it.id },
                    tileContent = { item, _, m -> TitlePreviewTile(item.id, m) },
                ) {
                    viewModel.setTitle(if (it.id == "none") null else it.id)
                }
            }
        } else if (activeTool == EditorTool.Speed) {
            Column(Modifier.background(VelocityColors.Surface).padding(horizontal = 20.dp, vertical = 10.dp)) {
                Text("${stringResource(R.string.speed)}  ${"%.1f".format(speed)}x", fontSize = 13.sp, color = VelocityColors.TextSecondary)
                Slider(
                    value = speed, onValueChange = { speed = it }, valueRange = 0.5f..2f,
                    colors = SliderDefaults.colors(thumbColor = VelocityColors.Teal, activeTrackColor = VelocityColors.Teal),
                )
                Text("${stringResource(R.string.volume)}  ${(volume * 100).toInt()}%", fontSize = 13.sp, color = VelocityColors.TextSecondary)
                Slider(
                    value = volume, onValueChange = { volume = it }, valueRange = 0f..1f,
                    colors = SliderDefaults.colors(thumbColor = VelocityColors.Teal, activeTrackColor = VelocityColors.Teal),
                )
            }
        }

        Row(
            Modifier.fillMaxWidth().background(VelocityColors.Surface).navigationBarsPadding()
                .horizontalScroll(rememberScrollState()).padding(vertical = 8.dp),
        ) {
            EditorTool.values().forEach { tool ->
                val active = tool == activeTool
                Column(
                    Modifier.width(72.dp).clip(RoundedCornerShape(8.dp)).clickable {
                        when (tool) {
                            EditorTool.Media -> { activeTool = null; onOpenLibrary(state.projectId, 0) }
                            EditorTool.Audio -> { activeTool = null; onOpenLibrary(state.projectId, 1) }
                            EditorTool.Split -> selectedClip?.let { /* split handled in a later pass */ }
                            else -> toggleTool(tool)
                        }
                    }.padding(vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(tool.icon, null, tint = if (active) VelocityColors.Teal else VelocityColors.TextSecondary)
                    Text(
                        stringResource(tool.label), fontSize = 10.sp, maxLines = 1,
                        color = if (active) VelocityColors.Teal else VelocityColors.TextSecondary,
                    )
                }
            }
        }
    }
}
