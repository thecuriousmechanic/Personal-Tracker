package com.krishna.personalos.core.launcher

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.launcherDataStore by preferencesDataStore(name = "launcher_preferences")

class LauncherPreferences(context: Context) {
    private val dataStore = context.applicationContext.launcherDataStore

    val favoritePackages: Flow<List<String>> = dataStore.data.map { preferences ->
        preferences[FAVORITES]
            .orEmpty()
            .lineSequence()
            .map(String::trim)
            .filter(String::isNotEmpty)
            .distinct()
            .toList()
    }

    suspend fun toggleFavorite(packageName: String) {
        dataStore.edit { preferences ->
            val current = preferences[FAVORITES]
                .orEmpty()
                .lineSequence()
                .filter(String::isNotBlank)
                .toMutableList()
            if (!current.remove(packageName)) current += packageName
            preferences[FAVORITES] = current.distinct().joinToString("\n")
        }
    }

    companion object {
        private val FAVORITES = stringPreferencesKey("favorite_packages")
    }
}
