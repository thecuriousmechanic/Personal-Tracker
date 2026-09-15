package com.krishna.personalos.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackerDao {
    @Query("SELECT * FROM trackers ORDER BY createdAt, id") fun observeTrackers(): Flow<List<TrackerEntity>>
    @Query("SELECT * FROM tracker_fields ORDER BY position, id") fun observeFields(): Flow<List<TrackerFieldEntity>>
    @Query("SELECT * FROM tracker_entries ORDER BY createdAt, id") fun observeEntries(): Flow<List<TrackerEntryEntity>>
    @Query("SELECT * FROM tracker_cells") fun observeCells(): Flow<List<TrackerCellEntity>>
    @Query("SELECT * FROM trackers WHERE id = :id") suspend fun tracker(id: String): TrackerEntity?
    @Query("SELECT * FROM tracker_fields WHERE trackerId = :id ORDER BY position, id") suspend fun fields(id: String): List<TrackerFieldEntity>
    @Query("SELECT * FROM tracker_entries WHERE trackerId = :id ORDER BY createdAt, id") suspend fun entries(id: String): List<TrackerEntryEntity>
    @Query("SELECT * FROM tracker_entries WHERE id = :id") suspend fun entry(id: String): TrackerEntryEntity?
    @Query("SELECT * FROM tracker_cells WHERE entryId = :id") suspend fun cells(id: String): List<TrackerCellEntity>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun saveTracker(item: TrackerEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun saveFields(items: List<TrackerFieldEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun saveEntry(item: TrackerEntryEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun saveCells(items: List<TrackerCellEntity>)
    @Query("DELETE FROM tracker_cells WHERE entryId = :id") suspend fun deleteCells(id: String)
    @Query("DELETE FROM tracker_entries WHERE id = :id") suspend fun deleteEntry(id: String)
    @Query("DELETE FROM tracker_cells WHERE entryId IN (:ids)") suspend fun deleteCells(ids: List<String>)
    @Query("DELETE FROM tracker_entries WHERE id IN (:ids)") suspend fun deleteEntries(ids: List<String>)
    @Query("DELETE FROM tracker_cells WHERE entryId IN (SELECT id FROM tracker_entries WHERE trackerId = :id) OR fieldId IN (SELECT id FROM tracker_fields WHERE trackerId = :id)") suspend fun deleteCellsForTracker(id: String)
    @Query("DELETE FROM tracker_entries WHERE trackerId = :id") suspend fun deleteEntriesForTracker(id: String)
    @Query("DELETE FROM tracker_fields WHERE trackerId = :id") suspend fun deleteFieldsForTracker(id: String)
    @Query("DELETE FROM trackers WHERE id = :id") suspend fun deleteTracker(id: String)
}
