package com.krishna.personalos.core.launcher

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class AppLauncherRepository(context: Context) {
    private val appContext = context.applicationContext
    private val cacheMutex = Mutex()
    private var cachedApps: List<InstalledApp>? = null

    @Suppress("DEPRECATION")
    suspend fun installedApps(forceRefresh: Boolean = false): List<InstalledApp> = withContext(Dispatchers.IO) {
        cacheMutex.withLock {
            if (!forceRefresh) cachedApps?.let { return@withLock it }

            val packageManager = appContext.packageManager
            val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            packageManager.queryIntentActivities(launcherIntent, 0)
                .asSequence()
                .filter { it.activityInfo.packageName != appContext.packageName }
                .distinctBy { it.activityInfo.packageName }
                .mapNotNull { result ->
                    runCatching {
                        InstalledApp(
                            packageName = result.activityInfo.packageName,
                            label = result.loadLabel(packageManager).toString(),
                            icon = result.loadIcon(packageManager),
                        )
                    }.getOrNull()
                }
                .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER, InstalledApp::label))
                .toList()
                .also { cachedApps = it }
        }
    }

    fun launch(packageName: String): Boolean = runCatching {
        val intent = appContext.packageManager.getLaunchIntentForPackage(packageName) ?: return false
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        appContext.startActivity(intent)
        true
    }.getOrDefault(false)

    fun openAppInfo(packageName: String): Boolean = runCatching {
        appContext.startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = "package:$packageName".toUri()
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            },
        )
        true
    }.getOrDefault(false)
}
