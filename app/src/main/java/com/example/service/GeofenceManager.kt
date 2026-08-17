package com.example.service

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.model.TrustedPlace
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices

class GeofenceManager(private val context: Context) {
    private val geofencingClient: GeofencingClient by lazy {
        LocationServices.getGeofencingClient(context)
    }
    
    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(context, GeofenceBroadcastReceiver::class.java)
        PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
    }

    @SuppressLint("MissingPermission")
    fun updateGeofences(places: List<TrustedPlace>) {
        if (places.isEmpty()) {
            removeGeofences()
            return
        }
        
        val geofenceList = places.map { place ->
            Geofence.Builder()
                .setRequestId(place.placeId)
                .setCircularRegion(
                    place.latitude,
                    place.longitude,
                    place.radius.toFloat()
                )
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT)
                .build()
        }
        
        val request = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER or GeofencingRequest.INITIAL_TRIGGER_EXIT)
            .addGeofences(geofenceList)
            .build()
            
        try {
            geofencingClient.addGeofences(request, geofencePendingIntent)?.run {
                addOnSuccessListener {
                    Log.d("GeofenceManager", "Geofences added successfully")
                }
                addOnFailureListener {
                    Log.e("GeofenceManager", "Failed to add geofences", it)
                }
            }
        } catch (e: Throwable) {
            Log.e("GeofenceManager", "Geofencing failed: ${e.message}")
        }
    }
    
    fun removeGeofences() {
        try {
            geofencingClient.removeGeofences(geofencePendingIntent)
        } catch (e: Throwable) {
            Log.e("GeofenceManager", "Failed to remove geofences", e)
        }
    }
}
