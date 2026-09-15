package com.krishna.personalos.core.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "trackers")
data class TrackerEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val isTemplate: Boolean = false,
    val archived: Boolean = false,
    val createdAt: Long,
)

@Entity(tableName = "tracker_fields", indices = [Index("trackerId")])
data class TrackerFieldEntity(
    @PrimaryKey val id: String,
    val trackerId: String,
    val name: String,
    val kind: String,
    val options: String = "",
    val position: Int,
    val archived: Boolean = false,
)

@Entity(tableName = "tracker_entries", indices = [Index("trackerId")])
data class TrackerEntryEntity(
    @PrimaryKey val id: String,
    val trackerId: String,
    val title: String,
    val body: String = "",
    val createdAt: Long,
    val archived: Boolean = false,
)

@Entity(tableName = "tracker_cells", primaryKeys = ["entryId", "fieldId"], indices = [Index("fieldId")])
data class TrackerCellEntity(val entryId: String, val fieldId: String, val value: String)
