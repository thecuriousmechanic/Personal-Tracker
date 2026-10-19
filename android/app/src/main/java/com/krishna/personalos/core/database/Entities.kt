package com.krishna.personalos.core.database

import androidx.room.Entity
import androidx.room.ColumnInfo
import androidx.room.Index
import androidx.room.PrimaryKey

enum class TaskStatus { INBOX, TODAY, UPCOMING, DONE }

enum class Priority { LOW, MEDIUM, HIGH }

enum class ProjectStatus { ACTIVE, PAUSED, COMPLETED }

enum class RelationType { SUPPORTS, BELONGS_TO, REFERENCES }

enum class ActivityType { CREATED, COMPLETED, REOPENED, SCHEDULED, CHECKED_IN }

@Entity(
    tableName = "tasks",
    indices = [Index("status"), Index("dueEpochDay"), Index("projectId")],
)
data class TaskEntity(
    @PrimaryKey val id: String,
    val title: String,
    val status: TaskStatus,
    val priority: Priority,
    val dueEpochDay: Long?,
    val projectId: String?,
    val createdAt: Long,
    val completedAt: Long? = null,
    val deletedAt: Long? = null,
    @ColumnInfo(defaultValue = "0") val repeatDays: Int = 0,
    @ColumnInfo(defaultValue = "0") val repeatPaused: Boolean = false,
    val reminderMinutes: Int? = null,
)

@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey val id: String,
    val title: String,
    val targetDaysPerWeek: Int,
    val createdAt: Long,
    val archived: Boolean = false,
)

@Entity(
    tableName = "habit_logs",
    primaryKeys = ["habitId", "epochDay"],
    indices = [Index("epochDay")],
)
data class HabitLogEntity(
    val habitId: String,
    val epochDay: Long,
    val completedAt: Long,
)

@Entity(tableName = "projects", indices = [Index("status"), Index("dueEpochDay")])
data class ProjectEntity(
    @PrimaryKey val id: String,
    val title: String,
    val area: String,
    val status: ProjectStatus,
    val dueEpochDay: Long?,
    val createdAt: Long,
)

@Entity(tableName = "goals")
data class GoalEntity(
    @PrimaryKey val id: String,
    val title: String,
    val horizon: String,
    val progress: Int,
    val createdAt: Long,
)

@Entity(tableName = "notes", indices = [Index("updatedAt")])
data class NoteEntity(
    @PrimaryKey val id: String,
    val title: String,
    val body: String,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(
    tableName = "entity_relations",
    indices = [Index("sourceId"), Index("targetId")],
)
data class EntityRelationEntity(
    @PrimaryKey val id: String,
    val sourceId: String,
    val targetId: String,
    val type: RelationType,
    val createdAt: Long,
)

@Entity(tableName = "activity_events", indices = [Index("occurredAt"), Index("entityId")])
data class ActivityEventEntity(
    @PrimaryKey val id: String,
    val type: ActivityType,
    val entityId: String,
    val title: String,
    val occurredAt: Long,
)
