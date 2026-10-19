package com.krishna.personalos.core.model

import com.krishna.personalos.core.database.*
import com.krishna.personalos.core.repository.CaptureKind

data class WorkspaceActions(
    val saveTask: suspend (TaskEntity) -> Unit,
    val saveNote: suspend (NoteEntity) -> Unit,
    val saveProject: suspend (ProjectEntity) -> Unit,
    val saveGoal: suspend (GoalEntity) -> Unit,
    val delete: suspend (CaptureKind, String) -> Unit,
    val toggleNoteChecklist: suspend (String, Int) -> Unit,
    val skipTask: suspend (String) -> Unit = { error("Skipping is unavailable") },
)
