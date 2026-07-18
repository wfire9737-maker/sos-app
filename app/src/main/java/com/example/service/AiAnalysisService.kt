package com.example.service

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.model.AiAnalysisResult
import com.example.model.SensorReading
import com.example.model.TimelineEvent
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class AiAnalysisService(
    private val context: Context,
    private val firestore: FirebaseFirestore?
) {
    private val _analysisLogs = MutableStateFlow<List<AiAnalysisResult>>(emptyList())
    val analysisLogs: StateFlow<List<AiAnalysisResult>> = _analysisLogs.asStateFlow()

    private val _currentLiveReading = MutableStateFlow<SensorReading>(SensorReading())
    val currentLiveReading: StateFlow<SensorReading> = _currentLiveReading.asStateFlow()

    private val _currentLiveAnalysis = MutableStateFlow<AiAnalysisResult?>(null)
    val currentLiveAnalysis: StateFlow<AiAnalysisResult?> = _currentLiveAnalysis.asStateFlow()

    private val sharedPrefs: SharedPreferences = context.getSharedPreferences("guardian_sos_ai_analysis", Context.MODE_PRIVATE)
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var simulationJob: Job? = null

    init {
        loadLocalLogs()
        syncWithFirestore()
    }

    private fun loadLocalLogs() {
        val jsonStr = sharedPrefs.getString("logs_json", "[]") ?: "[]"
        try {
            val jsonArray = JSONArray(jsonStr)
            val list = mutableListOf<AiAnalysisResult>()
            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)
                val map = jsonToMap(jsonObject)
                list.add(AiAnalysisResult.fromMap(map))
            }
            _analysisLogs.value = list
        } catch (e: Exception) {
            Log.e("AiAnalysisService", "Error loading local AI logs", e)
        }
    }

    private fun saveLocalLogs() {
        try {
            val jsonArray = JSONArray()
            _analysisLogs.value.forEach { item ->
                val json = JSONObject(item.toMap())
                jsonArray.put(json)
            }
            sharedPrefs.edit().putString("logs_json", jsonArray.toString()).apply()
        } catch (e: Exception) {
            Log.e("AiAnalysisService", "Error saving local AI logs", e)
        }
    }

    private fun syncWithFirestore() {
        val fs = firestore ?: return
        fs.collection("ai_analysis")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("AiAnalysisService", "Firestore listen failed.", e)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = mutableListOf<AiAnalysisResult>()
                    for (doc in snapshot) {
                        try {
                            list.add(AiAnalysisResult.fromMap(doc.data))
                        } catch (ex: Exception) {
                            Log.e("AiAnalysisService", "Parsing Firestore AI item failed", ex)
                        }
                    }
                    if (list.isNotEmpty()) {
                        // Merge with any unique local logs
                        val existingIds = list.map { it.id }.toSet()
                        val uniqueLocals = _analysisLogs.value.filter { it.id !in existingIds }
                        _analysisLogs.value = (list + uniqueLocals).sortedByDescending { it.timestampMs }
                    }
                }
            }
    }

    fun addAnalysisResult(result: AiAnalysisResult) {
        val updated = (_analysisLogs.value.filter { it.id != result.id } + result)
            .sortedByDescending { result.timestampMs }
        _analysisLogs.value = updated
        saveLocalLogs()

        // Sync to Firestore
        val fs = firestore
        if (fs != null) {
            serviceScope.launch {
                try {
                    fs.collection("ai_analysis").document(result.id).set(result.toMap())
                    Log.d("AiAnalysisService", "Synced AI log ${result.id} to Firestore")
                } catch (e: Exception) {
                    Log.e("AiAnalysisService", "Failed to sync AI log to Firestore", e)
                }
            }
        }
    }

    fun generateAnalysisForAlert(alertId: String, triggerType: String) {
        val sensorData = generateSampleSensorData(triggerType)
        val timeline = generateSampleTimeline(triggerType)

        val result = when (triggerType) {
            "FALL_DETECTED" -> AiAnalysisResult(
                id = UUID.randomUUID().toString(),
                alertId = alertId,
                confidenceScore = 96,
                falseAlarmProbability = 4,
                motionPattern = "RAPID_ACCELERATION_FOLLOWED_BY_STILLNESS",
                activityRecognition = "FALL DETECTED (STATIC LAYING)",
                riskLevel = "CRITICAL",
                recommendedAction = "IMMEDIATE HELPLINE EN-ROUTE + VOICE VERIFICATION DISPATCH",
                sensorData = sensorData,
                timeline = timeline,
                timestampMs = System.currentTimeMillis()
            )
            "ESP32_BUTTON" -> AiAnalysisResult(
                id = UUID.randomUUID().toString(),
                alertId = alertId,
                confidenceScore = 100,
                falseAlarmProbability = 0,
                motionPattern = "COORDINATED_VOLUNTARY_ARM_MOVEMENT",
                activityRecognition = "MANUAL SOS BUTTON PRESS (STANDING)",
                riskLevel = "HIGH",
                recommendedAction = "CALL USER PHONE AND COMMENCE COORDINATE INTERCEPT",
                sensorData = sensorData,
                timeline = timeline,
                timestampMs = System.currentTimeMillis()
            )
            else -> AiAnalysisResult(
                id = UUID.randomUUID().toString(),
                alertId = alertId,
                confidenceScore = 72,
                falseAlarmProbability = 28,
                motionPattern = "IRREGULAR_LOCOMOTION",
                activityRecognition = "UNKNOWN DISTRESS PATTERN",
                riskLevel = "MEDIUM",
                recommendedAction = "PROBE VOICE CHAT & NOTIFY EMERGENCY SPONSORS",
                sensorData = sensorData,
                timeline = timeline,
                timestampMs = System.currentTimeMillis()
            )
        }

        _currentLiveAnalysis.value = result
        addAnalysisResult(result)
        // Start streaming matching real-time sensor updates to represent the active status
        startSensorStreamingSimulation(triggerType)
    }

    fun startSensorStreamingSimulation(patternType: String) {
        simulationJob?.cancel()
        simulationJob = serviceScope.launch {
            var tick = 0
            val r = Random()
            while (isActive) {
                // Generate a continuous stream representing real-time telemetry from ESP32 MPU6050
                val reading = when (patternType) {
                    "FALL_DETECTED" -> {
                        // A simulated sequence: impact at tick 5-8, stillness thereafter
                        when {
                            tick < 4 -> { // Normal walking before fall
                                val cycle = tick * 0.8
                                SensorReading(
                                    timestampMs = System.currentTimeMillis(),
                                    ax = (0.2 * Math.sin(cycle)).toFloat() + r.nextGaussian().toFloat() * 0.1f,
                                    ay = 1.0f + (0.3 * Math.cos(cycle)).toFloat() + r.nextGaussian().toFloat() * 0.1f,
                                    az = (0.1 * Math.sin(cycle)).toFloat() + r.nextGaussian().toFloat() * 0.05f,
                                    gx = (10.0 * Math.sin(cycle)).toFloat(),
                                    gy = (15.0 * Math.cos(cycle)).toFloat(),
                                    gz = (5.0 * Math.sin(cycle)).toFloat()
                                )
                            }
                            tick in 4..6 -> { // Freefall & Extreme Impact G-force spike
                                SensorReading(
                                    timestampMs = System.currentTimeMillis(),
                                    ax = (r.nextFloat() * 4.5f) - 2.2f,
                                    ay = (r.nextFloat() * 6.5f) - 3.2f,
                                    az = (r.nextFloat() * 5.0f) + 1.5f, // Big impact spike
                                    gx = (r.nextFloat() * 180f) - 90f,
                                    gy = (r.nextFloat() * 240f) - 120f,
                                    gz = (r.nextFloat() * 150f) - 75f
                                )
                            }
                            else -> { // Motionless post-fall flatline
                                SensorReading(
                                    timestampMs = System.currentTimeMillis(),
                                    ax = 0.02f + r.nextGaussian().toFloat() * 0.02f,
                                    ay = 0.05f + r.nextGaussian().toFloat() * 0.02f,
                                    az = -0.98f + r.nextGaussian().toFloat() * 0.02f, // Gravitational force pointing downwards
                                    gx = 0.1f + r.nextGaussian().toFloat() * 0.2f,
                                    gy = -0.1f + r.nextGaussian().toFloat() * 0.2f,
                                    gz = 0.05f + r.nextGaussian().toFloat() * 0.2f
                                )
                            }
                        }
                    }
                    "RUNNING" -> {
                        val cycle = tick * 1.5
                        SensorReading(
                            timestampMs = System.currentTimeMillis(),
                            ax = (1.2 * Math.sin(cycle)).toFloat() + r.nextGaussian().toFloat() * 0.3f,
                            ay = 1.0f + (2.5 * Math.abs(Math.cos(cycle))).toFloat() + r.nextGaussian().toFloat() * 0.4f,
                            az = (0.8 * Math.sin(cycle)).toFloat() + r.nextGaussian().toFloat() * 0.2f,
                            gx = (80.0 * Math.sin(cycle)).toFloat() + r.nextGaussian().toFloat() * 5f,
                            gy = (110.0 * Math.cos(cycle)).toFloat() + r.nextGaussian().toFloat() * 5f,
                            gz = (45.0 * Math.sin(cycle)).toFloat() + r.nextGaussian().toFloat() * 5f
                        )
                    }
                    "WALKING" -> {
                        val cycle = tick * 0.6
                        SensorReading(
                            timestampMs = System.currentTimeMillis(),
                            ax = (0.3 * Math.sin(cycle)).toFloat() + r.nextGaussian().toFloat() * 0.1f,
                            ay = 1.0f + (0.5 * Math.abs(Math.cos(cycle))).toFloat() + r.nextGaussian().toFloat() * 0.1f,
                            az = (0.2 * Math.sin(cycle)).toFloat() + r.nextGaussian().toFloat() * 0.05f,
                            gx = (25.0 * Math.sin(cycle)).toFloat(),
                            gy = (30.0 * Math.cos(cycle)).toFloat(),
                            gz = (15.0 * Math.sin(cycle)).toFloat()
                        )
                    }
                    else -> { // STATIC / STANDING / STILL
                        SensorReading(
                            timestampMs = System.currentTimeMillis(),
                            ax = 0.01f + r.nextGaussian().toFloat() * 0.01f,
                            ay = 1.0f + r.nextGaussian().toFloat() * 0.01f, // standard 1G
                            az = 0.01f + r.nextGaussian().toFloat() * 0.01f,
                            gx = 0.0f + r.nextGaussian().toFloat() * 0.1f,
                            gy = 0.0f + r.nextGaussian().toFloat() * 0.1f,
                            gz = 0.0f + r.nextGaussian().toFloat() * 0.1f
                        )
                    }
                }

                _currentLiveReading.value = reading

                // Append reading to currently active analysis to display on the chart
                _currentLiveAnalysis.value?.let { current ->
                    val updatedList = (current.sensorData + reading).takeLast(30) // keep last 30 readings
                    val updatedAnalysis = current.copy(
                        sensorData = updatedList,
                        // Dynamically update confidence if still or active
                        confidenceScore = if (patternType == "FALL_DETECTED" && tick > 8) {
                            (current.confidenceScore + 1).coerceAtMost(99)
                        } else current.confidenceScore,
                        falseAlarmProbability = if (patternType == "FALL_DETECTED" && tick > 8) {
                            (current.falseAlarmProbability - 1).coerceAtLeast(1)
                        } else current.falseAlarmProbability
                    )
                    _currentLiveAnalysis.value = updatedAnalysis
                }

                tick++
                delay(300) // fast 300ms polling simulation
            }
        }
    }

    fun stopSimulation() {
        simulationJob?.cancel()
    }

    private fun generateSampleSensorData(triggerType: String): List<SensorReading> {
        val list = mutableListOf<SensorReading>()
        val baseTime = System.currentTimeMillis() - 10000L
        val r = Random()
        // Seed with 15 initial readings
        for (i in 0 until 15) {
            val reading = when (triggerType) {
                "FALL_DETECTED" -> {
                    if (i in 8..10) { // Impact window
                        SensorReading(
                            timestampMs = baseTime + (i * 500),
                            ax = (r.nextFloat() * 4.5f) - 2.2f,
                            ay = (r.nextFloat() * 6.5f) - 3.2f,
                            az = (r.nextFloat() * 5.0f) + 1.5f,
                            gx = (r.nextFloat() * 180f) - 90f,
                            gy = (r.nextFloat() * 240f) - 120f,
                            gz = (r.nextFloat() * 150f) - 75f
                        )
                    } else if (i > 10) { // Stillness laying down
                        SensorReading(
                            timestampMs = baseTime + (i * 500),
                            ax = 0.03f, ay = 0.05f, az = -0.98f,
                            gx = 0.1f, gy = -0.1f, gz = 0.0f
                        )
                    } else { // Normal standing before impact
                        SensorReading(
                            timestampMs = baseTime + (i * 500),
                            ax = 0.05f, ay = 1.0f, az = 0.05f,
                            gx = 1.0f, gy = -2.0f, gz = 0.5f
                        )
                    }
                }
                else -> { // Normal walking
                    val cycle = i * 0.8
                    SensorReading(
                        timestampMs = baseTime + (i * 500),
                        ax = (0.2 * Math.sin(cycle)).toFloat() + r.nextGaussian().toFloat() * 0.05f,
                        ay = 1.0f + (0.3 * Math.cos(cycle)).toFloat() + r.nextGaussian().toFloat() * 0.05f,
                        az = (0.1 * Math.sin(cycle)).toFloat(),
                        gx = (8.0 * Math.sin(cycle)).toFloat(),
                        gy = (12.0 * Math.cos(cycle)).toFloat(),
                        gz = (4.0 * Math.sin(cycle)).toFloat()
                    )
                }
            }
            list.add(reading)
        }
        return list
    }

    private fun generateSampleTimeline(triggerType: String): List<TimelineEvent> {
        val sdf = SimpleDateFormat("hh:mm:ss a", Locale.getDefault())
        val now = System.currentTimeMillis()
        return when (triggerType) {
            "FALL_DETECTED" -> listOf(
                TimelineEvent(sdf.format(Date(now - 12000L)), "Normal Gait Recognized", "User motion stable, accelerometer at standard gravitational vector (~1G).", "🚶"),
                TimelineEvent(sdf.format(Date(now - 8000L)), "G-Force Threshold Exceeded", "Impact registered on ESP32 MPU6050: Acceleration ax=4.2G, ay=5.8G, az=6.1G.", "💥"),
                TimelineEvent(sdf.format(Date(now - 6000L)), "Orientation Change Checked", "Sudden 90-degree pitch and roll transformation logged. Gravitational plane shifted.", "📐"),
                TimelineEvent(sdf.format(Date(now - 4000L)), "Stillness Guard Fired", "No muscular motion detected for 4.0s following impact. Activity state: UNCONSCIOUS.", "💤"),
                TimelineEvent(sdf.format(Date(now)), "AI Decision Rendered", "Neural pattern classifies fall event with 96% confidence score. Risk: CRITICAL.", "🤖")
            )
            else -> listOf(
                TimelineEvent(sdf.format(Date(now - 6000L)), "SOS Key-Press Down", "User manually toggled mechanical SOS broadcast switch on wearables.", "🔘"),
                TimelineEvent(sdf.format(Date(now - 4000L)), "Coordinate Lock In", "High-accuracy GPS telemetry received (Error margins ±2.5 meters).", "📡"),
                TimelineEvent(sdf.format(Date(now)), "AI Evaluation Safe-State", "Motion analysis classifies posture as STANDING/WALKING. Risk level: HIGH.", "🤖")
            )
        }
    }

    private fun jsonToMap(json: JSONObject): Map<String, Any> {
        val map = mutableMapOf<String, Any>()
        val keys = json.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val value = json.get(key)
            if (value is JSONArray) {
                val list = mutableListOf<Map<String, Any>>()
                for (i in 0 until value.length()) {
                    val obj = value.get(i)
                    if (obj is JSONObject) {
                        list.add(jsonToMap(obj))
                    }
                }
                map[key] = list
            } else if (value is JSONObject) {
                map[key] = jsonToMap(value)
            } else if (value != JSONObject.NULL) {
                map[key] = value
            }
        }
        return map
    }
}
