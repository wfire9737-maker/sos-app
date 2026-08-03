package com.example.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent

class GeofenceBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val geofencingEvent = GeofencingEvent.fromIntent(intent)
        if (geofencingEvent == null || geofencingEvent.hasError()) {
            Log.e("GeofenceReceiver", "Geofencing error")
            return
        }

        val geofenceTransition = geofencingEvent.geofenceTransition
        val triggeringGeofences = geofencingEvent.triggeringGeofences
        
        if (triggeringGeofences != null) {
            for (geofence in triggeringGeofences) {
                val placeId = geofence.requestId
                val isInside = geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER
                
                // We should store this state somewhere. For now, log it.
                Log.d("GeofenceReceiver", "Geofence transition: $placeId - Inside: $isInside")
                
                // You could use a DataStore or SharedPreferences to keep track of currently active trusted places
                val prefs = context.getSharedPreferences("trusted_places_state", Context.MODE_PRIVATE)
                prefs.edit().putBoolean("is_inside_$placeId", isInside).apply()
            }
        }
    }
}
