package com.example.service

import android.content.Context
import android.util.Log
import com.example.data.local.dao.EmergencyContactDao
import com.example.data.local.dao.SosHistoryDao
import com.example.data.local.dao.TrustedPlaceDao
import com.example.model.HistoryModel
import com.example.model.SyncResult
import com.example.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class SyncService(
    private val context: Context,
    private val authService: AuthService,
    private val databaseService: DatabaseService,
    private val trustedPlacesService: TrustedPlacesService,
    private val historyService: HistoryService,
    private val contactDao: EmergencyContactDao?,
    private val trustedPlaceDao: TrustedPlaceDao?,
    private val sosHistoryDao: SosHistoryDao?,
    private val geofenceManager: GeofenceManager
) {
    private val firestore: FirebaseFirestore?
        get() = try {
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            null
        }

    suspend fun syncAllUserData(uid: String): SyncResult = withContext(Dispatchers.IO) {
        val currentAuthUid = try {
            FirebaseAuth.getInstance().currentUser?.uid
        } catch (e: Exception) {
            null
        }

        val targetUid = when {
            !currentAuthUid.isNullOrBlank() -> currentAuthUid
            uid.isNotBlank() && !uid.startsWith("demo-", ignoreCase = true) && uid != "user-101" && uid != "anonymous" -> uid
            else -> null
        }

        if (targetUid.isNullOrBlank()) {
            Log.w("CLOUD_DEBUG", "MANUAL_SYNC_ABORTED: No authenticated Firebase user")
            return@withContext SyncResult(
                success = false,
                syncedCategories = emptyList(),
                failedCategories = listOf("Authentication"),
                message = "Please log in to sync your data."
            )
        }

        val db = firestore
        if (db == null) {
            Log.e("CLOUD_DEBUG", "MANUAL_SYNC_ABORTED: Firestore is unavailable")
            return@withContext SyncResult(
                success = false,
                syncedCategories = emptyList(),
                failedCategories = listOf("Cloud Firestore"),
                message = "Cloud services are temporarily unavailable. Please check your network connection."
            )
        }

        Log.d("CLOUD_DEBUG", "MANUAL_SYNC_START uid=$targetUid")
        val syncedCategories = mutableListOf<String>()
        val failedCategories = mutableListOf<String>()

        // -------------------------------------------------------------
        // 1. USER PROFILE SYNC
        // -------------------------------------------------------------
        try {
            // Upload Local Profile
            val currentAuthState = authService.authState.value
            val localUser = (currentAuthState as? AuthState.Success)?.user
            if (localUser != null && localUser.uid.isNotBlank()) {
                val userToUpload = localUser.copy(uid = targetUid)
                db.collection("users").document(targetUid)
                    .set(userToUpload.toMap(), SetOptions.merge())
                    .await()
            }
            Log.d("CLOUD_DEBUG", "MANUAL_SYNC_UPLOAD_COMPLETE category=profile")

            // Download Cloud Profile
            val profileDoc = db.collection("users").document(targetUid).get().await()
            if (profileDoc.exists()) {
                val remoteUser = User.fromMap(profileDoc.data ?: emptyMap()).copy(uid = targetUid)
                authService.updateProfile(remoteUser)
            }
            Log.d("CLOUD_DEBUG", "MANUAL_SYNC_DOWNLOAD_COMPLETE category=profile")
            syncedCategories.add("Profile")
        } catch (e: Exception) {
            Log.e("CLOUD_DEBUG", "MANUAL_SYNC_ERROR category=profile error=${e.message}", e)
            failedCategories.add("Profile")
        }

        // -------------------------------------------------------------
        // 2. EMERGENCY CONTACTS SYNC
        // -------------------------------------------------------------
        try {
            // Upload Local Contacts
            val localContacts = databaseService.contacts.value.filter { contact ->
                contact.id.isNotBlank() && !contact.id.startsWith("demo-contact-", ignoreCase = true)
            }
            for (contact in localContacts) {
                val contactToUpload = contact.copy(userId = targetUid)
                db.collection("users").document(targetUid)
                    .collection("contacts").document(contactToUpload.id)
                    .set(contactToUpload.toMap(), SetOptions.merge())
                    .await()
            }
            Log.d("CLOUD_DEBUG", "MANUAL_SYNC_UPLOAD_COMPLETE category=contacts")

            // Download Cloud Contacts into Room and StateFlow
            databaseService.syncContactsFromCloud(targetUid)
            Log.d("CLOUD_DEBUG", "MANUAL_SYNC_DOWNLOAD_COMPLETE category=contacts")
            syncedCategories.add("Contacts")
        } catch (e: Exception) {
            Log.e("CLOUD_DEBUG", "MANUAL_SYNC_ERROR category=contacts error=${e.message}", e)
            failedCategories.add("Contacts")
        }

        // -------------------------------------------------------------
        // 3. TRUSTED PLACES SYNC
        // -------------------------------------------------------------
        try {
            // Upload Local Trusted Places
            val localPlaces = trustedPlacesService.trustedPlaces.value
            for (place in localPlaces) {
                if (place.placeId.isNotBlank()) {
                    val placeToUpload = place.copy(userId = targetUid)
                    db.collection("users").document(targetUid)
                        .collection("trusted_places").document(placeToUpload.placeId)
                        .set(placeToUpload.toMap(), SetOptions.merge())
                        .await()
                }
            }
            Log.d("CLOUD_DEBUG", "MANUAL_SYNC_UPLOAD_COMPLETE category=trustedPlaces")

            // Download Cloud Trusted Places
            trustedPlacesService.initialize(targetUid)
            Log.d("CLOUD_DEBUG", "MANUAL_SYNC_DOWNLOAD_COMPLETE category=trustedPlaces")
            syncedCategories.add("Trusted Places")
        } catch (e: Exception) {
            Log.e("CLOUD_DEBUG", "MANUAL_SYNC_ERROR category=trustedPlaces error=${e.message}", e)
            failedCategories.add("Trusted Places")
        }

        // -------------------------------------------------------------
        // 4. EMERGENCY HISTORY SYNC
        // -------------------------------------------------------------
        try {
            // Upload Local History
            val localHistory = historyService.history.value
            for (item in localHistory) {
                if (item.id.isNotBlank()) {
                    val map = serializeHistoryToMap(item, targetUid)
                    db.collection("users").document(targetUid)
                        .collection("emergency_history").document(item.id)
                        .set(map, SetOptions.merge())
                        .await()
                }
            }
            Log.d("CLOUD_DEBUG", "MANUAL_SYNC_UPLOAD_COMPLETE category=history")

            // Download Cloud History
            historyService.syncHistoryFromCloud(targetUid)
            Log.d("CLOUD_DEBUG", "MANUAL_SYNC_DOWNLOAD_COMPLETE category=history")
            syncedCategories.add("Emergency History")
        } catch (e: Exception) {
            Log.e("CLOUD_DEBUG", "MANUAL_SYNC_ERROR category=history error=${e.message}", e)
            failedCategories.add("Emergency History")
        }

        // -------------------------------------------------------------
        // 5. SETTINGS & PREFERENCES SYNC
        // -------------------------------------------------------------
        try {
            // Upload Local Safe Settings
            val prefs = context.getSharedPreferences("smart_sos_settings", Context.MODE_PRIVATE)
            val allPrefs = prefs.all
            val safePrefsMap = mutableMapOf<String, Any>()

            for ((key, value) in allPrefs) {
                if (value == null) continue
                // Strict isolation: never upload PINs, tokens, encryption keys, or device bonding secrets
                if (key.equals("EMERGENCY_PIN", ignoreCase = true) ||
                    key.contains("pin", ignoreCase = true) ||
                    key.contains("key", ignoreCase = true) ||
                    key.contains("token", ignoreCase = true) ||
                    key == "bonded_esp32_mac" ||
                    key == "emergency_custom_sound_uri"
                ) {
                    continue
                }
                safePrefsMap[key] = value
            }

            if (safePrefsMap.isNotEmpty()) {
                db.collection("users").document(targetUid)
                    .collection("settings").document("preferences")
                    .set(safePrefsMap, SetOptions.merge())
                    .await()
            }
            Log.d("CLOUD_DEBUG", "MANUAL_SYNC_UPLOAD_COMPLETE category=settings")

            // Download Cloud Settings
            databaseService.loadUserSettingsFromCloud(targetUid)
            Log.d("CLOUD_DEBUG", "MANUAL_SYNC_DOWNLOAD_COMPLETE category=settings")
            syncedCategories.add("Settings")
        } catch (e: Exception) {
            Log.e("CLOUD_DEBUG", "MANUAL_SYNC_ERROR category=settings error=${e.message}", e)
            failedCategories.add("Settings")
        }

        // -------------------------------------------------------------
        // FINAL RESULT ASSEMBLY
        // -------------------------------------------------------------
        val isAllSuccess = failedCategories.isEmpty()
        Log.d("CLOUD_DEBUG", "MANUAL_SYNC_COMPLETE success=$isAllSuccess")

        val resultMessage = when {
            isAllSuccess -> "Sync completed! All user data is synchronized with the cloud."
            syncedCategories.isEmpty() -> "Sync failed for all categories. Please check your connection."
            else -> "Sync completed with warnings. Synced: ${syncedCategories.joinToString(", ")}. Failed: ${failedCategories.joinToString(", ")}."
        }

        SyncResult(
            success = isAllSuccess,
            syncedCategories = syncedCategories,
            failedCategories = failedCategories,
            message = resultMessage
        )
    }

    private fun serializeHistoryToMap(item: HistoryModel, uid: String): Map<String, Any> {
        val now = System.currentTimeMillis()
        return mapOf(
            "historyId" to item.id,
            "uid" to uid,
            "date" to now,
            "updatedAt" to now,
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
}
