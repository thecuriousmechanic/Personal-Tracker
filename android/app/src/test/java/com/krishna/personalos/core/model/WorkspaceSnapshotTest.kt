package com.krishna.personalos.core.model

import com.krishna.personalos.core.database.Priority
import com.krishna.personalos.core.database.TaskEntity
import com.krishna.personalos.core.database.TaskStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class WorkspaceSnapshotTest {
    @Test
    fun todayProgressOnlyCountsTasksScheduledForToday() {
        val today = 20_000L
        val snapshot = WorkspaceSnapshot(
            tasks = listOf(
                task("active", TaskStatus.TODAY, today),
                task("complete", TaskStatus.DONE, today),
                task("old", TaskStatus.DONE, today - 1),
            ),
        )

        assertEquals(50, snapshot.todayProgress(today))
        assertEquals(listOf("active", "complete"), snapshot.todayTasks(today).map { it.id })
    }

    @Test
    fun emptyDayHasZeroProgress() {
        assertEquals(0, WorkspaceSnapshot().todayProgress(20_000L))
    }

    private fun task(id: String, status: TaskStatus, day: Long) = TaskEntity(
        id = id,
        title = id,
        status = status,
        priority = Priority.MEDIUM,
        dueEpochDay = day,
        projectId = null,
        createdAt = 1L,
        completedAt = if (status == TaskStatus.DONE) 2L else null,
    )
}
