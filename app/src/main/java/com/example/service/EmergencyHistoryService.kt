package com.example.service

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.model.EmergencyHistoryItem
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class EmergencyHistoryService(private val context: Context, private val firestore: FirebaseFirestore?) {

    private val _history = MutableStateFlow<List<EmergencyHistoryItem>>(emptyList())
    val history: StateFlow<List<EmergencyHistoryItem>> = _history.asStateFlow()

    private val sharedPrefs: SharedPreferences = context.getSharedPreferences("guardian_sos_history", Context.MODE_PRIVATE)

    init {
        loadHistory()
        listenToFirestoreHistory()
    }

    private fun loadHistory() {
        val jsonStr = sharedPrefs.getString("history_items", "[]") ?: "[]"
        try {
            val arr = JSONArray(jsonStr)
            val list = mutableListOf<EmergencyHistoryItem>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(parseJsonToHistoryItem(obj))
            }
            if (list.isEmpty()) {
                populateSimulatedDefaults()
            } else {
                _history.value = list
            }
        } catch (e: Exception) {
            Log.e("EmergencyHistoryService", "Failed to deserialize local history: ${e.message}")
            populateSimulatedDefaults()
        }
    }

    private fun saveHistory() {
        val arr = JSONArray()
        for (item in _history.value) {
            arr.put(serializeHistoryItemToJson(item))
        }
        sharedPrefs.edit().putString("history_items", arr.toString()).apply()
    }

    private fun listenToFirestoreHistory() {
        val db = firestore ?: return
        db.collection("emergency_history")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w("EmergencyHistoryService", "Firestore history sync failed", e)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = mutableListOf<EmergencyHistoryItem>()
                    for (doc in snapshot.documents) {
                        try {
                            list.add(parseDocToHistoryItem(doc.id, doc.data ?: emptyMap()))
                        } catch (ex: Exception) {
                            Log.e("EmergencyHistoryService", "Failed to parse history doc: ${ex.message}")
                        }
                    }
                    if (list.isNotEmpty()) {
                        val merged = (list + _history.value).distinctBy { it.id }
                        _history.value = merged
                        saveHistory()
                    }
                }
            }
    }

    fun addHistoryItem(item: EmergencyHistoryItem) {
        val updated = (_history.value + item).distinctBy { it.id }
        _history.value = updated
        saveHistory()

        val db = firestore
        if (db != null) {
            val map = serializeHistoryItemToMap(item)
            db.collection("emergency_history").document(item.id).set(map)
        }
    }

    fun deleteHistoryItem(id: String) {
        val updated = _history.value.filter { it.id != id }
        _history.value = updated
        saveHistory()

        val db = firestore
        if (db != null) {
            db.collection("emergency_history").document(id).delete()
        }
    }

    // --- CSV & PDF EXPORT UTILS ---

    fun generateCSVString(): String {
        val sb = StringBuilder()
        sb.append("ID,Date,Time,Duration(Sec),ResponseTime(Sec),Location,Latitude,Longitude,Severity,DeviceUsed,ContactsNotified,AIScore,TriggerType,ResolutionNotes,ResolvedBy\n")
        for (item in _history.value) {
            val contacts = item.contactsNotified.joinToString(";")
            sb.append("\"${item.id}\",")
            sb.append("\"${item.date}\",")
            sb.append("\"${item.time}\",")
            sb.append("${item.durationSeconds},")
            sb.append("${item.responseTimeSeconds},")
            sb.append("\"${item.locationName}\",")
            sb.append("${item.latitude},")
            sb.append("${item.longitude},")
            sb.append("\"${item.severity}\",")
            sb.append("\"${item.deviceUsed}\",")
            sb.append("\"$contacts\",")
            sb.append("${item.aiScore},")
            sb.append("\"${item.triggerType}\",")
            sb.append("\"${item.resolutionNotes}\",")
            sb.append("\"${item.resolvedBy}\"\n")
        }
        return sb.toString()
    }

    fun generatePDFReportText(): String {
        val sb = StringBuilder()
        sb.append("==================================================\n")
        sb.append("             GUARDIAN SOS EMERGENCY REPORT         \n")
        sb.append("==================================================\n")
        sb.append("Report Generated on: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())}\n")
        sb.append("Total SOS Incidents Logged: ${_history.value.size}\n\n")

        for ((index, item) in _history.value.withIndex()) {
            sb.append("${index + 1}. [${item.severity}] SOS TRIGGERED on ${item.date} at ${item.time}\n")
            sb.append("   - Incident ID: ${item.id}\n")
            sb.append("   - Source Device: ${item.deviceUsed}\n")
            sb.append("   - Incident Trigger Type: ${item.triggerType}\n")
            sb.append("   - Location Plot: Lat: ${item.latitude}, Lng: ${item.longitude} (${item.locationName})\n")
            sb.append("   - Active AI Fall Confidence Score: ${item.aiScore}%\n")
            sb.append("   - Alert Active Duration: ${item.durationSeconds} seconds\n")
            sb.append("   - First Contact Response Time: ${item.responseTimeSeconds} seconds\n")
            sb.append("   - Contacts Successfully Notified: ${item.contactsNotified.joinToString(", ")}\n")
            sb.append("   - Resolution Triage Status: ${item.resolutionNotes}\n")
            sb.append("   - Signed Off By: ${item.resolvedBy}\n")
            sb.append("--------------------------------------------------\n")
        }
        return sb.toString()
    }

    private fun populateSimulatedDefaults() {
        val defaults = listOf(
            EmergencyHistoryItem(
                date = "2026-07-15",
                time = "10:24 AM",
                durationSeconds = 184,
                responseTimeSeconds = 24,
                locationName = "North Ward Corridor B, Suite 104",
                latitude = 37.77492,
                longitude = -122.41941,
                severity = "CRITICAL",
                deviceUsed = "ESP32-SOS-BAND-81F4",
                contactsNotified = listOf("Dr. Sarah Jenkins", "Warden Vance", "Emergency Service 911"),
                aiScore = 96,
                triggerType = "FALL_DETECTED",
                resolutionNotes = "Sudden vertical acceleration signature detected followed by complete absence of motion. Medical staff arrived in 184 seconds. Stabilized and resolved.",
                resolvedBy = "Nurse Julia"
            ),
            EmergencyHistoryItem(
                date = "2026-07-10",
                time = "06:12 PM",
                durationSeconds = 95,
                responseTimeSeconds = 12,
                locationName = "Main Garden Courtyard Area",
                latitude = 37.77511,
                longitude = -122.41893,
                severity = "HIGH",
                deviceUsed = "ESP32-SOS-BAND-81F4",
                contactsNotified = listOf("Warden Vance", "Dr. Sarah Jenkins"),
                aiScore = 100,
                triggerType = "MANUAL_BUTTON",
                resolutionNotes = "User manual SOS button depressed due to heart arrhythmia warning onset. Help dispatched to garden immediately.",
                resolvedBy = "Officer Marcus"
            ),
            EmergencyHistoryItem(
                date = "2026-07-01",
                time = "02:45 AM",
                durationSeconds = 230,
                responseTimeSeconds = 45,
                locationName = "Living Quarters Restroom 4",
                latitude = 37.77456,
                longitude = -122.41999,
                severity = "CRITICAL",
                deviceUsed = "ESP32-SOS-BAND-81F4",
                contactsNotified = listOf("Night Guard Thomas", "Emergency Service 911"),
                aiScore = 91,
                triggerType = "FALL_DETECTED",
                resolutionNotes = "Slip detected on restroom wet flooring. Automated fall alarm activated. Night guard accessed with key-card and assisted user up.",
                resolvedBy = "Guard Thomas"
            ),
            EmergencyHistoryItem(
                date = "2026-06-25",
                time = "09:15 AM",
                durationSeconds = 54,
                responseTimeSeconds = 8,
                locationName = "West Entrance Reception",
                latitude = 37.77488,
                longitude = -122.41912,
                severity = "WARNING",
                deviceUsed = "ESP32-SOS-BAND-81F4",
                contactsNotified = listOf("Warden Vance"),
                aiScore = 88,
                triggerType = "MANUAL_BUTTON",
                resolutionNotes = "Accidental click during strap replacement. User apologized and marked device safe inside 54 seconds. False alarm cleared.",
                resolvedBy = "Self (Marcus Vance)"
            )
        )
        _history.value = defaults
        saveHistory()
    }

    // --- MAP & JSON PARSERS ---

    private fun serializeHistoryItemToJson(item: EmergencyHistoryItem): JSONObject {
        val obj = JSONObject()
        obj.put("id", item.id)
        obj.put("date", item.date)
        obj.put("time", item.time)
        obj.put("durationSeconds", item.durationSeconds)
        obj.put("responseTimeSeconds", item.responseTimeSeconds)
        obj.put("locationName", item.locationName)
        obj.put("latitude", item.latitude)
        obj.put("longitude", item.longitude)
        obj.put("severity", item.severity)
        obj.put("deviceUsed", item.deviceUsed)
        obj.put("contactsNotified", JSONArray(item.contactsNotified))
        obj.put("aiScore", item.aiScore)
        obj.put("triggerType", item.triggerType)
        obj.put("resolutionNotes", item.resolutionNotes)
        obj.put("resolvedBy", item.resolvedBy)
        return obj
    }

    private fun parseJsonToHistoryItem(obj: JSONObject): EmergencyHistoryItem {
        val contactsArr = obj.optJSONArray("contactsNotified") ?: JSONArray()
        val contacts = mutableListOf<String>()
        for (i in 0 until contactsArr.length()) {
            contacts.add(contactsArr.optString(i))
        }

        return EmergencyHistoryItem(
            id = obj.optString("id", UUID.randomUUID().toString()),
            date = obj.optString("date", ""),
            time = obj.optString("time", ""),
            durationSeconds = obj.optLong("durationSeconds", 0L),
            responseTimeSeconds = obj.optLong("responseTimeSeconds", 0L),
            locationName = obj.optString("locationName", ""),
            latitude = obj.optDouble("latitude", 0.0),
            longitude = obj.optDouble("longitude", 0.0),
            severity = obj.optString("severity", "WARNING"),
            deviceUsed = obj.optString("deviceUsed", ""),
            contactsNotified = contacts,
            aiScore = obj.optInt("aiScore", 0),
            triggerType = obj.optString("triggerType", "MANUAL_BUTTON"),
            resolutionNotes = obj.optString("resolutionNotes", ""),
            resolvedBy = obj.optString("resolvedBy", "")
        )
    }

    private fun serializeHistoryItemToMap(item: EmergencyHistoryItem): Map<String, Any> {
        return mapOf(
            "date" to item.date,
            "time" to item.time,
            "durationSeconds" to item.durationSeconds,
            "responseTimeSeconds" to item.responseTimeSeconds,
            "locationName" to item.locationName,
            "latitude" to item.latitude,
            "longitude" to item.longitude,
            "severity" to item.severity,
            "deviceUsed" to item.deviceUsed,
            "contactsNotified" to item.contactsNotified,
            "aiScore" to item.aiScore,
            "triggerType" to item.triggerType,
            "resolutionNotes" to item.resolutionNotes,
            "resolvedBy" to item.resolvedBy
        )
    }

    private fun parseDocToHistoryItem(id: String, map: Map<String, Any>): EmergencyHistoryItem {
        val contacts = (map["contactsNotified"] as? List<*>)?.map { it.toString() } ?: emptyList()
        return EmergencyHistoryItem(
            id = id,
            date = map["date"]?.toString() ?: "",
            time = map["time"]?.toString() ?: "",
            durationSeconds = (map["durationSeconds"] as? Number)?.toLong() ?: 0L,
            responseTimeSeconds = (map["responseTimeSeconds"] as? Number)?.toLong() ?: 0L,
            locationName = map["locationName"]?.toString() ?: "",
            latitude = (map["latitude"] as? Number)?.toDouble() ?: 0.0,
            longitude = (map["longitude"] as? Number)?.toDouble() ?: 0.0,
            severity = map["severity"]?.toString() ?: "WARNING",
            deviceUsed = map["deviceUsed"]?.toString() ?: "",
            contactsNotified = contacts,
            aiScore = (map["aiScore"] as? Number)?.toInt() ?: 0,
            triggerType = map["triggerType"]?.toString() ?: "MANUAL_BUTTON",
            resolutionNotes = map["resolutionNotes"]?.toString() ?: "",
            resolvedBy = map["resolvedBy"]?.toString() ?: ""
        )
    }
}
