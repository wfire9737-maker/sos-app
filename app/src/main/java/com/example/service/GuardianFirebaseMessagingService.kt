package com.example.service

import android.util.Log
import com.example.model.NotificationItem
import com.example.model.NotificationType
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import java.util.UUID

class GuardianFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("GuardianFCMService", "FCM token updated: $token")
        // Typically, this new token is uploaded securely to the user profiles or device configuration database.
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d("GuardianFCMService", "Inbound FCM message received from sender: ${remoteMessage.from}")

        val title = remoteMessage.notification?.title ?: remoteMessage.data["title"] ?: "Guardian SOS Update"
        val body = remoteMessage.notification?.body ?: remoteMessage.data["body"] ?: "New physical wearable event."
        val typeStr = remoteMessage.data["type"] ?: "EMERGENCY"
        val deviceId = remoteMessage.data["deviceId"] ?: "ESP32-SOS-BAND-81F4"

        val type = try {
            NotificationType.valueOf(typeStr)
        } catch (e: Exception) {
            NotificationType.EMERGENCY
        }

        val item = NotificationItem(
            id = UUID.randomUUID().toString(),
            title = title,
            body = body,
            timestamp = System.currentTimeMillis(),
            type = type,
            isRead = false,
            deviceId = deviceId
        )

        // Inject this directly using the global NotificationService provider
        try {
            val dbService = DatabaseService(applicationContext)
            val notificationService = NotificationService(applicationContext, dbService.firestoreInstance)
            notificationService.addNotification(item)
            Log.d("GuardianFCMService", "FCM notification processed and synced successfully.")
        } catch (e: Exception) {
            Log.e("GuardianFCMService", "FCM processor failed: ${e.message}")
        }
    }
}
