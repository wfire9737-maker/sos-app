package com.example.service

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

data class HourlyBatteryStats(
    val hourLabel: String,
    val levelPercentage: Int
)

data class IncidentCategoryStat(
    val categoryName: String,
    val count: Int,
    val colorHex: Long
)

data class HeatMapPoint(
    val id: String = UUID.randomUUID().toString(),
    val zoneName: String,
    val x: Float, // proportional X (0f to 1f)
    val y: Float, // proportional Y (0f to 1f)
    val intensity: Int // 1 to 10
)

class AnalyticsService(private val context: Context) {

    // Metrics Overview
    private val _totalEmergencies = MutableStateFlow(27)
    val totalEmergencies: StateFlow<Int> = _totalEmergencies.asStateFlow()

    private val _weeklyEmergencies = MutableStateFlow(4)
    val weeklyEmergencies: StateFlow<Int> = _weeklyEmergencies.asStateFlow()

    private val _monthlyEmergencies = MutableStateFlow(12)
    val monthlyEmergencies: StateFlow<Int> = _monthlyEmergencies.asStateFlow()

    private val _averageResponseTimeSeconds = MutableStateFlow(194) // ~3.2 minutes
    val averageResponseTimeSeconds: StateFlow<Int> = _averageResponseTimeSeconds.asStateFlow()

    private val _deviceUptimePercentage = MutableStateFlow(99.64f)
    val deviceUptimePercentage: StateFlow<Float> = _deviceUptimePercentage.asStateFlow()

    // Battery Statistics
    private val _batteryHistory = MutableStateFlow<List<HourlyBatteryStats>>(
        listOf(
            HourlyBatteryStats("08:00", 100),
            HourlyBatteryStats("10:00", 94),
            HourlyBatteryStats("12:00", 88),
            HourlyBatteryStats("14:00", 81),
            HourlyBatteryStats("16:00", 74),
            HourlyBatteryStats("18:00", 69),
            HourlyBatteryStats("20:00", 65),
            HourlyBatteryStats("22:00", 58)
        )
    )
    val batteryHistory: StateFlow<List<HourlyBatteryStats>> = _batteryHistory.asStateFlow()

    // Emergency Categories Distribution
    private val _emergencyCategories = MutableStateFlow<List<IncidentCategoryStat>>(
        listOf(
            IncidentCategoryStat("Hard Fall Detected", 13, 0xFFEF4444),
            IncidentCategoryStat("Voice SOS Trigger", 6, 0xFF3B82F6),
            IncidentCategoryStat("Manual SOS Button", 5, 0xFFF59E0B),
            IncidentCategoryStat("ESP32 Kinetic Shake", 3, 0xFF10B981)
        )
    )
    val emergencyCategories: StateFlow<List<IncidentCategoryStat>> = _emergencyCategories.asStateFlow()

    // Heat Map Coordinate Nodes
    private val _heatMapPoints = MutableStateFlow<List<HeatMapPoint>>(
        listOf(
            HeatMapPoint(zoneName = "Downtown Core", x = 0.5f, y = 0.45f, intensity = 9),
            HeatMapPoint(zoneName = "Industrial Park", x = 0.72f, y = 0.3f, intensity = 4),
            HeatMapPoint(zoneName = "West Residential", x = 0.25f, y = 0.6f, intensity = 7),
            HeatMapPoint(zoneName = "North Highway", x = 0.48f, y = 0.15f, intensity = 2),
            HeatMapPoint(zoneName = "Metro Transit Hub", x = 0.55f, y = 0.72f, intensity = 8),
            HeatMapPoint(zoneName = "South Suburbs", x = 0.35f, y = 0.85f, intensity = 5)
        )
    )
    val heatMapPoints: StateFlow<List<HeatMapPoint>> = _heatMapPoints.asStateFlow()

    fun logSimulatedIncident(triggerType: String) {
        _totalEmergencies.value = _totalEmergencies.value + 1
        _weeklyEmergencies.value = _weeklyEmergencies.value + 1
        _monthlyEmergencies.value = _monthlyEmergencies.value + 1

        val currentCategories = _emergencyCategories.value.toMutableList()
        val index = currentCategories.indexOfFirst { it.categoryName.contains(triggerType, ignoreCase = true) }
        if (index != -1) {
            val oldStat = currentCategories[index]
            currentCategories[index] = oldStat.copy(count = oldStat.count + 1)
        } else {
            currentCategories.add(IncidentCategoryStat(triggerType, 1, 0xFF8B5CF6))
        }
        _emergencyCategories.value = currentCategories

        // Add heat map point near downtown with random jitter
        val newPoint = HeatMapPoint(
            zoneName = "Live Incident Site",
            x = 0.4f + (Math.random() * 0.2).toFloat(),
            y = 0.4f + (Math.random() * 0.2).toFloat(),
            intensity = 8
        )
        _heatMapPoints.value = _heatMapPoints.value + newPoint
    }
}
