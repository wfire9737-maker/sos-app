package com.example.model

data class SensorReading(
    val timestampMs: Long = 0L,
    val ax: Float = 0f,
    val ay: Float = 0f,
    val az: Float = 0f,
    val gx: Float = 0f,
    val gy: Float = 0f,
    val gz: Float = 0f
) {
    fun toMap(): Map<String, Any> {
        return mapOf(
            "timestampMs" to timestampMs,
            "ax" to ax,
            "ay" to ay,
            "az" to az,
            "gx" to gx,
            "gy" to gy,
            "gz" to gz
        )
    }

    companion object {
        fun fromMap(map: Map<String, Any>): SensorReading {
            return SensorReading(
                timestampMs = (map["timestampMs"] as? Number)?.toLong() ?: 0L,
                ax = (map["ax"] as? Number)?.toFloat() ?: 0f,
                ay = (map["ay"] as? Number)?.toFloat() ?: 0f,
                az = (map["az"] as? Number)?.toFloat() ?: 0f,
                gx = (map["gx"] as? Number)?.toFloat() ?: 0f,
                gy = (map["gy"] as? Number)?.toFloat() ?: 0f,
                gz = (map["gz"] as? Number)?.toFloat() ?: 0f
            )
        }
    }
}

data class TimelineEvent(
    val time: String = "",
    val event: String = "",
    val description: String = "",
    val iconEmoji: String = "⏱️"
) {
    fun toMap(): Map<String, Any> {
        return mapOf(
            "time" to time,
            "event" to event,
            "description" to description,
            "iconEmoji" to iconEmoji
        )
    }

    companion object {
        fun fromMap(map: Map<String, Any>): TimelineEvent {
            return TimelineEvent(
                time = map["time"] as? String ?: "",
                event = map["event"] as? String ?: "",
                description = map["description"] as? String ?: "",
                iconEmoji = map["iconEmoji"] as? String ?: "⏱️"
            )
        }
    }
}

data class AiAnalysisResult(
    val id: String = "",
    val alertId: String = "",
    val confidenceScore: Int = 94,
    val falseAlarmProbability: Int = 6,
    val motionPattern: String = "SUDDEN_DECELERATION",
    val activityRecognition: String = "FALL DETECTED (LAYING)",
    val riskLevel: String = "CRITICAL",
    val recommendedAction: String = "IMMEDIATE PARAMEDICS DISPATCH",
    val sensorData: List<SensorReading> = emptyList(),
    val timeline: List<TimelineEvent> = emptyList(),
    val timestampMs: Long = System.currentTimeMillis()
) {
    fun toMap(): Map<String, Any> {
        return mapOf(
            "id" to id,
            "alertId" to alertId,
            "confidenceScore" to confidenceScore,
            "falseAlarmProbability" to falseAlarmProbability,
            "motionPattern" to motionPattern,
            "activityRecognition" to activityRecognition,
            "riskLevel" to riskLevel,
            "recommendedAction" to recommendedAction,
            "sensorData" to sensorData.map { it.toMap() },
            "timeline" to timeline.map { it.toMap() },
            "timestampMs" to timestampMs
        )
    }

    companion object {
        @Suppress("UNCHECKED_CAST")
        fun fromMap(map: Map<String, Any>): AiAnalysisResult {
            val rawSensorList = map["sensorData"] as? List<Map<String, Any>> ?: emptyList()
            val sensorList = rawSensorList.map { SensorReading.fromMap(it) }

            val rawTimelineList = map["timeline"] as? List<Map<String, Any>> ?: emptyList()
            val timelineList = rawTimelineList.map { TimelineEvent.fromMap(it) }

            return AiAnalysisResult(
                id = map["id"] as? String ?: "",
                alertId = map["alertId"] as? String ?: "",
                confidenceScore = (map["confidenceScore"] as? Number)?.toInt() ?: 94,
                falseAlarmProbability = (map["falseAlarmProbability"] as? Number)?.toInt() ?: 6,
                motionPattern = map["motionPattern"] as? String ?: "SUDDEN_DECELERATION",
                activityRecognition = map["activityRecognition"] as? String ?: "FALL DETECTED (LAYING)",
                riskLevel = map["riskLevel"] as? String ?: "CRITICAL",
                recommendedAction = map["recommendedAction"] as? String ?: "IMMEDIATE PARAMEDICS DISPATCH",
                sensorData = sensorList,
                timeline = timelineList,
                timestampMs = (map["timestampMs"] as? Number)?.toLong() ?: System.currentTimeMillis()
            )
        }
    }
}
