package com.krishna.personalos.core.repository

import androidx.room.withTransaction
import com.krishna.personalos.core.database.ActivityEventEntity
import com.krishna.personalos.core.database.ActivityType
import com.krishna.personalos.core.database.AppDatabase
import com.krishna.personalos.core.database.GoalEntity
import com.krishna.personalos.core.database.HabitEntity
import com.krishna.personalos.core.database.HabitLogEntity
import com.krishna.personalos.core.database.NoteEntity
import com.krishna.personalos.core.database.Priority
import com.krishna.personalos.core.database.ProjectEntity
import com.krishna.personalos.core.database.ProjectStatus
import com.krishna.personalos.core.database.TaskEntity
import com.krishna.personalos.core.database.TaskStatus
import com.krishna.personalos.core.model.WorkspaceSnapshot
import com.krishna.personalos.core.model.toggleChecklistLine
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

enum class CaptureKind(val label: String) { TASK("Task"), NOTE("Note"), PROJECT("Project"), GOAL("Goal"), HABIT("Habit") }

class PersonalOSRepository(private val database: AppDatabase) {
    private val dao = database.appDao()
    private val recovery = RecoveryRepository(database)

    suspend fun saveTask(item: TaskEntity) = database.withTransaction {
        require(item.title.isNotBlank()) { "Add a title" }
        require(item.repeatDays in listOf(0, 1, 7)) { "Choose daily or weekly repeat" }
        require(item.reminderMinutes == null || item.reminderMinutes in 0..1439) { "Use a valid reminder time" }
        require(item.reminderMinutes == null || item.dueEpochDay != null) { "A reminder needs a due date" }
        require(item.projectId == null || dao.project(item.projectId) != null) { "Choose an existing project" }
        val previous = dao.task(item.id)
        val value = item.copy(title = item.title.trim(), completedAt = if (item.status == TaskStatus.DONE) item.completedAt ?: System.currentTimeMillis() else null)
        if (previous == null) dao.insertTask(value) else {
            require(previous.deletedAt == null) { "This task was deleted" }
            dao.updateTask(value)
        }
        if (value.status == TaskStatus.DONE && previous?.status != TaskStatus.DONE) createNext(value, LocalDate.now())
    }
    suspend fun saveNote(item: NoteEntity) {
        require(item.title.isNotBlank()) { "Add a title" }
        dao.insertNote(item.copy(title = item.title.trim(), updatedAt = System.currentTimeMillis()))
    }
    suspend fun saveProject(item: ProjectEntity) {
        require(item.title.isNotBlank()) { "Add a title" }
        dao.insertProject(item.copy(title = item.title.trim(), area = item.area.trim()))
    }
    suspend fun saveGoal(item: GoalEntity) {
        require(item.title.isNotBlank()) { "Add a title" }
        require(item.progress in 0..100) { "Progress must be between 0 and 100" }
        dao.insertGoal(item.copy(title = item.title.trim(), horizon = item.horizon.trim()))
    }
    suspend fun delete(kind: CaptureKind, id: String) = database.withTransaction {
        val table = when (kind) { CaptureKind.TASK -> "tasks"; CaptureKind.NOTE -> "notes"; CaptureKind.PROJECT -> "projects"; CaptureKind.GOAL -> "goals"; else -> error("Unsupported item") }
        val title = when (kind) { CaptureKind.TASK -> dao.task(id)?.title; CaptureKind.NOTE -> dao.note(id)?.title; CaptureKind.PROJECT -> dao.project(id)?.title; CaptureKind.GOAL -> dao.goal(id)?.title; CaptureKind.HABIT -> null }
        requireNotNull(title) { "Item no longer exists" }
        val queries = linkedMapOf<String, Pair<String, Array<out Any?>>>(table to ("id = ?" to arrayOf(id)), "entity_relations" to ("sourceId = ? OR targetId = ?" to arrayOf(id, id)), "activity_events" to ("entityId = ?" to arrayOf(id)))
        if (kind == CaptureKind.PROJECT) queries["tasks"] = "projectId = ?" to arrayOf(id)
        recovery.capture(kind.name.lowercase(), title, id, queries)
        when (kind) {
            CaptureKind.TASK -> dao.deleteTask(id, System.currentTimeMillis())
            CaptureKind.NOTE -> dao.deleteNote(id)
            CaptureKind.PROJECT -> { dao.unlinkProject(id); dao.deleteProject(id) }
            CaptureKind.GOAL -> dao.deleteGoal(id)
            CaptureKind.HABIT -> error("Unsupported item")
        }
        dao.removeRelations(id)
        dao.removeActivity(id)
    }

    val workspace: Flow<WorkspaceSnapshot> = combine(
        dao.observeTasks(),
        dao.observeProjects(),
        dao.observeGoals(),
        dao.observeNotes(),
    ) { tasks, projects, goals, notes ->
        WorkspaceSnapshot(
            tasks = tasks,
            projects = projects,
            goals = goals,
            notes = notes,
        )
    }

    suspend fun toggleNoteChecklist(noteId: String, lineIndex: Int) {
        val note = dao.note(noteId) ?: return
        val changed = note.body.toggleChecklistLine(lineIndex)
        if (changed != note.body) dao.updateNote(note.copy(body = changed, updatedAt = System.currentTimeMillis()))
    }

    /** Removes only the IDs shipped by the old demo seeder. User-created UUID records are untouched. */
    suspend fun removeBundledDemoData() = database.withTransaction {
        dao.removeBundledRelations()
        dao.removeBundledActivity()
        dao.removeBundledHabitLogs()
        dao.removeBundledHabits()
        dao.removeBundledTasks()
        dao.removeBundledNotes()
        dao.removeBundledGoals()
        dao.removeBundledProjects()
    }

    suspend fun toggleTask(id: String, today: LocalDate = LocalDate.now()) = database.withTransaction {
        val task = dao.task(id)?.takeIf { it.deletedAt == null } ?: return@withTransaction
        val now = System.currentTimeMillis()
        val completing = task.status != TaskStatus.DONE
        dao.updateTask(
            task.copy(
                status = if (completing) TaskStatus.DONE else TaskStatus.TODAY,
                dueEpochDay = task.dueEpochDay ?: today.toEpochDay(),
                completedAt = if (completing) now else null,
            ),
        )
        dao.insertActivity(
            ActivityEventEntity(
                UUID.randomUUID().toString(),
                if (completing) ActivityType.COMPLETED else ActivityType.REOPENED,
                task.id,
                task.title,
                now,
            ),
        )
        if (completing) createNext(task, today)
    }

    private suspend fun createNext(task: TaskEntity, today: LocalDate) {
        if (task.repeatDays == 0 || task.repeatPaused) return
        val nextId = UUID.nameUUIDFromBytes("repeat:${task.id}".toByteArray()).toString()
        if (dao.task(nextId) != null) return
        val nextDay = LocalDate.ofEpochDay(maxOf(task.dueEpochDay ?: today.toEpochDay(), today.toEpochDay())).plusDays(task.repeatDays.toLong()).toEpochDay()
        dao.insertTask(task.copy(id = nextId, status = TaskStatus.UPCOMING, dueEpochDay = nextDay, createdAt = System.currentTimeMillis(), completedAt = null, deletedAt = null))
    }

    suspend fun skipTask(id: String) = database.withTransaction {
        val task = requireNotNull(dao.task(id))
        require(task.deletedAt == null && task.status != TaskStatus.DONE && task.repeatDays > 0 && !task.repeatPaused) { "Only active recurring tasks can be skipped" }
        createNext(task, LocalDate.now())
        delete(CaptureKind.TASK, id)
    }

    suspend fun scheduleToday(id: String, today: LocalDate = LocalDate.now()) {
        val task = dao.task(id) ?: return
        val now = System.currentTimeMillis()
        dao.updateTask(task.copy(status = TaskStatus.TODAY, dueEpochDay = today.toEpochDay()))
        dao.insertActivity(ActivityEventEntity(UUID.randomUUID().toString(), ActivityType.SCHEDULED, id, task.title, now))
    }

    suspend fun toggleHabit(habitId: String, today: LocalDate = LocalDate.now()) {
        val day = today.toEpochDay()
        val existing = dao.habitLog(habitId, day)
        if (existing == null) {
            val now = System.currentTimeMillis()
            dao.insertHabitLog(HabitLogEntity(habitId, day, now))
            dao.insertActivity(ActivityEventEntity(UUID.randomUUID().toString(), ActivityType.CHECKED_IN, habitId, "Habit checked in", now))
        } else {
            dao.deleteHabitLog(existing)
        }
    }

    suspend fun capture(kind: CaptureKind, title: String, today: LocalDate = LocalDate.now()) {
        val cleanTitle = title.trim()
        if (cleanTitle.isBlank()) return
        val id = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        when (kind) {
            CaptureKind.TASK -> dao.insertTask(TaskEntity(id, cleanTitle, TaskStatus.INBOX, Priority.MEDIUM, null, null, now))
            CaptureKind.NOTE -> dao.insertNote(NoteEntity(id, cleanTitle, "", now, now))
            CaptureKind.PROJECT -> dao.insertProject(ProjectEntity(id, cleanTitle, "Personal", ProjectStatus.ACTIVE, today.plusDays(14).toEpochDay(), now))
            CaptureKind.GOAL -> dao.insertGoal(GoalEntity(id, cleanTitle, "This quarter", 0, now))
            CaptureKind.HABIT -> dao.insertHabit(HabitEntity(id, cleanTitle, 5, now))
        }
        dao.insertActivity(ActivityEventEntity(UUID.randomUUID().toString(), ActivityType.CREATED, id, cleanTitle, now))
    }
}
