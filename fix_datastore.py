content = """package com.example.data

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
        val SOS_SOUND_ENABLED_KEY = booleanPreferencesKey("sos_sound_enabled")
        val SOS_VIBRATION_ENABLED_KEY = booleanPreferencesKey("sos_vibration_enabled")
        val CRITICAL_ALARMS_ENABLED_KEY = booleanPreferencesKey("critical_alarms_enabled")
    }

    val developerModeFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[DEVELOPER_MODE_KEY] ?: false
        }

    val sosSoundEnabledFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[SOS_SOUND_ENABLED_KEY] ?: true
        }

    val sosVibrationEnabledFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[SOS_VIBRATION_ENABLED_KEY] ?: true
        }

    val criticalAlarmsEnabledFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[CRITICAL_ALARMS_ENABLED_KEY] ?: true
        }

    suspend fun setDeveloperMode(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[DEVELOPER_MODE_KEY] = enabled
        }
    }

    suspend fun setSosSoundEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[SOS_SOUND_ENABLED_KEY] = enabled
        }
    }

    suspend fun setSosVibrationEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[SOS_VIBRATION_ENABLED_KEY] = enabled
        }
    }

    suspend fun setCriticalAlarmsEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[CRITICAL_ALARMS_ENABLED_KEY] = enabled
        }
    }
}
"""

with open("app/src/main/java/com/example/data/SettingsDataStore.kt", "w") as f:
    f.write(content)
