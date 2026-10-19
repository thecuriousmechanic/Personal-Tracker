package com.krishna.personalos.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.krishna.personalos.core.database.*
import com.krishna.personalos.core.model.*
import com.krishna.personalos.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private data class HomePage(val id: String, val title: String, val subtitle: String, val kind: String, val custom: Boolean, val cover: String = "")

@Composable
internal fun WorkspaceOverview(
    pages: List<TemplatePageEntity>, trackers: TrackerWorkspace, workspace: WorkspaceSnapshot,
    pinnedIds: Set<String>, error: String?, insets: PaddingValues,
    onPage: (String) -> Unit, onCustom: (String) -> Unit, onCreate: () -> Unit,
    onEdit: (String, Boolean) -> Unit, onDelete: (String, String, Boolean) -> Unit,
    onPin: (String, Boolean) -> Unit, onSettings: () -> Unit, onPlan: () -> Unit,
    onTask: (String?) -> Unit, onToggleTask: (String) -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var archived by rememberSaveable { mutableStateOf(false) }
    var today by remember { mutableStateOf(LocalDate.now()) }
    LaunchedEffect(Unit) { while (true) { today = LocalDate.now(); delay(30_000) } }
    val matches by produceState<List<HomePage>?>(null, pages, trackers, query, archived) {
        // Debounce full-content searches and keep scans off the UI thread.
        if (query.isNotBlank()) delay(150)
        value = withContext(Dispatchers.Default) {
            val search = query.trim()
            val native = pages.filter { it.archived == archived && (search.isBlank() || it.title.contains(search, true) || it.subtitle.contains(search, true) || TemplateDocumentCache.getOrParse(it.id, it.data).matchesQuery(search)) }
                .map { HomePage(it.id, it.title, it.subtitle, it.kind, false, it.cover) }
            val entriesByPage = trackers.entries.groupBy { it.trackerId }
            val valuesByEntry = trackers.cells.groupBy { it.entryId }
            val custom = trackers.trackers.filter { tracker -> tracker.archived == archived && (search.isBlank() || tracker.title.contains(search, true) || tracker.description.contains(search, true) || entriesByPage[tracker.id].orEmpty().any { entry -> entry.title.contains(search, true) || entry.body.contains(search, true) || valuesByEntry[entry.id].orEmpty().any { it.value.contains(search, true) } }) }
                .map { HomePage(it.id, it.title, "${trackers.entriesFor(it.id).size} entries · ${it.description}", "custom", true) }
            native + custom
        }
    }
    val visible = matches.orEmpty()
    val pinned = visible.filter { it.id in pinnedIds }
    val other = visible.filterNot { it.id in pinnedIds }
    val todayTasks = workspace.todayTasks(today.toEpochDay())
    val active = todayTasks.filter { it.status != TaskStatus.DONE }
    val completed = todayTasks.count { it.status == TaskStatus.DONE }
    LazyColumn(Modifier.fillMaxSize().background(Paper).padding(insets).testTag("template-home"), contentPadding = PaddingValues(horizontal = 22.dp, vertical = 16.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Spa, null, tint = Cyan, modifier = Modifier.size(22.dp))
                Text("  PERSONAL-TRACKER", style = MaterialTheme.typography.labelMedium, color = Cyan, modifier = Modifier.weight(1f))
                IconButton(onClick = onSettings) { Icon(Icons.Rounded.Settings, "Workspace settings", tint = Muted) }
            }
            Spacer(Modifier.height(12.dp))
            Text(if (archived) "Your archive" else "Your workspace", style = MaterialTheme.typography.headlineLarge, color = Ice)
            Text(today.format(DateTimeFormatter.ofPattern("EEEE, d MMMM")), style = MaterialTheme.typography.bodyMedium, color = Muted, modifier = Modifier.padding(top = 6.dp, bottom = 24.dp))
            if (!archived && query.isBlank()) {
                Surface(color = SageWash, shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Today", style = MaterialTheme.typography.titleMedium, color = Ice, modifier = Modifier.weight(1f))
                            TextButton(onClick = onPlan, contentPadding = PaddingValues(horizontal = 8.dp)) { Text("Open plan") }
                        }
                        Text(if (todayTasks.isEmpty()) "A little focus goes a long way." else "$completed of ${todayTasks.size} complete", color = Muted, style = MaterialTheme.typography.bodySmall)
                        if (todayTasks.isNotEmpty()) LinearProgressIndicator(progress = { completed.toFloat() / todayTasks.size }, modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), color = Cyan, trackColor = Cyan.copy(alpha = .12f))
                        active.take(3).forEach { task ->
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                Checkbox(false, { onToggleTask(task.id) }, modifier = Modifier.semantics { contentDescription = "Complete ${task.title}" })
                                Text(task.title, style = MaterialTheme.typography.bodyMedium, color = Ice, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f).clickable { onTask(task.id) }.padding(vertical = 12.dp))
                            }
                        }
                        if (todayTasks.isNotEmpty() && active.isEmpty()) Text("You’ve finished today’s plan.", color = Cyan, modifier = Modifier.padding(top = 12.dp))
                        TextButton(onClick = { onTask(null) }, contentPadding = PaddingValues(horizontal = 0.dp)) { Icon(Icons.Rounded.Add, null, Modifier.size(18.dp)); Spacer(Modifier.width(5.dp)); Text("Add a task") }
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
            OutlinedTextField(query, { query = it }, placeholder = { Text("Search pages and contents") }, leadingIcon = { Icon(Icons.Rounded.Search, null) }, trailingIcon = if (query.isNotEmpty()) ({ IconButton(onClick = { query = "" }) { Icon(Icons.Rounded.Close, "Clear search") } }) else null,
                singleLine = true, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth())
            Row(Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(if (archived) "Archived pages" else "Your pages", style = MaterialTheme.typography.titleMedium, color = Ice, modifier = Modifier.weight(1f))
                TextButton(onClick = { archived = !archived }) { Text(if (archived) "Active pages" else "Archive") }
            }
            if (!archived) FilledTonalButton(onClick = onCreate, modifier = Modifier.fillMaxWidth().testTag("new-template"), contentPadding = PaddingValues(14.dp)) { Icon(Icons.Rounded.Add, null, Modifier.size(20.dp)); Spacer(Modifier.width(8.dp)); Text("New page") }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(vertical = 12.dp)) }
            Spacer(Modifier.height(12.dp))
        }
        if (pinned.isNotEmpty()) item { Text("PINNED", color = Cyan, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(vertical = 10.dp)) }
        items(pinned, key = { "pin:${it.id}" }) { item -> HomePageRow(item, true, onPage, onCustom, onEdit, onDelete, onPin) }
        if (other.isNotEmpty() && pinned.isNotEmpty()) item { Text("ALL PAGES", color = Muted, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 24.dp, bottom = 10.dp)) }
        items(other, key = { it.id }) { item -> HomePageRow(item, false, onPage, onCustom, onEdit, onDelete, onPin) }
        if (matches == null) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
        else if (visible.isEmpty()) item {
            Column(Modifier.fillMaxWidth().padding(vertical = 32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(if (query.isNotBlank()) Icons.Rounded.Search else Icons.Rounded.Description, null, tint = Muted, modifier = Modifier.size(32.dp))
                Text(if (query.isNotBlank()) "No matching pages" else if (archived) "Your archive is empty" else "Make this space yours", color = Ice, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 12.dp))
                Text(if (query.isNotBlank()) "Try a name, checklist item, or record value." else if (archived) "Archived pages keep all their records." else "Start with a routine, a budget, or a blank page.", color = Muted, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 6.dp))
            }
        }
    }
}

@Composable
private fun HomePageRow(item: HomePage, pinned: Boolean, onPage: (String) -> Unit, onCustom: (String) -> Unit, onEdit: (String, Boolean) -> Unit, onDelete: (String, String, Boolean) -> Unit, onPin: (String, Boolean) -> Unit) {
    var menu by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().clickable { if (item.custom) onCustom(item.id) else onPage(item.id) }.padding(vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        if (item.cover.isNotBlank()) PageCover(item.cover, Modifier.size(50.dp).clip(RoundedCornerShape(12.dp)))
        else Box(Modifier.size(50.dp).background(if (item.kind == "finance") SageWash else if (item.kind == "week") PeachWash else LilacWash, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
            Icon(when (item.kind) { "finance" -> Icons.Rounded.AccountBalanceWallet; "week" -> Icons.Rounded.Spa; "study" -> Icons.Rounded.School; "custom" -> Icons.Rounded.TableChart; else -> Icons.Rounded.Description }, null, tint = Cyan, modifier = Modifier.size(23.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(item.title, color = Ice, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (item.subtitle.isNotBlank()) Text(item.subtitle, color = Muted, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp))
        }
        Box {
            IconButton(onClick = { menu = true }) { Icon(Icons.Rounded.MoreVert, "Actions for ${item.title}", tint = Muted) }
            DropdownMenu(menu, { menu = false }) {
                DropdownMenuItem(text = { Text(if (pinned) "Unpin page" else "Pin to Home") }, onClick = { menu = false; onPin(item.id, !pinned) })
                DropdownMenuItem(text = { Text("Edit page") }, onClick = { menu = false; onEdit(item.id, item.custom) })
                DropdownMenuItem(text = { Text("Move to Trash") }, onClick = { menu = false; onDelete(item.id, item.title, item.custom) })
            }
        }
    }
    HorizontalDivider(color = Divider.copy(alpha = .7f))
}
