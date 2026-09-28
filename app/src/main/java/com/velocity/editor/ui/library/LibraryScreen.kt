package com.velocity.editor.ui.library

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.velocity.editor.R
import com.velocity.editor.data.ClipEntity
import com.velocity.editor.domain.Addon
import com.velocity.editor.ui.components.BackButton
import com.velocity.editor.ui.components.ExportPill
import com.velocity.editor.ui.components.VelocityTopBar
import com.velocity.editor.ui.theme.VelocityColors
import com.velocity.editor.util.formatMmSs

/** Video/Audio media browser. Filters, transitions and titles now live in the editor's Effects sheet for live preview. */
@Composable
fun LibraryScreen(
    initialTab: Int,
    onBack: () -> Unit,
    onExport: (Long) -> Unit,
    viewModel: LibraryViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableIntStateOf(initialTab.coerceIn(0, 1)) }
    val tabs = listOf(R.string.tab_video, R.string.tab_audio)

    Column(Modifier.fillMaxSize().background(VelocityColors.Background).statusBarsPadding().navigationBarsPadding()) {
        VelocityTopBar(
            leading = { BackButton(onBack) },
            trailing = { ExportPill { onExport(state.projectId) } },
        )
        TabRow(selectedTabIndex = tab, containerColor = Color.Transparent, contentColor = VelocityColors.Teal) {
            tabs.forEachIndexed { index, label ->
                Tab(
                    selected = tab == index, onClick = { tab = index },
                    text = { Text(stringResource(label), fontSize = 14.sp) },
                    selectedContentColor = VelocityColors.Teal,
                    unselectedContentColor = VelocityColors.TextSecondary,
                )
            }
        }
        when (tab) {
            0 -> VideoTab(state.videoClips, viewModel::addVideo, viewModel::deleteClips)
            else -> AudioTab(state.audioClips, state.addons[Addon.MUSIC_LIBRARY] != false, viewModel::addAudio, viewModel::deleteClips)
        }
    }
}

@Composable
private fun VideoTab(
    clips: List<ClipEntity>,
    onAdd: (List<Uri>) -> Unit,
    onDelete: (Collection<Long>) -> Unit,
) {
    var selected by remember { mutableStateOf(setOf<Long>()) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { if (it.isNotEmpty()) onAdd(it) }

    Column(Modifier.fillMaxSize()) {
        if (selected.isNotEmpty()) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(10.dp)).background(VelocityColors.SurfaceHigh)
                    .clickable { onDelete(selected); selected = emptySet() }.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.Delete, null, tint = Color.White)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.delete_selected, selected.size), fontSize = 14.sp)
            }
        }
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            contentPadding = PaddingValues(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(clips, key = { it.id }) { clip ->
                val isSelected = clip.id in selected
                Column(Modifier.clickable { selected = if (isSelected) selected - clip.id else selected + clip.id }) {
                    Box(
                        Modifier.fillMaxWidth().aspectRatio(1.2f).clip(RoundedCornerShape(10.dp))
                            .background(VelocityColors.SurfaceHigh)
                            .then(if (isSelected) Modifier.border(2.dp, VelocityColors.Teal, RoundedCornerShape(10.dp)) else Modifier),
                    ) {
                        AsyncImage(
                            model = Uri.parse(clip.uri), contentDescription = null,
                            contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize(),
                        )
                        Box(
                            Modifier.align(Alignment.TopEnd).padding(6.dp).size(20.dp).clip(CircleShape)
                                .background(if (isSelected) VelocityColors.Teal else Color.Black.copy(alpha = 0.45f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (isSelected) Icon(Icons.Outlined.Check, null, tint = Color.White, modifier = Modifier.size(14.dp))
                        }
                    }
                    Text(clip.name, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp))
                }
            }
            item {
                Box(
                    Modifier.fillMaxWidth().aspectRatio(1.2f).clip(RoundedCornerShape(10.dp))
                        .background(VelocityColors.Surface)
                        .border(1.dp, VelocityColors.Outline, RoundedCornerShape(10.dp))
                        .clickable { picker.launch(arrayOf("video/*")) },
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Outlined.Add, null, tint = VelocityColors.Teal)
                        Text(stringResource(R.string.add_media), fontSize = 12.sp, color = VelocityColors.Teal)
                    }
                }
            }
        }
    }
}

@Composable
private fun AudioTab(
    clips: List<ClipEntity>,
    unlocked: Boolean,
    onAdd: (List<Uri>) -> Unit,
    onDelete: (Collection<Long>) -> Unit,
) {
    val context = LocalContext.current
    val hint = stringResource(R.string.locked_hint)
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { if (it.isNotEmpty()) onAdd(it) }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                    .background(Brush.horizontalGradient(listOf(VelocityColors.Teal, VelocityColors.TealDark)))
                    .clickable { if (unlocked) picker.launch(arrayOf("audio/*")) else Toast.makeText(context, hint, Toast.LENGTH_SHORT).show() }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(if (unlocked) Icons.Outlined.Add else Icons.Outlined.Lock, null, tint = Color.White)
                Spacer(Modifier.width(10.dp))
                Text(stringResource(R.string.add_music), color = Color.White, fontWeight = FontWeight.SemiBold)
            }
        }
        if (clips.isEmpty()) {
            item { Text(stringResource(R.string.no_audio), color = VelocityColors.TextSecondary, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp)) }
        }
        items(clips, key = { it.id }) { clip ->
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(VelocityColors.Surface).padding(start = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.MusicNote, null, tint = VelocityColors.TrackMusic)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(clip.name, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 14.sp)
                    Text(formatMmSs(clip.durationMs), fontSize = 11.sp, color = VelocityColors.TextSecondary)
                }
                IconButton(onClick = { onDelete(listOf(clip.id)) }) { Icon(Icons.Outlined.Delete, null, tint = VelocityColors.TextSecondary) }
            }
        }
    }
}
