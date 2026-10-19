package com.krishna.personalos

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import android.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.SideEffect
import com.krishna.personalos.ui.PersonalOSApp
import com.krishna.personalos.ui.theme.PersonalOSTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import com.krishna.personalos.core.reminders.TaskReminders

class MainActivity : ComponentActivity() {
    private val homeRequests = MutableStateFlow(0)
    private val launchTask = MutableStateFlow<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        launchTask.value = intent.getStringExtra(TaskReminders.OPEN_TASK)
        setContent {
            val model: AppViewModel = viewModel()
            val darkMode = model.darkMode.collectAsStateWithLifecycle().value
            val currencySymbol = model.currencySymbol.collectAsStateWithLifecycle().value
            SideEffect {
                val barStyle = if (darkMode) SystemBarStyle.dark(Color.TRANSPARENT) else SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                enableEdgeToEdge(statusBarStyle = barStyle, navigationBarStyle = barStyle)
            }
            PersonalOSTheme(darkMode) {
                val workspace = model.workspace.collectAsStateWithLifecycle().value
                val templatePages = model.templatePages.collectAsStateWithLifecycle().value
                val importFailure = model.importFailure.collectAsStateWithLifecycle().value
                val homeRequest = homeRequests.collectAsStateWithLifecycle().value
                val trackerWorkspace = model.trackerWorkspace.collectAsStateWithLifecycle().value
                val pinnedIds = model.pinnedIds.collectAsStateWithLifecycle().value
                val trash = model.trash.collectAsStateWithLifecycle().value
                val launchTaskId = launchTask.collectAsStateWithLifecycle().value
                PersonalOSApp(
                    trackerWorkspace = trackerWorkspace,
                    trackerActions = model.trackerActions,
                    workspace = workspace,
                    workspaceActions = model.workspaceActions,
                    templatePages = templatePages,
                    templateActions = model.templateActions,
                    importFailure = importFailure,
                    onToggleTask = model::toggleTask,
                    onScheduleToday = model::scheduleToday,
                    onCapture = model::capture,
                    darkMode = darkMode,
                    onDarkMode = model::setDarkMode,
                    currencySymbol = currencySymbol,
                    onCurrencySymbol = model::setCurrencySymbol,
                    homeRequest = homeRequest,
                    launchTaskId = launchTaskId,
                    pinnedIds = pinnedIds, onPin = model::pinPage,
                    trash = trash, recovery = model.recovery, backup = model.backup,
                    notices = model.notices, onRestoreBackup = model::restoreBackup,
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        launchTask.value = intent.getStringExtra(TaskReminders.OPEN_TASK)
        homeRequests.update { it + 1 }
    }
}
