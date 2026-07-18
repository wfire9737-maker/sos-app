package com.example.service

import android.content.Context
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log

class AlarmVibratorService(private val context: Context) {
    private var ringtone: Ringtone? = null
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

    fun startAlarm() {
        try {
            if (ringtone == null) {
                var alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                if (alarmUri == null) {
                    alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                }
                if (alarmUri == null) {
                    alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                }
                
                ringtone = RingtoneManager.getRingtone(context, alarmUri)
                
                // Set audio attributes to use the Alarm stream
                ringtone?.let {
                    val attributes = AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        it.audioAttributes = attributes
                    }
                }
            }

            if (ringtone?.isPlaying == false) {
                ringtone?.play()
                Log.d("AlarmVibratorService", "Emergency Alarm Started")
            }
        } catch (e: Exception) {
            Log.e("AlarmVibratorService", "Failed to play emergency alarm: ${e.message}")
        }
    }

    fun stopAlarm() {
        try {
            ringtone?.let {
                if (it.isPlaying) {
                    it.stop()
                    Log.d("AlarmVibratorService", "Emergency Alarm Stopped")
                }
            }
        } catch (e: Exception) {
            Log.e("AlarmVibratorService", "Failed to stop alarm: ${e.message}")
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
