package com.example.service

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.model.AIAnalysisModel
import com.example.model.AISensorReading
import com.example.model.AITimelineEvent
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class AIService(
    private val context: Context,
    private val firestore: FirebaseFirestore?
) {
    private val _analysisLogs = MutableStateFlow<List<AIAnalysisModel>>(emptyList())
    val analysisLogs: StateFlow<List<AIAnalysisModel>> = _analysisLogs.asStateFlow()

    private val _currentLiveReading = MutableStateFlow<AISensorReading>(AISensorReading())
    val currentLiveReading: StateFlow<AISensorReading> = _currentLiveReading.asStateFlow()

    private val _currentLiveAnalysis = MutableStateFlow<AIAnalysisModel?>(null)
    val currentLiveAnalysis: StateFlow<AIAnalysisModel?> = _currentLiveAnalysis.asStateFlow()

    private val sharedPrefs: SharedPreferences = context.getSharedPreferences("guardian_sos_ai_new_service", Context.MODE_PRIVATE)
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var simulationJob: Job? = null

    init {
        loadLocalLogs()
        syncWithFirestore()
    }

    private fun loadLocalLogs() {
        val jsonStr = sharedPrefs.getString("new_ai_logs", "[]") ?: "[]"
        try {
            val jsonArray = JSONArray(jsonStr)
            val list = mutableListOf<AIAnalysisModel>()
            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)
                list.add(parseJsonToModel(jsonObject))
            }
            if (list.isEmpty()) {
                generateMockHistoricalLogs()
            } else {
                _analysisLogs.value = list
            }
        } catch (e: Exception) {
            Log.e("AIService", "Error loading local AI logs", e)
            generateMockHistoricalLogs()
        }
    }

    private fun saveLocalLogs() {
        try {
            val jsonArray = JSONArray()
            _analysisLogs.value.forEach { item ->
                jsonArray.put(serializeModelToJson(item))
            }
            sharedPrefs.edit().putString("new_ai_logs", jsonArray.toString()).apply()
        } catch (e: Exception) {
            Log.e("AIService", "Error saving local AI logs", e)
        }
    }

    private fun syncWithFirestore() {
        val fs = firestore ?: return
        fs.collection("ai_emergency_analysis_new")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("AIService", "Firestore listen failed.", e)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = mutableListOf<AIAnalysisModel>()
                    for (doc in snapshot) {
                        try {
                            list.add(parseDocToModel(doc.id, doc.data))
                        } catch (ex: Exception) {
                            Log.e("AIService", "Parsing Firestore AI item failed", ex)
                        }
                    }
                    if (list.isNotEmpty()) {
                        val existingIds = list.map { it.id }.toSet()
                        val uniqueLocals = _analysisLogs.value.filter { it.id !in existingIds }
                        _analysisLogs.value = (list + uniqueLocals).sortedByDescending { it.timestampMs }
                        saveLocalLogs()
                    }
                }
            }
    }

    fun addAnalysisLog(result: AIAnalysisModel) {
        val updated = (_analysisLogs.value.filter { it.id != result.id } + result)
            .sortedByDescending { it.timestampMs }
        _analysisLogs.value = updated
        saveLocalLogs()

        val fs = firestore
        if (fs != null) {
            serviceScope.launch {
                try {
                    fs.collection("ai_emergency_analysis_new").document(result.id).set(serializeModelToMap(result))
                } catch (e: Exception) {
                    Log.e("AIService", "Failed to sync AI log to Firestore", e)
                }
            }
        }
    }

    fun startSensorStreamingSimulation(pattern: String) {
        simulationJob?.cancel()
        simulationJob = serviceScope.launch {
            var tick = 0
            val mockReadings = mutableListOf<AISensorReading>()

            while (isActive) {
                val reading = generateSimulatedSensorReading(pattern, tick)
                _currentLiveReading.value = reading
                mockReadings.add(reading)
                if (mockReadings.size > 20) mockReadings.removeAt(0)

                if (tick % 5 == 0) {
                    val analysis = generateLiveAnalysisForPattern(pattern, mockReadings.toList())
                    _currentLiveAnalysis.value = analysis
                }

                delay(150)
                tick++
            }
        }
    }

    fun stopSimulation() {
        simulationJob?.cancel()
        simulationJob = null
    }

    private fun generateSimulatedSensorReading(pattern: String, tick: Int): AISensorReading {
        val angleRad = (tick * 0.3)
        return when (pattern) {
            "STILL" -> {
                AISensorReading(
                    timestampMs = System.currentTimeMillis(),
                    ax = 0.05f + (Math.sin(angleRad) * 0.02).toFloat(),
                    ay = 0.02f + (Math.cos(angleRad) * 0.02).toFloat(),
                    az = 0.98f + (Math.sin(angleRad * 0.5) * 0.01).toFloat(),
                    gx = 0.1f, gy = -0.2f, gz = 0.15f
                )
            }
            "WALKING" -> {
                AISensorReading(
                    timestampMs = System.currentTimeMillis(),
                    ax = 0.1f + (Math.sin(angleRad * 2.0) * 0.25).toFloat(),
                    ay = 0.05f + (Math.cos(angleRad * 2.0) * 0.15).toFloat(),
                    az = 1.0f + (Math.sin(angleRad * 2.0) * 0.45).toFloat(),
                    gx = (Math.sin(angleRad) * 20.0).toFloat(),
                    gy = (Math.cos(angleRad) * 15.0).toFloat(),
                    gz = (Math.sin(angleRad) * 10.0).toFloat()
                )
            }
            "RUNNING" -> {
                AISensorReading(
                    timestampMs = System.currentTimeMillis(),
                    ax = 0.2f + (Math.sin(angleRad * 4.0) * 0.85).toFloat(),
                    ay = 0.1f + (Math.cos(angleRad * 4.0) * 0.65).toFloat(),
                    az = 1.1f + (Math.sin(angleRad * 4.0) * 1.55).toFloat(),
                    gx = (Math.sin(angleRad * 2.0) * 85.0).toFloat(),
                    gy = (Math.cos(angleRad * 2.0) * 65.0).toFloat(),
                    gz = (Math.sin(angleRad * 2.0) * 45.0).toFloat()
                )
            }
            "FALL_DETECTED" -> {
                // Generate high impact spike followed by horizontal layout (gravity shifts)
                if (tick < 8) {
                    // Transition/Spike
                    AISensorReading(
                        timestampMs = System.currentTimeMillis(),
                        ax = 2.8f + (Math.sin(tick.toDouble()) * 1.5).toFloat(),
                        ay = -1.9f + (Math.cos(tick.toDouble()) * 1.2).toFloat(),
                        az = 0.4f + (Math.sin(tick.toDouble()) * 2.1).toFloat(),
                        gx = 180f, gy = -240f, gz = 135f
                    )
                } else {
                    // Laying still on side
                    AISensorReading(
                        timestampMs = System.currentTimeMillis(),
                        ax = 0.95f + (Math.sin(angleRad) * 0.02).toFloat(), // Gravity mostly on X now (lying on side)
                        ay = 0.15f,
                        az = 0.18f,
                        gx = 0.2f, gy = 0.1f, gz = -0.1f
                    )
                }
            }
            else -> AISensorReading()
        }
    }

    private fun generateLiveAnalysisForPattern(pattern: String, readings: List<AISensorReading>): AIAnalysisModel {
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val timeNow = sdf.format(Date())

        return when (pattern) {
            "STILL" -> AIAnalysisModel(
                id = "live_still",
                confidenceScore = 1,
                falseAlarmProbability = 99,
                motionAnalysis = "STATIC_VERTICAL_STABILITY",
                activityRecognition = "SITTING_OR_STANDING",
                riskLevel = "LOW",
                suggestedAction = "CONTINUE PASSIVE BACKGROUND BIO-HEART MONITORING",
                sensorReadings = readings,
                timeline = listOf(
                    AITimelineEvent(timeNow, "Activity Settled", "User is resting or standing perfectly still.", "🟢")
                )
            )
            "WALKING" -> AIAnalysisModel(
                id = "live_walking",
                confidenceScore = 3,
                falseAlarmProbability = 97,
                motionAnalysis = "PERIODIC_VERTICAL_OSCILLATION_LOW",
                activityRecognition = "NORMAL_WALKING",
                riskLevel = "LOW",
                suggestedAction = "GAIT ANALYSIS CALIBRATED. CADENCE REMAINS NORMAL.",
                sensorReadings = readings,
                timeline = listOf(
                    AITimelineEvent(timeNow, "Walking Cadence Lock", "Rhythmic walking strides detected regularly.", "🚶")
                )
            )
            "RUNNING" -> AIAnalysisModel(
                id = "live_running",
                confidenceScore = 8,
                falseAlarmProbability = 92,
                motionAnalysis = "PERIODIC_ACCELERATION_HIGH_IMPACT",
                activityRecognition = "ATHLETIC_RUNNING",
                riskLevel = "LOW",
                suggestedAction = "FITNESS TRACKING ENGAGED. METABOLIC PEAK DETECTED.",
                sensorReadings = readings,
                timeline = listOf(
                    AITimelineEvent(timeNow, "Elevated CADENCE", "High speed rhythmic acceleration logged.", "🏃")
                )
            )
            "FALL_DETECTED" -> AIAnalysisModel(
                id = "live_fall",
                confidenceScore = 98,
                falseAlarmProbability = 2,
                motionAnalysis = "CRITICAL_ACCELERATION_SPIKE_FOLLOWED_BY_HORIZONTAL_AXIS_SHIFT",
                activityRecognition = "SUDDEN FALL DETECTED (STATIC LAYING)",
                riskLevel = "CRITICAL",
                suggestedAction = "ALERT ALL PRIMARY FAMILY CONTACTS AND LAUNCH COUNTY DISPATCH CODES",
                sensorReadings = readings,
                timeline = listOf(
                    AITimelineEvent(timeNow, "Impact Shock Detected", "Accelerometer force spike exceeded 3.2G threshold.", "💥"),
                    AITimelineEvent(timeNow, "Orientation Check", "Device pitch and roll shift indicates laying horizontal.", "📐"),
                    AITimelineEvent(timeNow, "No Motion Alert", "Complete cessation of movement for 5+ seconds post-impact.", "🛑")
                )
            )
            else -> AIAnalysisModel()
        }
    }

    private fun generateMockHistoricalLogs() {
        val logs = listOf(
            AIAnalysisModel(
                id = UUID.randomUUID().toString(),
                confidenceScore = 96,
                falseAlarmProbability = 4,
                motionAnalysis = "HIGH_ACCELERATION_IMPACT_REST",
                activityRecognition = "UNRESPONSIVE FALL (BEDROOM Restroom)",
                riskLevel = "CRITICAL",
                suggestedAction = "DISPATCH EMERGENCY SERVICE UNITS IMMEDIATELY",
                timeline = listOf(
                    AITimelineEvent("10:24:02 AM", "Impact Detected", "Sudden 3.5G impact logged on restroom floor.", "💥"),
                    AITimelineEvent("10:24:12 AM", "Unresponsive Stillness", "Zero micro-motion detected for 10 seconds.", "🛑"),
                    AITimelineEvent("10:24:20 AM", "Auto SOS", "First contact team successfully alerted.", "📞")
                ),
                timestampMs = System.currentTimeMillis() - 3600000
            ),
            AIAnalysisModel(
                id = UUID.randomUUID().toString(),
                confidenceScore = 92,
                falseAlarmProbability = 8,
                motionAnalysis = "ACCELERATED_DECELERATION_STILL",
                activityRecognition = "HARD SLIP DURING WALKING (OUTDOOR GRAVEL PATH)",
                riskLevel = "HIGH",
                suggestedAction = "INFORM LOCAL GUARDIANS AND COMMENCE LIVE TALK-BACK",
                timeline = listOf(
                    AITimelineEvent("08:12:15 PM", "Sliding Pattern", "High speed slide detected by IMU.", "🚶"),
                    AITimelineEvent("08:12:18 PM", "Impact Logged", "2.8G deceleration registered.", "💥")
                ),
                timestampMs = System.currentTimeMillis() - 7200000
            )
        )
        _analysisLogs.value = logs
        saveLocalLogs()
    }

    // --- JSON & MAP PARSERS ---

    private fun serializeModelToJson(item: AIAnalysisModel): JSONObject {
        val obj = JSONObject()
        obj.put("id", item.id)
        obj.put("alertId", item.alertId)
        obj.put("confidenceScore", item.confidenceScore)
        obj.put("falseAlarmProbability", item.falseAlarmProbability)
        obj.put("motionAnalysis", item.motionAnalysis)
        obj.put("activityRecognition", item.activityRecognition)
        obj.put("riskLevel", item.riskLevel)
        obj.put("suggestedAction", item.suggestedAction)
        obj.put("timestampMs", item.timestampMs)

        val timelineArr = JSONArray()
        item.timeline.forEach { ev ->
            val evObj = JSONObject()
            evObj.put("time", ev.timeString)
            evObj.put("event", ev.eventName)
            evObj.put("desc", ev.eventDescription)
            evObj.put("emoji", ev.categoryEmoji)
            timelineArr.put(evObj)
        }
        obj.put("timeline", timelineArr)

        val readingsArr = JSONArray()
        item.sensorReadings.forEach { rd ->
            val rObj = JSONObject()
            rObj.put("t", rd.timestampMs)
            rObj.put("ax", rd.ax)
            rObj.put("ay", rd.ay)
            rObj.put("az", rd.az)
            rObj.put("gx", rd.gx)
            rObj.put("gy", rd.gy)
            rObj.put("gz", rd.gz)
            readingsArr.put(rObj)
        }
        obj.put("readings", readingsArr)

        return obj
    }

    private fun parseJsonToModel(obj: JSONObject): AIAnalysisModel {
        val timelineList = mutableListOf<AITimelineEvent>()
        val timelineArr = obj.optJSONArray("timeline") ?: JSONArray()
        for (i in 0 until timelineArr.length()) {
            val evObj = timelineArr.getJSONObject(i)
            timelineList.add(
                AITimelineEvent(
                    timeString = evObj.optString("time", ""),
                    eventName = evObj.optString("event", ""),
                    eventDescription = evObj.optString("desc", ""),
                    categoryEmoji = evObj.optString("emoji", "⏱️")
                )
            )
        }

        val readingsList = mutableListOf<AISensorReading>()
        val readingsArr = obj.optJSONArray("readings") ?: JSONArray()
        for (i in 0 until readingsArr.length()) {
            val rObj = readingsArr.getJSONObject(i)
            readingsList.add(
                AISensorReading(
                    timestampMs = rObj.optLong("t", 0L),
                    ax = rObj.optDouble("ax", 0.0).toFloat(),
                    ay = rObj.optDouble("ay", 0.0).toFloat(),
                    az = rObj.optDouble("az", 0.0).toFloat(),
                    gx = rObj.optDouble("gx", 0.0).toFloat(),
                    gy = rObj.optDouble("gy", 0.0).toFloat(),
                    gz = rObj.optDouble("gz", 0.0).toFloat()
                )
            )
        }

        return AIAnalysisModel(
            id = obj.optString("id", UUID.randomUUID().toString()),
            alertId = obj.optString("alertId", "none"),
            confidenceScore = obj.optInt("confidenceScore", 94),
            falseAlarmProbability = obj.optInt("falseAlarmProbability", 6),
            motionAnalysis = obj.optString("motionAnalysis", "SUDDEN_DECELERATION"),
            activityRecognition = obj.optString("activityRecognition", "FALL DETECTED (LAYING)"),
            riskLevel = obj.optString("riskLevel", "CRITICAL"),
            suggestedAction = obj.optString("suggestedAction", ""),
            timestampMs = obj.optLong("timestampMs", System.currentTimeMillis()),
            sensorReadings = readingsList,
            timeline = timelineList
        )
    }

    private fun serializeModelToMap(item: AIAnalysisModel): Map<String, Any> {
        return mapOf(
            "alertId" to item.alertId,
            "confidenceScore" to item.confidenceScore,
            "falseAlarmProbability" to item.falseAlarmProbability,
            "motionAnalysis" to item.motionAnalysis,
            "activityRecognition" to item.activityRecognition,
            "riskLevel" to item.riskLevel,
            "suggestedAction" to item.suggestedAction,
            "timestampMs" to item.timestampMs,
            "timeline" to item.timeline.map {
                mapOf(
                    "time" to it.timeString,
                    "event" to it.eventName,
                    "desc" to it.eventDescription,
                    "emoji" to it.categoryEmoji
                )
            }
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseDocToModel(id: String, map: Map<String, Any>?): AIAnalysisModel {
        if (map == null) return AIAnalysisModel(id = id)

        val timelineRaw = map["timeline"] as? List<Map<String, Any>> ?: emptyList()
        val timeline = timelineRaw.map {
            AITimelineEvent(
                timeString = it["time"]?.toString() ?: "",
                eventName = it["event"]?.toString() ?: "",
                eventDescription = it["desc"]?.toString() ?: "",
                categoryEmoji = it["emoji"]?.toString() ?: "⏱️"
            )
        }

        return AIAnalysisModel(
            id = id,
            alertId = map["alertId"]?.toString() ?: "none",
            confidenceScore = (map["confidenceScore"] as? Number)?.toInt() ?: 94,
            falseAlarmProbability = (map["falseAlarmProbability"] as? Number)?.toInt() ?: 6,
            motionAnalysis = map["motionAnalysis"]?.toString() ?: "SUDDEN_DECELERATION",
            activityRecognition = map["activityRecognition"]?.toString() ?: "FALL DETECTED",
            riskLevel = map["riskLevel"]?.toString() ?: "CRITICAL",
            suggestedAction = map["suggestedAction"]?.toString() ?: "",
            timestampMs = (map["timestampMs"] as? Number)?.toLong() ?: System.currentTimeMillis(),
            timeline = timeline
        )
    }
}
