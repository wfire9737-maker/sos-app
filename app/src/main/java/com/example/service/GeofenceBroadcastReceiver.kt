package com.example.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent

class GeofenceBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val geofencingEvent = try {
            GeofencingEvent.fromIntent(intent)
        } catch (e: Exception) {
            Log.e("GeofenceReceiver", "Failed to parse GeofencingEvent from intent", e)
            null
        }

        if (geofencingEvent == null || geofencingEvent.hasError()) {
            val errorCode = geofencingEvent?.errorCode ?: -1
            Log.e("GeofenceReceiver", "Geofencing error code: $errorCode")
            return
        }

        val geofenceTransition = geofencingEvent.geofenceTransition
        val triggeringGeofences = geofencingEvent.triggeringGeofences

        if (triggeringGeofences != null) {
            val prefs = context.getSharedPreferences("trusted_places_state", Context.MODE_PRIVATE)
            val editor = prefs.edit()
            val now = System.currentTimeMillis()

            for (geofence in triggeringGeofences) {
                val placeId = geofence.requestId
                if (placeId.isBlank()) continue

                val isInside = geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER
                
                Log.d("GeofenceReceiver", "Geofence transition: $placeId - Inside: $isInside at timestamp: $now")
                
                editor.putBoolean("is_inside_$placeId", isInside)
                editor.putLong("timestamp_$placeId", now)
            }
            editor.apply()
        }
    }
}
