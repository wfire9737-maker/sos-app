package com.example.service

import android.content.Context
import android.util.Log
import com.example.model.FallEvent
import com.example.repository.FallRepository
import com.example.repository.SettingsRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class FallDetectionService(
    private val context: Context,
    private val fallRepository: FallRepository
) {
    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val settingsRepository = SettingsRepository(context)

    // Active Gait State Flow
    private val _currentState = MutableStateFlow("STANDING") // "WALKING", "RUNNING", "SITTING", "STANDING", "SUDDEN_FALL_DETECTED", "FALL_COUNTDOWN", "FALL_CANCELLED", "FALL_SOS_AUTO_TRIGGER"
    val currentState: StateFlow<String> = _currentState.asStateFlow()

    // Countdown state (initialized from persisted setting or default 12s)
    private val _countdownSeconds = MutableStateFlow(settingsRepository.getFallResponseDelaySeconds())
    val countdownSeconds: StateFlow<Int> = _countdownSeconds.asStateFlow()

    private var countdownJob: Job? = null

    // Callback when SOS is fully triggered via fall expiry
    var onSosTriggeredCallback: (() -> Unit)? = null

    // Callback when fall countdown is cancelled by wearer
    var onFallCancelledCallback: (() -> Unit)? = null

    init {
        // Standby monitoring initialized
    }

    fun triggerFall() {
        Log.d("SOS_FALL_DEBUG", "FallDetectionService.triggerFall() entered")
        com.example.ble.FallDebugBridge.log("FallDetectionService entered", "triggerFall() entered")

        synchronized(this) {
            val currentStateVal = _currentState.value
            if (currentStateVal == "SUDDEN_FALL_DETECTED" ||
                currentStateVal == "FALL_COUNTDOWN" ||
                currentStateVal == "FALL_SOS_AUTO_TRIGGER" ||
                countdownJob?.isActive == true
            ) {
                Log.d("FallDetectionService", "Ignored duplicate triggerFall() in state: $currentStateVal")
                com.example.ble.FallDebugBridge.log("FallDetectionService", "Ignored duplicate triggerFall() ($currentStateVal)")
                return
            }
            _currentState.value = "SUDDEN_FALL_DETECTED"
        }

        logEvent(
            "SUDDEN_FALL_DETECTED",
            "High impact IMU spike detected."
        )
        startFallCountdown()
    }

    private fun startFallCountdown() {
        val initialSeconds = settingsRepository.getFallResponseDelaySeconds()
        synchronized(this) {
            countdownJob?.cancel()
            _currentState.value = "FALL_COUNTDOWN"
            _countdownSeconds.value = initialSeconds

            countdownJob = serviceScope.launch {
                while (_countdownSeconds.value > 0) {
                    delay(1000)
                    _countdownSeconds.value = _countdownSeconds.value - 1
                    Log.d("FallDetectionService", "Fall countdown tick: ${_countdownSeconds.value}")
                }

                // Countdown reached 0 - Trigger SOS
                setGaitState(
                    "FALL_SOS_AUTO_TRIGGER",
                    "Countdown expired. Fall was not cancelled by wearer. Dispatching SOS workflow."
                )
                try {
                    onSosTriggeredCallback?.invoke()
                } catch (e: Exception) {
                    Log.e("FallDetectionService", "Error dispatching Fall SOS callback", e)
                } finally {
                    synchronized(this@FallDetectionService) {
                        _currentState.value = "STANDING"
                    }
                }
            }
        }
    }

    fun cancelFallCountdown() {
        synchronized(this) {
            countdownJob?.cancel()
            countdownJob = null
            _currentState.value = "STANDING"
        }
        logEvent(
            "FALL_CANCELLED",
            "Wearer pressed Cancel on fall response countdown modal. Restored standby monitoring."
        )
        onFallCancelledCallback?.invoke()
    }

    fun setGaitState(state: String, details: String) {
        _currentState.value = state
        logEvent(state, details)
    }

    private fun logEvent(state: String, details: String) {
        serviceScope.launch {
            try {
                val event = FallEvent(
                    eventType = state,
                    sensorReadingDetails = details
                )
                fallRepository.insertEvent(event)
                Log.d("FallDetectionService", "Logged fall event: $state")
            } catch (e: Exception) {
                Log.e("FallDetectionService", "Failed to insert fall event to Room", e)
            }
        }
    }

    fun cleanup() {
        synchronized(this) {
            countdownJob?.cancel()
            countdownJob = null
            _currentState.value = "STANDING"
        }
    }
}
