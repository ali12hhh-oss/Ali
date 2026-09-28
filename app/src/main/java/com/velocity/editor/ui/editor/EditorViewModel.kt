package com.velocity.editor.ui.editor

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.velocity.editor.data.ClipEntity
import com.velocity.editor.data.ProjectRepository
import com.velocity.editor.data.SettingsRepository
import com.velocity.editor.domain.Addon
import com.velocity.editor.domain.Tracks
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TrackUi(
    val id: String,
    val clips: List<ClipEntity>,
    val muted: Boolean,
    val locked: Boolean,
)

data class EditorUiState(
    val projectId: Long = 0,
    val projectName: String = "",
    val tracks: List<TrackUi> = emptyList(),
    val totalMs: Long = 0,
    val filterId: String? = null,
    val titleId: String? = null,
    val transitionId: String? = null,
    val addons: Map<Addon, Boolean> = emptyMap(),
)

@HiltViewModel
class EditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repo: ProjectRepository,
    settings: SettingsRepository,
) : ViewModel() {

    private val projectId: Long = checkNotNull(savedStateHandle["projectId"])
    private val locked = MutableStateFlow(emptySet<String>())

    val state: StateFlow<EditorUiState> = combine(
        repo.project(projectId), repo.clips(projectId), locked, settings.addons,
    ) { project, clips, lockedSet, addons ->
        val muted = project?.mutedTracks.orEmpty().split(",").filter { it.isNotBlank() }.toSet()
        EditorUiState(
            projectId = projectId,
            projectName = project?.name.orEmpty(),
            tracks = Tracks.order.map { id ->
                TrackUi(id, clips.filter { it.trackId == id }.sortedBy { it.startMs }, id in muted, id in lockedSet)
            },
            totalMs = clips.maxOfOrNull { it.startMs + it.durationMs } ?: 0L,
            filterId = project?.filterId,
            titleId = project?.titleId,
            transitionId = project?.transitionId,
            addons = addons,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EditorUiState(projectId = projectId))

    fun addVideo(uris: List<Uri>) {
        viewModelScope.launch { repo.addClips(projectId, Tracks.V1, uris) }
    }

    fun deleteClip(clipId: Long) {
        val track = state.value.tracks.firstOrNull { t -> t.clips.any { it.id == clipId } } ?: return
        if (track.locked || track.id == Tracks.V2) return
        viewModelScope.launch { repo.deleteClips(projectId, listOf(clipId)) }
    }

    fun trimClip(clipId: Long, newTrimStartMs: Long, newDurationMs: Long) {
        viewModelScope.launch { repo.trimClip(projectId, clipId, newTrimStartMs, newDurationMs) }
    }

    fun toggleMute(trackId: String) {
        viewModelScope.launch { repo.toggleMute(projectId, trackId) }
    }

    fun toggleLock(trackId: String) {
        locked.update { if (trackId in it) it - trackId else it + trackId }
    }

    fun setFilter(id: String?) { viewModelScope.launch { repo.setFilter(projectId, id) } }
    fun setTitle(id: String?) { viewModelScope.launch { repo.setTitle(projectId, id) } }
    fun setTransition(id: String?) { viewModelScope.launch { repo.setTransition(projectId, id) } }
}
