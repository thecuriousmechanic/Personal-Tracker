package com.krishna.personalos.core.model

import com.krishna.personalos.core.database.ActivityEventEntity
import com.krishna.personalos.core.database.GoalEntity
import com.krishna.personalos.core.database.HabitEntity
import com.krishna.personalos.core.database.HabitLogEntity
import com.krishna.personalos.core.database.NoteEntity
import com.krishna.personalos.core.database.ProjectEntity
import com.krishna.personalos.core.database.TaskEntity
import com.krishna.personalos.core.database.TaskStatus

data class WorkspaceSnapshot(
    val tasks: List<TaskEntity> = emptyList(),
    val habits: List<HabitEntity> = emptyList(),
    val habitLogs: List<HabitLogEntity> = emptyList(),
    val projects: List<ProjectEntity> = emptyList(),
    val goals: List<GoalEntity> = emptyList(),
    val notes: List<NoteEntity> = emptyList(),
    val relationCount: Int = 0,
    val activity: List<ActivityEventEntity> = emptyList(),
) {
    fun todayTasks(today: Long): List<TaskEntity> = tasks.filter {
        it.status == TaskStatus.TODAY || (it.dueEpochDay == today && it.status in setOf(TaskStatus.UPCOMING, TaskStatus.DONE))
    }

    fun todayProgress(today: Long): Int {
        val relevant = todayTasks(today)
        if (relevant.isEmpty()) return 0
        return relevant.count { it.status == TaskStatus.DONE } * 100 / relevant.size
    }

    fun isHabitDone(habitId: String, today: Long): Boolean =
        habitLogs.any { it.habitId == habitId && it.epochDay == today }
}
