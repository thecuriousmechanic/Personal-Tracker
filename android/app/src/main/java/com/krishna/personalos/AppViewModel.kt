package com.krishna.personalos

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.krishna.personalos.core.model.WorkspaceSnapshot
import com.krishna.personalos.core.repository.CaptureKind
import com.krishna.personalos.core.repository.TrackerRepository
import com.krishna.personalos.core.database.AppDatabase
import com.krishna.personalos.core.model.TrackerWorkspace
import com.krishna.personalos.core.model.TrackerActions
import com.krishna.personalos.core.model.TemplateActions
import com.krishna.personalos.core.repository.TemplateRepository
import com.krishna.personalos.core.preferences.AppearancePreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import com.krishna.personalos.core.repository.RecoveryRepository
import com.krishna.personalos.core.export.WorkspaceBackup
import com.krishna.personalos.core.reminders.TaskReminders
import com.krishna.personalos.core.model.WorkspaceNotice
import com.krishna.personalos.core.model.TemplateDocumentCache
import kotlinx.coroutines.CancellationException

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as PersonalOSApplication
    private val repository = app.repository
    private val database = AppDatabase.get(application)
    val recovery = RecoveryRepository(database)
    val backup = WorkspaceBackup(database, application)
    val trash = recovery.trash.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val pinnedIds = recovery.preferences.map { items -> items.filter { it.key.startsWith("pin:") && it.value == "true" }.map { it.key.removePrefix("pin:") }.toSet() }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())
    val notices = MutableSharedFlow<WorkspaceNotice>(extraBufferCapacity = 32)
    private val deletionLock = Mutex()
    private suspend fun saved(operation: suspend () -> Unit) { operation(); notices.emit(WorkspaceNotice("Saved on this device", quiet = true)) }
    private suspend fun removed(operation: suspend () -> Unit) = deletionLock.withLock {
        operation()
        notices.emit(WorkspaceNotice("Moved to Trash", database.recoveryDao().latest()?.id))
    }
    val workspaceActions = com.krishna.personalos.core.model.WorkspaceActions(
        saveTask = { saved { repository.saveTask(it) } }, saveNote = { saved { repository.saveNote(it) } },
        saveProject = { saved { repository.saveProject(it) } }, saveGoal = { saved { repository.saveGoal(it) } },
        delete = { kind, id -> removed { repository.delete(kind, id) } },
        toggleNoteChecklist = { id, line -> saved { repository.toggleNoteChecklist(id, line) } },
        skipTask = { id -> removed { repository.skipTask(id) } },
    )
    private val appearance = AppearancePreferences(application)
    val darkMode = appearance.darkMode.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)
    val currencySymbol = appearance.currencySymbol.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "\$")
    private val trackers = TrackerRepository(AppDatabase.get(application))
    val trackerWorkspace = trackers.workspace.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TrackerWorkspace())
    val trackerActions = TrackerActions(
        create = trackers::create,
        saveTracker = { saved { trackers.saveTracker(it) } },
        saveField = { saved { trackers.saveField(it) } },
        saveEntry = { entry, values -> saved { trackers.saveEntry(entry, values) } },
        toggleCheckbox = { entry, field -> saved { trackers.toggleCheckbox(entry, field) } },
        archiveEntry = { id, archived -> saved { trackers.archiveEntry(id, archived) } },
        deleteEntry = { id -> removed { trackers.deleteEntry(id) } },
        deleteEntries = { ids -> removed { trackers.deleteEntries(ids) } },
        duplicate = trackers::duplicate,
        delete = { id -> removed { trackers.delete(id) } },
    )
    private val templates = TemplateRepository(AppDatabase.get(application))
    val templatePages = templates.pages.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val templateActions = TemplateActions(
        create = templates::create,
        change = { id, transform ->
            val before = database.recoveryDao().latest()?.id
            templates.change(id, transform)
            val latest = database.recoveryDao().latest()
            if (latest != null && latest.id != before && latest.kind == "page_sections" && latest.ownerId == id) notices.emit(WorkspaceNotice("Section moved to Trash", latest.id))
        },
        rename = { id, title, subtitle -> saved { templates.rename(id, title, subtitle) } },
        archive = { id, archived -> saved { templates.archive(id, archived) } },
        duplicate = templates::duplicate,
        saveRow = { page, table, row -> saved { templates.saveRow(page, table, row) } },
        deleteRow = { page, table, row -> removed { templates.deleteRow(page, table, row) } },
        deleteRows = { page, table, rows -> removed { templates.deleteRows(page, table, rows) } },
        delete = { id -> removed { templates.delete(id) } },
        importFinance = { templates.importFinance(application) },
        importDocument = templates::importDocument,
    )
    private val importFailureState = MutableStateFlow<String?>(null)
    val importFailure = importFailureState.asStateFlow()

    val workspace: StateFlow<WorkspaceSnapshot> = repository.workspace.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = WorkspaceSnapshot(),
    )

    init {
        viewModelScope.launch {
            try {
                appearance.migrate()
                if (database.recoveryDao().preference("bundled_import_disabled")?.value != "true") {
                    repository.removeBundledDemoData()
                    templates.importBundled(application)
                }
            }
            catch (e: kotlinx.coroutines.CancellationException) { throw e }
            catch (e: Exception) { importFailureState.value = "Could not load imported templates. Reopen the app to retry." }
        }
        viewModelScope.launch {
            val reminders = TaskReminders(application)
            var previous = emptyMap<String, com.krishna.personalos.core.database.TaskEntity>()
            repository.workspace.map { it.tasks }.distinctUntilChanged().collect { tasks ->
                val current = tasks.associateBy { it.id }
                (previous.keys - current.keys).forEach(reminders::cancel)
                current.values.filter { previous[it.id] != it }.forEach { task ->
                    try { reminders.schedule(task) }
                    catch (e: Exception) { notices.emit(WorkspaceNotice("Could not schedule a reminder. Reopen the app to retry.")) }
                }
                previous = current
            }
        }
    }

    fun toggleTask(id: String) = viewModelScope.launch {
        try { repository.toggleTask(id); notices.emit(WorkspaceNotice("Task updated")) }
        catch (e: CancellationException) { throw e }
        catch (e: Exception) { notices.emit(WorkspaceNotice(e.message ?: "Could not update task. Tap again to retry.")) }
    }
    fun pinPage(id: String, pinned: Boolean) = viewModelScope.launch {
        try { recovery.pin(id, pinned) }
        catch (e: CancellationException) { throw e }
        catch (e: Exception) { notices.emit(WorkspaceNotice("Could not pin page. Try again.")) }
    }
    suspend fun restoreBackup(preview: WorkspaceBackup.Preview) {
        backup.restore(preview)
        TemplateDocumentCache.clear()
    }
    fun scheduleToday(id: String) = viewModelScope.launch { repository.scheduleToday(id) }
    fun capture(kind: CaptureKind, title: String) = viewModelScope.launch { repository.capture(kind, title) }
    private fun savePreference(operation: suspend () -> Unit) = viewModelScope.launch {
        try { saved(operation) }
        catch (e: CancellationException) { throw e }
        catch (e: Exception) { notices.emit(WorkspaceNotice(e.message ?: "Could not save setting. Try again.")) }
    }
    fun setDarkMode(enabled: Boolean) = savePreference { appearance.setDarkMode(enabled) }
    fun setCurrencySymbol(symbol: String) = savePreference { appearance.setCurrencySymbol(symbol) }
}
