package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.ble.BleManager.BleState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

@AndroidEntryPoint
class BleForegroundService : Service() {

    @Inject
    lateinit var deviceService: DeviceService

    @Inject
    lateinit var emergencyProvider: EmergencyProvider

    @Inject
    lateinit var fallDetectionService: FallDetectionService

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var stateObserverJob: Job? = null

    companion object {
        const val CHANNEL_ID = "ble_service_channel"
        const val NOTIFICATION_ID = 2001
        const val ACTION_START_BLE_SERVICE = "com.example.service.START_BLE_SERVICE"
        const val ACTION_STOP_BLE_SERVICE = "com.example.service.STOP_BLE_SERVICE"

        fun start(context: Context) {
            val intent = Intent(context, BleForegroundService::class.java).apply {
                action = ACTION_START_BLE_SERVICE
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                Log.e("BleForegroundService", "Failed to start BleForegroundService: ${e.message}")
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, BleForegroundService::class.java).apply {
                action = ACTION_STOP_BLE_SERVICE
            }
            try {
                context.startService(intent)
            } catch (e: Exception) {
                Log.e("BleForegroundService", "Failed to stop BleForegroundService: ${e.message}")
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        Log.d("BleForegroundService", "BLE_SERVICE: started")
        createNotificationChannel()
        setupFallDetection()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == ACTION_STOP_BLE_SERVICE) {
            Log.d("BleForegroundService", "BLE_SERVICE: stopping")
            stateObserverJob?.cancel()
            deviceService.stopEsp32Polling()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        startForegroundWithNotification("Initializing ESP32 SOS monitoring...")
        deviceService.startEsp32Polling()
        setupFallDetection()
        observeBleConnectionState()

        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        Log.d("BleForegroundService", "App task removed from Recents; maintaining BLE foreground service.")
        deviceService.startEsp32Polling()
        setupFallDetection()
    }

    private fun setupFallDetection() {
        deviceService.bleManager.motionProcessor.onPossibleFallDetected = { reading, eventId ->
            Log.d("BleForegroundService", "MOTION: possible fall detected from MPU6050 event $eventId (MAG=${reading.accelerationMagnitudeG}g)")
            val isFallEnabled = getSharedPreferences("smart_sos_settings", Context.MODE_PRIVATE)
                .getBoolean("fall_detection_enabled", true)
            val emergencyActive = emergencyProvider.emergencyService.isEmergencyActive()
            val fallState = fallDetectionService.currentState.value

            Log.d("SOS_FALL_DEBUG", "fallDetectionEnabled=$isFallEnabled")
            com.example.ble.FallDebugBridge.log("Fall enabled", "$isFallEnabled")
            Log.d("SOS_FALL_DEBUG", "emergencyActive=$emergencyActive")
            com.example.ble.FallDebugBridge.log("Emergency active", "$emergencyActive")
            Log.d("SOS_FALL_DEBUG", "fallState=$fallState")
            com.example.ble.FallDebugBridge.log("Fall state", "$fallState")

            if (isFallEnabled && !emergencyActive && fallState != "FALL_COUNTDOWN") {
                Log.d("SOS_FALL_DEBUG", "Calling FallDetectionService.triggerFall()")
                com.example.ble.FallDebugBridge.log("Calling triggerFall", "Invoking triggerFall()")
                fallDetectionService.triggerFall()
            } else {
                Log.d("SOS_FALL_DEBUG", "Fall trigger blocked: isFallEnabled=$isFallEnabled, emergencyActive=$emergencyActive, fallState=$fallState")
                com.example.ble.FallDebugBridge.log("Calling triggerFall", "BLOCKED (enabled=$isFallEnabled, emergencyActive=$emergencyActive, state=$fallState)")
            }
        }

        fallDetectionService.onFallCancelledCallback = {
            deviceService.bleManager.motionProcessor.resetToNormal()
        }

        fallDetectionService.onSosTriggeredCallback = {
            emergencyProvider.triggerFallEmergency()
        }
    }

    private fun observeBleConnectionState() {
        stateObserverJob?.cancel()
        stateObserverJob = serviceScope.launch {
            deviceService.bleManager.connectionState.collect { state ->
                val statusText = when (state) {
                    BleState.CONNECTED, BleState.READY -> "Connected to Physical-SOS-ESP32 (GPIO 4 SOS, GPS & MPU6050 active)"
                    BleState.CONNECTING,
                    BleState.DISCOVERING_SERVICES,
                    BleState.SUBSCRIBING_STATUS_NOTIFICATIONS,
                    BleState.SUBSCRIBING_GPS_NOTIFICATIONS,
                    BleState.SUBSCRIBING_MPU_NOTIFICATIONS,
                    BleState.READING_BATTERY,
                    BleState.READING_GPS,
                    BleState.READING_MPU -> "Connecting to Physical-SOS-ESP32..."
                    BleState.SCANNING -> "Scanning for Physical-SOS-ESP32..."
                    BleState.DEVICE_NOT_FOUND -> "ESP32 not found. Retrying in background..."
                    BleState.CONNECTION_FAILED, BleState.ERROR -> "Reconnecting to Physical-SOS-ESP32..."
                    BleState.DISCONNECTED -> "Disconnected. Auto-reconnect active."
                    else -> "ESP32 BLE Monitoring Active"
                }
                updateForegroundNotification(statusText)
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "ESP32 BLE Hardware Monitoring",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Maintains continuous connection with physical ESP32 SOS hardware button."
                setShowBadge(false)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(statusText: String): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Smart SOS Hardware Monitor")
            .setContentText(statusText)
            .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    private fun startForegroundWithNotification(statusText: String) {
        val notification = buildNotification(statusText)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            Log.e("BleForegroundService", "Error starting foreground notification: ${e.message}")
        }
    }

    private fun updateForegroundNotification(statusText: String) {
        val notification = buildNotification(statusText)
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        manager?.notify(NOTIFICATION_ID, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        stateObserverJob?.cancel()
        serviceScope.cancel()
        Log.d("BleForegroundService", "BLE_SERVICE: stopped")
    }
}
