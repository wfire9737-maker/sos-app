package com.example.service

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import com.example.R

class AlarmVibratorService(private val context: Context) {
    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var isVibrating = false

    init {
        initializeVibrator()
    }

    private fun initializeVibrator() {
        try {
            vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
        } catch (e: Exception) {
            Log.e("AlarmVibratorService", "Vibrator initialization failed: ${e.message}")
        }
    }

    private fun createMediaPlayerForSound(): MediaPlayer? {
        val prefs = context.getSharedPreferences("smart_sos_settings", Context.MODE_PRIVATE)
        val soundId = prefs.getString("emergency_sound_id", "builtin_siren") ?: "builtin_siren"

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        if (soundId == "custom") {
            val customUriStr = prefs.getString("emergency_custom_sound_uri", null)
            if (!customUriStr.isNullOrBlank()) {
                try {
                    val customUri = Uri.parse(customUriStr)
                    val player = MediaPlayer().apply {
                        setAudioAttributes(audioAttributes)
                        setDataSource(context, customUri)
                        isLooping = true
                        prepare()
                    }
                    Log.d("AlarmVibratorService", "Custom emergency audio initialized successfully")
                    return player
                } catch (e: Exception) {
                    Log.w("AlarmVibratorService", "Failed to load custom audio URI, falling back to default siren: ${e.message}")
                }
            }
        }

        // Resolve built-in raw resource
        val rawResId = when (soundId) {
            "builtin_rapid_alarm" -> R.raw.emergency_rapid_alarm
            "builtin_warning_pulse" -> R.raw.emergency_warning_pulse
            "builtin_double_beep" -> R.raw.emergency_double_beep
            "builtin_critical_alert" -> R.raw.emergency_critical_alert
            "builtin_evacuation_tone" -> R.raw.emergency_evacuation_tone
            else -> R.raw.sos_emergency_siren // default for builtin_siren, custom fallback, or unknown
        }

        return try {
            MediaPlayer.create(context, rawResId)?.apply {
                setAudioAttributes(audioAttributes)
                isLooping = true
            }
        } catch (e: Exception) {
            Log.e("AlarmVibratorService", "Failed to create MediaPlayer for res $rawResId: ${e.message}")
            if (rawResId != R.raw.sos_emergency_siren) {
                try {
                    MediaPlayer.create(context, R.raw.sos_emergency_siren)?.apply {
                        setAudioAttributes(audioAttributes)
                        isLooping = true
                    }
                } catch (_: Exception) { null }
            } else null
        }
    }

    fun startAlarm() {
        try {
            if (mediaPlayer == null) {
                mediaPlayer = createMediaPlayerForSound()
            }

            mediaPlayer?.let { player ->
                if (!player.isPlaying) {
                    player.start()
                    Log.d("AlarmVibratorService", "Emergency alarm started (looping)")
                }
            } ?: run {
                Log.e("AlarmVibratorService", "Failed to initialize MediaPlayer for emergency alarm")
            }
        } catch (e: Exception) {
            Log.e("AlarmVibratorService", "Failed to play emergency alarm: ${e.message}")
        }
    }

    fun stopAlarm() {
        try {
            mediaPlayer?.let { player ->
                if (player.isPlaying) {
                    player.stop()
                }
                player.release()
                Log.d("AlarmVibratorService", "Emergency Alarm Stopped and MediaPlayer released")
            }
            mediaPlayer = null
        } catch (e: Exception) {
            Log.e("AlarmVibratorService", "Failed to stop alarm: ${e.message}")
            try {
                mediaPlayer?.release()
            } catch (_: Exception) {}
            mediaPlayer = null
        }
    }

    fun startVibration() {
        if (isVibrating) return
        isVibrating = true
        val vib = vibrator ?: return

        try {
            val pattern = longArrayOf(0, 800, 400, 800, 400) // Pulse pattern
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val amplitudes = intArrayOf(0, 255, 0, 255, 0)
                // Repeat index 1 (pulses infinitely until canceled)
                val effect = VibrationEffect.createWaveform(pattern, amplitudes, 1)
                vib.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                vib.vibrate(pattern, 1)
            }
            Log.d("AlarmVibratorService", "Emergency Vibration Pulsing")
        } catch (e: Exception) {
            Log.e("AlarmVibratorService", "Vibration failed: ${e.message}")
        }
    }

    fun stopVibration() {
        isVibrating = false
        try {
            vibrator?.cancel()
            Log.d("AlarmVibratorService", "Emergency Vibration Stopped")
        } catch (e: Exception) {
            Log.e("AlarmVibratorService", "Failed to cancel vibration: ${e.message}")
        }
    }

    fun mute() {
        stopAlarm()
    }

    fun cleanUp() {
        stopAlarm()
        stopVibration()
    }
}
