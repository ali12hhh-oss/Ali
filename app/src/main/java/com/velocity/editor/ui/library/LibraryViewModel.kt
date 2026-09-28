package com.velocity.editor.ui.library

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.velocity.editor.data.ClipEntity
import com.velocity.editor.data.ProjectEntity
import com.velocity.editor.data.ProjectRepository
import com.velocity.editor.data.SettingsRepository
import com.velocity.editor.domain.Addon
import com.velocity.editor.domain.Tracks
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LibraryUiState(
    val projectId: Long = 0,
    val project: ProjectEntity? = null,
    val videoClips: List<ClipEntity> = emptyList(),
    val audioClips: List<ClipEntity> = emptyList(),
    val addons: Map<Addon, Boolean> = emptyMap(),
)

@HiltViewModel
class LibraryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repo: ProjectRepository,
    settings: SettingsRepository,
) : ViewModel() {

    private val projectId: Long = checkNotNull(savedStateHandle["projectId"])

    val state: StateFlow<LibraryUiState> = combine(repo.project(projectId), repo.clips(projectId), settings.addons) { project, clips, addons ->
        LibraryUiState(
            projectId = projectId,
            project = project,
            videoClips = clips.filter { it.trackId == Tracks.V1 },
            audioClips = clips.filter { it.trackId == Tracks.A2 },
            addons = addons,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LibraryUiState(projectId = projectId))

    fun addVideo(uris: List<Uri>) { viewModelScope.launch { repo.addClips(projectId, Tracks.V1, uris) } }
    fun addAudio(uris: List<Uri>) { viewModelScope.launch { repo.addClips(projectId, Tracks.A2, uris) } }
    fun deleteClips(ids: Collection<Long>) { viewModelScope.launch { repo.deleteClips(projectId, ids.toList()) } }
    fun setTransition(id: String?) { viewModelScope.launch { repo.setTransition(projectId, id) } }
    fun setFilter(id: String?) { viewModelScope.launch { repo.setFilter(projectId, id) } }
    fun setTitle(id: String?) { viewModelScope.launch { repo.setTitle(projectId, id) } }
}
