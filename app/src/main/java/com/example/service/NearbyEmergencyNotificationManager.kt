package com.example.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.model.NearbyEmergencyAlert
import com.example.model.NearbyEmergencyAlertType
import com.example.repository.NearbyEmergencyAlertRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NearbyEmergencyNotificationManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: NearbyEmergencyAlertRepository
) {

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val scope = CoroutineScope(Dispatchers.Default)
    private val shownNotifications = mutableSetOf<String>()

    companion object {
        private const val CHANNEL_ID = "nearby_emergency_alerts"
        private const val CHANNEL_NAME = "Nearby Emergency Alerts"
    }

    init {
        createNotificationChannel()
        observeAlerts()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "High priority emergency alerts from nearby devices"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun observeAlerts() {
        scope.launch {
            repository.activeEmergencyAlerts.collect { alerts ->
                for (alert in alerts) {
                    if (!shownNotifications.contains(alert.alertId)) {
                        showNotification(alert)
                        shownNotifications.add(alert.alertId)
                    }
                }
            }
        }
    }

    private fun showNotification(alert: NearbyEmergencyAlert) {
        // Prepare human-readable strings
        val alertTypeName = when (alert.alertType) {
            NearbyEmergencyAlertType.MANUAL_SOS -> "Manual SOS"
            NearbyEmergencyAlertType.FALL_DETECTED -> "Fall Detected"
            NearbyEmergencyAlertType.VOICE_SOS -> "Voice SOS"
            NearbyEmergencyAlertType.OTHER -> "Emergency Alert"
        }
        val senderName = alert.senderDeviceName ?: "Nearby Smart SOS User"
        val bodyText = "$alertTypeName from $senderName"

        // Use deep link to navigate directly to the alerts screen
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("smartsos://nearby_alerts"), context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            alert.alertId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert) // Built-in alert icon
            .setContentTitle("Nearby Emergency Alert")
            .setContentText(bodyText)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (androidx.core.content.ContextCompat.checkSelfPermission(
                        context,
                        android.Manifest.permission.POST_NOTIFICATIONS
                    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                ) {
                    notificationManager.notify(alert.alertId.hashCode(), builder.build())
                }
            } else {
                notificationManager.notify(alert.alertId.hashCode(), builder.build())
            }
        } catch (e: Exception) {
            // Fails gracefully if permissions are not granted or another error occurs
        }
    }
}
