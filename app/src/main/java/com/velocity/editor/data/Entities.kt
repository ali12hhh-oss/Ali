package com.velocity.editor.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val thumbnailUri: String? = null,
    val updatedAt: Long = System.currentTimeMillis(),
    val durationMs: Long = 0,
    val mutedTracks: String = "",
    val transitionId: String? = null,
    val filterId: String? = null,
    val titleId: String? = null,
)

@Entity(
    tableName = "clips",
    foreignKeys = [ForeignKey(
        entity = ProjectEntity::class,
        parentColumns = ["id"],
        childColumns = ["projectId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("projectId")],
)
data class ClipEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val trackId: String,
    val uri: String,
    val name: String,
    val startMs: Long,
    val durationMs: Long,
    /** Offset into the source file where this clip starts playing (trim handle support). */
    val trimStartMs: Long = 0,
    /** Full duration of the untrimmed source media, used to bound trim handles. */
    val sourceDurationMs: Long = 0,
)
