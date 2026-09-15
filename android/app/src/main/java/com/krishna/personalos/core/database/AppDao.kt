package com.krishna.personalos.core.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    @Query("SELECT * FROM tasks WHERE deletedAt IS NULL ORDER BY CASE priority WHEN 'HIGH' THEN 0 WHEN 'MEDIUM' THEN 1 ELSE 2 END, createdAt")
    fun observeTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM habits WHERE archived = 0 ORDER BY createdAt")
    fun observeHabits(): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habit_logs ORDER BY epochDay DESC")
    fun observeHabitLogs(): Flow<List<HabitLogEntity>>

    @Query("SELECT * FROM projects ORDER BY CASE status WHEN 'ACTIVE' THEN 0 WHEN 'PAUSED' THEN 1 ELSE 2 END, createdAt")
    fun observeProjects(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM goals ORDER BY createdAt")
    fun observeGoals(): Flow<List<GoalEntity>>

    @Query("SELECT * FROM notes ORDER BY updatedAt DESC")
    fun observeNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM entity_relations ORDER BY createdAt DESC")
    fun observeRelations(): Flow<List<EntityRelationEntity>>

    @Query("SELECT * FROM activity_events ORDER BY occurredAt DESC LIMIT 20")
    fun observeActivity(): Flow<List<ActivityEventEntity>>

    @Query("SELECT * FROM tasks WHERE id = :id LIMIT 1")
    suspend fun task(id: String): TaskEntity?

    @Query("SELECT * FROM habit_logs WHERE habitId = :habitId AND epochDay = :epochDay LIMIT 1")
    suspend fun habitLog(habitId: String, epochDay: Long): HabitLogEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTasks(items: List<TaskEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(item: TaskEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabits(items: List<HabitEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabit(item: HabitEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabitLog(item: HabitLogEntity)

    @Delete
    suspend fun deleteHabitLog(item: HabitLogEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProjects(items: List<ProjectEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(item: ProjectEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoals(items: List<GoalEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(item: GoalEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotes(items: List<NoteEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(item: NoteEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRelations(items: List<EntityRelationEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivity(item: ActivityEventEntity)

    @Update
    suspend fun updateTask(item: TaskEntity)

    @Query("SELECT * FROM notes WHERE id = :id") suspend fun note(id: String): NoteEntity?
    @Query("SELECT * FROM projects WHERE id = :id") suspend fun project(id: String): ProjectEntity?
    @Query("SELECT * FROM goals WHERE id = :id") suspend fun goal(id: String): GoalEntity?
    @Update suspend fun updateNote(item: NoteEntity)
    @Update suspend fun updateProject(item: ProjectEntity)
    @Update suspend fun updateGoal(item: GoalEntity)
    @Query("DELETE FROM notes WHERE id = :id") suspend fun deleteNote(id: String)
    @Query("DELETE FROM projects WHERE id = :id") suspend fun deleteProject(id: String)
    @Query("DELETE FROM goals WHERE id = :id") suspend fun deleteGoal(id: String)
    @Query("UPDATE tasks SET deletedAt = :at WHERE id = :id") suspend fun deleteTask(id: String, at: Long)
    @Query("UPDATE tasks SET projectId = NULL WHERE projectId = :id") suspend fun unlinkProject(id: String)
    @Query("DELETE FROM entity_relations WHERE sourceId = :id OR targetId = :id") suspend fun removeRelations(id: String)
    @Query("DELETE FROM activity_events WHERE entityId = :id") suspend fun removeActivity(id: String)

    @Query("DELETE FROM habit_logs WHERE habitId IN ('habit-plan', 'habit-move', 'habit-read')") suspend fun removeBundledHabitLogs()
    @Query("DELETE FROM habits WHERE id IN ('habit-plan', 'habit-move', 'habit-read')") suspend fun removeBundledHabits()
    @Query("DELETE FROM tasks WHERE id IN ('task-focus', 'task-review', 'task-reading', 'task-done', 'task-upcoming')") suspend fun removeBundledTasks()
    @Query("DELETE FROM notes WHERE id IN ('note-principles', 'note-training')") suspend fun removeBundledNotes()
    @Query("DELETE FROM goals WHERE id IN ('goal-focus', 'goal-strength')") suspend fun removeBundledGoals()
    @Query("DELETE FROM projects WHERE id IN ('project-launch', 'project-health')") suspend fun removeBundledProjects()
    @Query("DELETE FROM entity_relations WHERE id IN ('relation-1', 'relation-2', 'relation-3') OR sourceId IN ('task-focus', 'task-review', 'task-reading', 'task-done', 'task-upcoming', 'note-principles', 'note-training', 'goal-focus', 'goal-strength', 'project-launch', 'project-health', 'habit-plan', 'habit-move', 'habit-read') OR targetId IN ('task-focus', 'task-review', 'task-reading', 'task-done', 'task-upcoming', 'note-principles', 'note-training', 'goal-focus', 'goal-strength', 'project-launch', 'project-health', 'habit-plan', 'habit-move', 'habit-read')") suspend fun removeBundledRelations()
    @Query("DELETE FROM activity_events WHERE entityId IN ('task-focus', 'task-review', 'task-reading', 'task-done', 'task-upcoming', 'note-principles', 'note-training', 'goal-focus', 'goal-strength', 'project-launch', 'project-health', 'habit-plan', 'habit-move', 'habit-read')") suspend fun removeBundledActivity()
}
