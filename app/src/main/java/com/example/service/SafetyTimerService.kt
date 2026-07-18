package com.example.service

import android.content.Context
import com.example.model.NotificationCategory
import com.example.model.NotificationModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

enum class SafetyTimerStatus {
    INACTIVE,
    ACTIVE,
    WARNING, // less than 60s
    EXPIRED,
    CHECKED_IN
}

class SafetyTimerService(
    private val context: Context,
    private val notificationProvider: NotificationProvider
) {
    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val _status = MutableStateFlow(SafetyTimerStatus.INACTIVE)
    val status: StateFlow<SafetyTimerStatus> = _status.asStateFlow()

    private val _totalDurationSeconds = MutableStateFlow(0)
    val totalDurationSeconds: StateFlow<Int> = _totalDurationSeconds.asStateFlow()

    private val _secondsRemaining = MutableStateFlow(0)
    val secondsRemaining: StateFlow<Int> = _secondsRemaining.asStateFlow()

    private val _activityDescription = MutableStateFlow("")
    val activityDescription: StateFlow<String> = _activityDescription.asStateFlow()

    private var timerJob: Job? = null
    private var reminderTriggered = false

    // Callbacks to ViewModel
    var onTimerExpiredCallback: (() -> Unit)? = null

    fun setTimer(minutes: Int, description: String) {
        timerJob?.cancel()
        reminderTriggered = false
        val seconds = minutes * 60
        _totalDurationSeconds.value = seconds
        _secondsRemaining.value = seconds
        _activityDescription.value = description
        _status.value = SafetyTimerStatus.ACTIVE

        notificationProvider.addNotification(
            NotificationModel(
                title = "Safety Timer Activated",
                body = "Timer set for $minutes mins ($description). Safe check-in required.",
                category = NotificationCategory.SAFE_ARRIVAL,
                severity = "INFO"
            )
        )

        startCountdown()
    }

    private fun startCountdown() {
        timerJob = serviceScope.launch {
            while (_secondsRemaining.value > 0) {
                delay(1000)
                _secondsRemaining.value = _secondsRemaining.value - 1

                val remaining = _secondsRemaining.value
                if (remaining <= 60 && remaining > 0 && !reminderTriggered) {
                    reminderTriggered = true
                    _status.value = SafetyTimerStatus.WARNING
                    pushWarningNotification()
                }
            }

            _status.value = SafetyTimerStatus.EXPIRED
            notificationProvider.addNotification(
                NotificationModel(
                    title = "Safety Timer EXPIRED!",
                    body = "Wearer failed to check-in for: ${_activityDescription.value}. Dispatching SOS immediately.",
                    category = NotificationCategory.EMERGENCY_ALERTS,
                    severity = "CRITICAL"
                )
            )

            withContext(Dispatchers.Main) {
                onTimerExpiredCallback?.invoke()
            }
        }
    }

    fun extendTimer(extraMinutes: Int) {
        if (_status.value == SafetyTimerStatus.INACTIVE) return

        val addedSeconds = extraMinutes * 60
        _totalDurationSeconds.value = _totalDurationSeconds.value + addedSeconds
        _secondsRemaining.value = _secondsRemaining.value + addedSeconds

        if (_secondsRemaining.value > 60) {
            _status.value = SafetyTimerStatus.ACTIVE
            reminderTriggered = false
        }

        notificationProvider.addNotification(
            NotificationModel(
                title = "Safety Timer Extended",
                body = "Added $extraMinutes mins. New time remaining: ${formatTimeRemaining(_secondsRemaining.value)}",
                category = NotificationCategory.SAFE_ARRIVAL,
                severity = "INFO"
            )
        )
    }

    fun cancelTimer() {
        timerJob?.cancel()
        timerJob = null
        _status.value = SafetyTimerStatus.INACTIVE
        _secondsRemaining.value = 0
        _totalDurationSeconds.value = 0

        notificationProvider.addNotification(
            NotificationModel(
                title = "Safety Timer Cancelled",
                body = "Timer was cancelled. Standby monitoring active.",
                category = NotificationCategory.SAFE_ARRIVAL,
                severity = "INFO"
            )
        )
    }

    fun safeCheckIn() {
        timerJob?.cancel()
        timerJob = null
        _status.value = SafetyTimerStatus.CHECKED_IN
        val desc = _activityDescription.value

        notificationProvider.addNotification(
            NotificationModel(
                title = "Checked In Safely!",
                body = "Successfully checked in from: $desc.",
                category = NotificationCategory.SAFE_ARRIVAL,
                severity = "INFO"
            )
        )

        serviceScope.launch {
            delay(3000)
            if (_status.value == SafetyTimerStatus.CHECKED_IN) {
                _status.value = SafetyTimerStatus.INACTIVE
                _secondsRemaining.value = 0
                _totalDurationSeconds.value = 0
                _activityDescription.value = ""
            }
        }
    }

    private fun pushWarningNotification() {
        notificationProvider.addNotification(
            NotificationModel(
                title = "⚠️ Safety Timer Reminder",
                body = "Only 1 minute left to check in for: ${_activityDescription.value}. SOS will trigger automatically!",
                category = NotificationCategory.EMERGENCY_ALERTS,
                severity = "WARNING"
            )
        )
    }

    private fun formatTimeRemaining(totalSecs: Int): String {
        val mins = totalSecs / 60
        val secs = totalSecs % 60
        return String.format(Locale.US, "%02d:%02d", mins, secs)
    }

    fun cleanup() {
        timerJob?.cancel()
    }
}
