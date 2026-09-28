package com.velocity.editor.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects ORDER BY updatedAt DESC")
    fun observeProjects(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE id = :id")
    fun observeProject(id: Long): Flow<ProjectEntity?>

    @Query("SELECT * FROM projects WHERE id = :id")
    suspend fun getProject(id: Long): ProjectEntity?

    @Insert
    suspend fun insertProject(project: ProjectEntity): Long

    @Update
    suspend fun updateProject(project: ProjectEntity)

    @Query("DELETE FROM projects WHERE id = :id")
    suspend fun deleteProject(id: Long)

    @Query("SELECT * FROM clips WHERE projectId = :projectId ORDER BY startMs")
    fun observeClips(projectId: Long): Flow<List<ClipEntity>>

    @Query("SELECT * FROM clips WHERE projectId = :projectId ORDER BY startMs")
    suspend fun getClips(projectId: Long): List<ClipEntity>

    @Insert
    suspend fun insertClips(clips: List<ClipEntity>)

    @Update
    suspend fun updateClips(clips: List<ClipEntity>)

    @Query("DELETE FROM clips WHERE id IN (:ids)")
    suspend fun deleteClips(ids: List<Long>)

    @Query("DELETE FROM clips WHERE projectId = :projectId AND trackId = :trackId")
    suspend fun deleteTrackClips(projectId: Long, trackId: String)
}

@Database(entities = [ProjectEntity::class, ClipEntity::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao
}
