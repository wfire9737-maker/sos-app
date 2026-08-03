import os

filepath = "app/src/main/java/com/example/service/LocationService.kt"
content = """package com.example.service

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.location.Location
import android.os.Looper
import android.util.Log
import com.example.model.FavoritePlace
import com.example.model.UserLocation
import com.google.android.gms.location.*
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.json.JSONObject
import java.io.File
import java.util.UUID
import java.util.Locale

class LocationService(
    private val context: Context,
    private val firestore: FirebaseFirestore?
) {
    private val scope = CoroutineScope(Dispatchers.IO)
    private val fusedLocationClient: FusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(context)
    }
    private val cacheFile = File(context.cacheDir, "guardian_location_cache.json")

    private val _currentLocation = MutableStateFlow(UserLocation())
    val currentLocation: StateFlow<UserLocation> = _currentLocation.asStateFlow()

    private val _routePoints = MutableStateFlow<List<Pair<Double, Double>>>(emptyList())
    val routePoints: StateFlow<List<Pair<Double, Double>>> = _routePoints.asStateFlow()

    private val _isTracking = MutableStateFlow(false)
    val isTracking: StateFlow<Boolean> = _isTracking.asStateFlow()

    // No simulation mode anymore, so this is just for UI compatibility if needed, but we can keep it false
    private val _isSimulationMode = MutableStateFlow(false)
    val isSimulationMode: StateFlow<Boolean> = _isSimulationMode.asStateFlow()

    private var locationCallback: LocationCallback? = null
    private var totalDistance = 0.0

    init {
        loadCachedLocation()
        if (_currentLocation.value.favorites.isEmpty()) {
            val defaultFavorites = listOf(
                FavoritePlace("fav-home", "My Safehouse Home", 37.7739, -122.4312, "HOME"),
                FavoritePlace("fav-college", "State Tech College", 37.7801, -122.4121, "COLLEGE"),
                FavoritePlace("fav-work", "Guardian HQ Office", 37.7698, -122.4468, "WORK")
            )
            updateLocationState { it.copy(favorites = defaultFavorites) }
        }
    }

    private fun loadCachedLocation() {
        try {
            if (cacheFile.exists()) {
                val jsonStr = cacheFile.readText()
                val json = JSONObject(jsonStr)
                val userLoc = UserLocation.fromJsonObject(json)
                _currentLocation.value = userLoc
                totalDistance = userLoc.distanceTraveled
            }
        } catch (e: Exception) {
            Log.e("LocationService", "Failed to load cached location: ${e.message}")
        }
    }

    private fun saveLocationLocally(userLoc: UserLocation) {
        try {
            cacheFile.writeText(userLoc.toJsonObject().toString())
        } catch (e: Exception) {
            Log.e("LocationService", "Failed to cache location locally: ${e.message}")
        }
    }

    private fun updateLocationState(update: (UserLocation) -> UserLocation) {
        val updated = update(_currentLocation.value)
        _currentLocation.value = updated
        saveLocationLocally(updated)
        syncLocationToCloud(updated)
    }

    private fun syncLocationToCloud(userLoc: UserLocation) {
        val fs = firestore ?: return
        val uid = userLoc.userId.ifBlank { "anonymous" }
        scope.launch {
            try {
                fs.collection("locations").document(uid).set(userLoc.toMap()).await()
            } catch (e: Exception) {
                Log.e("LocationService", "Cloud location sync failed: ${e.message}")
            }
        }
    }

    fun setUserId(userId: String) {
        updateLocationState { it.copy(userId = userId) }
    }

    fun setSimulationMode(enabled: Boolean) {
        _isSimulationMode.value = false
    }

    @SuppressLint("MissingPermission")
    fun startLocationTracking(userId: String) {
        if (_isTracking.value) return
        _isTracking.value = true
        setUserId(userId)
        
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3000)
            .setWaitForAccurateLocation(false)
            .setMinUpdateIntervalMillis(2000)
            .build()
            
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                val loc: Location = locationResult.lastLocation ?: return
                
                val lastLoc = _currentLocation.value
                val results = FloatArray(1)
                Location.distanceBetween(lastLoc.latitude, lastLoc.longitude, loc.latitude, loc.longitude, results)
                val segmentDistanceKm = results[0] / 1000.0
                totalDistance += segmentDistanceKm
                
                val updatedPoints = _routePoints.value.toMutableList().apply {
                    add(Pair(loc.latitude, loc.longitude))
                    if (size > 150) removeAt(0)
                }
                _routePoints.value = updatedPoints
                
                val speedKmh = loc.speed * 3.6
                
                // Get address string
                scope.launch(Dispatchers.IO) {
                    var addressStr = ""
                    try {
                        val geocoder = Geocoder(context, Locale.getDefault())
                        val addresses = geocoder.getFromLocation(loc.latitude, loc.longitude, 1)
                        if (!addresses.isNullOrEmpty()) {
                            addressStr = addresses[0].getAddressLine(0) ?: ""
                        }
                    } catch (e: Exception) {
                        Log.e("LocationService", "Geocoder failed", e)
                    }
                    
                    updateLocationState {
                        it.copy(
                            latitude = loc.latitude,
                            longitude = loc.longitude,
                            speed = speedKmh,
                            bearing = loc.bearing,
                            altitude = loc.altitude,
                            accuracy = loc.accuracy,
                            timestamp = System.currentTimeMillis(),
                            distanceTraveled = totalDistance,
                            address = if (addressStr.isNotEmpty()) addressStr else it.address
                        )
                    }
                }
            }
        }
        
        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback!!,
                Looper.getMainLooper()
            )
        } catch (e: Exception) {
            Log.e("LocationService", "Failed to register real GPS listeners: ${e.message}")
            _isTracking.value = false
        }
    }

    fun stopLocationTracking() {
        _isTracking.value = false
        locationCallback?.let {
            fusedLocationClient.removeLocationUpdates(it)
        }
        locationCallback = null
    }

    fun saveFavoritePlace(name: String, lat: Double, lng: Double, type: String) {
        val newFav = FavoritePlace(
            id = "fav-" + UUID.randomUUID().toString().take(6),
            name = name.trim(),
            latitude = lat,
            longitude = lng,
            type = type.uppercase()
        )
        updateLocationState { loc ->
            val updatedList = loc.favorites.toMutableList().apply {
                if (type.uppercase() in listOf("HOME", "WORK", "COLLEGE")) {
                    removeAll { it.type == type.uppercase() }
                }
                add(newFav)
            }
            loc.copy(favorites = updatedList)
        }
    }

    fun deleteFavoritePlace(id: String) {
        updateLocationState { loc ->
            val updatedList = loc.favorites.filter { it.id != id }
            loc.copy(favorites = updatedList)
        }
    }

    fun updateMapOptions(mode: String, trafficEnabled: Boolean) {
        updateLocationState {
            it.copy(viewMode = mode, trafficEnabled = trafficEnabled)
        }
    }

    fun resetDistance() {
        totalDistance = 0.0
        _routePoints.value = listOf(Pair(_currentLocation.value.latitude, _currentLocation.value.longitude))
        updateLocationState { it.copy(distanceTraveled = 0.0) }
    }

    fun searchCoordinatesForQuery(query: String): Pair<Double, Double>? {
        return try {
            val geocoder = Geocoder(context, Locale.getDefault())
            val addresses = geocoder.getFromLocationName(query, 1)
            if (!addresses.isNullOrEmpty()) {
                Pair(addresses[0].latitude, addresses[0].longitude)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    fun updateCurrentLocationManually(lat: Double, lng: Double) {
        // Obsolete in real GPS but keeping for compatibility if invoked
        val updatedPoints = _routePoints.value.toMutableList().apply {
            add(Pair(lat, lng))
        }
        _routePoints.value = updatedPoints
        updateLocationState {
            it.copy(
                latitude = lat,
                longitude = lng,
                timestamp = System.currentTimeMillis()
            )
        }
    }
    
    suspend fun getCurrentLocationOnce(): Location? {
        return try {
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null).await()
        } catch (e: Exception) {
            null
        }
    }
}
"""

with open(filepath, "w") as f:
    f.write(content)
print("Updated LocationService for real GPS only")
