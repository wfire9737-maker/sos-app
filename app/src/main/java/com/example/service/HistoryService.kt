package com.example.service

import android.content.Context
import android.util.Log
import com.example.data.local.dao.SosHistoryDao
import com.example.data.local.entity.SosHistoryEntity
import com.example.model.HistoryModel
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoryService(
    private val context: Context,
    private val firestore: FirebaseFirestore?,
    private val sosHistoryDao: SosHistoryDao? = null
) {

    private val _history = MutableStateFlow<List<HistoryModel>>(emptyList())
    val history: StateFlow<List<HistoryModel>> = _history.asStateFlow()

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    init {
        observeRoomHistory()
        listenToFirestoreHistory()
    }

    private fun observeRoomHistory() {
        val dao = sosHistoryDao ?: return
        serviceScope.launch {
            dao.getAllHistory().collect { entities ->
                val models = entities.map { it.toHistoryModel() }
                _history.value = models
            }
        }
    }

    private fun SosHistoryEntity.toHistoryModel(): HistoryModel {
        val dateObj = Date(date)
        val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(dateObj)
        val timeStr = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(dateObj)
        val contactsList = if (contactsNotified.isNotBlank()) {
            contactsNotified.split(";").map { it.trim() }.filter { it.isNotEmpty() }
        } else emptyList()

        return HistoryModel(
            id = historyId,
            date = dateStr,
            time = timeStr,
            durationSeconds = durationSeconds,
            responseTimeSeconds = 14L,
            address = address.ifBlank { "GPS Coordinates: $latitude, $longitude" },
            latitude = latitude,
            longitude = longitude,
            aiConfidence = aiConfidence,
            severity = severity,
            contactsNotified = contactsList,
            deviceUsed = deviceUsed,
            triggerType = triggerSource,
            resolutionNotes = resolutionNotes.ifBlank { "Status: $status" },
            resolvedBy = resolvedBy.ifBlank { "User" }
        )
    }

    private fun listenToFirestoreHistory() {
        val db = firestore ?: return
        db.collection("emergency_history_records")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w("HistoryService", "Firestore history sync failed", e)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = mutableListOf<HistoryModel>()
                    for (doc in snapshot.documents) {
                        try {
                            list.add(parseDocToHistoryItem(doc.id, doc.data ?: emptyMap()))
                        } catch (ex: Exception) {
                            Log.e("HistoryService", "Failed to parse history doc: ${ex.message}")
                        }
                    }
                    if (list.isNotEmpty()) {
                        serviceScope.launch {
                            for (item in list) {
                                val entity = SosHistoryEntity(
                                    historyId = item.id,
                                    uid = "user-101",
                                    latitude = item.latitude,
                                    longitude = item.longitude,
                                    googleMapsLink = "https://maps.google.com/?q=${item.latitude},${item.longitude}",
                                    triggerSource = item.triggerType,
                                    date = System.currentTimeMillis(),
                                    status = "RESOLVED",
                                    durationSeconds = item.durationSeconds,
                                    address = item.address,
                                    severity = item.severity,
                                    contactsNotified = item.contactsNotified.joinToString("; "),
                                    deviceUsed = item.deviceUsed,
                                    resolutionNotes = item.resolutionNotes,
                                    resolvedBy = item.resolvedBy,
                                    aiConfidence = item.aiConfidence
                                )
                                sosHistoryDao?.insertHistory(entity)
                            }
                        }
                    }
                }
            }
    }

    fun addHistoryItem(item: HistoryModel) {
        serviceScope.launch {
            val entity = SosHistoryEntity(
                historyId = item.id,
                uid = "user-101",
                latitude = item.latitude,
                longitude = item.longitude,
                googleMapsLink = "https://maps.google.com/?q=${item.latitude},${item.longitude}",
                triggerSource = item.triggerType,
                date = System.currentTimeMillis(),
                status = "RESOLVED",
                durationSeconds = item.durationSeconds,
                address = item.address,
                severity = item.severity,
                contactsNotified = item.contactsNotified.joinToString("; "),
                deviceUsed = item.deviceUsed,
                resolutionNotes = item.resolutionNotes,
                resolvedBy = item.resolvedBy,
                aiConfidence = item.aiConfidence
            )
            sosHistoryDao?.insertHistory(entity)
        }

        val db = firestore
        if (db != null) {
            val map = serializeHistoryItemToMap(item)
            db.collection("emergency_history_records").document(item.id).set(map)
        }
    }

    fun deleteHistoryItem(id: String) {
        serviceScope.launch {
            sosHistoryDao?.deleteHistory(id)
        }

        val db = firestore
        if (db != null) {
            db.collection("emergency_history_records").document(id).delete()
        }
    }

    // --- CSV & PDF EXPORT UTILS ---

    fun generateCSVString(): String {
        val sb = StringBuilder()
        sb.append("ID,Date,Time,Duration(Sec),ResponseTime(Sec),Address,Latitude,Longitude,Severity,AIScore,ContactsNotified,TriggerType,ResolutionNotes,ResolvedBy\n")
        for (item in _history.value) {
            val contacts = item.contactsNotified.joinToString(";")
            sb.append("\"${item.id}\",")
            sb.append("\"${item.date}\",")
            sb.append("\"${item.time}\",")
            sb.append("${item.durationSeconds},")
            sb.append("${item.responseTimeSeconds},")
            sb.append("\"${item.address}\",")
            sb.append("${item.latitude},")
            sb.append("${item.longitude},")
            sb.append("\"${item.severity}\",")
            sb.append("${item.aiConfidence},")
            sb.append("\"$contacts\",")
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
        sb.append("Report Generated on: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}\n")
        sb.append("Total SOS Incidents Logged: ${_history.value.size}\n\n")

        for ((index, item) in _history.value.withIndex()) {
            sb.append("${index + 1}. [${item.severity}] SOS TRIGGERED on ${item.date} at ${item.time}\n")
            sb.append("   - Incident ID: ${item.id}\n")
            sb.append("   - Incident Trigger Type: ${item.triggerType}\n")
            sb.append("   - Location Plot: Lat: ${item.latitude}, Lng: ${item.longitude} (${item.address})\n")
            sb.append("   - Active AI Fall Confidence Score: ${item.aiConfidence}%\n")
            sb.append("   - Alert Active Duration: ${item.durationSeconds} seconds\n")
            sb.append("   - First Contact Response Time: ${item.responseTimeSeconds} seconds\n")
            sb.append("   - Contacts Successfully Notified: ${item.contactsNotified.joinToString(", ")}\n")
            sb.append("   - Resolution Triage Notes: ${item.resolutionNotes}\n")
            sb.append("   - Signed Off By: ${item.resolvedBy}\n")
            sb.append("--------------------------------------------------\n")
        }
        return sb.toString()
    }

    private fun serializeHistoryItemToMap(item: HistoryModel): Map<String, Any> {
        return mapOf(
            "date" to item.date,
            "time" to item.time,
            "durationSeconds" to item.durationSeconds,
            "responseTimeSeconds" to item.responseTimeSeconds,
            "address" to item.address,
            "latitude" to item.latitude,
            "longitude" to item.longitude,
            "severity" to item.severity,
            "contactsNotified" to item.contactsNotified,
            "aiConfidence" to item.aiConfidence,
            "triggerType" to item.triggerType,
            "resolutionNotes" to item.resolutionNotes,
            "resolvedBy" to item.resolvedBy
        )
    }

    private fun parseDocToHistoryItem(id: String, map: Map<String, Any>): HistoryModel {
        val contacts = (map["contactsNotified"] as? List<*>)?.map { it.toString() } ?: emptyList()
        return HistoryModel(
            id = id,
            date = map["date"]?.toString() ?: "",
            time = map["time"]?.toString() ?: "",
            durationSeconds = (map["durationSeconds"] as? Number)?.toLong() ?: 0L,
            responseTimeSeconds = (map["responseTimeSeconds"] as? Number)?.toLong() ?: 0L,
            address = map["address"]?.toString() ?: map["locationName"]?.toString() ?: "",
            latitude = (map["latitude"] as? Number)?.toDouble() ?: 0.0,
            longitude = (map["longitude"] as? Number)?.toDouble() ?: 0.0,
            severity = map["severity"]?.toString() ?: "WARNING",
            contactsNotified = contacts,
            aiConfidence = (map["aiConfidence"] as? Number)?.toInt() ?: (map["aiScore"] as? Number)?.toInt() ?: 0,
            triggerType = map["triggerType"]?.toString() ?: "MANUAL_BUTTON",
            resolutionNotes = map["resolutionNotes"]?.toString() ?: "",
            resolvedBy = map["resolvedBy"]?.toString() ?: ""
        )
    }
}
