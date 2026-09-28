package com.velocity.editor.data

import android.content.Context
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import com.velocity.editor.domain.Tracks
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProjectRepository @Inject constructor(
    private val dao: ProjectDao,
    @ApplicationContext private val context: Context,
) {
    fun projects(): Flow<List<ProjectEntity>> = dao.observeProjects()
    fun project(id: Long): Flow<ProjectEntity?> = dao.observeProject(id)
    fun clips(projectId: Long): Flow<List<ClipEntity>> = dao.observeClips(projectId)
    suspend fun getProject(id: Long): ProjectEntity? = dao.getProject(id)
    suspend fun getClips(projectId: Long): List<ClipEntity> = dao.getClips(projectId)

    suspend fun createProject(name: String, uris: List<Uri>): Long = withContext(Dispatchers.IO) {
        val id = dao.insertProject(ProjectEntity(name = name))
        appendClips(id, Tracks.V1, uris)
        id
    }

    suspend fun addClips(projectId: Long, trackId: String, uris: List<Uri>) =
        withContext(Dispatchers.IO) { appendClips(projectId, trackId, uris) }

    suspend fun deleteClips(projectId: Long, ids: List<Long>) = withContext(Dispatchers.IO) {
        dao.deleteClips(ids)
        refresh(projectId)
    }

    suspend fun deleteProject(id: Long) = withContext(Dispatchers.IO) { dao.deleteProject(id) }

    suspend fun setTransition(projectId: Long, transitionId: String?) = withContext(Dispatchers.IO) {
        dao.getProject(projectId)?.let { dao.updateProject(it.copy(transitionId = transitionId)) }
        refresh(projectId)
    }

    suspend fun setFilter(projectId: Long, filterId: String?) = withContext(Dispatchers.IO) {
        dao.getProject(projectId)?.let { dao.updateProject(it.copy(filterId = filterId)) }
        refresh(projectId)
    }

    suspend fun setTitle(projectId: Long, titleId: String?) = withContext(Dispatchers.IO) {
        dao.getProject(projectId)?.let { dao.updateProject(it.copy(titleId = titleId)) }
        refresh(projectId)
    }

    suspend fun toggleMute(projectId: Long, trackId: String) = withContext(Dispatchers.IO) {
        val project = dao.getProject(projectId) ?: return@withContext
        val muted = project.mutedTracks.split(",").filter { it.isNotBlank() }.toMutableSet()
        if (!muted.add(trackId)) muted.remove(trackId)
        dao.updateProject(project.copy(mutedTracks = muted.joinToString(",")))
    }

    /** Trims a clip in place. newTrimStartMs is the offset into the source file; newDurationMs is the visible length. */
    suspend fun trimClip(projectId: Long, clipId: Long, newTrimStartMs: Long, newDurationMs: Long) = withContext(Dispatchers.IO) {
        val clip = dao.getClips(projectId).firstOrNull { it.id == clipId } ?: return@withContext
        val cap = clip.sourceDurationMs.takeIf { it > 0 } ?: (newTrimStartMs + newDurationMs)
        val boundedStart = newTrimStartMs.coerceIn(0L, (cap - MIN_CLIP_MS).coerceAtLeast(0L))
        val boundedDuration = newDurationMs.coerceIn(MIN_CLIP_MS, (cap - boundedStart).coerceAtLeast(MIN_CLIP_MS))
        dao.updateClips(listOf(clip.copy(trimStartMs = boundedStart, durationMs = boundedDuration)))
        refresh(projectId)
    }

    private suspend fun appendClips(projectId: Long, trackId: String, uris: List<Uri>) {
        var cursor = dao.getClips(projectId)
            .filter { it.trackId == trackId }
            .maxOfOrNull { it.startMs + it.durationMs } ?: 0L
        val clips = uris.map { uri ->
            persist(uri)
            val (name, duration) = probe(uri)
            val clip = ClipEntity(
                projectId = projectId, trackId = trackId, uri = uri.toString(),
                name = name, startMs = cursor, durationMs = duration,
                trimStartMs = 0, sourceDurationMs = duration,
            )
            cursor += duration
            clip
        }
        dao.insertClips(clips)
        refresh(projectId)
    }

    /** Re-packs the video and music tracks sequentially, rebuilds transition markers, updates project metadata. */
    private suspend fun refresh(projectId: Long) {
        val project = dao.getProject(projectId) ?: return
        val all = dao.getClips(projectId)

        var v1Cursor = 0L
        val v1 = all.filter { it.trackId == Tracks.V1 }.sortedBy { it.startMs }.map { c ->
            c.copy(startMs = v1Cursor).also { v1Cursor += c.durationMs }
        }
        dao.updateClips(v1)

        var a2Cursor = 0L
        val a2 = all.filter { it.trackId == Tracks.A2 }.sortedBy { it.startMs }.map { c ->
            c.copy(startMs = a2Cursor).also { a2Cursor += c.durationMs }
        }
        dao.updateClips(a2)

        dao.deleteTrackClips(projectId, Tracks.V2)
        val tid = project.transitionId
        if (tid != null && v1.size > 1) {
            val markers = (1 until v1.size).map { i ->
                ClipEntity(
                    projectId = projectId, trackId = Tracks.V2, uri = "effect:$tid", name = tid,
                    startMs = (v1[i].startMs - TRANSITION_MS / 2).coerceAtLeast(0),
                    durationMs = TRANSITION_MS,
                )
            }
            dao.insertClips(markers)
        }

        val total = dao.getClips(projectId).maxOfOrNull { it.startMs + it.durationMs } ?: 0L
        dao.updateProject(
            (dao.getProject(projectId) ?: project).copy(
                durationMs = total,
                thumbnailUri = v1.firstOrNull()?.uri,
                updatedAt = System.currentTimeMillis(),
            ),
        )
    }

    private fun persist(uri: Uri) {
        runCatching {
            context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    private fun probe(uri: Uri): Pair<String, Long> {
        var name = uri.lastPathSegment ?: "clip"
        runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
                if (it.moveToFirst()) name = it.getString(0).substringBeforeLast('.')
            }
        }
        var duration = 5_000L
        val retriever = MediaMetadataRetriever()
        runCatching {
            retriever.setDataSource(context, uri)
            duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull() ?: duration
        }
        runCatching { retriever.release() }
        return name to duration
    }

    private companion object {
        const val TRANSITION_MS = 1_000L
        const val MIN_CLIP_MS = 500L
    }
}
