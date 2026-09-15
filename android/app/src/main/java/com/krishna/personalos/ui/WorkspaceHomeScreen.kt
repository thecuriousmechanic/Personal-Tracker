package com.krishna.personalos.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.krishna.personalos.core.database.TaskStatus
import com.krishna.personalos.core.model.TrackerWorkspace
import com.krishna.personalos.core.model.WorkspaceSnapshot
import com.krishna.personalos.ui.theme.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
internal fun WorkspaceHomeScreen(
    workspace: WorkspaceSnapshot,
    trackerWorkspace: TrackerWorkspace,
    onToggleTask: (String) -> Unit,
    onToggleHabit: (String) -> Unit,
    onOpenTrackers: () -> Unit,
    onOpenLibrary: () -> Unit,
    onOpenPlan: () -> Unit,
    onOpenSettings: () -> Unit,
    insets: PaddingValues,
) {
    val today = LocalDate.now()
    val tasks = workspace.todayTasks(today.toEpochDay())
    val trackers = trackerWorkspace.trackers.filter { !it.isTemplate && !it.archived }
    LazyColumn(Modifier.fillMaxSize().padding(insets), contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)) {
        item {
            VectorHeader(today.format(DateTimeFormatter.ofPattern("EEEE, d MMMM")), "Today") {
                IconButton(onClick = onOpenSettings) { Icon(Icons.Rounded.Settings, "Open settings", tint = Muted) }
            }
            Spacer(Modifier.height(24.dp))
            Text("Your space, your pace.", style = MaterialTheme.typography.headlineLarge, color = Ice)
            Spacer(Modifier.height(8.dp))
            Text("Make room for the things you want to follow through on.", color = Muted)
            Spacer(Modifier.height(28.dp))
            WorkspaceLine("My trackers", if (trackers.isEmpty()) "Start a routine, reading list, or spending log" else "${trackers.size} personal trackers · open your workspace", onOpenTrackers)
            Spacer(Modifier.height(24.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Today", style = MaterialTheme.typography.titleLarge, color = Ice, modifier = Modifier.weight(1f))
                TextButton(onClick = onOpenPlan) { Text("Open plan") }
            }
            Text("${tasks.count { it.status == TaskStatus.DONE }} of ${tasks.size} complete", color = Muted, style = MaterialTheme.typography.bodySmall)
        }
        items(tasks, key = { it.id }) { task ->
            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Checkbox(task.status == TaskStatus.DONE, { onToggleTask(task.id) }, modifier = Modifier.semantics { contentDescription = task.title })
                Text(task.title, color = if (task.status == TaskStatus.DONE) Muted else Ice, modifier = Modifier.weight(1f))
            }
            HorizontalDivider(color = Divider.copy(alpha = .5f))
        }
        if (tasks.isEmpty()) item { Text("Nothing planned today. Leave room, or add something that matters.", color = Muted, modifier = Modifier.padding(vertical = 16.dp)) }
        item {
            Spacer(Modifier.height(24.dp))
            Text("Daily rhythm", style = MaterialTheme.typography.titleLarge, color = Ice)
            Spacer(Modifier.height(8.dp))
        }
        items(workspace.habits, key = { it.id }) { habit ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(workspace.isHabitDone(habit.id, today.toEpochDay()), { onToggleHabit(habit.id) }, modifier = Modifier.semantics { contentDescription = habit.title })
                Text(habit.title, color = Ice)
            }
        }
        item {
            Spacer(Modifier.height(24.dp))
            WorkspaceLine("Knowledge & projects", "${workspace.notes.size} notes · ${workspace.projects.size} projects", onOpenLibrary)
        }
    }
}
