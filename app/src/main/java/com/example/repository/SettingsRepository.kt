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

        const val KEY_EMERGENCY_SOUND_ID = "emergency_sound_id"
        const val DEFAULT_EMERGENCY_SOUND_ID = "builtin_siren"
        const val KEY_EMERGENCY_CUSTOM_SOUND_URI = "emergency_custom_sound_uri"
        const val KEY_EMERGENCY_CUSTOM_SOUND_NAME = "emergency_custom_sound_name"

        const val SOUND_BUILTIN_SIREN = "builtin_siren"
        const val SOUND_BUILTIN_RAPID_ALARM = "builtin_rapid_alarm"
        const val SOUND_BUILTIN_WARNING_PULSE = "builtin_warning_pulse"
        const val SOUND_BUILTIN_DOUBLE_BEEP = "builtin_double_beep"
        const val SOUND_BUILTIN_CRITICAL_ALERT = "builtin_critical_alert"
        const val SOUND_BUILTIN_EVACUATION_TONE = "builtin_evacuation_tone"
        const val SOUND_CUSTOM = "custom"

        val AVAILABLE_FALL_RESPONSE_TIMES = listOf(5, 10, 12, 15, 20, 30)
        val AVAILABLE_NEARBY_PRESENCE_INTERVALS = listOf(0, 5, 10, 30, 60)

        const val KEY_PHYSICAL_SOS_CANCELLATION_WINDOW_SECONDS = "physical_sos_cancellation_window_seconds"
        const val DEFAULT_PHYSICAL_SOS_CANCELLATION_WINDOW_SECONDS = 5
        val AVAILABLE_PHYSICAL_SOS_CANCELLATION_WINDOWS = listOf(3, 5, 10, 15, 30)

        const val KEY_IN_APP_SOS_ACTIVATION_DELAY_SECONDS = "in_app_sos_activation_delay_seconds"
        const val DEFAULT_IN_APP_SOS_ACTIVATION_DELAY_SECONDS = 10
        val AVAILABLE_IN_APP_SOS_ACTIVATION_DELAYS = listOf(0, 5, 10, 15, 30, 60)
    }

    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getEmergencySoundId(): String {
        return try {
            prefs.getString(KEY_EMERGENCY_SOUND_ID, DEFAULT_EMERGENCY_SOUND_ID) ?: DEFAULT_EMERGENCY_SOUND_ID
        } catch (e: Exception) {
            DEFAULT_EMERGENCY_SOUND_ID
        }
    }

    fun setEmergencySoundId(soundId: String) {
        prefs.edit().putString(KEY_EMERGENCY_SOUND_ID, soundId).apply()
    }

    fun getEmergencyCustomSoundUri(): String? {
        return try {
            prefs.getString(KEY_EMERGENCY_CUSTOM_SOUND_URI, null)
        } catch (e: Exception) {
            null
        }
    }

    fun getEmergencyCustomSoundName(): String? {
        return try {
            prefs.getString(KEY_EMERGENCY_CUSTOM_SOUND_NAME, null)
        } catch (e: Exception) {
            null
        }
    }

    fun setEmergencyCustomSound(uri: String?, name: String?) {
        val editor = prefs.edit()
        if (uri != null) {
            editor.putString(KEY_EMERGENCY_CUSTOM_SOUND_URI, uri)
            if (name != null) {
                editor.putString(KEY_EMERGENCY_CUSTOM_SOUND_NAME, name)
            }
            editor.putString(KEY_EMERGENCY_SOUND_ID, SOUND_CUSTOM)
        } else {
            editor.remove(KEY_EMERGENCY_CUSTOM_SOUND_URI)
            editor.remove(KEY_EMERGENCY_CUSTOM_SOUND_NAME)
            if (getEmergencySoundId() == SOUND_CUSTOM) {
                editor.putString(KEY_EMERGENCY_SOUND_ID, DEFAULT_EMERGENCY_SOUND_ID)
            }
        }
        editor.apply()
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

    fun getPhysicalSosCancellationWindowSeconds(): Int {
        val value = try {
            prefs.getInt(KEY_PHYSICAL_SOS_CANCELLATION_WINDOW_SECONDS, DEFAULT_PHYSICAL_SOS_CANCELLATION_WINDOW_SECONDS)
        } catch (e: Exception) {
            DEFAULT_PHYSICAL_SOS_CANCELLATION_WINDOW_SECONDS
        }
        return if (value in AVAILABLE_PHYSICAL_SOS_CANCELLATION_WINDOWS) value else DEFAULT_PHYSICAL_SOS_CANCELLATION_WINDOW_SECONDS
    }

    fun setPhysicalSosCancellationWindowSeconds(seconds: Int) {
        val validValue = if (seconds in AVAILABLE_PHYSICAL_SOS_CANCELLATION_WINDOWS) seconds else DEFAULT_PHYSICAL_SOS_CANCELLATION_WINDOW_SECONDS
        prefs.edit().putInt(KEY_PHYSICAL_SOS_CANCELLATION_WINDOW_SECONDS, validValue).apply()
    }

    fun getInAppSosActivationDelaySeconds(): Int {
        val value = try {
            prefs.getInt(KEY_IN_APP_SOS_ACTIVATION_DELAY_SECONDS, DEFAULT_IN_APP_SOS_ACTIVATION_DELAY_SECONDS)
        } catch (e: Exception) {
            DEFAULT_IN_APP_SOS_ACTIVATION_DELAY_SECONDS
        }
        return if (value in AVAILABLE_IN_APP_SOS_ACTIVATION_DELAYS) value else DEFAULT_IN_APP_SOS_ACTIVATION_DELAY_SECONDS
    }

    fun setInAppSosActivationDelaySeconds(seconds: Int) {
        val validValue = if (seconds in AVAILABLE_IN_APP_SOS_ACTIVATION_DELAYS) seconds else DEFAULT_IN_APP_SOS_ACTIVATION_DELAY_SECONDS
        prefs.edit().putInt(KEY_IN_APP_SOS_ACTIVATION_DELAY_SECONDS, validValue).apply()
    }
}
