package com.krishna.personalos.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import android.Manifest
import android.os.Build
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import com.krishna.personalos.core.database.*
import com.krishna.personalos.core.model.*
import com.krishna.personalos.core.repository.CaptureKind
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

@Composable
internal fun ConfirmDelete(title: String, detail: String, busy: Boolean = false, onDismiss: () -> Unit, onConfirm: () -> Unit, recoverable: Boolean = false) {
    AlertDialog(onDismissRequest = { if (!busy) onDismiss() }, title = { Text("Delete $title?") }, text = { Text(detail) },
        confirmButton = { TextButton(onClick = onConfirm, enabled = !busy, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text(if (recoverable) "Move to Trash" else "Delete permanently") } },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !busy) { Text("Keep it") } })
}

@Composable
internal fun WorkspaceEntityEditor(kind: CaptureKind, id: String?, workspace: WorkspaceSnapshot, actions: WorkspaceActions, onDismiss: () -> Unit) {
    if (id != null && when (kind) { CaptureKind.TASK -> workspace.tasks.none { it.id == id }; CaptureKind.NOTE -> workspace.notes.none { it.id == id }; CaptureKind.PROJECT -> workspace.projects.none { it.id == id }; CaptureKind.GOAL -> workspace.goals.none { it.id == id }; else -> true }) {
        Editor("Open ${kind.label.lowercase()}", false, null, onDismiss) { Text("This item is loading or is no longer available. You can restore deleted items in Settings → Trash.") }
        return
    }
    val task = remember(id) { workspace.tasks.find { it.id == id } }
    val note = remember(id) { workspace.notes.find { it.id == id } }
    val project = remember(id) { workspace.projects.find { it.id == id } }
    val goal = remember(id) { workspace.goals.find { it.id == id } }
    val initialTitle = task?.title ?: note?.title ?: project?.title ?: goal?.title ?: ""
    val draftId = rememberSaveable(id) { id ?: UUID.randomUUID().toString() }
    var title by rememberSaveable(id) { mutableStateOf(initialTitle) }
    var body by rememberSaveable(id) { mutableStateOf(note?.body ?: "") }
    var area by rememberSaveable(id) { mutableStateOf(project?.area ?: "Personal") }
    var horizon by rememberSaveable(id) { mutableStateOf(goal?.horizon ?: "This quarter") }
    var progress by rememberSaveable(id) { mutableStateOf((goal?.progress ?: 0).toString()) }
    var status by rememberSaveable(id) { mutableStateOf(task?.status?.name ?: project?.status?.name ?: if (kind == CaptureKind.TASK) TaskStatus.TODAY.name else ProjectStatus.ACTIVE.name) }
    var priority by rememberSaveable(id) { mutableStateOf(task?.priority?.name ?: Priority.MEDIUM.name) }
    var projectId by rememberSaveable(id) { mutableStateOf(task?.projectId) }
    var repeatDays by rememberSaveable(id) { mutableIntStateOf(task?.repeatDays ?: 0) }
    var repeatPaused by rememberSaveable(id) { mutableStateOf(task?.repeatPaused ?: false) }
    var reminderEnabled by rememberSaveable(id) { mutableStateOf(task?.reminderMinutes != null) }
    var reminderTime by rememberSaveable(id) { mutableStateOf(task?.reminderMinutes?.let { LocalTime.of(it / 60, it % 60).toString() } ?: "09:00") }
    var date by rememberSaveable(id) { mutableStateOf((task?.dueEpochDay ?: project?.dueEpochDay)?.let { LocalDate.ofEpochDay(it).toString() } ?: if (id == null && kind == CaptureKind.TASK) LocalDate.now().toString() else "") }
    var dirty by rememberSaveable(id) { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }
    var discard by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var skipping by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted -> reminderEnabled = granted; dirty = true; if (!granted) error = "Notifications are off. You can enable them in Settings." }
    val scope = rememberCoroutineScope()
    val run: (suspend () -> Unit) -> Unit = { operation -> if (!busy) scope.launch {
        busy = true; error = null
        try { operation(); onDismiss() } catch (e: CancellationException) { throw e } catch (e: Exception) { error = e.message ?: "Could not save. Try again."; deleting = false } finally { busy = false }
    } }
    val save = {
        run {
            val now = System.currentTimeMillis()
            val due = if (date.isBlank()) null else runCatching { LocalDate.parse(date).toEpochDay() }.getOrElse { error("Use YYYY-MM-DD for the date") }
            when (kind) {
                CaptureKind.TASK -> {
                    val time = if (reminderEnabled) runCatching { LocalTime.parse(reminderTime) }.getOrElse { error("Use HH:mm for the reminder time") } else null
                    actions.saveTask(TaskEntity(draftId, title, TaskStatus.valueOf(status), Priority.valueOf(priority), due, projectId, task?.createdAt ?: now, task?.completedAt,
                        repeatDays = repeatDays, repeatPaused = repeatDays > 0 && repeatPaused, reminderMinutes = time?.let { it.hour * 60 + it.minute }))
                }
                CaptureKind.NOTE -> actions.saveNote(NoteEntity(draftId, title, body, note?.createdAt ?: now, now))
                CaptureKind.PROJECT -> actions.saveProject(ProjectEntity(draftId, title, area, ProjectStatus.valueOf(status), due, project?.createdAt ?: now))
                CaptureKind.GOAL -> actions.saveGoal(GoalEntity(draftId, title, horizon, progress.toIntOrNull() ?: error("Progress must be a whole number from 0 to 100"), goal?.createdAt ?: now))
                else -> error("Unsupported item")
            }
        }
    }
    Editor("${if (id == null) "New" else "Edit"} ${kind.label.lowercase()}", busy, error, { if (dirty) discard = true else onDismiss() }, footer = {
        HorizontalDivider()
        Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (id != null) TextButton(onClick = { deleting = true }, enabled = !busy, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text("Delete ${kind.label.lowercase()}") }
            Button(onClick = save, enabled = !busy && title.isNotBlank(), modifier = Modifier.weight(1f)) { Text(if (busy) "Saving…" else if (error != null) "Retry save" else "Save ${kind.label.lowercase()}") }
        }
    }) {
        OutlinedTextField(title, { title = it; dirty = true }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth())
        if (kind == CaptureKind.NOTE) NoteBlockEditor(body) { body = it; dirty = true }
        if (kind == CaptureKind.PROJECT) OutlinedTextField(area, { area = it; dirty = true }, label = { Text("Area") }, modifier = Modifier.fillMaxWidth())
        if (kind == CaptureKind.TASK || kind == CaptureKind.PROJECT) {
            Text("Status")
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                (if (kind == CaptureKind.TASK) TaskStatus.entries.map { it.name } else ProjectStatus.entries.map { it.name }).forEach { value ->
                    FilterChip(status == value, { status = value; dirty = true }, label = { Text(value.lowercase().replaceFirstChar(Char::uppercase)) })
                }
            }
            OutlinedTextField(date, { date = it; dirty = true }, label = { Text("Due date") }, supportingText = { Text("YYYY-MM-DD · leave blank for no date") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        }
        if (kind == CaptureKind.TASK) {
            Text("Repeat", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(0 to "Once", 1 to "Daily", 7 to "Weekly").forEach { (days, label) -> FilterChip(repeatDays == days, { repeatDays = days; if (days == 0) repeatPaused = false; dirty = true }, label = { Text(label) }) }
            }
            if (repeatDays > 0) {
                Text("Completing or skipping this task creates its next occurrence. Overdue repeats start from today.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(verticalAlignment = Alignment.CenterVertically) { Text("Pause repeat and reminders", modifier = Modifier.weight(1f)); Switch(repeatPaused, { repeatPaused = it; dirty = true }, modifier = Modifier.semantics { contentDescription = "Pause repeat and reminders" }) }
                if (id != null && task?.status != TaskStatus.DONE) TextButton(onClick = { skipping = true }, enabled = !busy && !dirty && !repeatPaused) { Text("Skip this occurrence") }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Remind me", modifier = Modifier.weight(1f))
                Switch(reminderEnabled, { enabled ->
                    dirty = true
                    if (enabled && Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) permission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    else reminderEnabled = enabled
                }, modifier = Modifier.semantics { contentDescription = "Remind me" })
            }
            if (reminderEnabled) OutlinedTextField(reminderTime, { reminderTime = it; dirty = true }, label = { Text("Reminder time") }, supportingText = { Text("HH:mm on the due date · Android may delay delivery to save battery") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Text("Priority")
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { Priority.entries.forEach { value -> FilterChip(priority == value.name, { priority = value.name; dirty = true }, label = { Text(value.name.lowercase().replaceFirstChar(Char::uppercase)) }) } }
            Text("Project")
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(projectId == null, { projectId = null; dirty = true }, label = { Text("No project") })
                workspace.projects.forEach { item -> FilterChip(projectId == item.id, { projectId = item.id; dirty = true }, label = { Text(item.title) }) }
            }
        }
        if (kind == CaptureKind.GOAL) {
            OutlinedTextField(horizon, { horizon = it; dirty = true }, label = { Text("Timeframe") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(progress, { progress = it; dirty = true }, label = { Text("Progress (%)") }, supportingText = { Text("0 to 100") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        }
    }
    if (deleting) ConfirmDelete(kind.label.lowercase(), "Move “$title” to Trash? You can restore it in Settings." + if (kind == CaptureKind.PROJECT) " Its tasks will be kept without a project." else "", busy, { deleting = false }, { run { actions.delete(kind, draftId) } }, recoverable = true)
    if (skipping) AlertDialog(onDismissRequest = { if (!busy) skipping = false }, title = { Text("Skip this occurrence?") }, text = { Text("The next occurrence will be scheduled. This task will move to Trash.") }, confirmButton = { TextButton(onClick = { run { actions.skipTask(draftId) } }, enabled = !busy) { Text("Skip") } }, dismissButton = { TextButton(onClick = { skipping = false }, enabled = !busy) { Text("Cancel") } })
    if (discard) AlertDialog(onDismissRequest = { discard = false }, title = { Text("Discard unsaved changes?") }, confirmButton = { TextButton(onClick = onDismiss) { Text("Discard") } }, dismissButton = { TextButton(onClick = { discard = false }) { Text("Keep editing") } })
}

@Composable
internal fun NoteBlockEditor(body: String, onBody: (String) -> Unit) {
    val checklist = remember(body) { body.checklistItems().associateBy { it.lineIndex } }
    val drag = remember { WorkspaceDragState() }
    Text("NOTE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        body.lines().forEachIndexed { index, line ->
            val item = checklist[index]
            Row(
                Modifier.fillMaxWidth().workspaceDropTarget(drag, index.toString(), "note") { source, after -> onBody(body.moveNoteLine(source.toInt(), index, after)) }.padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                WorkspaceDragHandle(index.toString(), "note", "note line ${index + 1}", state = drag)
                if (item != null) {
                    Checkbox(
                        checked = item.checked,
                        onCheckedChange = { onBody(body.toggleChecklistLine(index)) },
                        modifier = Modifier.size(42.dp),
                    )
                    BasicTextField(
                        value = item.label,
                        onValueChange = { onBody(body.renameChecklistLine(index, it.replace("\n", " "))) },
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onBackground),
                        modifier = Modifier.weight(1f).padding(vertical = 10.dp).semantics { contentDescription = "Checklist item ${index + 1}" },
                        decorationBox = { field -> Box { if (item.label.isEmpty()) Text("Checklist item", color = MaterialTheme.colorScheme.onSurfaceVariant); field() } },
                    )
                } else {
                    BasicTextField(
                        value = line,
                        onValueChange = { onBody(body.replaceNoteLine(index, it)) },
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onBackground),
                        modifier = Modifier.weight(1f).heightIn(min = 42.dp).padding(vertical = 10.dp).semantics { contentDescription = "Note text ${index + 1}" },
                        decorationBox = { field -> Box { if (line.isEmpty()) Text("Type text…", color = MaterialTheme.colorScheme.onSurfaceVariant); field() } },
                    )
                }
                IconButton(onClick = { onBody(body.removeNoteLine(index)) }, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Rounded.Close, contentDescription = "Remove line", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        TextButton(onClick = { onBody(body.appendNoteLine("")) }) { Icon(Icons.Rounded.Add, null, Modifier.size(18.dp)); Text("Text") }
        TextButton(onClick = { onBody(body.appendNoteLine("- [ ] ")) }) { Icon(Icons.Rounded.CheckBox, null, Modifier.size(18.dp)); Text("Checklist") }
    }
    if (checklist.isNotEmpty()) Row {
        TextButton(onClick = { onBody(body.setChecklistCompletion(true)) }) { Text("Complete all") }
        TextButton(onClick = { onBody(body.setChecklistCompletion(false)) }) { Text("Reset checks") }
    }
    Text("Checklist items use Markdown and stay compatible with - [ ] and - [x].", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
internal fun EntryChecklistEditor(label: String, value: String, onValueChange: (String) -> Unit) {
    val items = remember(value) { value.checklistItems() }
    val drag = remember(label) { WorkspaceDragState() }
    Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    if (items.isEmpty()) Text("No checklist items yet.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    items.forEachIndexed { position, item ->
        Row(Modifier.fillMaxWidth().workspaceDropTarget(drag, item.lineIndex.toString(), "checklist") { source, after -> onValueChange(value.moveNoteLine(source.toInt(), item.lineIndex, after)) }, verticalAlignment = Alignment.CenterVertically) {
            WorkspaceDragHandle(item.lineIndex.toString(), "checklist", "$label item ${position + 1}", state = drag)
            Checkbox(
                checked = item.checked,
                onCheckedChange = { onValueChange(value.toggleChecklistLine(item.lineIndex)) },
                modifier = Modifier.semantics { contentDescription = "$label item ${position + 1}" },
            )
            OutlinedTextField(
                value = item.label,
                onValueChange = { onValueChange(value.renameChecklistLine(item.lineIndex, it.replace("\n", " "))) },
                placeholder = { Text("Checklist item") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = { onValueChange(value.removeNoteLine(item.lineIndex)) }) {
                Icon(Icons.Rounded.Close, contentDescription = "Remove ${item.label.ifBlank { "checklist item" }}")
            }
        }
    }
    TextButton(onClick = { onValueChange(value.appendNoteLine("- [ ] ")) }) {
        Icon(Icons.Rounded.Add, null, Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text("Add checklist item")
    }
    if (items.isNotEmpty()) Row {
        TextButton(onClick = { onValueChange(value.setChecklistCompletion(true)) }) { Text("Complete all") }
        TextButton(onClick = { onValueChange(value.setChecklistCompletion(false)) }) { Text("Reset checks") }
    }
}
