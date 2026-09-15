package com.krishna.personalos.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.krishna.personalos.core.database.*
import com.krishna.personalos.core.model.*
import com.krishna.personalos.ui.theme.*
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun TrackerScreen(workspace: TrackerWorkspace, actions: TrackerActions, insets: PaddingValues, initialTrackerId: String? = null, onHome: (() -> Unit)? = null, initiallyEditing: Boolean = false) {
    var selectedId by rememberSaveable { mutableStateOf(initialTrackerId) }
    var templates by rememberSaveable { mutableStateOf(initialTrackerId == null && onHome != null) }
    var archived by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable(selectedId) { mutableStateOf("") }
    var table by rememberSaveable(selectedId) { mutableStateOf(false) }
    var sortByName by rememberSaveable(selectedId) { mutableStateOf(false) }
    var checkedOnly by rememberSaveable(selectedId) { mutableStateOf(false) }
    var entryId by rememberSaveable { mutableStateOf<String?>(null) }
    var fieldId by rememberSaveable { mutableStateOf<String?>(null) }
    var starterId by rememberSaveable { mutableStateOf<String?>(null) }
    var settings by rememberSaveable { mutableStateOf(initiallyEditing) }
    var deleting by remember { mutableStateOf(false) }
    var deletingEntries by remember { mutableStateOf(false) }
    var selectedEntries by remember(selectedId, archived) { mutableStateOf(emptySet<String>()) }
    var copyMode by rememberSaveable { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val run: (suspend () -> Unit) -> Unit = { operation ->
        if (!busy) scope.launch {
            busy = true
            error = null
            try { operation() } catch (cancelled: CancellationException) { throw cancelled }
            catch (failure: Exception) { error = failure.message ?: "Could not save. Please try again." }
            finally { busy = false }
        }
    }
    val tracker = workspace.trackers.find { it.id == selectedId }
    val toggleSelection: (String) -> Unit = { id -> selectedEntries = if (id in selectedEntries) selectedEntries - id else selectedEntries + id }
    BackHandler(selectedEntries.isNotEmpty() || selectedId != null) {
        if (selectedEntries.isNotEmpty()) selectedEntries = emptySet()
        else if (onHome != null) onHome() else { selectedId = null; archived = false }
    }
    val fields = tracker?.let { workspace.fieldsFor(it.id) }.orEmpty()
    if (deleting && tracker != null) ConfirmDelete("template", "This removes “${tracker.title}” and its entries permanently.", busy, { deleting = false }, { run { actions.delete(tracker.id); deleting = false; selectedId = null; onHome?.invoke() } })
    if (deletingEntries && tracker != null) ConfirmDelete(
        "${selectedEntries.size} ${if (selectedEntries.size == 1) "entry" else "entries"}",
        "This permanently removes the selected entries and all their saved values.",
        busy,
        { deletingEntries = false },
        { val ids = selectedEntries; run { actions.deleteEntries(ids); selectedEntries = emptySet(); deletingEntries = false } },
    )

    Column(Modifier.fillMaxSize().padding(insets).padding(horizontal = 20.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            if (selectedId != null || onHome != null) IconButton(onClick = {
                if (selectedEntries.isNotEmpty()) selectedEntries = emptySet()
                else if (onHome != null) onHome() else { selectedId = null; archived = false }
            }) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back to trackers")
            }
            Text(if (tracker == null) "YOUR WORKSPACE" else if (tracker.isTemplate) "TEMPLATE" else "TRACKER", style = MaterialTheme.typography.labelMedium, color = Muted, modifier = Modifier.weight(1f))
            if (tracker != null) { TextButton(onClick = { settings = true }) { Text("Customize") }; TextButton(onClick = { deleting = true }) { Text("Delete") } }
        }
        Text(tracker?.title ?: if (templates) "Made for your life" else "Track what matters", style = MaterialTheme.typography.headlineLarge, color = Ice, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(8.dp))
        Text(tracker?.description ?: "Routines, reading, spending, or something entirely your own.", style = MaterialTheme.typography.bodyMedium, color = Muted, maxLines = 3, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(12.dp))
        if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(vertical = 8.dp)) }
        if (tracker == null) {
            Row(Modifier.horizontalScroll(rememberScrollState())) {
                TextButton(onClick = { templates = false; archived = false }) { Text("My trackers", color = if (!templates && !archived) Cyan else Muted) }
                TextButton(onClick = { templates = true; archived = false }) { Text("Templates", color = if (templates && !archived) Cyan else Muted) }
                TextButton(onClick = { archived = !archived }) { Text("Archive", color = if (archived) Cyan else Muted) }
            }
            SearchLine(query, { query = it }, "Find a tracker")
            LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = 24.dp)) {
                val trackers = workspace.trackers.filter { it.archived == archived && (archived || it.isTemplate == templates) && it.title.contains(query, true) }
                if (!templates && !archived) item {
                    TextButton(onClick = { templates = true }) { Icon(Icons.Rounded.Add, null); Spacer(Modifier.width(8.dp)); Text("Create a tracker") }
                }
                items(trackers, key = { it.id }) { item ->
                    WorkspaceLine(item.title, "${entryCount(workspace.entriesFor(item.id).size)} · ${workspace.fieldsFor(item.id).size} fields", onClick = { selectedId = item.id; archived = false })
                }
                if (trackers.isEmpty() && !templates) item {
                    Text(if (archived) "Archived trackers appear here. Open one to restore it." else if (query.isNotBlank()) "No matching trackers." else "Start with a template, then make it yours. Every field and entry is editable.", color = Muted, modifier = Modifier.padding(vertical = 28.dp))
                }
                if (templates && !archived) {
                    item { Text("STARTING POINTS", color = Muted, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)) }
                    items(TrackerStarters.all.filter { it.title.contains(query, true) }, key = { "starter-${it.id}" }) { starter ->
                        WorkspaceLine(starter.title, starter.description) { starterId = starter.id }
                    }
                }
            }
        } else {
            Row(Modifier.horizontalScroll(rememberScrollState()), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { entryId = "new" }, enabled = !busy && !tracker.archived) { Icon(Icons.Rounded.Add, null); Text("Entry") }
                TextButton(onClick = { table = !table }) { Text(if (table) "List view" else "Table view") }
                TextButton(onClick = { sortByName = !sortByName }) { Text(if (sortByName) "A–Z" else "Oldest first") }
                if (tracker.isTemplate) TextButton(onClick = { copyMode = "use" }) { Text("Use template") }
                TextButton(onClick = { archived = !archived }) { Text(if (archived) "Active entries" else "Archived entries") }
            }
            SearchLine(query, { query = it }, "Search entries and values")
            val checkbox = fields.firstOrNull { it.kind == FieldKind.CHECKBOX.name }
            if (checkbox != null) Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checkedOnly, { checkedOnly = it }, modifier = Modifier.semantics { contentDescription = "Filter by ${checkbox.name}" })
                Text("${checkbox.name} checked", color = Muted, style = MaterialTheme.typography.bodySmall)
            }
            val entries = workspace.entriesFor(tracker.id, archived).filter { entry ->
                val values = workspace.valuesFor(entry.id)
                (entry.title.contains(query, true) || entry.body.contains(query, true) || fields.any { values[it.id].orEmpty().contains(query, true) }) &&
                    (!checkedOnly || checkbox == null || values[checkbox.id] == "true")
            }.let { if (sortByName) it.sortedBy { entry -> entry.title.lowercase() } else it }
            if (selectedEntries.isNotEmpty()) {
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), verticalAlignment = Alignment.CenterVertically) {
                    Text("${selectedEntries.size} selected", color = Ice, modifier = Modifier.padding(end = 8.dp))
                    TextButton(onClick = { selectedEntries = entries.map { it.id }.toSet() }, modifier = Modifier.testTag("select-all-entries")) { Text("Select all") }
                    TextButton(onClick = { selectedEntries = emptySet() }) { Text("Clear") }
                    TextButton(onClick = { deletingEntries = true }, modifier = Modifier.testTag("delete-selected-entries"), colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text("Delete") }
                }
            } else if (entries.isNotEmpty()) {
                Text("Long press an entry to select multiple.", color = Muted, style = MaterialTheme.typography.bodySmall)
            }
            Text("${entryCount(entries.size)}${if (archived) " archived" else ""} · ${fields.size} fields", color = Muted, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(vertical = 8.dp))
            if (entries.isEmpty()) {
                Text(if (query.isNotBlank() || checkedOnly) "No entries match. Try changing your search or filter." else if (archived) "Nothing archived." else "Add your first entry. Tap Customize to add or rename fields.", color = Muted, modifier = Modifier.padding(vertical = 24.dp))
            }
            if (table) {
                Text("Swipe sideways for more fields. Tap a value to edit.", color = Muted, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(bottom = 8.dp))
                Column(Modifier.weight(1f).testTag("tracker-table").horizontalScroll(rememberScrollState()).width((180 + fields.sumOf { if (it.kind == FieldKind.CHECKBOX.name) 64 else 140 }).dp)) {
                    Row(Modifier.heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("Name", modifier = Modifier.width(180.dp), color = Muted)
                        fields.forEach { Text(it.name, modifier = Modifier.width(it.columnWidth()), color = Muted, maxLines = 2) }
                    }
                    HorizontalDivider(color = Divider)
                    LazyColumn {
                        items(entries, key = { it.id }) { entry ->
                            val values = workspace.valuesFor(entry.id)
                            Row(Modifier.background(if (entry.id in selectedEntries) Cyan.copy(alpha = .10f) else androidx.compose.ui.graphics.Color.Transparent), verticalAlignment = Alignment.CenterVertically) {
                                Row(
                                    Modifier.width(180.dp).heightIn(min = 56.dp).combinedClickable(
                                        onClick = { if (selectedEntries.isNotEmpty()) toggleSelection(entry.id) else entryId = entry.id },
                                        onLongClick = { toggleSelection(entry.id) },
                                    ).padding(horizontal = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    if (selectedEntries.isNotEmpty()) Checkbox(entry.id in selectedEntries, { toggleSelection(entry.id) })
                                    Text(entry.title, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f), color = Ice)
                                }
                                fields.forEach { field ->
                                    Box(Modifier.width(field.columnWidth()).heightIn(min = 56.dp), contentAlignment = Alignment.CenterStart) {
                                        if (field.kind == FieldKind.CHECKBOX.name) Checkbox(values[field.id] == "true", { run { actions.toggleCheckbox(entry.id, field.id) } }, enabled = selectedEntries.isEmpty() && !busy && !archived && !tracker.archived, modifier = Modifier.semantics { contentDescription = "${entry.title}, ${field.name}" })
                                        else Text(values[field.id].orEmpty().let { if (field.kind == FieldKind.CHECKLIST.name) it.checklistSummary() else it.ifBlank { "—" } }, modifier = Modifier.fillMaxWidth().clickable { if (selectedEntries.isNotEmpty()) toggleSelection(entry.id) else entryId = entry.id }.padding(vertical = 16.dp), maxLines = 2, overflow = TextOverflow.Ellipsis, color = Ice)
                                    }
                                }
                            }
                            HorizontalDivider(color = Divider.copy(alpha = .5f))
                        }
                    }
                }
            } else {
                LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = 24.dp)) {
                    items(entries, key = { it.id }) { entry ->
                        val values = workspace.valuesFor(entry.id)
                        val summary = fields.mapNotNull { field -> values[field.id]?.takeIf { it.isNotBlank() }?.let {
                            "${field.name}: ${when (field.kind) { FieldKind.CHECKBOX.name -> if (it == "true") "✓" else "–"; FieldKind.CHECKLIST.name -> it.checklistSummary(); else -> it }}"
                        } }.joinToString(" · ")
                        SelectableWorkspaceLine(
                            title = entry.title,
                            subtitle = summary.ifBlank { entry.body.ifBlank { "Tap to add details" } },
                            selected = entry.id in selectedEntries,
                            selectionMode = selectedEntries.isNotEmpty(),
                            onClick = { if (selectedEntries.isNotEmpty()) toggleSelection(entry.id) else entryId = entry.id },
                            onLongClick = { toggleSelection(entry.id) },
                        )
                    }
                }
            }
        }
    }

    starterId?.let { id ->
        val starter = TrackerStarters.all.first { it.id == id }
        NameDialog("Create tracker", if (id == "blank") "" else starter.title, busy, error, { starterId = null }) { name ->
            run { selectedId = actions.create(id, name); starterId = null; templates = false }
        }
    }
    if (tracker != null) {
        if (settings) TrackerSettings(tracker, workspace.fields.filter { it.trackerId == tracker.id }, busy, error,
            onDismiss = { settings = false },
            onSave = { title, description -> run { actions.saveTracker(tracker.copy(title = title, description = description)); settings = false } },
            onField = { fieldId = it },
            onDuplicate = { copyMode = "copy" }, onTemplate = { copyMode = "template" },
            onArchive = { run { actions.saveTracker(tracker.copy(archived = !tracker.archived)); settings = false; selectedId = null } },
        )
        copyMode?.let { mode ->
            CopyDialog(mode, tracker, busy, error, { copyMode = null }) { name, include ->
                run { selectedId = actions.duplicate(tracker.id, name, mode == "template", include); copyMode = null; settings = false; archived = false }
            }
        }
        fieldId?.let { id ->
            val field = remember(id, tracker.id) { workspace.fields.find { it.id == id } ?: TrackerFieldEntity(UUID.randomUUID().toString(), tracker.id, "", FieldKind.TEXT.name, position = (workspace.fields.filter { it.trackerId == tracker.id }.maxOfOrNull { it.position } ?: -1) + 1) }
            FieldDialog(field, id == "new", busy, error, { fieldId = null }) { value -> run { actions.saveField(value); fieldId = null } }
        }
        entryId?.let { id ->
            val entry = remember(id, tracker.id) { workspace.entries.find { it.id == id } ?: TrackerEntryEntity(UUID.randomUUID().toString(), tracker.id, "", createdAt = System.currentTimeMillis()) }
            EntryDialog(entry, fields, workspace.valuesFor(entry.id), busy, error, { entryId = null },
                onSave = { value, values -> run { actions.saveEntry(value, values); entryId = null } },
                onArchive = if (id == "new") null else ({ run { actions.archiveEntry(id, !entry.archived); entryId = null } }),
                onDelete = if (id == "new") null else ({ run { actions.deleteEntry(id); entryId = null } }),
            )
        }
    }
}

@Composable
private fun SearchLine(value: String, onValue: (String) -> Unit, placeholder: String) {
    TextField(value, onValue, placeholder = { Text(placeholder) }, singleLine = true, modifier = Modifier.fillMaxWidth(),
        colors = TextFieldDefaults.colors(unfocusedContainerColor = androidx.compose.ui.graphics.Color.Transparent, focusedContainerColor = androidx.compose.ui.graphics.Color.Transparent))
}

private fun TrackerFieldEntity.columnWidth() = (if (kind == FieldKind.CHECKBOX.name) 64 else 140).dp
private fun entryCount(count: Int) = "$count ${if (count == 1) "entry" else "entries"}"

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SelectableWorkspaceLine(title: String, subtitle: String, selected: Boolean, selectionMode: Boolean, onClick: () -> Unit, onLongClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().background(if (selected) Cyan.copy(alpha = .10f) else androidx.compose.ui.graphics.Color.Transparent)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick).padding(vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (selectionMode) Checkbox(selected, { onClick() })
        Column(Modifier.weight(1f)) {
            Text(title, color = Ice, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(5.dp))
            Text(subtitle, color = Muted, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        if (!selectionMode) Icon(Icons.Rounded.ChevronRight, null, tint = Muted, modifier = Modifier.padding(start = 12.dp))
    }
    HorizontalDivider(color = Divider.copy(alpha = .5f))
}

@Composable
internal fun WorkspaceLine(title: String, subtitle: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 18.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, color = Ice, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(5.dp))
            Text(subtitle, color = Muted, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        Icon(Icons.Rounded.ChevronRight, null, tint = Muted, modifier = Modifier.padding(start = 12.dp))
    }
    HorizontalDivider(color = Divider.copy(alpha = .5f))
}

@Composable
internal fun Editor(title: String, busy: Boolean, error: String?, onDismiss: () -> Unit, footer: (@Composable () -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    Dialog(onDismissRequest = { if (!busy) onDismiss() }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize().testTag("workspace-editor"), color = Ink) {
            Column(Modifier.fillMaxSize().systemBarsPadding().imePadding().padding(horizontal = 24.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f), color = Ice)
                    IconButton(onClick = onDismiss, enabled = !busy) { Icon(Icons.Rounded.Close, "Cancel") }
                }
                if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(vertical = 8.dp)) }
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(16.dp), content = content)
                footer?.invoke()
            }
        }
    }
}

@Composable
private fun NameDialog(title: String, initial: String, busy: Boolean, error: String?, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var name by rememberSaveable { mutableStateOf(initial) }
    Editor(title, busy, error, onDismiss) {
        OutlinedTextField(name, { name = it }, label = { Text("Tracker name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        Text("This creates an independent tracker on your device. You can change its fields at any time.", color = Muted)
        Button(onClick = { onSave(name) }, enabled = name.isNotBlank() && !busy) { Text("Create tracker") }
    }
}

@Composable
private fun CopyDialog(mode: String, tracker: TrackerEntity, busy: Boolean, error: String?, onDismiss: () -> Unit, onSave: (String, Boolean) -> Unit) {
    var name by rememberSaveable { mutableStateOf(tracker.title + if (mode == "template") " template" else " copy") }
    var include by rememberSaveable { mutableStateOf(mode == "use") }
    Editor(if (mode == "template") "Save as template" else if (mode == "use") "Use template" else "Duplicate tracker", busy, error, onDismiss) {
        OutlinedTextField(name, { name = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(include, { include = it }); Text("Include entries and their values") }
        Text("The copy is independent. Editing it will not change the original.", color = Muted)
        Button(onClick = { onSave(name, include) }, enabled = name.isNotBlank() && !busy) { Text("Create copy") }
    }
}

@Composable
private fun TrackerSettings(tracker: TrackerEntity, fields: List<TrackerFieldEntity>, busy: Boolean, error: String?, onDismiss: () -> Unit, onSave: (String, String) -> Unit, onField: (String) -> Unit, onDuplicate: () -> Unit, onTemplate: () -> Unit, onArchive: () -> Unit) {
    var title by rememberSaveable(tracker.id) { mutableStateOf(tracker.title) }
    var description by rememberSaveable(tracker.id) { mutableStateOf(tracker.description) }
    Editor("Customize tracker", busy, error, onDismiss) {
        OutlinedTextField(title, { title = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(description, { description = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth())
        Button(onClick = { onSave(title, description) }, enabled = title.isNotBlank() && !busy) { Text("Save changes") }
        Text("FIELDS", color = Muted, style = MaterialTheme.typography.labelMedium)
        fields.forEach { field -> WorkspaceLine(field.name, "${FieldKind.valueOf(field.kind).label}${if (field.archived) " · Hidden" else ""}") { onField(field.id) } }
        TextButton(onClick = { onField("new") }, enabled = !busy) { Text("+ Add a field") }
        HorizontalDivider(color = Divider)
        TextButton(onClick = onDuplicate, enabled = !busy) { Text("Duplicate tracker") }
        TextButton(onClick = onTemplate, enabled = !busy) { Text("Save as reusable template") }
        TextButton(onClick = onArchive, enabled = !busy) { Text(if (tracker.archived) "Restore tracker" else "Archive tracker") }
        Text("Archived trackers and hidden fields keep their data. You can restore them later.", color = Muted, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun FieldDialog(field: TrackerFieldEntity, isNew: Boolean, busy: Boolean, error: String?, onDismiss: () -> Unit, onSave: (TrackerFieldEntity) -> Unit) {
    var name by rememberSaveable(field.id) { mutableStateOf(field.name) }
    var kind by rememberSaveable(field.id) { mutableStateOf(field.kind) }
    var options by rememberSaveable(field.id) { mutableStateOf(field.options.replace("|", "\n")) }
    var hidden by rememberSaveable(field.id) { mutableStateOf(field.archived) }
    Editor(if (isNew) "Add a field" else "Edit field", busy, error, onDismiss) {
        OutlinedTextField(name, { name = it }, label = { Text("Field name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        if (isNew) FieldKind.entries.forEach { type ->
            Row(Modifier.fillMaxWidth().clickable { kind = type.name }, verticalAlignment = Alignment.CenterVertically) { RadioButton(kind == type.name, { kind = type.name }); Text(type.label) }
        } else Text("Type: ${FieldKind.valueOf(kind).label}. Add a new field to use a different type.", color = Muted)
        if (kind == FieldKind.CHOICE.name) OutlinedTextField(options, { options = it }, label = { Text("Choices, one per line") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
        if (!isNew) Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(hidden, { hidden = it }); Text("Hide this field (keep its data)") }
        Button(onClick = { onSave(field.copy(name = name, kind = kind, options = options.lines().joinToString("|"), archived = hidden)) }, enabled = name.isNotBlank() && !busy && (kind != FieldKind.CHOICE.name || options.isNotBlank())) { Text("Save field") }
    }
}

@Composable
private fun EntryDialog(entry: TrackerEntryEntity, fields: List<TrackerFieldEntity>, original: Map<String, String>, busy: Boolean, error: String?, onDismiss: () -> Unit, onSave: (TrackerEntryEntity, Map<String, String>) -> Unit, onArchive: (() -> Unit)?, onDelete: (() -> Unit)?) {
    var title by rememberSaveable(entry.id) { mutableStateOf(entry.title) }
    var body by rememberSaveable(entry.id) { mutableStateOf(entry.body) }
    var values by rememberSaveable(entry.id) { mutableStateOf<Map<String, String>>(HashMap(original)) }
    var discard by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }
    val dirty = title != entry.title || body != entry.body || values != original
    Editor(if (entry.title.isBlank()) "New entry" else "Edit entry", busy, error, { if (dirty) discard = true else onDismiss() }) {
        OutlinedTextField(title, { title = it }, label = { Text("Entry name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        fields.forEach { field ->
            val value = values[field.id].orEmpty()
            val set: (String) -> Unit = { values = HashMap(values).apply { put(field.id, it) } }
            when (FieldKind.valueOf(field.kind)) {
                FieldKind.CHECKBOX -> Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(value == "true", { set(it.toString()) }, modifier = Modifier.semantics { contentDescription = field.name }); Text(field.name) }
                FieldKind.CHECKLIST -> EntryChecklistEditor(field.name, value, set)
                FieldKind.CHOICE -> {
                    Text(field.name, color = Muted)
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = value.isBlank(), onClick = { set("") }, label = { Text("None") })
                        field.choices().forEach { choice -> FilterChip(selected = choice == value, onClick = { set(choice) }, label = { Text(choice) }) }
                    }
                }
                else -> OutlinedTextField(value, set, label = { Text(field.name) }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = if (field.kind == FieldKind.NUMBER.name) KeyboardType.Decimal else KeyboardType.Text),
                    isError = field.valueError(value) != null,
                    supportingText = { Text(field.valueError(value) ?: if (field.kind == FieldKind.DATE.name) "YYYY-MM-DD" else "Optional") })
            }
        }
        Text("Notes & reflection", color = Muted, style = MaterialTheme.typography.labelMedium)
        NoteBlockEditor(body) { body = it }
        Button(onClick = { onSave(entry.copy(title = title, body = body), values) }, enabled = !busy && title.isNotBlank() && fields.all { it.valueError(values[it.id].orEmpty()) == null }) { Text("Save entry") }
        onArchive?.let { TextButton(onClick = it, enabled = !busy) { Text(if (entry.archived) "Restore entry" else "Archive entry") } }
        onDelete?.let { TextButton(onClick = { deleting = true }, enabled = !busy, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text("Delete entry") } }
    }
    if (discard) AlertDialog(onDismissRequest = { discard = false }, title = { Text("Discard changes?") }, text = { Text("Your unsaved entry changes will be lost.") }, confirmButton = { TextButton(onClick = onDismiss) { Text("Discard") } }, dismissButton = { TextButton(onClick = { discard = false }) { Text("Keep editing") } })
    if (deleting) ConfirmDelete("entry", "This permanently removes “${entry.title}” and its saved values.", busy, { deleting = false }, { deleting = false; onDelete?.invoke() })
}
