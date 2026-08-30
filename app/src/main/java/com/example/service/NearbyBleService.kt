package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.ble.nearby.NearbyBleManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

@AndroidEntryPoint
class NearbyBleService : Service() {

    @Inject
    lateinit var nearbyBleManager: NearbyBleManager

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val prefs by lazy { getSharedPreferences("smart_sos_settings", Context.MODE_PRIVATE) }
    
    private val prefListener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
        if (key == "nearby_presence_interval") {
            val interval = sharedPreferences.getInt("nearby_presence_interval", 0)
            nearbyBleManager.updatePresenceSettings(interval)
            if (interval > 0) {
                startForegroundWithNotification()
            } else {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
    }

    companion object {
        const val CHANNEL_ID = "nearby_presence_channel"
        const val NOTIFICATION_ID = 2002
        
        const val ACTION_ACCEPT_CONNECTION = "com.example.service.NearbyBleService.ACTION_ACCEPT_CONNECTION"
        const val ACTION_DECLINE_CONNECTION = "com.example.service.NearbyBleService.ACTION_DECLINE_CONNECTION"
        const val EXTRA_MAC_ADDRESS = "extra_mac_address"

        
        fun startOrStop(context: Context) {
            val prefs = context.getSharedPreferences("smart_sos_settings", Context.MODE_PRIVATE)
            val interval = prefs.getInt("nearby_presence_interval", 0)
            val intent = Intent(context, NearbyBleService::class.java)
            try {
                if (interval > 0) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(intent)
                    } else {
                        context.startService(intent)
                    }
                } else {
                    context.stopService(intent)
                }
            } catch (e: Exception) {
                Log.e("NearbyBleService", "Failed to start/stop NearbyBleService", e)
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        prefs.registerOnSharedPreferenceChangeListener(prefListener)
        createNotificationChannel()
        Log.d("NearbyBleService", "NEARBY_SERVICE: started")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("NearbyBleService", "NEARBY_SERVICE: onStartCommand")
        
        if (intent?.action == ACTION_ACCEPT_CONNECTION) {
            val mac = intent.getStringExtra(EXTRA_MAC_ADDRESS)
            if (mac != null) nearbyBleManager.acceptIncomingConnection(mac)
            return START_STICKY
        } else if (intent?.action == ACTION_DECLINE_CONNECTION) {
            val mac = intent.getStringExtra(EXTRA_MAC_ADDRESS)
            if (mac != null) nearbyBleManager.declineIncomingConnection(mac)
            return START_STICKY
        }
        
        val initialInterval = prefs.getInt("nearby_presence_interval", 0)
        if (initialInterval > 0) {
            startForegroundWithNotification()
            nearbyBleManager.updatePresenceSettings(initialInterval)
            return START_STICKY
        } else {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Nearby Emergency Presence",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Maintains background BLE presence for nearby emergency discovery."
                setShowBadge(false)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun startForegroundWithNotification() {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Smart SOS Nearby")
            .setContentText("Nearby Emergency Presence Active")
            .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()

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
            Log.e("NearbyBleService", "Error starting foreground notification: ${e.message}")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        prefs.unregisterOnSharedPreferenceChangeListener(prefListener)
        nearbyBleManager.updatePresenceSettings(0)
        Log.d("NearbyBleService", "NEARBY_SERVICE: stopped")
    }
}
