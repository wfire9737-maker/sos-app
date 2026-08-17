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
            } else {
                _analysisLogs.value = list
            }
        } catch (e: Exception) {
            Log.e("AIService", "Error loading local AI logs", e)
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
