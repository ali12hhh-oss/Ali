package com.velocity.editor.ui.export

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import com.velocity.editor.data.ProjectRepository
import com.velocity.editor.data.SettingsRepository
import com.velocity.editor.domain.Addon
import com.velocity.editor.domain.Quality
import com.velocity.editor.domain.Tracks
import com.velocity.editor.domain.estimateBitrateMbps
import com.velocity.editor.export.ExportEvent
import com.velocity.editor.export.ExportRequest
import com.velocity.editor.export.VideoExporter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface ExportStatus {
    data object Idle : ExportStatus
    data object NoMedia : ExportStatus
    data class Running(val progress: Float) : ExportStatus
    data class Done(val uri: Uri) : ExportStatus
    data class Failed(val message: String) : ExportStatus
}

data class ExportUiState(
    val projectName: String = "",
    val height: Int = 1080,
    val fps: Int = 30,
    val quality: Quality = Quality.HIGH,
    val addons: Map<Addon, Boolean> = emptyMap(),
    val status: ExportStatus = ExportStatus.Idle,
) {
    val bitrateMbps: Int get() = estimateBitrateMbps(height, fps, quality)
}

@OptIn(UnstableApi::class)
@HiltViewModel
class ExportViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repo: ProjectRepository,
    private val settings: SettingsRepository,
    private val exporter: VideoExporter,
) : ViewModel() {

    private val projectId: Long = checkNotNull(savedStateHandle["projectId"])
    private val local = MutableStateFlow(ExportUiState(height = 2160, fps = 60))
    private var job: Job? = null

    val ui: StateFlow<ExportUiState> = combine(local, repo.project(projectId), settings.addons) { l, project, addons ->
        val allow4k = addons[Addon.EXPORT_4K] != false
        l.copy(
            projectName = project?.name.orEmpty(),
            addons = addons,
            height = if (!allow4k && l.height >= 2160) 1080 else l.height,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ExportUiState())

    fun setResolution(height: Int) = local.update { it.copy(height = height) }
    fun setFps(fps: Int) = local.update { it.copy(fps = fps) }
    fun setQuality(quality: Quality) = local.update { it.copy(quality = quality) }
    fun setAddon(addon: Addon, enabled: Boolean) = settings.setAddon(addon, enabled)

    fun startExport() {
        if (local.value.status is ExportStatus.Running) return
        job = viewModelScope.launch {
            val project = repo.getProject(projectId) ?: return@launch
            val clips = repo.getClips(projectId)
            val video = clips.filter { it.trackId == Tracks.V1 }.sortedBy { it.startMs }
            if (video.isEmpty()) {
                local.update { it.copy(status = ExportStatus.NoMedia) }
                return@launch
            }
            val current = ui.value
            val muted = project.mutedTracks.split(",").filter { it.isNotBlank() }.toSet()
            val request = ExportRequest(
                projectName = project.name,
                videoClips = video,
                musicClips = clips.filter { it.trackId == Tracks.A2 }.sortedBy { it.startMs },
                muteVideoAudio = Tracks.V1 in muted,
                muteMusic = Tracks.A2 in muted,
                height = current.height,
                fps = current.fps,
                bitrateMbps = current.bitrateMbps,
                filterId = project.filterId,
                titleId = project.titleId,
                titleText = project.titleText,
                watermark = current.addons[Addon.REMOVE_WATERMARK] == false,
            )
            local.update { it.copy(status = ExportStatus.Running(0f)) }
            exporter.export(request).collect { event ->
                local.update {
                    it.copy(
                        status = when (event) {
                            is ExportEvent.Progress -> ExportStatus.Running(event.fraction.coerceIn(0f, 1f))
                            is ExportEvent.Completed -> ExportStatus.Done(event.uri)
                            is ExportEvent.Failure -> ExportStatus.Failed(event.message)
                        },
                    )
                }
            }
        }
    }

    fun cancelExport() {
        job?.cancel()
        local.update { it.copy(status = ExportStatus.Idle) }
    }
}
