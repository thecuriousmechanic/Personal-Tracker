package com.krishna.personalos.ui

import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.krishna.personalos.core.database.TemplatePageEntity
import com.krishna.personalos.core.export.TemplateWorkbookExporter
import com.krishna.personalos.core.model.*
import com.krishna.personalos.ui.theme.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.UUID

private fun accent(kind: String) = when (kind) { "week" -> Amber; "finance" -> Mint; "study" -> ElectricBlue; else -> Violet }
private fun pageIcon(kind: String) = when (kind) { "week" -> "🌿"; "finance" -> "💰"; "study" -> "◫"; else -> "✦" }
private fun pageCategory(kind: String) = when (kind) { "week" -> "Routine & wellbeing"; "finance" -> "Money & planning"; "study" -> "Learning workspace"; else -> "Personal growth" }
private fun pageWash(kind: String) = when (kind) { "week" -> PeachWash; "finance" -> SageWash; "study" -> SageWash; else -> LilacWash }
private val coverCache = android.util.LruCache<String, androidx.compose.ui.graphics.ImageBitmap>(6)

@Composable
private fun PageCover(file: String, modifier: Modifier) {
    if (file.isBlank()) {
        Box(modifier.background(MaterialTheme.colorScheme.surfaceContainer))
        return
    }
    val context = LocalContext.current
    val cover by produceState(coverCache.get(file), file) {
        value = coverCache.get(file) ?: withContext(Dispatchers.IO) { runCatching {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.assets.open("private-notion/$file").use { BitmapFactory.decodeStream(it, null, bounds) }
            val options = BitmapFactory.Options().apply { inSampleSize = 1; while (bounds.outWidth / inSampleSize > 1200) inSampleSize *= 2 }
            context.assets.open("private-notion/$file").use { BitmapFactory.decodeStream(it, null, options)?.asImageBitmap() }?.also { coverCache.put(file, it) }
        }.getOrNull() }
    }
    Box(modifier.background(SurfaceNavy)) { cover?.let { Image(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) } }
}

@Composable
internal fun TemplateHomeScreen(pages: List<TemplatePageEntity>, trackers: TrackerWorkspace, error: String?, insets: PaddingValues, onPage: (String) -> Unit, onCustom: (String) -> Unit, onCreate: () -> Unit, onEdit: (String, Boolean) -> Unit, onDelete: (String, String, Boolean) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    var archived by rememberSaveable { mutableStateOf(false) }
    val matchingPages by produceState(pages, pages, query, archived) {
        value = withContext(Dispatchers.Default) {
            val search = query.trim()
            pages.filter { it.archived == archived && (search.isBlank() || it.title.contains(search, true) || it.subtitle.contains(search, true) || TemplateDocumentCache.getOrParse(it.id, it.data).matchesQuery(search)) }
        }
    }
    val matchingTrackers = remember(trackers, query, archived) {
        val search = query.trim()
        trackers.trackers.filter { tracker -> tracker.archived == archived && (search.isBlank() || tracker.title.contains(search, true) || tracker.description.contains(search, true) || trackers.entries.filter { it.trackerId == tracker.id }.any { entry -> entry.title.contains(search, true) || entry.body.contains(search, true) || trackers.valuesFor(entry.id).values.any { it.contains(search, true) } }) }
    }
    LazyColumn(Modifier.fillMaxSize().padding(insets).testTag("template-home"), contentPadding = PaddingValues(bottom = 24.dp)) {
        item {
            Box(Modifier.fillMaxWidth().background(SageWash)) {
                Canvas(Modifier.align(Alignment.TopEnd).size(156.dp)) {
                    drawCircle(Amber.copy(alpha = .12f), radius = 43.dp.toPx(), center = androidx.compose.ui.geometry.Offset(size.width - 25.dp.toPx(), 56.dp.toPx()))
                    drawArc(Cyan.copy(alpha = .14f), 40f, 240f, false, topLeft = androidx.compose.ui.geometry.Offset(10.dp.toPx(), 48.dp.toPx()), size = androidx.compose.ui.geometry.Size(140.dp.toPx(), 140.dp.toPx()), style = androidx.compose.ui.graphics.drawscope.Stroke(1.dp.toPx()))
                    drawArc(Cyan.copy(alpha = .14f), 40f, 240f, false, topLeft = androidx.compose.ui.geometry.Offset(20.dp.toPx(), 38.dp.toPx()), size = androidx.compose.ui.geometry.Size(140.dp.toPx(), 140.dp.toPx()), style = androidx.compose.ui.graphics.drawscope.Stroke(1.dp.toPx()))
                }
                Column(Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.AutoAwesome, null, tint = Cyan, modifier = Modifier.size(18.dp))
                        Text("  PERSONAL SPACE", color = Cyan, style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
                        TextButton(onClick = { archived = !archived }) { Text(if (archived) "Back" else "Archive") }
                    }
                    Spacer(Modifier.height(18.dp))
                    Text(if (archived) "Archived templates" else "Your templates", color = Ice, fontFamily = FontFamily.Serif, fontSize = 37.sp, lineHeight = 42.sp, letterSpacing = (-1).sp)
                    Spacer(Modifier.height(8.dp))
                    Text("A little structure. More room to grow.", color = Muted, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(20.dp))
                    Text(LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d MMMM")), color = Cyan, style = MaterialTheme.typography.labelSmall)
                }
            }
            Column(Modifier.padding(horizontal = 24.dp, vertical = 18.dp)) {
                TextField(query, { query = it }, placeholder = { Text("Search templates and contents") }, leadingIcon = { Icon(Icons.Rounded.Search, null, Modifier.size(20.dp)) }, singleLine = true, modifier = Modifier.fillMaxWidth(), colors = TextFieldDefaults.colors(unfocusedContainerColor = Color.Transparent, focusedContainerColor = Color.Transparent, unfocusedIndicatorColor = Divider))
                if (!archived) FilledTonalButton(onClick = onCreate, modifier = Modifier.fillMaxWidth().padding(top = 12.dp).testTag("new-template")) { Icon(Icons.Rounded.Add, null, Modifier.size(18.dp)); Text(" New template") }
                Spacer(Modifier.height(20.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(if (archived) "ARCHIVE" else "MY PAGES", color = Muted, style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
                    Text("${matchingPages.size + matchingTrackers.size} templates", color = Muted, style = MaterialTheme.typography.labelSmall)
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        }
        items(matchingPages, key = { it.id }) { page ->
            Row(Modifier.fillMaxWidth().clickable { onPage(page.id) }.padding(horizontal = 24.dp, vertical = 17.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.width(70.dp).height(88.dp)) {
                    PageCover(page.cover, Modifier.fillMaxSize().clip(RoundedCornerShape(5.dp)))
                    Box(Modifier.align(Alignment.BottomStart).offset(x = (-5).dp, y = 5.dp).size(30.dp).background(pageWash(page.kind), RoundedCornerShape(5.dp)), contentAlignment = Alignment.Center) { Text(pageIcon(page.kind), fontSize = 19.sp, color = accent(page.kind)) }
                }
                Spacer(Modifier.width(19.dp))
                Column(Modifier.weight(1f)) {
                    Text(pageCategory(page.kind), color = accent(page.kind), style = MaterialTheme.typography.labelSmall)
                    Spacer(Modifier.height(5.dp))
                    Text(page.title, color = Ice, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(6.dp))
                    Text(page.subtitle, color = Muted, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                TemplateRowMenu(page.title, { onEdit(page.id, false) }, { onDelete(page.id, page.title, false) })
            }
            HorizontalDivider(color = Divider, modifier = Modifier.padding(horizontal = 24.dp))
        }
        items(matchingTrackers, key = { "custom-${it.id}" }) { item -> Row(Modifier.padding(horizontal = 24.dp), verticalAlignment = Alignment.CenterVertically) { Box(Modifier.weight(1f)) { WorkspaceLine(item.title, "${trackers.entriesFor(item.id).size} entries · Personal template") { onCustom(item.id) } }; TemplateRowMenu(item.title, { onEdit(item.id, true) }, { onDelete(item.id, item.title, true) }) } }
        if (matchingPages.isEmpty() && matchingTrackers.isEmpty()) item {
            Text(if (query.isNotBlank()) "No matches. Try a template name, checklist item, or record value." else if (archived) "Nothing archived." else "Create your first template to start organizing your space.", color = Muted, modifier = Modifier.padding(24.dp))
        }
    }
}

@Composable
internal fun TemplatePageScreen(page: TemplatePageEntity, actions: TemplateActions, insets: PaddingValues, onBack: () -> Unit, onCopy: (String) -> Unit, initiallyEditing: Boolean = false, currencySymbol: String = "\$", onCurrencySymbol: (String) -> Unit = {}) {
    var document by remember(page.id) { mutableStateOf(TemplateDocumentCache.peek(page.id, page.data)) }
    var pending by remember(page.id) { mutableIntStateOf(0) }
    val writeLock = remember(page.id) { Mutex() }
    LaunchedEffect(page.data) {
        if (pending == 0) document = withContext(Dispatchers.Default) { TemplateDocumentCache.getOrParse(page.id, page.data) }
    }
    val doc = document
    if (doc == null) { Box(Modifier.fillMaxSize().padding(insets), contentAlignment = Alignment.Center) { CircularProgressIndicator() }; return }
    var editMode by rememberSaveable(page.id) { mutableStateOf(initiallyEditing) }
    var deleting by remember { mutableStateOf(false) }
    var menu by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var rowTable by rememberSaveable { mutableStateOf<String?>(null) }
    var rowId by rememberSaveable { mutableStateOf<String?>(null) }
    var tableId by rememberSaveable { mutableStateOf<String?>(null) }
    var fieldTable by rememberSaveable { mutableStateOf<String?>(null) }
    var textBlock by remember { mutableStateOf<PageBlock?>(null) }
    var creatingSection by remember { mutableStateOf(false) }
    var creatingDatabase by remember { mutableStateOf(false) }
    var sectionParentId by remember { mutableStateOf<String?>(null) }
    var deleteBlock by remember { mutableStateOf<PageBlock?>(null) }
    var rename by rememberSaveable { mutableStateOf(false) }
    var currencyEditor by rememberSaveable { mutableStateOf(false) }
    val drag = remember(page.id) { WorkspaceDragState() }
    val pageScroll = rememberLazyListState()
    var pageBounds by remember { mutableStateOf(Rect.Zero) }
    WorkspaceDragAutoScroll(drag, pageBounds) { pageScroll.scrollBy(it) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")) { uri ->
        val snapshot = document
        if (uri != null && snapshot != null && !busy) scope.launch {
            busy = true; error = null
            try {
                withContext(Dispatchers.IO) {
                    val output = requireNotNull(context.contentResolver.openOutputStream(uri, "w")) { "Could not open the selected file" }
                    output.use { TemplateWorkbookExporter.write(page.title, snapshot, currencySymbol, it) }
                }
                Toast.makeText(context, "Excel workbook saved", Toast.LENGTH_SHORT).show()
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { error = e.message ?: "Could not export the workbook" }
            finally { busy = false }
        }
    }
    val run: (suspend () -> Unit) -> Unit = { task -> if (!busy) scope.launch {
        busy = true; error = null
        try { task() } catch (e: CancellationException) { throw e } catch (e: Exception) { error = e.message ?: "Could not save. Try again." } finally { busy = false }
    } }
    val change: ((TemplateDocument) -> TemplateDocument) -> Unit = { transform ->
        document = transform(requireNotNull(document)); pending++
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            try { withContext(NonCancellable) { writeLock.withLock { actions.change(page.id, transform) } } }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { error = "Could not save: ${e.message}. Please retry." }
            finally { pending--; if (pending == 0) TemplateDocumentCache.latest(page.id)?.let { document = it } }
        }
    }
    val openRow: (String, String?) -> Unit = { table, row -> rowTable = table; rowId = row }
    val toggle: (String, String, String) -> Unit = { table, row, field -> change { current -> current.changeTable(table) { t ->
        val existing = t.rows.find { it.id == row }
        when {
            existing != null -> t.copy(rows = t.rows.map { r -> if (r.id == row) r.copy(values = r.values + (field to (r.values[field] != "true").toString())) else r })
            row.startsWith("new@") -> t.copy(rows = t.rows + PageRow(UUID.randomUUID().toString(), "", mapOf("Date" to row.removePrefix("new@"), field to "true")))
            else -> t
        }
    } } }
    BackHandler { onBack() }
    CompositionLocalProvider(LocalWorkspaceDrag provides drag) {
    Column(Modifier.fillMaxSize().background(Paper).padding(insets)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back to templates", tint = Ice) }
            Text("My pages  /", color = Muted, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
            FilledTonalButton(onClick = { editMode = !editMode }, enabled = !busy, contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)) {
                Icon(if (editMode) Icons.Rounded.Done else Icons.Rounded.Edit, null, Modifier.size(17.dp))
                Spacer(Modifier.width(6.dp))
                Text(if (editMode) "Done" else "Edit")
            }
            Box {
                IconButton(onClick = { menu = true }) { Icon(Icons.Rounded.MoreVert, "Template actions") }
                DropdownMenu(menu, { menu = false }) {
                    DropdownMenuItem(text = { Text("Rename template") }, onClick = { menu = false; rename = true })
                    DropdownMenuItem(text = { Text("Duplicate template") }, onClick = { menu = false; run { onCopy(actions.duplicate(page.id)) } })
                    DropdownMenuItem(text = { Text("Export Excel workbook") }, onClick = { menu = false; exportLauncher.launch(exportFileName(page.title)) })
                    DropdownMenuItem(text = { Text("Currency symbol") }, onClick = { menu = false; currencyEditor = true })
                    DropdownMenuItem(text = { Text("Delete template") }, onClick = { menu = false; deleting = true })
                }
            }
        }
        HorizontalDivider(color = Divider.copy(alpha = .6f))
        if (busy) LinearProgressIndicator(Modifier.fillMaxWidth(), color = accent(page.kind))
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp)) }
        LazyColumn(Modifier.weight(1f).onGloballyPositioned { pageBounds = it.boundsInRoot() }.testTag("template-page"), state = pageScroll, contentPadding = PaddingValues(bottom = 32.dp)) {
            item {
                Box(Modifier.fillMaxWidth().height(if (page.cover.isBlank()) 98.dp else 156.dp)) {
                    PageCover(page.cover, Modifier.fillMaxWidth().height(if (page.cover.isBlank()) 74.dp else 132.dp))
                    Box(Modifier.align(Alignment.BottomStart).padding(start = 24.dp).size(56.dp).background(pageWash(page.kind), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) { Text(pageIcon(page.kind), color = accent(page.kind), fontSize = 30.sp, fontWeight = FontWeight.Bold) }
                }
                Column(Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
                    Text(page.title, color = Ice, style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold))
                    Spacer(Modifier.height(7.dp))
                    Text(page.subtitle, color = Muted, style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(15.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(6.dp).background(accent(page.kind), RoundedCornerShape(3.dp))); Text("  ${pageCategory(page.kind)}", color = accent(page.kind), style = MaterialTheme.typography.labelSmall) }
                    if (editMode) {
                        Row(Modifier.horizontalScroll(rememberScrollState())) {
                            TextButton(onClick = { rename = true }) { Text("Rename") }
                            TextButton(onClick = { run { onCopy(actions.duplicate(page.id)) } }, enabled = !busy) { Text("Duplicate") }
                            TextButton(onClick = { exportLauncher.launch(exportFileName(page.title)) }, enabled = !busy) { Text("Export Excel") }
                            TextButton(onClick = { currencyEditor = true }, enabled = !busy) { Text("Currency: $currencySymbol") }
                            TextButton(onClick = { run { actions.archive(page.id, !page.archived); onBack() } }, enabled = !busy) { Text(if (page.archived) "Restore" else "Archive") }
                        }
                        Surface(color = accent(page.kind).copy(alpha = .10f), shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
                            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.TouchApp, null, tint = accent(page.kind), modifier = Modifier.size(19.dp))
                                Spacer(Modifier.width(9.dp))
                                Text("Hold ⋮⋮ to drag sections. Drop above, below, or inside a group. Section menus offer more actions.", color = Ice, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
            itemsIndexed(doc.blocks.filter { editMode || !it.hidden }, key = { _, item -> item.id }) { index, block ->
                Column(Modifier.padding(horizontal = 24.dp, vertical = 8.dp).workspaceDropTarget(if (editMode) drag else null, block.id, "block") { source, after ->
                    change { it.relocateBlock(source, block.id, if (after) BlockPlacement.AFTER else BlockPlacement.BEFORE) }
                }) {
                    if (editMode) SectionControl(
                        block = block,
                        canMoveUp = index > 0,
                        canMoveDown = index < doc.blocks.lastIndex,
                        busy = busy,
                        onEdit = { textBlock = block },
                        onAddChild = if (block.kind == "GROUP") ({ sectionParentId = block.id; creatingSection = true }) else null,
                        onMove = { offset -> change { it.moveBlock(block.id, offset) } },
                        onVisibility = { change { it.changeBlock(block.id) { item -> item.copy(hidden = !item.hidden) } } },
                        onDelete = { deleteBlock = block },
                    )
                    if (!block.hidden) NativeBlock(block, doc, accent(page.kind), currencySymbol, editMode, busy, change, openRow, toggle, { tableId = it }, { fieldTable = it }, { textBlock = it }, { deleteBlock = it }, { parentId -> sectionParentId = parentId; creatingSection = true })
                    else Text("Hidden section · ${block.text}", color = Muted, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(vertical = 12.dp))
                }
            }
            if (editMode) item {
                Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { sectionParentId = null; creatingSection = true }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Rounded.Add, null)
                        Spacer(Modifier.width(6.dp))
                        Text("Add section")
                    }
                    OutlinedButton(onClick = { creatingDatabase = true }, modifier = Modifier.weight(1f).testTag("add-database")) {
                        Icon(Icons.Rounded.TableChart, null)
                        Spacer(Modifier.width(6.dp))
                        Text("Database")
                    }
                }
                Text("Imported ${page.importedAt} · Saved on this device", color = Muted, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(24.dp))
            }
        }
    }
    }
    if (deleting) ConfirmDelete("template", "This removes “${page.title}” and all its local records. It cannot be undone. The Notion original stays unchanged.", busy, { deleting = false }, { run { actions.delete(page.id); onBack() } })
    deleteBlock?.let { block -> ConfirmDelete("section", "This permanently removes “${block.text}”${if (block.children.isNotEmpty()) " and its ${block.children.size} nested items" else ""}.", busy, { deleteBlock = null }, {
        change { it.removeBlock(block.id) }
        deleteBlock = null
    }) }
    tableId?.let { id -> RecordBrowser(doc, id, busy || pending > 0, { tableId = null }, openRow, { fieldTable = id },
        onDelete = { selected -> actions.deleteRows(page.id, id, selected) },
        onArchive = { selected, archived -> actions.change(page.id) { it.archiveRows(id, selected, archived) } },
        onDuplicate = { row -> actions.saveRow(page.id, id, row.copy(id = UUID.randomUUID().toString(), title = "${row.displayTitle()} copy", archived = false)) },
    ) }
    rowTable?.let { id ->
        val table = doc.table(id)
        val existing = table.rows.find { it.id == rowId }
        val requestedDate = rowId?.takeIf { it.startsWith("new@") }?.removePrefix("new@")
        val draftId = rememberSaveable(id, rowId) { existing?.id ?: UUID.randomUUID().toString() }
        val row = remember(id, draftId) { existing ?: PageRow(draftId, "", table.fields.filter { it.kind == "DATE" }.associate { it.id to (requestedDate ?: LocalDate.now().toString()) }) }
        NativeRowEditor(doc, table, row, busy, error, { rowTable = null }, { value -> run { actions.saveRow(page.id, id, value); rowTable = null } },
            onDelete = if (existing == null) null else ({ run { actions.deleteRow(page.id, id, existing.id); rowTable = null } }))
    }
    fieldTable?.let { id -> NativeFieldManager(doc.table(id), busy, error, { fieldTable = null }) { fields -> run { actions.change(page.id) { it.changeTable(id) { t -> t.copy(fields = fields) } }; fieldTable = null } } }
    textBlock?.let { block -> SectionEditor(block, false, busy, error, { textBlock = null }) { updated ->
        change { current ->
            val edited = current.changeBlock(block.id) { updated }
            if (updated.kind == "CALENDAR" && edited.table(updated.table).fields.none { it.kind == "DATE" && !it.hidden })
                edited.changeTable(updated.table) { table -> table.copy(fields = table.fields + PageField(UUID.randomUUID().toString(), "Date", "DATE")) }
            else edited
        }
        textBlock = null
    } }
    if (creatingSection) {
        val draft = remember(creatingSection, sectionParentId) { PageBlock(UUID.randomUUID().toString(), "TEXT", "") }
        SectionEditor(draft, true, busy, error, { creatingSection = false; sectionParentId = null }) { created ->
            val parent = sectionParentId
            change { it.addBlock(created, parent) }
            creatingSection = false
            sectionParentId = null
        }
    }
    if (creatingDatabase) NativeDatabaseCreator(busy, error, { creatingDatabase = false }) { name, layout ->
        val table = UUID.randomUUID().toString()
        val fields = when (layout) {
            "board" -> listOf(PageField("$table-status", "Status", "CHOICE", listOf("Not started", "In progress", "Done")))
            "calendar" -> listOf(PageField("Date", "Date", "DATE"))
            else -> emptyList()
        }
        val block = PageBlock(UUID.randomUUID().toString(), if (layout == "calendar") "CALENDAR" else "TABLE", name, table = table, view = if (layout == "table") "matrix" else layout)
        change { it.copy(tables = it.tables + PageTable(table, name, fields, emptyList())).addBlock(block) }
        creatingDatabase = false
    }
    if (rename) {
        var title by rememberSaveable { mutableStateOf(page.title) }
        var subtitle by rememberSaveable { mutableStateOf(page.subtitle) }
        Editor("Rename template", busy, error, { rename = false }) {
            OutlinedTextField(title, { title = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(subtitle, { subtitle = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth())
            Button(onClick = { run { actions.rename(page.id, title, subtitle); rename = false } }, enabled = !busy && title.isNotBlank()) { Text("Save template") }
        }
    }
    if (currencyEditor) {
        var symbol by rememberSaveable(currencyEditor) { mutableStateOf(currencySymbol) }
        Editor("Currency symbol", false, null, { currencyEditor = false }) {
            OutlinedTextField(symbol, { symbol = it.take(4) }, label = { Text("Symbol") }, supportingText = { Text("Used in finance totals and Excel exports") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("\$", "₹", "€", "£", "¥").forEach { option -> FilterChip(symbol == option, { symbol = option }, label = { Text(option) }) }
            }
            Button(onClick = { onCurrencySymbol(symbol.trim()); currencyEditor = false }, enabled = symbol.isNotBlank()) { Text("Save currency") }
        }
    }
}

private fun exportFileName(title: String): String = title.replace(Regex("[^A-Za-z0-9._ -]"), "").trim().ifBlank { "template-records" } + "-${LocalDate.now()}.xlsx"

@Composable
private fun SectionControl(
    block: PageBlock,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    busy: Boolean,
    compact: Boolean = false,
    onEdit: () -> Unit,
    onAddChild: (() -> Unit)? = null,
    onMove: (Int) -> Unit,
    onVisibility: () -> Unit,
    onDelete: () -> Unit,
) {
    var menu by remember(block.id) { mutableStateOf(false) }
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth().padding(bottom = if (compact) 3.dp else 7.dp),
    ) {
        Row(Modifier.heightIn(min = 42.dp).padding(start = 10.dp, end = 2.dp), verticalAlignment = Alignment.CenterVertically) {
            WorkspaceDragHandle(block.id, "block", "section ${block.text}", !busy)
            Spacer(Modifier.width(7.dp))
            Text(block.kind.lowercase().replaceFirstChar(Char::uppercase), color = Muted, style = MaterialTheme.typography.labelSmall)
            if (!compact) Text("  ${block.text}", color = Ice, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            else Spacer(Modifier.weight(1f))
            IconButton(onClick = onEdit, enabled = !busy, modifier = Modifier.size(40.dp)) { Icon(Icons.Rounded.Edit, "Edit ${block.text}", tint = Ice, modifier = Modifier.size(18.dp)) }
            Box {
                IconButton(onClick = { menu = true }, enabled = !busy, modifier = Modifier.size(40.dp)) { Icon(Icons.Rounded.MoreHoriz, "Section actions for ${block.text}", tint = Ice) }
                DropdownMenu(menu, { menu = false }) {
                    DropdownMenuItem(text = { Text("Edit section") }, leadingIcon = { Icon(Icons.Rounded.Edit, null) }, onClick = { menu = false; onEdit() })
                    if (onAddChild != null) DropdownMenuItem(text = { Text("Add inside") }, leadingIcon = { Icon(Icons.Rounded.Add, null) }, onClick = { menu = false; onAddChild() })
                    DropdownMenuItem(text = { Text("Move up") }, leadingIcon = { Icon(Icons.Rounded.KeyboardArrowUp, null) }, enabled = canMoveUp, onClick = { menu = false; onMove(-1) })
                    DropdownMenuItem(text = { Text("Move down") }, leadingIcon = { Icon(Icons.Rounded.KeyboardArrowDown, null) }, enabled = canMoveDown, onClick = { menu = false; onMove(1) })
                    DropdownMenuItem(text = { Text(if (block.hidden) "Restore section" else "Hide section") }, leadingIcon = { Icon(if (block.hidden) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff, null) }, onClick = { menu = false; onVisibility() })
                    HorizontalDivider()
                    DropdownMenuItem(text = { Text("Delete section", color = MaterialTheme.colorScheme.error) }, leadingIcon = { Icon(Icons.Rounded.DeleteOutline, null, tint = MaterialTheme.colorScheme.error) }, onClick = { menu = false; onDelete() })
                }
            }
        }
    }
}

@Composable
private fun SectionEditor(block: PageBlock, creating: Boolean, busy: Boolean, error: String?, onDismiss: () -> Unit, onSave: (PageBlock) -> Unit) {
    val standardKinds = listOf("HEADING", "TEXT", "QUOTE", "CHECK", "GROUP")
    var text by rememberSaveable(block.id) { mutableStateOf(block.text) }
    var kind by rememberSaveable(block.id) { mutableStateOf(if (block.kind in standardKinds) block.kind else block.kind) }
    var view by rememberSaveable(block.id) { mutableStateOf(block.view) }
    Editor(if (creating) "Add section" else "Edit section", busy, error, onDismiss, footer = {
        HorizontalDivider(color = Divider)
        Button(onClick = { onSave(block.copy(kind = kind, view = view, text = text.trim())) }, enabled = !busy && text.isNotBlank(), modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
            Text(if (creating) "Add to page" else "Save changes")
        }
    }) {
        if (block.kind in listOf("TABLE", "CALENDAR")) {
            Text("Database view", color = Muted)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("matrix" to "Table", "list" to "List", "board" to "Board", "calendar" to "Calendar").forEach { (value, label) ->
                    FilterChip(if (value == "calendar") kind == "CALENDAR" else kind == "TABLE" && view == value,
                        { kind = if (value == "calendar") "CALENDAR" else "TABLE"; view = value }, label = { Text(label) })
                }
            }
        }
        if (creating || block.kind in standardKinds) {
            Text("Section type", color = Muted, style = MaterialTheme.typography.labelMedium)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                standardKinds.forEach { option ->
                    FilterChip(kind == option, { kind = option }, label = { Text(when (option) { "CHECK" -> "Checklist"; "GROUP" -> "Collapsible"; else -> option.lowercase().replaceFirstChar(Char::uppercase) }) })
                }
            }
        } else {
            Text("${block.kind.lowercase().replaceFirstChar(Char::uppercase)} section", color = Muted, style = MaterialTheme.typography.labelMedium)
        }
        OutlinedTextField(text, { text = it }, label = { Text(if (kind == "CHECK") "Checklist item" else "Section content") }, modifier = Modifier.fillMaxWidth(), minLines = if (kind in listOf("TEXT", "QUOTE")) 4 else 1)
        if (kind == "GROUP") Text("A collapsible section can contain checklist and text items.", color = Muted, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun NativeBlock(block: PageBlock, doc: TemplateDocument, color: Color, currencySymbol: String, editing: Boolean, busy: Boolean, change: ((TemplateDocument) -> TemplateDocument) -> Unit, openRow: (String, String?) -> Unit, toggle: (String, String, String) -> Unit, openTable: (String) -> Unit, fields: (String) -> Unit, editText: (PageBlock) -> Unit, deleteBlock: (PageBlock) -> Unit, addChild: (String) -> Unit) {
    val drag = LocalWorkspaceDrag.current
    when (block.kind) {
        "HEADING" -> Text(block.text, color = Ice, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
        "QUOTE" -> Row(Modifier.fillMaxWidth().background(color.copy(alpha = .07f), RoundedCornerShape(5.dp)).padding(14.dp)) {
            Text("❝", color = color, fontSize = 26.sp, modifier = Modifier.padding(end = 12.dp))
            Text(block.text, color = Ice.copy(alpha = .85f), style = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Serif, fontStyle = FontStyle.Italic))
        }
        "TEXT" -> Text(block.text, color = Muted, style = MaterialTheme.typography.bodyMedium)
        "CHECK" -> Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(block.checked, { change { it.changeBlock(block.id) { b -> b.copy(checked = !b.checked) } } }, enabled = !busy, modifier = Modifier.semantics { contentDescription = block.text })
            Text(block.text, color = if (block.checked) Muted else Ice, modifier = Modifier.weight(1f))
            if (editing) IconButton(onClick = { editText(block) }) { Icon(Icons.Rounded.Edit, "Edit checklist item", tint = Muted) }
        }
        "GROUP", "TOGGLE" -> {
            var expanded by rememberSaveable(block.id) { mutableStateOf(block.kind == "GROUP" && block.children.size <= 8 && (!block.text.contains("DAY 0") || block.text.contains(LocalDate.now().dayOfWeek.name))) }
            Row(Modifier.fillMaxWidth().clickable { expanded = !expanded }.padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                val visibleChecks = block.children.filter { it.kind == "CHECK" && !it.hidden }
                if (visibleChecks.isNotEmpty()) ProgressRing(visibleChecks.count { it.checked }.toFloat() / visibleChecks.size, color, "${block.text} completion", Modifier.padding(end = 12.dp))
                Column(Modifier.weight(1f)) {
                    Text(block.text, color = Ice, style = MaterialTheme.typography.titleMedium)
                    val checks = block.children.filter { it.kind == "CHECK" }
                    if (checks.isNotEmpty()) Text("${checks.count { it.checked }} / ${checks.size} complete", color = color, style = MaterialTheme.typography.labelSmall)
                }
                Icon(if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, null, tint = Muted)
            }
            if (editing) {
                Text(if (drag?.category == "block") "Drop inside ${block.text}" else "Drag here to nest a section", color = color, modifier = Modifier.fillMaxWidth().workspaceDropTarget(if (drag?.source == block.id) null else drag, "inside:${block.id}", "block") { source, _ ->
                    change { it.relocateBlock(source, block.id, BlockPlacement.INSIDE) }
                    expanded = true
                }.padding(18.dp))
            }
            if (expanded) block.children.filter { editing || !it.hidden }.forEachIndexed { index, child ->
                key(child.id) {
                Column(Modifier.padding(start = 10.dp).workspaceDropTarget(if (editing) drag else null, child.id, "block") { source, after ->
                    change { it.relocateBlock(source, child.id, if (after) BlockPlacement.AFTER else BlockPlacement.BEFORE) }
                }) {
                    if (editing) SectionControl(
                        block = child,
                        canMoveUp = index > 0,
                        canMoveDown = index < block.children.lastIndex,
                        busy = busy,
                        compact = true,
                        onEdit = { editText(child) },
                        onAddChild = if (child.kind == "GROUP") ({ addChild(child.id) }) else null,
                        onMove = { offset -> change { it.moveBlock(child.id, offset) } },
                        onVisibility = { change { it.changeBlock(child.id) { item -> item.copy(hidden = !item.hidden) } } },
                        onDelete = { deleteBlock(child) },
                    )
                    if (!child.hidden) NativeBlock(child, doc, color, currencySymbol, editing, busy, change, openRow, toggle, openTable, fields, editText, deleteBlock, addChild)
                    else Text("Hidden · ${child.text}", color = Muted, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(10.dp))
                }
                }
            }
            HorizontalDivider(color = Divider.copy(alpha = .6f))
        }
        "DAILY" -> DailyQuest(block, doc, color, busy, change, openRow, toggle)
        "CALENDAR" -> MonthCalendar(block, doc, color, openRow, openTable, fields)
        "TABLE" -> EmbeddedTable(block, doc, color, currencySymbol, busy, change, openRow, toggle, openTable, fields)
        "QUICK_ADD" -> { Text(block.text, color = Ice, style = MaterialTheme.typography.titleMedium); Row { TextButton(onClick = { openRow("expenses", null) }) { Text("+ Expense") }; TextButton(onClick = { openRow("incomes", null) }) { Text("+ Income") } } }
        "SUMMARY" -> {
            var monthText by rememberSaveable(block.id) { mutableStateOf(YearMonth.now().toString()) }
            val month = YearMonth.parse(monthText)
            PeriodHeading(block.text, month.toString(), { monthText = month.minusMonths(1).toString() }, { monthText = month.plusMonths(1).toString() })
            val income = doc.total("incomes", month); val expense = doc.total("expenses", month)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { MoneyStat("Income", income.toPlainString(), currencySymbol, color); MoneyStat("Expenses", expense.toPlainString(), currencySymbol, Muted); MoneyStat("Net", (income - expense).toPlainString(), currencySymbol, Ice) }
            HorizontalDivider(color = Divider, modifier = Modifier.padding(top = 20.dp, bottom = 8.dp))
        }
        "INDEX" -> { Text(block.text, color = Ice, style = MaterialTheme.typography.titleLarge); doc.tables.forEach { table -> WorkspaceLine(table.title, "${table.rows.count { !it.archived }} records") { openTable(table.id) } } }
    }
}

@Composable private fun MoneyStat(label: String, amount: String, currencySymbol: String, color: Color) { Column { Text(label, color = Muted, style = MaterialTheme.typography.labelSmall); Spacer(Modifier.height(6.dp)); Text("$currencySymbol$amount", color = color, fontFamily = FontFamily.Serif, fontSize = 24.sp) } }

@Composable private fun PeriodHeading(title: String, period: String, previous: () -> Unit, next: () -> Unit) {
    Text(title, color = Ice, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 12.dp))
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(period, color = Muted, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
        IconButton(onClick = previous) { Icon(Icons.Rounded.ChevronLeft, "Previous period", tint = Muted) }
        IconButton(onClick = next) { Icon(Icons.Rounded.ChevronRight, "Next period", tint = Muted) }
    }
}

@Composable private fun DailyQuest(block: PageBlock, doc: TemplateDocument, color: Color, busy: Boolean, change: ((TemplateDocument) -> TemplateDocument) -> Unit, openRow: (String, String?) -> Unit, toggle: (String, String, String) -> Unit) {
    var dayText by rememberSaveable(block.id) { mutableStateOf(LocalDate.now().toString()) }
    val day = LocalDate.parse(dayText); val table = doc.table(block.table)
    val row = table.activeByDate[day]?.firstOrNull()
    PeriodHeading(block.text, day.format(DateTimeFormatter.ofPattern("EEEE, d MMM")), { dayText = day.minusDays(1).toString() }, { dayText = day.plusDays(1).toString() })
    val progress = row?.let { doc.completion(it, table) } ?: 0f
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        ProgressRing(progress, color, "Daily quest progress", Modifier.padding(end = 12.dp).testTag("daily-progress-ring"))
        Text("${(progress * 100).toInt()}% complete", color = color, style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
        TextButton(onClick = { openRow(table.id, row?.id ?: "new@$day") }, modifier = Modifier.testTag("daily-quest-edit")) { Text(if (row == null) "Edit day" else "Edit") }
    }
    Spacer(Modifier.height(12.dp))
    table.fields.filter { it.kind == "CHECKBOX" && !it.hidden }.forEach { field ->
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(row?.values?.get(field.id) == "true", {
                if (row != null) toggle(table.id, row.id, field.id)
                else { val newId = UUID.randomUUID().toString(); change { current -> current.changeTable(table.id) { t ->
                    val existing = t.rows.firstOrNull { !it.archived && it.date() == day }
                    if (existing != null) t.copy(rows = t.rows.map { if (it.id == existing.id) it.copy(values = it.values + (field.id to "true")) else it })
                    else t.copy(rows = t.rows + PageRow(newId, "", mapOf("Date" to day.toString(), field.id to "true")))
                } } }
            }, enabled = !busy, modifier = Modifier.semantics { contentDescription = "Daily quest: ${field.label}" })
            Text(field.label, color = Ice, modifier = Modifier.weight(1f))
        }
    }
}

@Composable private fun MonthCalendar(block: PageBlock, doc: TemplateDocument, color: Color, openRow: (String, String?) -> Unit, openTable: (String) -> Unit, fields: (String) -> Unit) {
    var monthText by rememberSaveable(block.id) { mutableStateOf(YearMonth.now().toString()) }
    var selectedText by rememberSaveable(block.id) { mutableStateOf(LocalDate.now().toString()) }
    val month = YearMonth.parse(monthText)
    val selected = LocalDate.parse(selectedText)
    val table = doc.table(block.table)
    val byDate = remember(table) { table.rows.filter { !it.archived }.groupBy { table.dateOf(it) } }
    PeriodHeading(block.text, month.format(DateTimeFormatter.ofPattern("MMMM yyyy")), {
        monthText = month.minusMonths(1).toString(); selectedText = month.minusMonths(1).atDay(1).toString()
    }, {
        monthText = month.plusMonths(1).toString(); selectedText = month.plusMonths(1).atDay(1).toString()
    })
    Row { listOf("M", "T", "W", "T", "F", "S", "S").forEach { Text(it, Modifier.weight(1f), color = Muted, style = MaterialTheme.typography.labelSmall) } }
    val offset = month.atDay(1).dayOfWeek.value - 1
    (0 until (offset + month.lengthOfMonth() + 6) / 7).forEach { week -> Row {
        (0..6).forEach { weekday ->
            val date = week * 7 + weekday - offset + 1
            val day = if (date in 1..month.lengthOfMonth()) month.atDay(date) else null
            val records = byDate[day].orEmpty()
            Column(Modifier.weight(1f).height(62.dp)
                .background(if (day == selected) color.copy(alpha = .15f) else Color.Transparent).tableRule()
                .then(if (day != null) Modifier.clickable {
                    selectedText = day.toString()
                    if (records.isEmpty()) openRow(table.id, "new@$day")
                }.semantics { contentDescription = "Calendar $day, ${records.size} records" } else Modifier)
                .padding(start = 6.dp), verticalArrangement = Arrangement.Center) {
                Text(if (day == null) "" else date.toString(), color = if (day == LocalDate.now()) color else Ice, style = MaterialTheme.typography.bodySmall)
                if (day != null && records.isNotEmpty()) Text("${records.size} ${if (records.size == 1) "entry" else "entries"}", color = color, style = MaterialTheme.typography.labelSmall, maxLines = 1)
            }
        }
    } }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(selected.format(DateTimeFormatter.ofPattern("EEE, d MMM")), modifier = Modifier.weight(1f), color = Muted)
        TextButton(onClick = { val today = LocalDate.now(); monthText = YearMonth.from(today).toString(); selectedText = today.toString() }) { Text("Today") }
    }
    byDate[selected].orEmpty().forEach { row ->
        WorkspaceLine(row.displayTitle(), table.fields.filter { !it.hidden && it.kind != "DATE" }.take(2).joinToString(" · ") { doc.displayValue(table, row, it, month) }.ifBlank { "Tap to edit" }) { openRow(table.id, row.id) }
    }
    if (byDate[selected].isNullOrEmpty()) Text("No entries on this day. Tap a day to add one.", color = Muted, style = MaterialTheme.typography.bodySmall)
    Row(Modifier.horizontalScroll(rememberScrollState())) {
        TextButton(onClick = { openRow(table.id, "new@$selected") }) { Text("+ Add entry") }
        TextButton(onClick = { openTable(table.id) }) { Text("All ${table.rows.count { !it.archived }} records") }
        TextButton(onClick = { fields(table.id) }) { Text("Fields") }
    }
}

@Composable private fun EmbeddedTable(block: PageBlock, doc: TemplateDocument, color: Color, currencySymbol: String, busy: Boolean, change: ((TemplateDocument) -> TemplateDocument) -> Unit, openRow: (String, String?) -> Unit, toggle: (String, String, String) -> Unit, openTable: (String) -> Unit, fields: (String) -> Unit) {
    val table = doc.table(block.table)
    var periodText by rememberSaveable(block.id) { mutableStateOf(LocalDate.now().toString()) }
    val day = LocalDate.parse(periodText); val month = YearMonth.from(day)
    val weekStart = day.minusDays((day.dayOfWeek.value - 1).toLong())
    if (block.view in listOf("week", "month", "budget")) PeriodHeading(block.text, if (block.view == "week") "Week of $weekStart" else month.toString(), { periodText = (if (block.view == "week") day.minusWeeks(1) else day.minusMonths(1)).toString() }, { periodText = (if (block.view == "week") day.plusWeeks(1) else day.plusMonths(1)).toString() })
    else Text(block.text, color = Ice, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 12.dp, bottom = 12.dp))
    val active = table.rows.filter { !it.archived }
    val all = if (table.fields.any { it.id == "Date" }) active.sortedBy { it.date()?.toString() ?: it.title } else active
    val rows = when (block.view) {
        "week" -> (0L..6L).map { offset ->
            val date = weekStart.plusDays(offset)
            table.activeByDate[date]?.firstOrNull() ?: PageRow("new@$date", "", mapOf("Date" to date.toString()))
        }
        "month" -> all.filter { table.dateOf(it)?.let(YearMonth::from) == month }
        else -> all
    }
    val shown = rows.take(if (block.view in listOf("matrix", "budget")) 20 else 7)
    if (block.view == "matrix" || block.view == "week") NativeGrid(doc, table, shown, month, busy, openRow, toggle)
    else if (block.view == "board") {
        WorkspaceBoard(table, busy, { openRow(table.id, it) }) { row, field, status -> change { it.setBoardStatus(table.id, row, field, status) } }
    }
    else shown.forEach { row ->
        val summary = when (block.view) {
            "budget" -> "$currencySymbol${doc.budgetSpent(row, month)} spent · $currencySymbol${doc.amount(row, "Monthly Budget")} budget"
            "overview" -> doc.computed(table, row, PageField("Monthly Average", "Progress", "FORMULA"), month) + " average"
            "summary" -> "Income $currencySymbol${doc.computed(table, row, PageField("Monthly Income", "", "FORMULA"), month)} · Expenses $currencySymbol${doc.computed(table, row, PageField("Monthly Expenses", "", "FORMULA"), month)}"
            else -> table.fields.filter { !it.hidden && it.kind !in listOf("RELATION", "FORMULA") }.take(3).joinToString(" · ") { doc.displayValue(table, row, it, month) }
        }
        if (block.view in listOf("budget", "overview")) {
            val percent = if (block.view == "budget") doc.computed(table, row, PageField("Utilization", "", "FORMULA"), month)
                else doc.computed(table, row, PageField("Monthly Average", "", "FORMULA"), month)
            val ratio = percent.removeSuffix("%").toFloatOrNull()?.div(100f) ?: 0f
            Row(Modifier.fillMaxWidth().clickable { openRow(table.id, row.id) }.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                ProgressRing(ratio, if (ratio > 1f) MaterialTheme.colorScheme.error else color, "${row.displayTitle()} progress", valueText = if (percent.endsWith("%")) percent else "—")
                Column(Modifier.weight(1f).padding(start = 14.dp)) {
                    Text(row.displayTitle(), color = Ice, style = MaterialTheme.typography.titleMedium)
                    Text(summary, color = Muted, style = MaterialTheme.typography.bodySmall)
                    if (block.view == "budget") Text(if (percent.endsWith("%")) "$percent used" else percent, color = color, style = MaterialTheme.typography.labelSmall)
                }
            }
        } else WorkspaceLine(row.displayTitle(), summary) { openRow(table.id, row.id) }
    }
    if (rows.isEmpty()) Text("No entries in this period.", color = Muted, modifier = Modifier.padding(vertical = 12.dp))
    Row(Modifier.horizontalScroll(rememberScrollState())) {
        TextButton(onClick = { openRow(table.id, null) }) { Text("+ Add entry") }
        TextButton(onClick = { openTable(table.id) }) { Text("All ${all.size} records") }
        TextButton(onClick = { fields(table.id) }) { Text("Fields") }
    }
}

@Composable private fun NativeGrid(doc: TemplateDocument, table: PageTable, rows: List<PageRow>, month: YearMonth, busy: Boolean, openRow: (String, String?) -> Unit, toggle: (String, String, String) -> Unit) {
    val fields = table.fields.filter { !it.hidden && it.kind != "RELATION" }
    Row(Modifier.fillMaxWidth().border(.5.dp, Divider)) {
        Column(Modifier.width(126.dp)) {
            Text(if (table.id == "daily") "Aa  Date" else "Aa  Routine", color = Muted, style = MaterialTheme.typography.labelSmall, modifier = Modifier.fillMaxWidth().height(44.dp).background(SurfaceNavy).tableRule().padding(12.dp))
            rows.forEach { row -> Box(Modifier.fillMaxWidth().height(56.dp).tableRule().clickable { openRow(table.id, row.id) }.padding(horizontal = 10.dp), contentAlignment = Alignment.CenterStart) {
                Column {
                    Text(row.displayTitle().substringBefore("•").trim(), color = Ice, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium), maxLines = 2, overflow = TextOverflow.Ellipsis)
                    if (row.displayTitle().contains("•")) Text(row.displayTitle().substringAfter("•").trim(), color = Muted, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            } }
        }
        Column(Modifier.weight(1f).horizontalScroll(rememberScrollState()).testTag("native-grid")) {
            Row { fields.forEach { Text(it.label, color = Muted, style = MaterialTheme.typography.labelSmall, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.width(if (it.kind == "CHECKBOX") 64.dp else 130.dp).height(44.dp).background(SurfaceNavy).tableRule().padding(horizontal = 10.dp, vertical = 6.dp)) } }
            rows.forEach { row -> Row {
                fields.forEach { field -> Box(Modifier.width(if (field.kind == "CHECKBOX") 64.dp else 130.dp).height(56.dp).tableRule(), contentAlignment = if (field.kind == "CHECKBOX") Alignment.Center else Alignment.CenterStart) {
                    if (field.kind == "CHECKBOX") Checkbox(row.values[field.id] == "true", { toggle(table.id, row.id, field.id) }, enabled = !busy, modifier = Modifier.semantics { contentDescription = "${row.displayTitle()}, ${field.label}" })
                    else {
                        val value = doc.displayValue(table, row, field, month)
                        val percent = if (field.kind == "FORMULA" && value.endsWith("%")) value.removeSuffix("%").toFloatOrNull() else null
                        if (percent != null) ProgressRing(percent / 100f, if (percent > 100) MaterialTheme.colorScheme.error else Mint, "${row.displayTitle()}, ${field.label}", Modifier.clickable { openRow(table.id, row.id) }.padding(horizontal = 10.dp), valueText = value, compact = true)
                        else Text(value, color = Ice, style = MaterialTheme.typography.bodySmall, maxLines = 2, modifier = Modifier.clickable { openRow(table.id, row.id) }.padding(horizontal = 10.dp))
                    }
                } }
            } }
        }
    }
    HorizontalDivider(color = Divider)
    Text("Swipe fields sideways · names stay in view", color = Muted, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(vertical = 8.dp))
}

private fun Modifier.tableRule() = drawBehind {
    val line = .5.dp.toPx()
    drawLine(Divider, androidx.compose.ui.geometry.Offset(0f, size.height), androidx.compose.ui.geometry.Offset(size.width, size.height), line)
    drawLine(Divider, androidx.compose.ui.geometry.Offset(size.width, 0f), androidx.compose.ui.geometry.Offset(size.width, size.height), line)
}


@Composable private fun NativeRowEditor(doc: TemplateDocument, table: PageTable, initial: PageRow, busy: Boolean, error: String?, onDismiss: () -> Unit, onSave: (PageRow) -> Unit, onDelete: (() -> Unit)?) {
    var title by rememberSaveable(initial.id) { mutableStateOf(initial.title) }
    var values by rememberSaveable(initial.id) { mutableStateOf<Map<String, String>>(HashMap(initial.values)) }
    var discard by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }
    Editor("Edit ${if (table.id == "matrix") "routine" else "entry"}", busy, error, { if (title != initial.title || values != initial.values) discard = true else onDismiss() }, footer = {
        HorizontalDivider(color = Divider)
        Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            if (onDelete != null) TextButton(onClick = { deleting = true }, enabled = !busy, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text("Delete entry") }
            Button(onClick = { onSave(initial.copy(title = title, values = values)) }, enabled = !busy, modifier = Modifier.weight(1f)) { Text("Save entry") }
        }
    }) {
        OutlinedTextField(title, { title = it }, label = { Text(if (table.id == "daily") "Notes" else "Name") }, modifier = Modifier.fillMaxWidth().testTag("template-entry-title").semantics { contentDescription = if (table.id == "daily") "Daily quest notes" else "Entry name" })
        table.fields.filter { !it.hidden && it.kind != "FORMULA" }.forEach { field ->
            val value = values[field.id].orEmpty()
            val set: (String) -> Unit = { values = HashMap(values + (field.id to it)) }
            when (field.kind) {
                "CHECKBOX" -> Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(value == "true", { set(it.toString()) }, modifier = Modifier.semantics { contentDescription = field.label }); Text(field.label, color = Ice) }
                "CHECKLIST" -> EntryChecklistEditor(field.label, value, set)
                "CHOICE" -> { Text(field.label, color = Muted); Row(Modifier.horizontalScroll(rememberScrollState())) { FilterChip(value.isBlank(), { set("") }, label = { Text("None") }); field.options.forEach { option -> FilterChip(value == option, { set(option) }, label = { Text(option) }) } } }
                "RELATION" -> {
                    var expanded by remember { mutableStateOf(false) }
                    TextButton(onClick = { expanded = !expanded }) { Text("${field.label}: ${doc.displayValue(table, initial.copy(values = values), field, YearMonth.now())}") }
                    if (expanded) {
                        val selected = doc.references(initial.copy(values = values), field.id)
                        LazyColumn(Modifier.height(220.dp)) { items(doc.tables.find { it.id == field.relation }?.rows.orEmpty().filter { !it.archived }, key = { it.id }) { linked ->
                            Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(linked.id in selected, { set(JSONArray(if (linked.id in selected) selected - linked.id else selected + linked.id).toString()) }); Text(linked.displayTitle()) }
                        } }
                    }
                }
                else -> OutlinedTextField(value, set, label = { Text(field.label) }, modifier = Modifier.fillMaxWidth(), singleLine = field.kind != "TEXT", keyboardOptions = KeyboardOptions(keyboardType = if (field.kind == "NUMBER") KeyboardType.Decimal else KeyboardType.Text), supportingText = if (field.kind == "DATE") ({ Text("YYYY-MM-DD") }) else null)
            }
        }
        if (table.rows.any { it.id == initial.id }) TextButton(onClick = { onSave(initial.copy(title = title, values = values, archived = !initial.archived)) }, enabled = !busy) { Text(if (initial.archived) "Restore entry" else "Archive entry") }
    }
    if (discard) AlertDialog(onDismissRequest = { discard = false }, title = { Text("Discard unsaved changes?") }, confirmButton = { TextButton(onClick = onDismiss) { Text("Discard") } }, dismissButton = { TextButton(onClick = { discard = false }) { Text("Keep editing") } })
    if (deleting) ConfirmDelete("entry", "This permanently removes “${initial.displayTitle()}” and its saved values.", busy, { deleting = false }, { deleting = false; onDelete?.invoke() })
}

@Composable private fun NativeFieldManager(table: PageTable, busy: Boolean, error: String?, onDismiss: () -> Unit, onSave: (List<PageField>) -> Unit) {
    var fields by remember(table.id) { mutableStateOf(table.fields) }
    var name by rememberSaveable { mutableStateOf("") }; var kind by rememberSaveable { mutableStateOf("TEXT") }; var options by rememberSaveable { mutableStateOf("") }
    Editor("Fields · ${table.title}", busy, error, onDismiss) {
        Text("Rename or hide fields without changing their saved values.", color = Muted)
        fields.forEach { field ->
            OutlinedTextField(field.label, { value -> fields = fields.map { if (it.id == field.id) it.copy(label = value) else it } }, label = { Text(field.kind.lowercase()) }, modifier = Modifier.fillMaxWidth())
            if (field.kind == "CHOICE") OutlinedTextField(field.options.joinToString("\n"), { value -> fields = fields.map { if (it.id == field.id) it.copy(options = value.lines().filter(String::isNotBlank).distinct()) else it } }, label = { Text("Choices, one per line") }, modifier = Modifier.fillMaxWidth())
            TextButton(onClick = { fields = fields.map { if (it.id == field.id) it.copy(hidden = !it.hidden) else it } }) { Text(if (field.hidden) "Restore field" else "Hide field") }
        }
        HorizontalDivider(color = Divider)
        OutlinedTextField(name, { name = it }, label = { Text("New field name") }, modifier = Modifier.fillMaxWidth())
        Row(Modifier.horizontalScroll(rememberScrollState())) { listOf("TEXT", "NUMBER", "DATE", "CHECKBOX", "CHECKLIST", "CHOICE").forEach { type -> FilterChip(kind == type, { kind = type }, label = { Text(type.lowercase()) }) } }
        if (kind == "CHOICE") OutlinedTextField(options, { options = it }, label = { Text("Choices, one per line") }, minLines = 3, modifier = Modifier.fillMaxWidth())
        TextButton(onClick = {
            fields = fields + PageField(UUID.randomUUID().toString(), name.trim(), kind, if (kind == "CHOICE") options.lines().map(String::trim).filter(String::isNotBlank).distinct() else emptyList())
            name = ""; options = ""
        }, enabled = name.isNotBlank() && (kind != "CHOICE" || options.isNotBlank())) { Text("+ Add field") }
        Button(onClick = { onSave(fields) }, enabled = !busy && fields.all { it.label.isNotBlank() }) { Text("Save fields") }
    }
}

@Composable private fun NativeDatabaseCreator(busy: Boolean, error: String?, onDismiss: () -> Unit, onCreate: (String, String) -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    var layout by rememberSaveable { mutableStateOf("table") }
    Editor("Add a database", busy, error, onDismiss) {
        OutlinedTextField(name, { name = it }, label = { Text("Database name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Text("Layout", color = Muted, style = MaterialTheme.typography.labelMedium)
        listOf("table" to "Table", "list" to "List", "board" to "Board", "calendar" to "Calendar").forEach { (value, label) ->
            Row(Modifier.fillMaxWidth().clickable { layout = value }, verticalAlignment = Alignment.CenterVertically) {
                RadioButton(layout == value, { layout = value })
                Text(label)
            }
        }
        Text("After creating it, use Fields to add text, dates, numbers, choices, checkboxes, or editable checklists.", color = Muted, style = MaterialTheme.typography.bodySmall)
        Button(onClick = { onCreate(name.trim(), layout) }, enabled = !busy && name.isNotBlank(), modifier = Modifier.fillMaxWidth()) { Text("Create database") }
    }
}

@Composable
internal fun NewTemplateDialog(busy: Boolean, error: String?, onDismiss: () -> Unit, onImport: () -> Unit = {}, onCreate: (String, String, String) -> Unit) {
    var title by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var starter by rememberSaveable { mutableStateOf("blank") }
    Editor("New template", busy, error, onDismiss, footer = {
        Button(onClick = { onCreate(title, description, starter) }, enabled = !busy && title.isNotBlank(), modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) { Text("Create template") }
    }) {
        OutlinedTextField(title, { title = it }, label = { Text("Template name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(description, { description = it }, label = { Text("Description (optional)") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
        OutlinedButton(onClick = onImport, enabled = !busy, modifier = Modifier.fillMaxWidth().testTag("open-notion-import")) { Text("Import from Notion") }
        Text("Choose a starting point", style = MaterialTheme.typography.titleMedium)
        PageStarters.all.forEach { option ->
            Row(Modifier.fillMaxWidth().clickable(enabled = !busy) { starter = option.id }.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                RadioButton(starter == option.id, { starter = option.id }, enabled = !busy)
                Column(Modifier.weight(1f)) { Text(option.title, color = Ice); Text(option.description, color = Muted, style = MaterialTheme.typography.bodySmall) }
            }
        }
    }
}

@Composable private fun TemplateRowMenu(title: String, edit: () -> Unit, delete: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) { Icon(Icons.Rounded.MoreVert, "Actions for $title", tint = Muted) }
        DropdownMenu(expanded, { expanded = false }) {
            DropdownMenuItem(text = { Text("Edit template") }, onClick = { expanded = false; edit() })
            DropdownMenuItem(text = { Text("Delete template") }, onClick = { expanded = false; delete() })
        }
    }
}
