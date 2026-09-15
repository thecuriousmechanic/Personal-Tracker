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

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as PersonalOSApplication
    private val repository = app.repository
    val workspaceActions = com.krishna.personalos.core.model.WorkspaceActions(repository::saveTask, repository::saveNote, repository::saveProject, repository::saveGoal, repository::delete, repository::toggleNoteChecklist)
    private val appearance = AppearancePreferences(application)
    val darkMode = appearance.darkMode.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)
    val currencySymbol = appearance.currencySymbol.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "\$")
    private val trackers = TrackerRepository(AppDatabase.get(application))
    val trackerWorkspace = trackers.workspace.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TrackerWorkspace())
    val trackerActions = TrackerActions(
        create = trackers::create,
        saveTracker = trackers::saveTracker,
        saveField = trackers::saveField,
        saveEntry = trackers::saveEntry,
        toggleCheckbox = trackers::toggleCheckbox,
        archiveEntry = trackers::archiveEntry,
        deleteEntry = trackers::deleteEntry,
        deleteEntries = trackers::deleteEntries,
        duplicate = trackers::duplicate,
        delete = trackers::delete,
    )
    private val templates = TemplateRepository(AppDatabase.get(application))
    val templatePages = templates.pages.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val templateActions = TemplateActions(
        create = templates::create,
        change = templates::change,
        rename = templates::rename,
        archive = templates::archive,
        duplicate = templates::duplicate,
        saveRow = templates::saveRow,
        deleteRow = templates::deleteRow,
        deleteRows = templates::deleteRows,
        delete = templates::delete,
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
        viewModelScope.launch { repository.removeBundledDemoData() }
        viewModelScope.launch {
            try { templates.importBundled(application) }
            catch (e: kotlinx.coroutines.CancellationException) { throw e }
            catch (e: Exception) { importFailureState.value = "Could not load imported templates. Reopen the app to retry." }
        }
    }

    fun toggleTask(id: String) = viewModelScope.launch { repository.toggleTask(id) }
    fun scheduleToday(id: String) = viewModelScope.launch { repository.scheduleToday(id) }
    fun capture(kind: CaptureKind, title: String) = viewModelScope.launch { repository.capture(kind, title) }
    fun setDarkMode(enabled: Boolean) = viewModelScope.launch { appearance.setDarkMode(enabled) }
    fun setCurrencySymbol(symbol: String) = viewModelScope.launch { appearance.setCurrencySymbol(symbol) }
}
