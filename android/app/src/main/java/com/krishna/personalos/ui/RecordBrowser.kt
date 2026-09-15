package com.krishna.personalos.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.krishna.personalos.core.model.*
import com.krishna.personalos.ui.theme.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.time.YearMonth

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
internal fun RecordBrowser(
    doc: TemplateDocument,
    tableId: String,
    parentBusy: Boolean,
    onDismiss: () -> Unit,
    openRow: (String, String?) -> Unit,
    onFields: () -> Unit,
    onDelete: suspend (Set<String>) -> Unit,
    onArchive: suspend (Set<String>, Boolean) -> Unit,
    onDuplicate: suspend (PageRow) -> Unit,
) {
    var query by rememberSaveable(tableId) { mutableStateOf("") }
    var archived by rememberSaveable(tableId) { mutableStateOf(false) }
    var sortName by rememberSaveable(tableId) { mutableStateOf(RecordSort.ADDED.name) }
    var choice by rememberSaveable(tableId, archived) { mutableStateOf<String?>(null) }
    var selection by remember(tableId, archived) { mutableStateOf(emptySet<String>()) }
    var deleting by remember { mutableStateOf(false) }
    var working by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var sortMenu by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val busy = parentBusy || working
    val table = doc.table(tableId)
    val choiceField = table.fields.firstOrNull { it.kind == "CHOICE" && !it.hidden }
    val records = remember(table, query, archived, sortName, choice) { table.records(query, archived, RecordSort.valueOf(sortName), choiceField?.id, choice) }
    val visibleIds = remember(records) { records.map { it.id }.toSet() }
    LaunchedEffect(visibleIds) { selection = selection.intersect(visibleIds) }
    val run: (suspend () -> Unit) -> Unit = { operation -> if (!busy) scope.launch {
        working = true; error = null
        try { operation(); selection = emptySet(); deleting = false }
        catch (e: CancellationException) { throw e }
        catch (e: Exception) { error = e.message ?: "Could not save. Please try again."; deleting = false }
        finally { working = false }
    } }
    val select: (String) -> Unit = { id -> if (!busy) selection = if (id in selection) selection - id else selection + id }
    Editor(table.title.ifBlank { "Records" }, busy, error, { if (selection.isNotEmpty()) selection = emptySet() else onDismiss() }, footer = {
        if (selection.isNotEmpty()) {
            HorizontalDivider()
            Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${selection.size} selected", modifier = Modifier.weight(1f))
                    TextButton(onClick = { selection = visibleIds }, enabled = !busy, modifier = Modifier.testTag("select-all-records")) { Text("Select all") }
                    TextButton(onClick = { selection = emptySet() }, enabled = !busy) { Text("Clear") }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { val ids = selection; run { onArchive(ids, !archived) } }, enabled = !busy, modifier = Modifier.weight(1f)) { Text(if (archived) "Restore selected" else "Archive selected") }
                    Button(onClick = { deleting = true }, enabled = !busy, modifier = Modifier.weight(1f).testTag("delete-selected-records"), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Delete selected") }
                }
            }
        }
    }) {
        OutlinedTextField(query, { query = it }, placeholder = { Text("Search names and values") }, modifier = Modifier.fillMaxWidth(), singleLine = true, enabled = !busy)
        Row(Modifier.horizontalScroll(rememberScrollState()), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { openRow(tableId, null) }, enabled = !busy) { Text("+ Add entry") }
            TextButton(onClick = onFields, enabled = !busy) { Text("Fields") }
            TextButton(onClick = { archived = !archived }, enabled = !busy) { Text(if (archived) "Active records" else "Archive") }
            Box {
                TextButton(onClick = { sortMenu = true }) { Text(RecordSort.valueOf(sortName).label) }
                DropdownMenu(sortMenu, { sortMenu = false }) {
                    RecordSort.entries.forEach { sort -> DropdownMenuItem(text = { Text(sort.label) }, onClick = { sortName = sort.name; sortMenu = false }) }
                }
            }
        }
        if (choiceField != null) Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FilterChip(choice == null, { choice = null }, label = { Text("All ${choiceField.label.lowercase()}") })
            (choiceField.options + "").forEach { value -> FilterChip(choice == value, { choice = value }, label = { Text(value.ifBlank { "Unassigned" }) }) }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("${records.size} ${if (archived) "archived " else ""}records", color = Muted, modifier = Modifier.weight(1f))
            if (records.isNotEmpty() && selection.isEmpty()) TextButton(onClick = { selection = visibleIds }, enabled = !busy) { Text("Select all") }
        }
        if (records.isEmpty()) {
            Text(if (query.isNotBlank() || choice != null) "No matching records. Clear the search or filter to see more." else if (archived) "Archived entries will appear here. You can restore them anytime." else "Your database is ready. Add your first entry above.", color = Muted)
        } else {
            if (selection.isEmpty()) Text("Long press to select · use ⋮ for record actions", color = Muted, style = MaterialTheme.typography.bodySmall)
            LazyColumn(Modifier.fillMaxWidth().heightIn(min = 120.dp, max = 500.dp).testTag("record-list")) {
                items(records, key = { it.id }) { row ->
                    var menu by remember(row.id) { mutableStateOf(false) }
                    val isSelected = row.id in selection
                    Row(Modifier.fillMaxWidth().background(if (isSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface)
                        .semantics { selected = isSelected }
                        .combinedClickable(enabled = !busy, onClick = { if (selection.isEmpty()) openRow(tableId, row.id) else select(row.id) }, onLongClick = { select(row.id) })
                        .padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (selection.isNotEmpty()) Checkbox(isSelected, { select(row.id) }, enabled = !busy)
                        Column(Modifier.weight(1f).padding(start = 4.dp)) {
                            Text(row.displayTitle(), color = Ice, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text(table.fields.filter { !it.hidden && it.kind != "RELATION" }.take(3).joinToString(" · ") { doc.displayValue(table, row, it, YearMonth.now()) }.ifBlank { "Tap to add details" }, color = Muted, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                        if (selection.isEmpty()) Box {
                            IconButton(onClick = { menu = true }, enabled = !busy) { Icon(Icons.Rounded.MoreVert, "Actions for ${row.displayTitle()}") }
                            DropdownMenu(menu, { menu = false }) {
                                DropdownMenuItem(text = { Text("Edit entry") }, onClick = { menu = false; openRow(tableId, row.id) })
                                DropdownMenuItem(text = { Text("Duplicate entry") }, onClick = { menu = false; run { onDuplicate(row) } })
                                DropdownMenuItem(text = { Text(if (archived) "Restore entry" else "Archive entry") }, onClick = { menu = false; run { onArchive(setOf(row.id), !archived) } })
                                DropdownMenuItem(text = { Text("Select entry") }, onClick = { menu = false; select(row.id) })
                            }
                        }
                    }
                    HorizontalDivider(color = Divider.copy(alpha = .5f))
                }
            }
        }
    }
    if (deleting) ConfirmDelete("${selection.size} selected records", "This permanently removes the selected records and their saved values. Archive them instead if you may need them later.", busy, { deleting = false }, { val ids = selection; run { onDelete(ids) } })
}
