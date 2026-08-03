package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsDataStore(private val context: Context) {

    companion object {
        val DEVELOPER_MODE_KEY = booleanPreferencesKey("developer_mode")
    }

    val developerModeFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[DEVELOPER_MODE_KEY] ?: false
        }

    suspend fun setDeveloperMode(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[DEVELOPER_MODE_KEY] = enabled
        }
    }
}
