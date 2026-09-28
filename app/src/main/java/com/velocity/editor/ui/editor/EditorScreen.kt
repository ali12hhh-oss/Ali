package com.velocity.editor.ui.editor

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
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
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import kotlinx.coroutines.delay

private enum class EditorTool(@StringRes val label: Int, val icon: ImageVector, val libraryTab: Int?) {
    Edit(R.string.tool_edit, Icons.Outlined.ContentCut, null),
    Media(R.string.tool_media, Icons.Outlined.VideoLibrary, 0),
    Audio(R.string.tool_audio, Icons.Outlined.MusicNote, 1),
    Effects(R.string.tool_effects, Icons.Outlined.AutoAwesome, null),
    Text(R.string.tool_text, Icons.Outlined.TextFields, null),
    Properties(R.string.tool_properties, Icons.Outlined.Tune, null),
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
    var activeTool by remember { mutableStateOf(EditorTool.Edit) }
    var showProperties by remember { mutableStateOf(false) }
    var showEffects by remember { mutableStateOf(false) }
    var effectsInitialTab by remember { mutableIntStateOf(0) }

    val hint = stringResource(R.string.locked_hint)
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNotEmpty()) viewModel.addVideo(uris)
    }

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

        // Preview — filters and title overlay render live via ExoPlayer video effects.
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

        // Transport controls (always left-to-right, like the reference design)
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = {
                    if (exoPlayer.playbackState == Player.STATE_ENDED) { exoPlayer.seekTo(0, 0); musicPlayer.seekTo(0, 0) }
                    exoPlayer.play()
                    if (musicClips.isNotEmpty()) musicPlayer.play()
                }) {
                    Icon(Icons.Outlined.PlayArrow, stringResource(R.string.play), tint = if (isPlaying) VelocityColors.Teal else Color.White)
                }
                IconButton(onClick = { exoPlayer.pause(); musicPlayer.pause() }) {
                    Icon(Icons.Outlined.Pause, stringResource(R.string.pause), tint = if (!isPlaying) VelocityColors.Teal else Color.White)
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

        // Timeline header
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
            onSeek = ::seekTo,
            onSelectClip = { selectedClip = it },
            onToggleMute = viewModel::toggleMute,
            onToggleLock = viewModel::toggleLock,
            onTrimClip = viewModel::trimClip,
            modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
        )

        // Bottom tool bar
        Row(Modifier.fillMaxWidth().background(VelocityColors.Surface).navigationBarsPadding().padding(vertical = 8.dp)) {
            EditorTool.values().forEach { tool ->
                val active = tool == activeTool
                Column(
                    Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).clickable {
                        activeTool = tool
                        when (tool) {
                            EditorTool.Properties -> showProperties = true
                            EditorTool.Effects -> { effectsInitialTab = 0; showEffects = true }
                            EditorTool.Text -> { effectsInitialTab = 1; showEffects = true }
                            else -> tool.libraryTab?.let { onOpenLibrary(state.projectId, it) }
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

    if (showProperties) {
        ModalBottomSheet(
            onDismissRequest = { showProperties = false },
            containerColor = VelocityColors.Surface,
        ) {
            Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 28.dp)) {
                Text(stringResource(R.string.properties_title), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(Modifier.size(12.dp))
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
    }

    if (showEffects) {
        ModalBottomSheet(
            onDismissRequest = { showEffects = false },
            containerColor = VelocityColors.Surface,
        ) {
            var sheetTab by remember { mutableIntStateOf(effectsInitialTab) }
            TabRow(selectedTabIndex = sheetTab, containerColor = Color.Transparent, contentColor = VelocityColors.Teal) {
                listOf(R.string.section_filters, R.string.section_titles, R.string.section_transitions).forEachIndexed { i, label ->
                    Tab(
                        selected = sheetTab == i, onClick = { sheetTab = i },
                        text = { Text(stringResource(label), fontSize = 13.sp) },
                        selectedContentColor = VelocityColors.Teal, unselectedContentColor = VelocityColors.TextSecondary,
                    )
                }
            }
            Column(Modifier.padding(bottom = 28.dp, top = 8.dp)) {
                when (sheetTab) {
                    0 -> {
                        val unlocked = state.addons[Addon.ADVANCED_FILTERS] != false
                        EffectSection(R.string.section_filters, EffectsCatalog.filters, unlocked, { (state.filterId ?: "original") == it.id }, showHeader = false) {
                            if (it.premium && !unlocked) Toast.makeText(context, hint, Toast.LENGTH_SHORT).show()
                            else viewModel.setFilter(if (it.id == "original") null else it.id)
                        }
                    }
                    1 -> EffectSection(R.string.section_titles, EffectsCatalog.titles, true, { (state.titleId ?: "none") == it.id }, showHeader = false) {
                        viewModel.setTitle(if (it.id == "none") null else it.id)
                    }
                    else -> {
                        val unlocked = state.addons[Addon.CINEMATIC_TRANSITIONS] != false
                        EffectSection(R.string.section_transitions, EffectsCatalog.transitions, unlocked, { state.transitionId == it.id }, showHeader = false) {
                            if (it.premium && !unlocked) Toast.makeText(context, hint, Toast.LENGTH_SHORT).show()
                            else viewModel.setTransition(if (state.transitionId == it.id) null else it.id)
                        }
                    }
                }
            }
        }
    }
}
