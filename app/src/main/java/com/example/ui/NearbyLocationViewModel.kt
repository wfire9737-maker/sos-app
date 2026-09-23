package com.example.ui

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ble.nearby.NearbyBleManager
import com.example.ble.nearby.NearbyConnectionState
import com.example.model.NearbyLocationUpdate
import com.example.repository.NearbyLocationRepository
import com.google.android.gms.location.*
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NearbyLocationViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: NearbyLocationRepository,
    private val nearbyBleManager: NearbyBleManager
) : ViewModel() {

    val latestLocations: StateFlow<Map<String, NearbyLocationUpdate>> =
        repository.latestLocations.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyMap()
        )

    val connectedDevices = nearbyBleManager.nearbyDevices.map { map ->
        map.values.filter { it.connectionState == NearbyConnectionState.CONNECTED }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _sharingMacs = MutableStateFlow<Set<String>>(emptySet())
    val sharingMacs: StateFlow<Set<String>> = _sharingMacs.asStateFlow()

    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    private var locationCallback: LocationCallback? = null

    fun toggleSharing(macAddress: String, enable: Boolean) {
        _sharingMacs.update { current ->
            if (enable) current + macAddress else current - macAddress
        }
    }

    init {
        viewModelScope.launch {
            sharingMacs.collect { macs ->
                if (macs.isNotEmpty()) {
                    startLocationUpdates()
                } else {
                    stopLocationUpdates()
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        if (locationCallback != null) return // Already running

        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000)
            .setMinUpdateIntervalMillis(5000)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val loc = result.lastLocation ?: return
                val currentMacs = _sharingMacs.value
                for (mac in currentMacs) {
                    repository.sendLocation(mac, loc.latitude, loc.longitude, loc.accuracy)
                }
            }
        }
        
        try {
            fusedLocationClient.requestLocationUpdates(
                request,
                locationCallback!!,
                Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            // Missing permissions handled by UI gracefully
            stopLocationUpdates()
            _sharingMacs.value = emptySet()
        }
    }

    private fun stopLocationUpdates() {
        locationCallback?.let {
            fusedLocationClient.removeLocationUpdates(it)
        }
        locationCallback = null
    }

    override fun onCleared() {
        super.onCleared()
        stopLocationUpdates()
    }
}
