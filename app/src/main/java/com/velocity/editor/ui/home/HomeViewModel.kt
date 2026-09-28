package com.velocity.editor.ui.home

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.velocity.editor.data.ProjectEntity
import com.velocity.editor.data.ProjectRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(private val repo: ProjectRepository) : ViewModel() {
    val query = MutableStateFlow("")

    val allProjects: StateFlow<List<ProjectEntity>> = repo.projects()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val projects: StateFlow<List<ProjectEntity>> = combine(repo.projects(), query) { list, q ->
        if (q.isBlank()) list else list.filter { it.name.contains(q.trim(), ignoreCase = true) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun onQueryChange(value: String) { query.value = value }

    fun createProject(name: String, uris: List<Uri>, onCreated: (Long) -> Unit) {
        viewModelScope.launch { onCreated(repo.createProject(name, uris)) }
    }

    fun deleteProject(id: Long) {
        viewModelScope.launch { repo.deleteProject(id) }
    }
}
