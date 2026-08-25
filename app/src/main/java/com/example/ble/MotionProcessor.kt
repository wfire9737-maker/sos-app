package com.example.ble

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.abs

/**
 * Real-time Motion Processor and Fall Detection State Machine for ESP32 MPU6050 BLE telemetry.
 *
 * Configurable thresholds are provided as initial testing starting points (not medically certified).
 * Processing is performed asynchronously on Dispatchers.Default to ensure the BluetoothGattCallback
 * is never blocked.
 */
class MotionProcessor(
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
) {
    companion object {
        private const val TAG = "MotionProcessor"

        // Configurable thresholds for experimental fall detection
        const val LOW_G_THRESHOLD = 0.5f // g
        const val HIGH_G_IMPACT_THRESHOLD = 2.5f // g
        const val DIRECT_SHOCK_THRESHOLD = 3.5f // g
        const val HIGH_GYRO_THRESHOLD = 2.0f // rad/s
        const val FREE_FALL_WINDOW_MS = 1500L
        const val STABILIZATION_WINDOW_MS = 800L
        const val COOLDOWN_MS = 15000L // 15s cooldown to prevent repeated triggers
        const val MAX_BUFFER_SIZE = 25
    }

    private val _motionState = MutableStateFlow(MotionState.NORMAL)
    val motionState: StateFlow<MotionState> = _motionState.asStateFlow()

    // Bounded ring buffer for recent sensor readings
    private val _recentReadings = MutableStateFlow<List<Mpu6050Reading>>(emptyList())
    val recentReadings: StateFlow<List<Mpu6050Reading>> = _recentReadings.asStateFlow()

    private val ringBuffer = ArrayDeque<Mpu6050Reading>(MAX_BUFFER_SIZE)

    // State machine tracking
    private var isFreeFallCandidate = false
    private var freeFallTimestamp = 0L
    private var isImpactCandidate = false
    private var impactTimestamp = 0L
    private var inCooldown = false
    private var lastFallEventTimestamp = 0L
    private val fallEventCounter = AtomicInteger(0)

    // Callback when a candidate fall is confirmed by motion analysis
    var onPossibleFallDetected: ((Mpu6050Reading, String) -> Unit)? = null

    /**
     * Ingests a new real MPU6050 reading from BLE outside the GATT callback thread.
     */
    fun onNewReading(reading: Mpu6050Reading) {
        scope.launch {
            processReadingInternal(reading)
        }
    }

    private fun processReadingInternal(reading: Mpu6050Reading) {
        val now = System.currentTimeMillis()

        // 1. Maintain bounded sensor history buffer (max 25 entries)
        synchronized(ringBuffer) {
            if (ringBuffer.size >= MAX_BUFFER_SIZE) {
                ringBuffer.removeFirst()
            }
            ringBuffer.addLast(reading)
            _recentReadings.value = ringBuffer.toList()
        }

        // Log real telemetry
        Log.d(TAG, "MPU_BLE: AX=%.2f AY=%.2f AZ=%.2f".format(reading.accelerationX, reading.accelerationY, reading.accelerationZ))
        Log.d(TAG, "MPU_BLE: GX=%.2f GY=%.2f GZ=%.2f".format(reading.gyroX, reading.gyroY, reading.gyroZ))
        Log.d(TAG, "MPU_BLE: MAG=%.2fg (Calc: %.2fg)".format(reading.accelerationMagnitudeG, reading.calculatedMagnitudeG))

        val mag = reading.accelerationMagnitudeG
        val totalGyro = abs(reading.gyroX) + abs(reading.gyroY) + abs(reading.gyroZ)

        // 2. Check cooldown recovery
        if (inCooldown) {
            if (now - lastFallEventTimestamp >= COOLDOWN_MS) {
                inCooldown = false
                isFreeFallCandidate = false
                isImpactCandidate = false
                _motionState.value = MotionState.NORMAL
                Log.d(TAG, "MOTION: NORMAL (cooldown expired, detector recovered)")
            } else {
                return // Suppress new events during active cooldown
            }
        }

        // 3. Fall Detection State Machine
        if (isFreeFallCandidate) {
            // Check if free fall window has timed out without an impact
            if (now - freeFallTimestamp > FREE_FALL_WINDOW_MS) {
                isFreeFallCandidate = false
                _motionState.value = MotionState.NORMAL
                Log.d(TAG, "MOTION: NORMAL (free-fall window expired without impact)")
            } else if (mag >= HIGH_G_IMPACT_THRESHOLD) {
                // Free fall immediately followed by impact candidate!
                isImpactCandidate = true
                impactTimestamp = now
                _motionState.value = MotionState.POSSIBLE_IMPACT
                Log.d(TAG, "MOTION: possible impact detected after free-fall (MAG=%.2fg)".format(mag))
            }
        } else if (isImpactCandidate) {
            // Impact occurred, check post-impact state (settling / stabilization)
            if (now - impactTimestamp >= STABILIZATION_WINDOW_MS) {
                // Confirm possible fall pattern
                val eventId = "FALL_MPU_${fallEventCounter.incrementAndGet()}_${now}"
                _motionState.value = MotionState.POSSIBLE_FALL
                Log.d(TAG, "MOTION: possible fall detected (Event ID: $eventId)")

                inCooldown = true
                lastFallEventTimestamp = now
                isFreeFallCandidate = false
                isImpactCandidate = false

                // Notify listener to initiate fall confirmation / countdown dialog
                onPossibleFallDetected?.invoke(reading, eventId)
            }
        } else {
            // In NORMAL state
            if (mag < LOW_G_THRESHOLD) {
                // Candidate low-g / weightlessness / free-fall detected
                isFreeFallCandidate = true
                freeFallTimestamp = now
                _motionState.value = MotionState.POSSIBLE_FREE_FALL
                Log.d(TAG, "MOTION: possible low-g event (MAG=%.2fg < %.2fg)".format(mag, LOW_G_THRESHOLD))
            } else if (mag >= DIRECT_SHOCK_THRESHOLD && totalGyro >= HIGH_GYRO_THRESHOLD) {
                // Direct violent impact shock with significant rotational motion
                isImpactCandidate = true
                impactTimestamp = now
                _motionState.value = MotionState.POSSIBLE_IMPACT
                Log.d(TAG, "MOTION: possible impact detected via direct high-g shock (MAG=%.2fg, Gyro=%.2f)".format(mag, totalGyro))
            } else {
                if (_motionState.value != MotionState.NORMAL) {
                    _motionState.value = MotionState.NORMAL
                    Log.d(TAG, "MOTION: NORMAL")
                }
            }
        }
    }

    /**
     * Resets the motion state to NORMAL (e.g. when user cancels fall dialog or upon reconnection).
     */
    fun resetToNormal() {
        isFreeFallCandidate = false
        isImpactCandidate = false
        inCooldown = false
        _motionState.value = MotionState.NORMAL
        Log.d(TAG, "MOTION: Reset to NORMAL")
    }

    /**
     * Clears all sensor state on disconnection.
     */
    fun reset() {
        isFreeFallCandidate = false
        isImpactCandidate = false
        inCooldown = false
        _motionState.value = MotionState.NORMAL
        synchronized(ringBuffer) {
            ringBuffer.clear()
            _recentReadings.value = emptyList()
        }
    }
}
