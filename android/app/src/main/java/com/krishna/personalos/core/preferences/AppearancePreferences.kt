package com.krishna.personalos.core.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import com.krishna.personalos.core.database.AppDatabase
import com.krishna.personalos.core.database.WorkspacePreferenceEntity
import androidx.room.withTransaction

private val Context.appearanceDataStore by preferencesDataStore(name = "appearance_preferences")

class AppearancePreferences(context: Context) {
    private val dataStore = context.applicationContext.appearanceDataStore
    private val database = AppDatabase.get(context)
    private val dao = database.recoveryDao()

    val darkMode: Flow<Boolean> = combine(dao.observePreferences(), dataStore.data) { saved, legacy ->
        saved.find { it.key == "dark_mode" }?.value?.toBooleanStrictOrNull() ?: legacy[DARK_MODE] ?: false
    }

    val currencySymbol: Flow<String> = combine(dao.observePreferences(), dataStore.data) { saved, legacy ->
        saved.find { it.key == "currency_symbol" }?.value ?: legacy[CURRENCY_SYMBOL] ?: "\$"
    }

    suspend fun migrate() {
        val legacy = dataStore.data.first()
        database.withTransaction {
            if (dao.preference("dark_mode") == null) dao.savePreference(WorkspacePreferenceEntity("dark_mode", (legacy[DARK_MODE] ?: false).toString()))
            if (dao.preference("currency_symbol") == null) dao.savePreference(WorkspacePreferenceEntity("currency_symbol", legacy[CURRENCY_SYMBOL] ?: "\$"))
        }
    }

    suspend fun setDarkMode(enabled: Boolean) {
        dao.savePreference(WorkspacePreferenceEntity("dark_mode", enabled.toString()))
    }

    suspend fun setCurrencySymbol(symbol: String) {
        val value = symbol.trim()
        require(value.isNotBlank()) { "Enter a currency symbol" }
        require(value.length <= 4 && value.none(Char::isISOControl)) { "Use up to 4 characters" }
        dao.savePreference(WorkspacePreferenceEntity("currency_symbol", value))
    }

    private companion object {
        val DARK_MODE = booleanPreferencesKey("dark_mode")
        val CURRENCY_SYMBOL = stringPreferencesKey("currency_symbol")
    }
}
