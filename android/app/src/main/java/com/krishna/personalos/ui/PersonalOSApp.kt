package com.krishna.personalos.ui

import androidx.activity.compose.BackHandler
import androidx.compose.material3.*
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.collectLatest
import com.krishna.personalos.core.database.TemplatePageEntity
import com.krishna.personalos.core.database.TrashEntity
import com.krishna.personalos.core.repository.RecoveryRepository
import com.krishna.personalos.core.export.WorkspaceBackup
import com.krishna.personalos.core.model.*
import com.krishna.personalos.core.repository.CaptureKind

@Composable
fun PersonalOSApp(
    trackerWorkspace: TrackerWorkspace,
    trackerActions: TrackerActions,
    workspace: WorkspaceSnapshot,
    workspaceActions: WorkspaceActions,
    templatePages: List<TemplatePageEntity>,
    templateActions: TemplateActions,
    importFailure: String?,
    onToggleTask: (String) -> Unit,
    onScheduleToday: (String) -> Unit,
    onCapture: (CaptureKind, String) -> Unit,
    darkMode: Boolean = false,
    onDarkMode: (Boolean) -> Unit = {},
    currencySymbol: String = "\$",
    onCurrencySymbol: (String) -> Unit = {},
    homeRequest: Int = 0,
    launchTaskId: String? = null,
    pinnedIds: Set<String> = emptySet(),
    onPin: (String, Boolean) -> Unit = { _, _ -> },
    trash: List<TrashEntity> = emptyList(),
    recovery: RecoveryRepository? = null,
    backup: WorkspaceBackup? = null,
    notices: Flow<WorkspaceNotice> = emptyFlow(),
    onRestoreBackup: suspend (WorkspaceBackup.Preview) -> Unit = {},
) {
    var destinationName by rememberSaveable { mutableStateOf(LiteDestination.HOME.name) }
    var pageId by rememberSaveable { mutableStateOf<String?>(null) }
    var customId by rememberSaveable { mutableStateOf<String?>(null) }
    var captureOpen by rememberSaveable { mutableStateOf(false) }
    var editorKind by rememberSaveable { mutableStateOf<String?>(null) }
    var editorId by rememberSaveable { mutableStateOf<String?>(null) }
    var libraryKind by rememberSaveable { mutableStateOf(CaptureKind.NOTE.name) }
    var editTemplate by rememberSaveable { mutableStateOf(false) }
    var createTemplate by rememberSaveable { mutableStateOf(false) }
    var importTemplate by rememberSaveable { mutableStateOf(false) }
    var creatingTemplate by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<Triple<String, String, Boolean>?>(null) }
    var deleting by remember { mutableStateOf(false) }
    var actionError by remember { mutableStateOf<String?>(null) }
    var toolsBusy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val openEditor: (CaptureKind, String?) -> Unit = { kind, id -> editorId = id; editorKind = kind.name }
    val destination = LiteDestination.entries.firstOrNull { it.name == destinationName } ?: LiteDestination.HOME
    val home = { destinationName = LiteDestination.HOME.name; pageId = null; customId = null }
    LaunchedEffect(homeRequest) {
        home(); captureOpen = false; editorKind = null
        if (launchTaskId != null) { destinationName = LiteDestination.PLAN.name; openEditor(CaptureKind.TASK, launchTaskId) }
    }
    LaunchedEffect(notices) {
        notices.collectLatest { notice ->
            // Page editors already show their save state. Reserve overlays for actionable feedback.
            if (notice.quiet) return@collectLatest
            val result = snackbar.showSnackbar(notice.message, actionLabel = if (notice.undoId != null) "Undo" else null, withDismissAction = true)
            if (result == SnackbarResult.ActionPerformed && notice.undoId != null && recovery != null) {
                try { withContext(NonCancellable) { recovery.restore(notice.undoId) }; snackbar.showSnackbar("Restored") }
                catch (e: CancellationException) { throw e }
                catch (e: Exception) { snackbar.showSnackbar(e.message ?: "Could not restore. Try again in Trash.") }
            }
        }
    }
    BackHandler(destination != LiteDestination.HOME || pageId != null) { home() }
    LiteBackground {
        Scaffold(containerColor = Color.Transparent, snackbarHost = { SnackbarHost(snackbar) }, bottomBar = {
            LiteBottomNavigation(destination) { selected ->
                if (!toolsBusy) {
                    if (selected == LiteDestination.CAPTURE) {
                        if (destination == LiteDestination.HOME || destination == LiteDestination.SETTINGS || destination == LiteDestination.TRACKERS) { captureOpen = true }
                        else openEditor(if (destination == LiteDestination.PLAN) CaptureKind.TASK else CaptureKind.valueOf(libraryKind), null)
                    } else { home(); destinationName = selected.name }
                }
            }
        }) { insets ->
            when (destination) {
                LiteDestination.HOME -> {
                    val page = templatePages.find { it.id == pageId }
                    if (page != null) key(page.id) { TemplatePageScreen(page, templateActions, insets, home, { pageId = it }, editTemplate, currencySymbol, onCurrencySymbol) }
                    else WorkspaceOverview(templatePages, trackerWorkspace, workspace, pinnedIds, actionError ?: importFailure, insets,
                        onPage = { editTemplate = false; pageId = it },
                        onCustom = { editTemplate = false; customId = it; destinationName = LiteDestination.TRACKERS.name },
                        onCreate = { createTemplate = true; actionError = null },
                        onEdit = { id, custom -> editTemplate = true; if (custom) { customId = id; destinationName = LiteDestination.TRACKERS.name } else pageId = id },
                        onDelete = { id, title, custom -> deleteTarget = Triple(id, title, custom) },
                        onPin = onPin, onSettings = { destinationName = LiteDestination.SETTINGS.name },
                        onPlan = { destinationName = LiteDestination.PLAN.name },
                        onTask = { id -> destinationName = LiteDestination.PLAN.name; openEditor(CaptureKind.TASK, id) },
                        onToggleTask = onToggleTask,
                    )
                }
                LiteDestination.TRACKERS -> key(customId) { TrackerScreen(trackerWorkspace, trackerActions, insets, customId, home, editTemplate) }
                LiteDestination.PLAN -> WorkspaceCollectionScreen(workspace, true, libraryKind, { libraryKind = it }, insets, openEditor, onToggleTask) { id, line -> workspaceActions.toggleNoteChecklist(id, line) }
                LiteDestination.LIBRARY -> WorkspaceCollectionScreen(workspace, false, libraryKind, { libraryKind = it }, insets, openEditor, onToggleTask) { id, line -> workspaceActions.toggleNoteChecklist(id, line) }
                LiteDestination.SETTINGS -> WorkspaceSettingsScreen(insets, darkMode, onDarkMode, currencySymbol, onCurrencySymbol, trash, recovery, backup, onRestoreBackup) { toolsBusy = it }
                else -> Unit
            }
        }
    }
    if (captureOpen) AlertDialog(onDismissRequest = { captureOpen = false }, title = { Text("Add to your workspace") }, text = {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(CaptureKind.TASK, CaptureKind.NOTE, CaptureKind.PROJECT, CaptureKind.GOAL).forEach { kind ->
                OutlinedButton(onClick = {
                    captureOpen = false; home()
                    destinationName = if (kind == CaptureKind.TASK) LiteDestination.PLAN.name else LiteDestination.LIBRARY.name
                    libraryKind = kind.name
                    openEditor(kind, null)
                }, modifier = Modifier.fillMaxWidth()) { Text("New ${kind.label.lowercase()}") }
            }
            FilledTonalButton(onClick = { captureOpen = false; home(); createTemplate = true; actionError = null }, modifier = Modifier.fillMaxWidth()) { Text("New page") }
        }
    }, confirmButton = {}, dismissButton = { TextButton(onClick = { captureOpen = false }) { Text("Cancel") } })
    if (importTemplate) NotionImportDialog({ importTemplate = false }) { title, document ->
        pageId = templateActions.importDocument(title, document)
        destinationName = LiteDestination.HOME.name; editTemplate = false; importTemplate = false
    }
    if (createTemplate) NewTemplateDialog(creatingTemplate, actionError, { if (!creatingTemplate) createTemplate = false }, onImport = { createTemplate = false; importTemplate = true }) { title, description, starter ->
        if (!creatingTemplate) scope.launch {
            creatingTemplate = true; actionError = null
            try {
                pageId = templateActions.create(title, description, starter)
                editTemplate = true
                createTemplate = false
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { actionError = e.message ?: "Could not create the template" }
            finally { creatingTemplate = false }
        }
    }
    editorKind?.let { kind -> WorkspaceEntityEditor(CaptureKind.valueOf(kind), editorId, workspace, workspaceActions) { editorKind = null } }
    deleteTarget?.let { (id, title, custom) -> ConfirmDelete("page", "Move “$title” and its records to Trash? You can restore them in Settings.", deleting, { deleteTarget = null }, {
        scope.launch {
            deleting = true
            try { if (custom) trackerActions.delete(id) else templateActions.delete(id); deleteTarget = null }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { actionError = e.message ?: "Could not delete"; deleteTarget = null }
            finally { deleting = false }
        }
    }, recoverable = true) }
}
