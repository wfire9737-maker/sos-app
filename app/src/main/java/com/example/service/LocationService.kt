package com.example.service

import android.annotation.SuppressLint
import android.content.Context
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
import kotlin.math.*

class LocationService(
    private val context: Context,
    private val firestore: FirebaseFirestore?
) {
    private val scope = CoroutineScope(Dispatchers.IO)
    private val fusedLocationClient: FusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(context)
    }

    private val cacheFile = File(context.cacheDir, "guardian_location_cache.json")

    // In-memory state of current location
    private val _currentLocation = MutableStateFlow(UserLocation())
    val currentLocation: StateFlow<UserLocation> = _currentLocation.asStateFlow()

    // Recorded polyline route points
    private val _routePoints = MutableStateFlow<List<Pair<Double, Double>>>(emptyList())
    val routePoints: StateFlow<List<Pair<Double, Double>>> = _routePoints.asStateFlow()

    // Simulation & tracking statuses
    private val _isTracking = MutableStateFlow(false)
    val isTracking: StateFlow<Boolean> = _isTracking.asStateFlow()

    private val _isSimulationMode = MutableStateFlow(true) // Default to simulation for rich preview
    val isSimulationMode: StateFlow<Boolean> = _isSimulationMode.asStateFlow()

    private var locationCallback: LocationCallback? = null
    private var simulationJob: kotlinx.coroutines.Job? = null

    // Base coordinates around a lively city center (e.g. San Francisco Golden Gate/Market St or Central Park NY)
    private var simLat = 37.7749
    private var simLng = -122.4194
    private var totalDistance = 0.0

    init {
        loadCachedLocation()
        // Generate standard default favorite places if none exist
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
                simLat = userLoc.latitude
                simLng = userLoc.longitude
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
        _isSimulationMode.value = enabled
        if (_isTracking.value) {
            // Restart tracking with new mode
            stopLocationTracking()
            startLocationTracking(_currentLocation.value.userId)
        }
    }

    @SuppressLint("MissingPermission")
    fun startLocationTracking(userId: String) {
        if (_isTracking.value) return
        _isTracking.value = true
        setUserId(userId)

        if (_isSimulationMode.value) {
            startSimulation()
        } else {
            startRealGpsTracking()
        }
    }

    fun stopLocationTracking() {
        _isTracking.value = false
        stopSimulation()
        stopRealGpsTracking()
    }

    private fun startSimulation() {
        stopSimulation()
        // Initialize simulation coordinates with current state
        simLat = _currentLocation.value.latitude
        simLng = _currentLocation.value.longitude

        // Keep current points or start clean
        if (_routePoints.value.isEmpty()) {
            _routePoints.value = listOf(Pair(simLat, simLng))
        }

        simulationJob = scope.launch {
            while (_isTracking.value) {
                kotlinx.coroutines.delay(3000) // Update every 3 seconds

                // Simulate realistic vector movement
                val speedKmh = 10.0 + (0..15).random() // 10 to 25 km/h
                val bearingChange = (-20..20).random().toFloat()
                val newBearing = (_currentLocation.value.bearing + bearingChange + 360f) % 360f

                // Convert speed (km/h) & 3 seconds interval to delta distance
                val distanceIn3s = (speedKmh / 3600.0) * 3.0 // km
                totalDistance += distanceIn3s

                // Simple coordinate projection based on bearing
                val bearingRad = Math.toRadians(newBearing.toDouble())
                // 1 degree latitude ~ 111km, 1 degree longitude ~ 111km * cos(lat)
                val latDelta = (distanceIn3s * cos(bearingRad)) / 111.0
                val lngDelta = (distanceIn3s * sin(bearingRad)) / (111.0 * cos(Math.toRadians(simLat)))

                simLat += latDelta
                simLng += lngDelta

                val simulatedAltitude = 45.0 + (0..50).random() / 10.0 // 45 to 50 meters
                val simulatedAccuracy = 3.0f + (0..40).random() / 10.0f // 3m to 7m accuracy

                val updatedPoints = _routePoints.value.toMutableList().apply {
                    add(Pair(simLat, simLng))
                    if (size > 150) removeAt(0) // Cap historical polyline route points
                }
                _routePoints.value = updatedPoints

                updateLocationState {
                    it.copy(
                        latitude = simLat,
                        longitude = simLng,
                        speed = speedKmh,
                        bearing = newBearing,
                        altitude = simulatedAltitude,
                        accuracy = simulatedAccuracy,
                        timestamp = System.currentTimeMillis(),
                        distanceTraveled = totalDistance
                    )
                }
            }
        }
    }

    private fun stopSimulation() {
        simulationJob?.cancel()
        simulationJob = null
    }

    @SuppressLint("MissingPermission")
    private fun startRealGpsTracking() {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3000)
            .setWaitForAccurateLocation(false)
            .setMinUpdateIntervalMillis(2000)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                val loc: Location = locationResult.lastLocation ?: return
                
                // Calculate distance traveled between real coordinates
                val lastLoc = _currentLocation.value
                val results = FloatArray(1)
                Location.distanceBetween(lastLoc.latitude, lastLoc.longitude, loc.latitude, loc.longitude, results)
                val segmentDistanceKm = results[0] / 1000.0
                totalDistance += segmentDistanceKm

                simLat = loc.latitude
                simLng = loc.longitude

                val updatedPoints = _routePoints.value.toMutableList().apply {
                    add(Pair(simLat, simLng))
                    if (size > 150) removeAt(0)
                }
                _routePoints.value = updatedPoints

                // Speed in km/h from m/s
                val speedKmh = loc.speed * 3.6

                updateLocationState {
                    it.copy(
                        latitude = loc.latitude,
                        longitude = loc.longitude,
                        speed = speedKmh,
                        bearing = loc.bearing,
                        altitude = loc.altitude,
                        accuracy = loc.accuracy,
                        timestamp = System.currentTimeMillis(),
                        distanceTraveled = totalDistance
                    )
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
        }
    }

    private fun stopRealGpsTracking() {
        locationCallback?.let {
            fusedLocationClient.removeLocationUpdates(it)
        }
        locationCallback = null
    }

    // --- FAVORITE PLACES OPERATIONS ---

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
                // If replacing existing matching types like HOME/WORK/COLLEGE
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
        // Mock geocoding search within San Francisco bounds for rapid responsive previews
        return when (query.lowercase().trim()) {
            "golden gate", "golden gate bridge" -> Pair(37.8199, -122.4783)
            "safeway", "safeway mart" -> Pair(37.7725, -122.4310)
            "silicon valley", "stanford" -> Pair(37.4275, -122.1697)
            "central park", "new york" -> Pair(40.7851, -73.9683)
            "downtown", "city hall" -> Pair(37.7793, -122.4193)
            else -> {
                // Return slight offset from current location as fallback search hit
                val current = _currentLocation.value
                val offsetLat = current.latitude + ((-5..5).random() / 1000.0)
                val offsetLng = current.longitude + ((-5..5).random() / 1000.0)
                Pair(offsetLat, offsetLng)
            }
        }
    }

    fun updateCurrentLocationManually(lat: Double, lng: Double) {
        simLat = lat
        simLng = lng
        val updatedPoints = _routePoints.value.toMutableList().apply {
            add(Pair(simLat, simLng))
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
}
