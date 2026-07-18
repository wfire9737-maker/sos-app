package com.example.service

import android.content.Context
import android.util.Log
import com.example.model.FallEvent
import com.example.repository.FallRepository
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

    // Active Gait State Flow
    private val _currentState = MutableStateFlow("STANDING") // "WALKING", "RUNNING", "SITTING", "STANDING", "SUDDEN_FALL_DETECTED", "FALL_COUNTDOWN", "FALL_CANCELLED", "FALL_SOS_AUTO_TRIGGER"
    val currentState: StateFlow<String> = _currentState.asStateFlow()

    // Countdown state
    private val _countdownSeconds = MutableStateFlow(15)
    val countdownSeconds: StateFlow<Int> = _countdownSeconds.asStateFlow()

    private var countdownJob: Job? = null
    private var simulationJob: Job? = null

    // Callback when SOS is fully triggered via fall expiry
    var onSosTriggeredCallback: (() -> Unit)? = null

    init {
        // Start MPU6050 polling simulation
        startMpuPolling()
    }

    private fun startMpuPolling() {
        simulationJob?.cancel()
        simulationJob = serviceScope.launch {
            while (isActive) {
                // If we are not currently in a fall countdown, let's keep simulating general states
                val state = _currentState.value
                if (state != "FALL_COUNTDOWN" && state != "FALL_SOS_AUTO_TRIGGER") {
                    // Random passive transitions to keep the simulation visually dynamic
                    delay(8000)
                    val states = listOf("STANDING", "SITTING", "WALKING", "RUNNING")
                    val randomState = states.random()
                    setGaitState(randomState, "Simulated gait change from passive IMU filter.")
                } else {
                    delay(1000)
                }
            }
        }
    }

    fun triggerSimulatedFall() {
        setGaitState(
            "SUDDEN_FALL_DETECTED",
            "High impact IMU spike: ax=3.8G, ay=-2.4G, az=0.15G. Yaw tilt 84 degrees."
        )
        startFallCountdown()
    }

    private fun startFallCountdown() {
        countdownJob?.cancel()
        _currentState.value = "FALL_COUNTDOWN"
        _countdownSeconds.value = 15

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
            onSosTriggeredCallback?.invoke()
        }
    }

    fun cancelFallCountdown() {
        countdownJob?.cancel()
        countdownJob = null
        setGaitState(
            "FALL_CANCELLED",
            "Wearer pressed Cancel on 15s countdown modal. Restored standby monitoring."
        )
        // Reset to standing
        _currentState.value = "STANDING"
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
        simulationJob?.cancel()
        countdownJob?.cancel()
    }
}
