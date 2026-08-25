package com.example.ble

import kotlin.math.sqrt

/**
 * Real MPU6050 sensor telemetry model received from ESP32 over BLE.
 *
 * Payload format from ESP32:
 * AX:0.15,AY:-0.32,AZ:9.74,GX:0.01,GY:-0.03,GZ:0.02,MAG:0.99
 *
 * AX, AY, AZ = Acceleration in m/s²
 * GX, GY, GZ = Angular velocity in rad/s
 * MAG = Acceleration magnitude in g
 */
data class Mpu6050Reading(
    val accelerationX: Float,
    val accelerationY: Float,
    val accelerationZ: Float,
    val gyroX: Float,
    val gyroY: Float,
    val gyroZ: Float,
    val accelerationMagnitudeG: Float,
    val calculatedMagnitudeG: Float = sqrt(
        (accelerationX * accelerationX + accelerationY * accelerationY + accelerationZ * accelerationZ).toDouble()
    ).toFloat() / 9.80665f,
    val receivedAt: Long = System.currentTimeMillis(),
    val rawPayload: String = ""
) {
    fun getFormattedAcceleration(): String =
        "X: %.2f m/s², Y: %.2f m/s², Z: %.2f m/s²".format(accelerationX, accelerationY, accelerationZ)

    fun getFormattedGyro(): String =
        "X: %.2f rad/s, Y: %.2f rad/s, Z: %.2f rad/s".format(gyroX, gyroY, gyroZ)

    fun getFormattedMagnitude(): String =
        "%.2f g (Calc: %.2f g)".format(accelerationMagnitudeG, calculatedMagnitudeG)

    fun getAgeInSeconds(): Long = ((System.currentTimeMillis() - receivedAt) / 1000L).coerceAtLeast(0L)

    fun getFreshnessDescription(): String {
        val ageSec = getAgeInSeconds()
        return when {
            ageSec < 5 -> "Live Stream (${ageSec}s ago)"
            ageSec < 30 -> "Fresh (${ageSec}s ago)"
            ageSec < 120 -> "Recent (${ageSec}s ago)"
            else -> "Last Known (${ageSec / 60}m ago)"
        }
    }

    fun formattedTime(): String {
        return java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(receivedAt))
    }

    companion object {
        private const val TAG = "MpuParser"

        /**
         * Safely parses raw ESP32 MPU6050 BLE notification payload.
         * Example: "AX:0.15,AY:-0.32,AZ:9.74,GX:0.01,GY:-0.03,GZ:0.02,MAG:0.99"
         *
         * Returns null if any required key is missing or non-numeric, preventing crashes.
         */
        fun parse(payload: String): Mpu6050Reading? {
            val clean = payload.trim()
            if (clean.isEmpty()) return null

            try {
                val tokens = clean.split(",")
                val map = mutableMapOf<String, Float>()

                for (token in tokens) {
                    val pair = token.split(":")
                    if (pair.size == 2) {
                        val key = pair[0].trim().uppercase()
                        val value = pair[1].trim().toFloatOrNull()
                        if (value != null) {
                            map[key] = value
                        }
                    }
                }

                val ax = map["AX"] ?: return null
                val ay = map["AY"] ?: return null
                val az = map["AZ"] ?: return null
                val gx = map["GX"] ?: return null
                val gy = map["GY"] ?: return null
                val gz = map["GZ"] ?: return null
                val mag = map["MAG"] ?: sqrt(
                    (ax * ax + ay * ay + az * az).toDouble()
                ).toFloat() / 9.80665f

                return Mpu6050Reading(
                    accelerationX = ax,
                    accelerationY = ay,
                    accelerationZ = az,
                    gyroX = gx,
                    gyroY = gy,
                    gyroZ = gz,
                    accelerationMagnitudeG = mag,
                    receivedAt = System.currentTimeMillis(),
                    rawPayload = clean
                )
            } catch (e: Exception) {
                return null
            }
        }
    }
}

/**
 * Real-time motion states categorized from MPU6050 motion readings.
 */
enum class MotionState(val displayName: String) {
    NORMAL("Normal"),
    POSSIBLE_FREE_FALL("Possible Free-Fall"),
    POSSIBLE_IMPACT("Possible Impact"),
    POSSIBLE_FALL("Possible Fall Detected")
}

/**
 * High-level connection and reception state of the ESP32 MPU6050 sensor.
 */
sealed class MpuHardwareState {
    object Unavailable : MpuHardwareState()
    object Connecting : MpuHardwareState()
    data class Receiving(
        val reading: Mpu6050Reading,
        val motionState: MotionState = MotionState.NORMAL
    ) : MpuHardwareState()
    data class Error(val message: String) : MpuHardwareState()
}
