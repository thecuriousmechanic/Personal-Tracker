package com.krishna.personalos.core.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "workspace_preferences")
data class WorkspacePreferenceEntity(@PrimaryKey val key: String, val value: String)

@Entity(tableName = "workspace_trash")
data class TrashEntity(
    @PrimaryKey val id: String,
    val title: String,
    val kind: String,
    val ownerId: String,
    val payload: String,
    val deletedAt: Long,
)

@Dao
interface RecoveryDao {
    @Query("SELECT * FROM workspace_trash ORDER BY deletedAt DESC, id") fun observeTrash(): Flow<List<TrashEntity>>
    @Query("SELECT * FROM workspace_trash WHERE id = :id") suspend fun trash(id: String): TrashEntity?
    @Query("SELECT * FROM workspace_trash ORDER BY deletedAt DESC LIMIT 1") suspend fun latest(): TrashEntity?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun saveTrash(item: TrashEntity)
    @Query("DELETE FROM workspace_trash WHERE id = :id") suspend fun removeTrash(id: String)
    @Query("SELECT * FROM workspace_preferences") fun observePreferences(): Flow<List<WorkspacePreferenceEntity>>
    @Query("SELECT * FROM workspace_preferences WHERE `key` = :key") suspend fun preference(key: String): WorkspacePreferenceEntity?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun savePreference(item: WorkspacePreferenceEntity)
}
