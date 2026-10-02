package com.example.repository

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        const val PREFS_NAME = "smart_sos_settings"
        const val KEY_FALL_RESPONSE_DELAY_SECONDS = "fall_response_delay_seconds"
        const val DEFAULT_FALL_RESPONSE_DELAY_SECONDS = 12

        const val KEY_NEARBY_PRESENCE_INTERVAL = "nearby_presence_interval"
        const val DEFAULT_NEARBY_PRESENCE_INTERVAL = 0

        const val KEY_VOICE_SOS_ENABLED = "voice_sos_enabled"
        const val DEFAULT_VOICE_SOS_ENABLED = false

        val AVAILABLE_FALL_RESPONSE_TIMES = listOf(5, 10, 12, 15, 20, 30)
        val AVAILABLE_NEARBY_PRESENCE_INTERVALS = listOf(0, 5, 10, 30, 60)
    }

    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun isVoiceSosEnabled(): Boolean {
        return try {
            prefs.getBoolean(KEY_VOICE_SOS_ENABLED, DEFAULT_VOICE_SOS_ENABLED)
        } catch (e: Exception) {
            DEFAULT_VOICE_SOS_ENABLED
        }
    }

    fun setVoiceSosEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_VOICE_SOS_ENABLED, enabled).apply()
    }

    fun getFallResponseDelaySeconds(): Int {
        val value = try {
            prefs.getInt(KEY_FALL_RESPONSE_DELAY_SECONDS, DEFAULT_FALL_RESPONSE_DELAY_SECONDS)
        } catch (e: Exception) {
            DEFAULT_FALL_RESPONSE_DELAY_SECONDS
        }
        return if (value in AVAILABLE_FALL_RESPONSE_TIMES) value else DEFAULT_FALL_RESPONSE_DELAY_SECONDS
    }

    fun setFallResponseDelaySeconds(seconds: Int) {
        val validValue = if (seconds in AVAILABLE_FALL_RESPONSE_TIMES) seconds else DEFAULT_FALL_RESPONSE_DELAY_SECONDS
        prefs.edit().putInt(KEY_FALL_RESPONSE_DELAY_SECONDS, validValue).apply()
    }

    fun getNearbyPresenceInterval(): Int {
        val value = try {
            prefs.getInt(KEY_NEARBY_PRESENCE_INTERVAL, DEFAULT_NEARBY_PRESENCE_INTERVAL)
        } catch (e: Exception) {
            DEFAULT_NEARBY_PRESENCE_INTERVAL
        }
        return if (value in AVAILABLE_NEARBY_PRESENCE_INTERVALS) value else DEFAULT_NEARBY_PRESENCE_INTERVAL
    }

    fun setNearbyPresenceInterval(interval: Int) {
        val validValue = if (interval in AVAILABLE_NEARBY_PRESENCE_INTERVALS) interval else DEFAULT_NEARBY_PRESENCE_INTERVAL
        prefs.edit().putInt(KEY_NEARBY_PRESENCE_INTERVAL, validValue).apply()
    }
}
