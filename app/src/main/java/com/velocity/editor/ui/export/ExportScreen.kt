package com.velocity.editor.ui.export

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.BrandingWatermark
import androidx.compose.material.icons.outlined.HighQuality
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.MovieFilter
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import com.velocity.editor.R
import com.velocity.editor.domain.Addon
import com.velocity.editor.domain.Quality
import com.velocity.editor.ui.components.BackButton
import com.velocity.editor.ui.components.ExportPill
import com.velocity.editor.ui.components.VelocityTopBar
import com.velocity.editor.ui.theme.VelocityColors

private class Option(val text: String, val enabled: Boolean = true, val onSelect: () -> Unit)

@OptIn(UnstableApi::class)
@Composable
fun ExportScreen(onBack: () -> Unit, viewModel: ExportViewModel = hiltViewModel()) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val allow4k = ui.addons[Addon.EXPORT_4K] != false

    val resolutions = listOf(
        Option("720p") { viewModel.setResolution(720) },
        Option("1080p") { viewModel.setResolution(1080) },
        Option("4K", enabled = allow4k) { viewModel.setResolution(2160) },
    )
    val fpsOptions = listOf(24, 30, 60).map { f -> Option(stringResource(R.string.fps_value, f)) { viewModel.setFps(f) } }
    val qualities = listOf(
        Triple(Quality.LOW, R.string.quality_low, 0),
        Triple(Quality.MEDIUM, R.string.quality_medium, 1),
        Triple(Quality.HIGH, R.string.quality_high, 2),
    )
    val qualityOptions = qualities.map { (q, label, _) -> Option(stringResource(label)) { viewModel.setQuality(q) } }
    val qualityLabel = stringResource(qualities.first { it.first == ui.quality }.second)

    Column(Modifier.fillMaxSize().background(VelocityColors.Background).statusBarsPadding().navigationBarsPadding()) {
        VelocityTopBar(
            leading = { BackButton(onBack) },
            trailing = { ExportPill { viewModel.startExport() } },
        )
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Column(Modifier.padding(horizontal = 16.dp).clip(RoundedCornerShape(14.dp))) {
                SettingRow(R.string.export_name, ui.projectName, emptyList(), shaded = false)
                SettingRow(R.string.export_resolution, if (ui.height >= 2160) "4K" else "${ui.height}p", resolutions, shaded = true)
                SettingRow(R.string.export_fps, stringResource(R.string.fps_value, ui.fps), fpsOptions, shaded = false)
                SettingRow(R.string.export_quality, qualityLabel, qualityOptions, shaded = true)
                SettingRow(R.string.export_format, "MP4", emptyList(), shaded = false)
                SettingRow(R.string.export_bitrate, stringResource(R.string.mbps_value, ui.bitrateMbps), emptyList(), shaded = true)
            }

            Spacer(Modifier.height(16.dp))
            Column(Modifier.padding(horizontal = 16.dp)) {
                when (val status = ui.status) {
                    is ExportStatus.Running -> {
                        Text(stringResource(R.string.exporting, (status.progress * 100).toInt()), fontSize = 14.sp)
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { status.progress },
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                            color = VelocityColors.Teal, trackColor = VelocityColors.Outline,
                        )
                        TextButton(onClick = viewModel::cancelExport) { Text(stringResource(R.string.cancel), color = VelocityColors.TextSecondary) }
                    }
                    else -> {
                        Button(
                            onClick = viewModel::startExport,
                            modifier = Modifier.fillMaxWidth().height(54.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = VelocityColors.Teal),
                        ) { Text(stringResource(R.string.start_export), fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White) }
                        when (status) {
                            is ExportStatus.Done -> Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(stringResource(R.string.export_done), Modifier.weight(1f), color = VelocityColors.Teal, fontSize = 13.sp)
                                TextButton(onClick = {
                                    runCatching {
                                        context.startActivity(
                                            Intent(Intent.ACTION_VIEW).setDataAndType(status.uri, "video/mp4")
                                                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION),
                                        )
                                    }
                                }) { Text(stringResource(R.string.open_video), color = VelocityColors.Teal) }
                            }
                            is ExportStatus.Failed -> Text(stringResource(R.string.export_failed, status.message), Modifier.padding(top = 8.dp), color = Color(0xFFFF6B6B), fontSize = 13.sp)
                            ExportStatus.NoMedia -> Text(stringResource(R.string.export_no_media), Modifier.padding(top = 8.dp), color = Color(0xFFFF6B6B), fontSize = 13.sp)
                            else -> Unit
                        }
                    }
                }
            }

            Text(
                stringResource(R.string.addons_title),
                Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 8.dp),
                fontWeight = FontWeight.SemiBold, fontSize = 16.sp,
            )
            Column(Modifier.padding(horizontal = 16.dp).clip(RoundedCornerShape(14.dp)).background(VelocityColors.Surface)) {
                AddonRow(Icons.Outlined.BrandingWatermark, R.string.addon_watermark, ui.addons[Addon.REMOVE_WATERMARK] != false) { viewModel.setAddon(Addon.REMOVE_WATERMARK, it) }
                AddonRow(Icons.Outlined.LibraryMusic, R.string.addon_music, ui.addons[Addon.MUSIC_LIBRARY] != false) { viewModel.setAddon(Addon.MUSIC_LIBRARY, it) }
                AddonRow(Icons.Outlined.MovieFilter, R.string.addon_transitions, ui.addons[Addon.CINEMATIC_TRANSITIONS] != false) { viewModel.setAddon(Addon.CINEMATIC_TRANSITIONS, it) }
                AddonRow(Icons.Outlined.AutoAwesome, R.string.addon_filters, ui.addons[Addon.ADVANCED_FILTERS] != false) { viewModel.setAddon(Addon.ADVANCED_FILTERS, it) }
                AddonRow(Icons.Outlined.HighQuality, R.string.addon_4k, allow4k) { viewModel.setAddon(Addon.EXPORT_4K, it) }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SettingRow(label: Int, value: String, options: List<Option>, shaded: Boolean) {
    var expanded by remember { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth()
            .background(if (shaded) VelocityColors.SurfaceHigh else VelocityColors.Surface)
            .then(if (options.isNotEmpty()) Modifier.clickable { expanded = true } else Modifier)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(stringResource(label), color = VelocityColors.TextSecondary, fontSize = 14.sp)
        Box {
            Text(value, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = if (options.isNotEmpty()) VelocityColors.Teal else Color.White)
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.text) },
                        enabled = option.enabled,
                        onClick = { expanded = false; option.onSelect() },
                    )
                }
            }
        }
    }
}

@Composable
private fun AddonRow(icon: ImageVector, label: Int, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = VelocityColors.TextSecondary, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(14.dp))
        Text(stringResource(label), Modifier.weight(1f), fontSize = 15.sp)
        Switch(
            checked = checked, onCheckedChange = onChange,
            colors = SwitchDefaults.colors(checkedTrackColor = VelocityColors.Teal, checkedThumbColor = Color.White),
        )
    }
}
