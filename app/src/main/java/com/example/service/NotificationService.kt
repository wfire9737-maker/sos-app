package com.example.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.model.NotificationItem
import com.example.model.NotificationType
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class NotificationService(private val context: Context, private val firestore: FirebaseFirestore?) {
    
    private val _notifications = MutableStateFlow<List<NotificationItem>>(emptyList())
    val notifications: StateFlow<List<NotificationItem>> = _notifications.asStateFlow()

    private val _fcmToken = MutableStateFlow<String>("")
    val fcmToken: StateFlow<String> = _fcmToken.asStateFlow()

    private val sharedPrefs: SharedPreferences = context.getSharedPreferences("guardian_sos_notifications", Context.MODE_PRIVATE)
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val CHANNEL_ID = "guardian_sos_alerts"

    init {
        createNotificationChannel()
        loadLocalNotifications()
        retrieveFCMToken()
        listenToFirestoreNotifications()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Guardian SOS Critical Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Critical SOS alert notifications and physical telemetry alerts."
                enableVibration(true)
                enableLights(true)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun retrieveFCMToken() {
        try {
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val token = task.result
                    _fcmToken.value = token
                    Log.d("NotificationService", "FCM Token acquired: $token")
                    // In a production app, you would send this token to your backend/ESP32 gateway.
                } else {
                    val defaultToken = "fcm_simulated_token_" + UUID.randomUUID().toString().take(8)
                    _fcmToken.value = defaultToken
                    Log.w("NotificationService", "FCM Token fetch failed, generating simulated token: ${task.exception?.message}")
                }
            }
        } catch (e: Exception) {
            val defaultToken = "fcm_simulated_token_" + UUID.randomUUID().toString().take(8)
            _fcmToken.value = defaultToken
            Log.e("NotificationService", "FCM service unavailable, using local simulation token.")
        }
    }

    private fun loadLocalNotifications() {
        val jsonStr = sharedPrefs.getString("notification_items", "[]") ?: "[]"
        try {
            val arr = JSONArray(jsonStr)
            val list = mutableListOf<NotificationItem>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(parseJsonToNotification(obj))
            }
            // Sort by timestamp descending so newer show first
            _notifications.value = list.sortedByDescending { it.timestamp }
        } catch (e: Exception) {
            Log.e("NotificationService", "Failed to deserialize local notifications: ${e.message}")
            // Populate defaults if empty to let user see something beautiful immediately!
            populateSimulatedDefaults()
        }
    }

    private fun saveLocalNotifications() {
        val arr = JSONArray()
        for (item in _notifications.value) {
            arr.put(serializeNotificationToJson(item))
        }
        sharedPrefs.edit().putString("notification_items", arr.toString()).apply()
    }

    private fun listenToFirestoreNotifications() {
        val db = firestore ?: return
        // Real-time listener for user alerts mapped to local notifications
        db.collection("notifications")
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w("NotificationService", "Firestore notifications listener failed", e)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = mutableListOf<NotificationItem>()
                    for (doc in snapshot.documents) {
                        try {
                            val id = doc.id
                            val title = doc.getString("title") ?: "Alert"
                            val body = doc.getString("body") ?: ""
                            val timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
                            val typeStr = doc.getString("type") ?: "EMERGENCY"
                            val isRead = doc.getBoolean("isRead") ?: false
                            val deviceId = doc.getString("deviceId")
                            
                            val type = try {
                                NotificationType.valueOf(typeStr)
                            } catch (ex: Exception) {
                                NotificationType.EMERGENCY
                            }

                            list.add(
                                NotificationItem(
                                    id = id,
                                    title = title,
                                    body = body,
                                    timestamp = timestamp,
                                    type = type,
                                    isRead = isRead,
                                    deviceId = deviceId
                                )
                            )
                        } catch (ex: Exception) {
                            Log.e("NotificationService", "Error parsing doc notification: ${ex.message}")
                        }
                    }
                    if (list.isNotEmpty()) {
                        // Merge or replace
                        val merged = (list + _notifications.value)
                            .distinctBy { it.id }
                            .sortedByDescending { it.timestamp }
                        _notifications.value = merged
                        saveLocalNotifications()
                    }
                }
            }
    }

    // --- MAIN CRUD OPERATIONS ---

    fun addNotification(item: NotificationItem) {
        val updated = (_notifications.value + item).distinctBy { it.id }.sortedByDescending { it.timestamp }
        _notifications.value = updated
        saveLocalNotifications()
        
        // Post real Android system notification bar alert!
        showSystemNotificationBar(item)

        // Upload to Firestore if online
        val db = firestore
        if (db != null) {
            val map = hashMapOf(
                "title" to item.title,
                "body" to item.body,
                "timestamp" to item.timestamp,
                "type" to item.type.name,
                "isRead" to item.isRead,
                "deviceId" to item.deviceId
            )
            db.collection("notifications").document(item.id).set(map)
                .addOnFailureListener { e ->
                    Log.w("NotificationService", "Failed to upload notification to Firestore: ${e.message}")
                }
        }
    }

    fun markAsRead(id: String) {
        val updated = _notifications.value.map {
            if (it.id == id) it.copy(isRead = true) else it
        }
        _notifications.value = updated
        saveLocalNotifications()

        // Sync to Firestore
        val db = firestore
        if (db != null) {
            db.collection("notifications").document(id).update("isRead", true)
        }
    }

    fun markAllAsRead() {
        val updated = _notifications.value.map { it.copy(isRead = true) }
        _notifications.value = updated
        saveLocalNotifications()

        // Bulk update in Firestore (simulated batch/async)
        val db = firestore
        if (db != null) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val batch = db.batch()
                    for (item in _notifications.value) {
                        if (!item.isRead) {
                            val docRef = db.collection("notifications").document(item.id)
                            batch.update(docRef, "isRead", true)
                        }
                    }
                    batch.commit().await()
                } catch (e: Exception) {
                    Log.e("NotificationService", "Batch mark read failed: ${e.message}")
                }
            }
        }
    }

    fun deleteNotification(id: String) {
        val updated = _notifications.value.filter { it.id != id }
        _notifications.value = updated
        saveLocalNotifications()

        val db = firestore
        if (db != null) {
            db.collection("notifications").document(id).delete()
        }
    }

    // --- SYSTEM NOTIFICATION PROVIDER BAR BRIDGE ---

    private fun showSystemNotificationBar(item: NotificationItem) {
        try {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                item.id.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_notify_chat)
                .setContentTitle(item.title)
                .setContentText(item.body)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setCategory(NotificationCompat.CATEGORY_ALARM)

            notificationManager.notify(item.id.hashCode(), builder.build())
        } catch (e: Exception) {
            Log.e("NotificationService", "Failed to build system notification bar icon: ${e.message}")
        }
    }

    // --- INITIALIZE BEAUTIFUL PRESET DEFAULTS IF EMPTY ---

    private fun populateSimulatedDefaults() {
        val defaults = listOf(
            NotificationItem(
                title = "🚨 EMERGENCY: Fall Detected!",
                body = "Critical drop detected on Marcus Vance's smart-band. Automated helpline alert dispatched.",
                type = NotificationType.EMERGENCY,
                deviceId = "ESP32-SOS-BAND-81F4",
                timestamp = System.currentTimeMillis() - 1000 * 60 * 5 // 5m ago
            ),
            NotificationItem(
                title = "🔋 Wearable Battery Low (15%)",
                body = "ESP32 SOS Band battery is dropping. Connect magnetic charging dock soon.",
                type = NotificationType.BATTERY_LOW,
                deviceId = "ESP32-SOS-BAND-81F4",
                timestamp = System.currentTimeMillis() - 1000 * 60 * 30 // 30m ago
            ),
            NotificationItem(
                title = "📡 Smart Device Offline",
                body = "ESP32 smart-band has lost BLE connectivity. Check user range or physical device status.",
                type = NotificationType.DEVICE_OFFLINE,
                deviceId = "ESP32-SOS-BAND-81F4",
                timestamp = System.currentTimeMillis() - 1000 * 60 * 120 // 2h ago
            ),
            NotificationItem(
                title = "🛰️ GPS Signal Lost",
                body = "Precision latitude telemetry unavailable. Re-establishing connection with satellite nodes.",
                type = NotificationType.GPS_UNAVAILABLE,
                deviceId = "ESP32-SOS-BAND-81F4",
                timestamp = System.currentTimeMillis() - 1000 * 60 * 240 // 4h ago
            ),
            NotificationItem(
                title = "📥 Firmware Update Available (v2.4)",
                body = "Critical wireless telemetry updates. Update firmware to preserve optimal battery lifetime.",
                type = NotificationType.FIRMWARE_UPDATE,
                deviceId = "ESP32-SOS-BAND-81F4",
                timestamp = System.currentTimeMillis() - 1000 * 60 * 600 // 10h ago
            ),
            NotificationItem(
                title = "🏠 Safe Arrival Confirmation",
                body = "Marcus Vance safely entered 'Home Geofence' radius. SOS watch deactivated.",
                type = NotificationType.SAFE_ARRIVAL,
                deviceId = "ESP32-SOS-BAND-81F4",
                timestamp = System.currentTimeMillis() - 1000 * 60 * 1200 // 20h ago
            )
        )
        _notifications.value = defaults
        saveLocalNotifications()
    }

    // --- SIMULATE EXTERNAL FCM INCOMING NOTIFICATIONS ---

    fun triggerSimulatedFCMNotification(type: NotificationType) {
        val item = when (type) {
            NotificationType.EMERGENCY -> NotificationItem(
                title = "🚨 EMERGENCY: Manual SOS Alert!",
                body = "Physical SOS button held down on ESP32 band. Initiating live triage tracking.",
                type = NotificationType.EMERGENCY,
                deviceId = "ESP32-SOS-BAND-81F4"
            )
            NotificationType.BATTERY_LOW -> NotificationItem(
                title = "🔋 CRITICAL battery: Smart-band 8%",
                body = "ESP32 battery level is critical. Fall detection and GPS will stop soon.",
                type = NotificationType.BATTERY_LOW,
                deviceId = "ESP32-SOS-BAND-81F4"
            )
            NotificationType.DEVICE_OFFLINE -> NotificationItem(
                title = "📡 BLE Beacon Offline",
                body = "Continuous wireless pulse check failed. Heartbeat signal lost.",
                type = NotificationType.DEVICE_OFFLINE,
                deviceId = "ESP32-SOS-BAND-81F4"
            )
            NotificationType.GPS_UNAVAILABLE -> NotificationItem(
                title = "🛰️ Telemetry Warning: GPS Lost",
                body = "Satellite line-of-sight obstructed. Switching to cellular tower triangulation.",
                type = NotificationType.GPS_UNAVAILABLE,
                deviceId = "ESP32-SOS-BAND-81F4"
            )
            NotificationType.FIRMWARE_UPDATE -> NotificationItem(
                title = "📥 Smart Band OTA Firmware v2.4.1",
                body = "Hotfix for Bluetooth reconnect jitter is ready to install via ESP32 interface.",
                type = NotificationType.FIRMWARE_UPDATE,
                deviceId = "ESP32-SOS-BAND-81F4"
            )
            NotificationType.SAFE_ARRIVAL -> NotificationItem(
                title = "🏠 Safe Arrival: Office",
                body = "User checked into Office boundary. Automated safe-arrival ping received.",
                type = NotificationType.SAFE_ARRIVAL,
                deviceId = "ESP32-SOS-BAND-81F4"
            )
            NotificationType.EMERGENCY_CANCELLED -> NotificationItem(
                title = "✅ Emergency SOS Cancelled",
                body = "User manually deactivated active distress state using emergency screen passcode.",
                type = NotificationType.EMERGENCY_CANCELLED,
                deviceId = "ESP32-SOS-BAND-81F4"
            )
        }
        addNotification(item)
    }

    // --- JSON PARSING UTILITIES ---

    private fun serializeNotificationToJson(item: NotificationItem): JSONObject {
        val obj = JSONObject()
        obj.put("id", item.id)
        obj.put("title", item.title)
        obj.put("body", item.body)
        obj.put("timestamp", item.timestamp)
        obj.put("type", item.type.name)
        obj.put("isRead", item.isRead)
        obj.put("deviceId", item.deviceId ?: "")
        return obj
    }

    private fun parseJsonToNotification(obj: JSONObject): NotificationItem {
        val id = obj.optString("id", UUID.randomUUID().toString())
        val title = obj.optString("title", "")
        val body = obj.optString("body", "")
        val timestamp = obj.optLong("timestamp", System.currentTimeMillis())
        val typeStr = obj.optString("type", "EMERGENCY")
        val isRead = obj.optBoolean("isRead", false)
        val deviceId = obj.optString("deviceId", "").let { if (it.isEmpty()) null else it }
        
        val type = try {
            NotificationType.valueOf(typeStr)
        } catch (e: Exception) {
            NotificationType.EMERGENCY
        }

        return NotificationItem(
            id = id,
            title = title,
            body = body,
            timestamp = timestamp,
            type = type,
            isRead = isRead,
            deviceId = deviceId
        )
    }
}
