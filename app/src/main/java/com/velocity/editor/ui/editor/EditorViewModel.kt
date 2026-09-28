package com.velocity.editor.ui.editor

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.velocity.editor.data.ClipEntity
import com.velocity.editor.data.ProjectRepository
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
)

@HiltViewModel
class EditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repo: ProjectRepository,
) : ViewModel() {

    private val projectId: Long = checkNotNull(savedStateHandle["projectId"])
    private val locked = MutableStateFlow(emptySet<String>())

    val state: StateFlow<EditorUiState> = combine(repo.project(projectId), repo.clips(projectId), locked) { project, clips, lockedSet ->
        val muted = project?.mutedTracks.orEmpty().split(",").filter { it.isNotBlank() }.toSet()
        EditorUiState(
            projectId = projectId,
            projectName = project?.name.orEmpty(),
            tracks = Tracks.order.map { id ->
                TrackUi(id, clips.filter { it.trackId == id }, id in muted, id in lockedSet)
            },
            totalMs = clips.maxOfOrNull { it.startMs + it.durationMs } ?: 0L,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EditorUiState(projectId = projectId))

    fun addVideo(uris: List<Uri>) {
        viewModelScope.launch { repo.addClips(projectId, Tracks.V1, uris) }
    }

    fun deleteClip(clipId: Long) {
        val track = state.value.tracks.firstOrNull { t -> t.clips.any { it.id == clipId } } ?: return
        // Transition markers are managed from the Effects screen; locked tracks are read-only.
        if (track.locked || track.id == Tracks.V2) return
        viewModelScope.launch { repo.deleteClips(projectId, listOf(clipId)) }
    }

    fun toggleMute(trackId: String) {
        viewModelScope.launch { repo.toggleMute(projectId, trackId) }
    }

    fun toggleLock(trackId: String) {
        locked.update { if (trackId in it) it - trackId else it + trackId }
    }
}
