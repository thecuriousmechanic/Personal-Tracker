package com.krishna.personalos.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.krishna.personalos.core.database.TaskStatus
import com.krishna.personalos.core.model.WorkspaceSnapshot
import com.krishna.personalos.core.model.checklistItems
import com.krishna.personalos.core.repository.CaptureKind
import com.krishna.personalos.ui.theme.*
import java.time.LocalDate
import kotlinx.coroutines.launch

@Composable
internal fun WorkspaceCollectionScreen(workspace: WorkspaceSnapshot, plan: Boolean, libraryKind: String, onKind: (String) -> Unit, insets: PaddingValues, onEdit: (CaptureKind, String?) -> Unit, onToggleTask: (String) -> Unit, onToggleNoteChecklist: suspend (String, Int) -> Unit) {
    var filter by rememberSaveable { mutableStateOf("All") }
    var query by rememberSaveable(plan, libraryKind) { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val kind = if (plan) CaptureKind.TASK else CaptureKind.valueOf(libraryKind)
    Column(Modifier.fillMaxSize().padding(insets).padding(horizontal = 24.dp)) {
        VectorHeader(if (plan) "Make room for what matters" else "Everything worth keeping", if (plan) "Plan" else "Library", Modifier.padding(top = 12.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Tap an item to edit or delete", color = Muted, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
            TextButton(onClick = { onEdit(kind, null) }) { Icon(Icons.Rounded.Add, null, Modifier.size(16.dp)); Text("New ${kind.label.lowercase()}") }
        }
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (plan) listOf("All", "Today", "Inbox", "Upcoming", "Done").forEach { label -> FilterChip(filter == label, { filter = label }, label = { Text(label) }) }
            else listOf(CaptureKind.NOTE, CaptureKind.PROJECT, CaptureKind.GOAL).forEach { value -> FilterChip(kind == value, { onKind(value.name) }, label = { Text("${value.label}s") }) }
        }
        TextField(query, { query = it }, placeholder = { Text("Search ${kind.label.lowercase()}s") }, singleLine = true, leadingIcon = { Icon(Icons.Rounded.Search, null) }, modifier = Modifier.fillMaxWidth(), colors = TextFieldDefaults.colors(unfocusedContainerColor = androidx.compose.ui.graphics.Color.Transparent, focusedContainerColor = androidx.compose.ui.graphics.Color.Transparent))
        val today = LocalDate.now().toEpochDay()
        val rows = when (kind) {
            CaptureKind.TASK -> workspace.tasks.filter { task -> when (filter) { "Today" -> task in workspace.todayTasks(today); "Inbox" -> task.status == TaskStatus.INBOX; "Upcoming" -> task.status == TaskStatus.UPCOMING; "Done" -> task.status == TaskStatus.DONE; else -> true } }.map { Triple(it.id, it.title, "${it.status.name.lowercase()} · ${it.priority.name.lowercase()} priority" + (it.dueEpochDay?.let { d -> " · ${LocalDate.ofEpochDay(d)}" } ?: "")) }
            CaptureKind.NOTE -> workspace.notes.map {
                val checks = it.body.checklistItems()
                Triple(it.id, it.title, if (checks.isEmpty()) it.body.ifBlank { "Add a note" } else "${checks.count { item -> item.checked }}/${checks.size} complete")
            }
            CaptureKind.PROJECT -> workspace.projects.map { Triple(it.id, it.title, "${it.area} · ${it.status.name.lowercase()}") }
            CaptureKind.GOAL -> workspace.goals.map { Triple(it.id, it.title, "${it.horizon} · ${it.progress}% complete") }
            else -> emptyList()
        }.filter { it.second.contains(query, true) || it.third.contains(query, true) }
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = 24.dp)) {
            if (rows.isEmpty()) item { Text(if (query.isBlank()) "No ${kind.label.lowercase()}s here. Tap New to add one." else "No matching items.", color = Muted, modifier = Modifier.padding(vertical = 32.dp)) }
            items(rows, key = { it.first }) { (id, title, subtitle) ->
                Column(Modifier.fillMaxWidth().clickable { onEdit(kind, id) }.padding(vertical = 12.dp)) {
                  Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    if (kind == CaptureKind.TASK) Checkbox(workspace.tasks.first { it.id == id }.status == TaskStatus.DONE, { onToggleTask(id) }, modifier = Modifier.semantics { contentDescription = "Complete $title" })
                    Column(Modifier.weight(1f)) { Text(title, style = MaterialTheme.typography.titleMedium, color = Ice, maxLines = 2, overflow = TextOverflow.Ellipsis); Spacer(Modifier.height(5.dp)); Text(subtitle, color = Muted, style = MaterialTheme.typography.bodySmall, maxLines = 3, overflow = TextOverflow.Ellipsis) }
                    IconButton(onClick = { onEdit(kind, id) }) { Icon(Icons.Rounded.Edit, "Edit $title", tint = Muted, modifier = Modifier.size(20.dp)) }
                  }
                  if (kind == CaptureKind.NOTE) {
                      val items = workspace.notes.first { it.id == id }.body.checklistItems()
                      items.take(4).forEach { item ->
                          Row(verticalAlignment = Alignment.CenterVertically) {
                              Checkbox(
                                  checked = item.checked,
                                  onCheckedChange = { scope.launch { onToggleNoteChecklist(id, item.lineIndex) } },
                                  modifier = Modifier.semantics { contentDescription = "${if (item.checked) "Reopen" else "Complete"} ${item.label}" },
                              )
                              Text(item.label, color = Ice, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                          }
                      }
                  }
                }
                HorizontalDivider(color = Divider)
            }
        }
    }
}
