package com.krishna.personalos.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.krishna.personalos.core.model.*
import com.krishna.personalos.ui.theme.*

@Composable
internal fun WorkspaceBoard(table: PageTable, busy: Boolean, onOpen: (String) -> Unit, onMove: (String, String, String) -> Unit) {
    val drag = remember(table.id) { WorkspaceDragState() }
    val scroll = rememberScrollState()
    var bounds by remember { mutableStateOf(Rect.Zero) }
    WorkspaceDragAutoScroll(drag, bounds, horizontal = true) { scroll.scrollBy(it) }
    val status = table.fields.firstOrNull { it.kind == "CHOICE" && !it.hidden }
    val rows = table.rows.filterNot { it.archived }
    if (status == null) {
        Text("Add a choice field in Fields to organize this board into columns.", color = Muted)
        rows.forEach { row -> WorkspaceLine(row.displayTitle(), "Tap to edit") { onOpen(row.id) } }
        return
    }
    Text("Swipe for columns · hold a card's ⋮⋮ handle to change its status", color = Muted, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(bottom = 10.dp))
    val groups = (status.options + rows.map { it.values[status.id].orEmpty() } + "").distinct()
    Row(Modifier.fillMaxWidth().onGloballyPositioned { bounds = it.boundsInRoot() }.horizontalScroll(scroll).testTag("workspace-board"), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        groups.forEach { group ->
            val records = rows.filter { it.values[status.id].orEmpty() == group }
            val label = group.ifBlank { "Unassigned" }
            Column(Modifier.width(230.dp).heightIn(min = 180.dp).background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(12.dp))
                .workspaceDropTarget(drag, "column:$group", "board:${table.id}") { row, _ -> onMove(row, status.id, group) }
                .padding(12.dp).testTag("board-column-$group")) {
                Text("$label · ${records.size}", style = MaterialTheme.typography.titleSmall, color = Ice, modifier = Modifier.padding(bottom = 12.dp))
                if (records.isEmpty()) Text(if (drag.source != null) "Drop here" else "No entries", color = Muted, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(vertical = 20.dp))
                records.forEach { row -> key(row.id) {
                    Surface(color = if (drag.source == row.id) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                        Row(Modifier.padding(vertical = 8.dp), verticalAlignment = Alignment.Top) {
                            WorkspaceDragHandle(row.id, "board:${table.id}", "card ${row.displayTitle()}", !busy, drag)
                            Column(Modifier.weight(1f).clickable(enabled = !busy) { if (drag.source == null) onOpen(row.id) }.padding(top = 8.dp, end = 10.dp, bottom = 8.dp)) {
                                Text(row.displayTitle(), style = MaterialTheme.typography.bodyLarge, maxLines = 3, overflow = TextOverflow.Ellipsis)
                                val checklist = table.fields.firstOrNull { it.kind == "CHECKLIST" && !it.hidden }
                                if (checklist != null) Text(row.values[checklist.id].orEmpty().checklistSummary(), color = Muted, style = MaterialTheme.typography.bodySmall)
                                table.dateOf(row)?.let { Text(it.toString(), color = Muted, style = MaterialTheme.typography.labelSmall) }
                            }
                        }
                    }
                } }
            }
        }
    }
}
