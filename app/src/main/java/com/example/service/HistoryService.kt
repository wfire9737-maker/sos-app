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
import kotlinx.coroutines.tasks.await
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
    }

    private fun getAuthenticatedUid(): String? {
        return try {
            val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
            if (uid.isNullOrBlank() || uid.startsWith("demo-", ignoreCase = true) || uid == "user-101" || uid == "anonymous") {
                null
            } else {
                uid
            }
        } catch (e: Exception) {
            null
        }
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

    suspend fun syncHistoryFromCloud(uid: String) {
        val authUid = getAuthenticatedUid() ?: uid.takeIf { !it.startsWith("demo-", ignoreCase = true) && it != "user-101" && it != "anonymous" } ?: return
        val db = firestore ?: return

        try {
            val snapshot = db.collection("users").document(authUid)
                .collection("emergency_history").get().await()

            if (snapshot.isEmpty) {
                Log.d("HistoryService", "No remote emergency history found for uid: $authUid")
                return
            }

            val localIds = _history.value.map { it.id }.toSet()
            val missingEntities = snapshot.documents.mapNotNull { doc ->
                if (!localIds.contains(doc.id)) {
                    parseDocToEntity(doc.id, doc.data ?: emptyMap(), authUid)
                } else {
                    null
                }
            }

            if (missingEntities.isNotEmpty()) {
                sosHistoryDao?.insertHistories(missingEntities)
                Log.d("HistoryService", "Merged ${missingEntities.size} missing history records from cloud for uid: $authUid")
            }
        } catch (e: Exception) {
            Log.e("HistoryService", "Failed to sync history from cloud for uid: $authUid (retaining local records): ${e.message}")
        }
    }

    private fun parseDocToEntity(id: String, map: Map<String, Any>, fallbackUid: String): SosHistoryEntity {
        val lat = (map["latitude"] as? Number)?.toDouble() ?: 0.0
        val lng = (map["longitude"] as? Number)?.toDouble() ?: 0.0
        val contactsRaw = map["contactsNotified"]
        val contactsStr = when (contactsRaw) {
            is List<*> -> contactsRaw.mapNotNull { it?.toString() }.joinToString("; ")
            is String -> contactsRaw
            else -> ""
        }

        return SosHistoryEntity(
            historyId = id,
            uid = map["uid"]?.toString() ?: fallbackUid,
            latitude = lat,
            longitude = lng,
            googleMapsLink = map["googleMapsLink"]?.toString() ?: "https://maps.google.com/?q=$lat,$lng",
            triggerSource = map["triggerSource"]?.toString() ?: map["triggerType"]?.toString() ?: "MANUAL_BUTTON",
            date = (map["date"] as? Number)?.toLong() ?: System.currentTimeMillis(),
            status = map["status"]?.toString() ?: "RESOLVED",
            durationSeconds = (map["durationSeconds"] as? Number)?.toLong() ?: 0L,
            address = map["address"]?.toString() ?: map["locationName"]?.toString() ?: "GPS Coordinate Plot",
            severity = map["severity"]?.toString() ?: "HIGH",
            contactsNotified = contactsStr,
            deviceUsed = map["deviceUsed"]?.toString() ?: "MOBILE-APP-SOS",
            resolutionNotes = map["resolutionNotes"]?.toString() ?: "",
            resolvedBy = map["resolvedBy"]?.toString() ?: "",
            aiConfidence = (map["aiConfidence"] as? Number)?.toInt() ?: 90
        )
    }

    fun addHistoryItem(item: HistoryModel) {
        val authUid = getAuthenticatedUid()
        val effectiveUid = authUid ?: "local-user"

        serviceScope.launch {
            val entity = SosHistoryEntity(
                historyId = item.id,
                uid = effectiveUid,
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
            try {
                sosHistoryDao?.insertHistory(entity)
            } catch (e: Exception) {
                Log.e("HistoryService", "Failed to save history item to Room: ${e.message}")
            }

            if (authUid != null && firestore != null) {
                try {
                    val map = serializeHistoryItemToMap(item, authUid)
                    firestore.collection("users").document(authUid)
                        .collection("emergency_history").document(item.id)
                        .set(map, com.google.firebase.firestore.SetOptions.merge())
                        .await()
                    Log.d("HistoryService", "Synced history item to users/$authUid/emergency_history/${item.id}")
                } catch (e: Exception) {
                    Log.e("HistoryService", "Failed to sync history item to cloud (retaining local record): ${e.message}")
                }
            }
        }
    }

    fun deleteHistoryItem(id: String) {
        val authUid = getAuthenticatedUid()
        serviceScope.launch {
            try {
                sosHistoryDao?.deleteHistory(id)
            } catch (e: Exception) {
                Log.e("HistoryService", "Failed to delete history item from Room: ${e.message}")
            }

            if (authUid != null && firestore != null) {
                try {
                    firestore.collection("users").document(authUid)
                        .collection("emergency_history").document(id)
                        .delete()
                        .await()
                    Log.d("HistoryService", "Deleted history item from users/$authUid/emergency_history/$id")
                } catch (e: Exception) {
                    Log.e("HistoryService", "Failed to delete history item from cloud: ${e.message}")
                }
            }
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

    private fun serializeHistoryItemToMap(item: HistoryModel, uid: String): Map<String, Any> {
        return mapOf(
            "historyId" to item.id,
            "uid" to uid,
            "date" to System.currentTimeMillis(),
            "durationSeconds" to item.durationSeconds,
            "responseTimeSeconds" to item.responseTimeSeconds,
            "address" to item.address,
            "latitude" to item.latitude,
            "longitude" to item.longitude,
            "googleMapsLink" to "https://maps.google.com/?q=${item.latitude},${item.longitude}",
            "severity" to item.severity,
            "contactsNotified" to item.contactsNotified.joinToString("; "),
            "aiConfidence" to item.aiConfidence,
            "triggerSource" to item.triggerType,
            "status" to "RESOLVED",
            "deviceUsed" to item.deviceUsed,
            "resolutionNotes" to item.resolutionNotes,
            "resolvedBy" to item.resolvedBy
        )
    }

    suspend fun clearSession(uid: String) {
        try {
            if (uid.isNotBlank()) {
                sosHistoryDao?.deleteHistoryForUser(uid)
            }
        } catch (e: Exception) {
            Log.e("HistoryService", "Failed to clear history for user $uid: ${e.message}")
        }
        _history.value = emptyList()
    }
}
