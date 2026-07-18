package com.example.model

import java.util.UUID

data class AISensorReading(
    val timestampMs: Long = System.currentTimeMillis(),
    val ax: Float = 0f,
    val ay: Float = 0f,
    val az: Float = 0f,
    val gx: Float = 0f,
    val gy: Float = 0f,
    val gz: Float = 0f
)

data class AITimelineEvent(
    val timeString: String,
    val eventName: String,
    val eventDescription: String,
    val categoryEmoji: String = "⏱️"
)

data class AIAnalysisModel(
    val id: String = UUID.randomUUID().toString(),
    val alertId: String = "none",
    val confidenceScore: Int = 94,
    val falseAlarmProbability: Int = 6,
    val motionAnalysis: String = "SUDDEN_DECELERATION",
    val activityRecognition: String = "FALL DETECTED (LAYING)",
    val riskLevel: String = "CRITICAL",
    val suggestedAction: String = "IMMEDIATE PARAMEDICS DISPATCH",
    val sensorReadings: List<AISensorReading> = emptyList(),
    val timeline: List<AITimelineEvent> = emptyList(),
    val timestampMs: Long = System.currentTimeMillis()
)
