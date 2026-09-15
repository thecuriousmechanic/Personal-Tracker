package com.krishna.personalos.core.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.appearanceDataStore by preferencesDataStore(name = "appearance_preferences")

class AppearancePreferences(context: Context) {
    private val dataStore = context.applicationContext.appearanceDataStore

    val darkMode: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[DARK_MODE] ?: false
    }

    val currencySymbol: Flow<String> = dataStore.data.map { preferences ->
        preferences[CURRENCY_SYMBOL] ?: "\$"
    }

    suspend fun setDarkMode(enabled: Boolean) {
        dataStore.edit { preferences -> preferences[DARK_MODE] = enabled }
    }

    suspend fun setCurrencySymbol(symbol: String) {
        val value = symbol.trim()
        require(value.isNotBlank()) { "Enter a currency symbol" }
        require(value.length <= 4 && value.none(Char::isISOControl)) { "Use up to 4 characters" }
        dataStore.edit { preferences -> preferences[CURRENCY_SYMBOL] = value }
    }

    private companion object {
        val DARK_MODE = booleanPreferencesKey("dark_mode")
        val CURRENCY_SYMBOL = stringPreferencesKey("currency_symbol")
    }
}
