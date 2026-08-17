package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class VoiceSosForegroundService : Service() {

    @Inject
    lateinit var voiceSosService: VoiceSosService

    private val CHANNEL_ID = "VOICE_SOS_CHANNEL"
    private val NOTIFICATION_ID = 1002

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == "STOP") {
            Log.d("VoiceSosFgService", "Stopping Foreground Service")
            voiceSosService.isContinuousMode = false
            voiceSosService.stopSpeechRecognition()
            stopForeground(true)
            stopSelf()
            return START_NOT_STICKY
        }

        Log.d("VoiceSosFgService", "Starting Foreground Service")
        val notification = createNotification()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        voiceSosService.isContinuousMode = true
        voiceSosService.startSpeechRecognition(this)

        return START_STICKY
    }

    override fun onDestroy() {
        voiceSosService.isContinuousMode = false
        voiceSosService.stopSpeechRecognition()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Voice SOS",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Listening for Voice SOS wake phrase"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Voice SOS Active")
            .setContentText("Listening for emergency phrase...")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now) // We can use this or any existing app icon
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}
