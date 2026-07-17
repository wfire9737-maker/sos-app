package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.Alert
import com.example.model.Device
import com.example.model.EmergencyContact
import com.example.model.User
import com.example.service.AuthState
import com.example.service.AuthService
import com.example.service.DatabaseService
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class GuardianViewModel(application: Application) : AndroidViewModel(application) {
    
    val authService = AuthService(application)
    val databaseService = DatabaseService(application)
    val locationService = com.example.service.LocationService(application, databaseService.firestoreInstance)

    // Bridge AuthState flow from service to UI
    val authState: StateFlow<AuthState> = authService.authState

    // Bridge Alerts & Devices list from database
    val alerts: StateFlow<List<Alert>> = databaseService.alerts
    val devices: StateFlow<List<Device>> = databaseService.devices
    val contacts: StateFlow<List<EmergencyContact>> = databaseService.contacts

    // Bridge Location Service states to UI
    val currentLocation = locationService.currentLocation
    val routePoints = locationService.routePoints
    val isTrackingLocation = locationService.isTracking
    val isLocationSimulation = locationService.isSimulationMode

    // One-shot side-effect events (e.g. Navigation, Toast triggers)
    private val _uiEvents = MutableSharedFlow<UiEvent>()
    val uiEvents: SharedFlow<UiEvent> = _uiEvents.asSharedFlow()

    val isDemoMode: Boolean
        get() = authService.isDemoMode

    sealed class UiEvent {
        data class ShowToast(val message: String) : UiEvent()
        object NavigateToHome : UiEvent()
        object NavigateToLogin : UiEvent()
    }

    fun loginUser(email: String, pass: String) {
        viewModelScope.launch {
            authService.login(email, pass)
            val currentState = authService.authState.value
            if (currentState is AuthState.Success) {
                _uiEvents.emit(UiEvent.ShowToast("Welcome back, ${currentState.user.name}!"))
                _uiEvents.emit(UiEvent.NavigateToHome)
            } else if (currentState is AuthState.Error) {
                _uiEvents.emit(UiEvent.ShowToast(currentState.message))
            }
        }
    }

    fun registerUser(name: String, email: String, phone: String, medical: String, contactName: String, contactPhone: String, pass: String) {
        viewModelScope.launch {
            val newUser = User(
                name = name,
                email = email,
                phone = phone,
                medicalInfo = medical,
                emergencyContactName = contactName,
                emergencyContactPhone = contactPhone,
                role = "User"
            )
            authService.register(newUser, pass)
            val currentState = authService.authState.value
            if (currentState is AuthState.Success) {
                _uiEvents.emit(UiEvent.ShowToast("Account created successfully!"))
                _uiEvents.emit(UiEvent.NavigateToHome)
            } else if (currentState is AuthState.Error) {
                _uiEvents.emit(UiEvent.ShowToast(currentState.message))
            }
        }
    }

    fun resetPassword(email: String) {
        viewModelScope.launch {
            val success = authService.resetPassword(email)
            if (success) {
                _uiEvents.emit(UiEvent.ShowToast("Password reset link dispatched to $email!"))
                _uiEvents.emit(UiEvent.NavigateToLogin)
            } else {
                val currentState = authService.authState.value
                if (currentState is AuthState.Error) {
                    _uiEvents.emit(UiEvent.ShowToast(currentState.message))
                } else {
                    _uiEvents.emit(UiEvent.ShowToast("Failed to reset password."))
                }
            }
        }
    }

    fun logout() {
        authService.logout()
        viewModelScope.launch {
            _uiEvents.emit(UiEvent.ShowToast("Logged out successfully."))
            _uiEvents.emit(UiEvent.NavigateToLogin)
        }
    }

    fun updateUserProfile(updatedUser: User) {
        viewModelScope.launch {
            authService.updateProfile(updatedUser)
            val currentState = authService.authState.value
            if (currentState is AuthState.Success) {
                _uiEvents.emit(UiEvent.ShowToast("Profile successfully updated!"))
            } else if (currentState is AuthState.Error) {
                _uiEvents.emit(UiEvent.ShowToast(currentState.message))
            }
        }
    }

    // --- SOS TRIGGERS ---

    fun triggerManualSOS(lat: Double = 37.7749, lng: Double = -122.4194) {
        viewModelScope.launch {
            val currentUser = (authState.value as? AuthState.Success)?.user
            val uid = currentUser?.uid ?: "anonymous"
            val name = currentUser?.name ?: "Unknown User"
            val phone = currentUser?.phone ?: "No Registered Phone"

            val alert = databaseService.triggerSOS(
                userId = uid,
                userName = name,
                userPhone = phone,
                lat = lat,
                lng = lng,
                triggerType = "MANUAL"
            )
            _uiEvents.emit(UiEvent.ShowToast("ALERT TRANSMITTED: Manual SOS Triggered!"))
        }
    }

    fun triggerESP32SimulatedSOS(triggerType: String) {
        viewModelScope.launch {
            val currentUser = (authState.value as? AuthState.Success)?.user
            val uid = currentUser?.uid ?: "anonymous"
            val name = currentUser?.name ?: "Unknown User"
            val phone = currentUser?.phone ?: "No Registered Phone"

            // Simulate slight variation in coordinates
            val lat = 37.7749 + (Math.random() - 0.5) * 0.01
            val lng = -122.4194 + (Math.random() - 0.5) * 0.01

            databaseService.triggerSOS(
                userId = uid,
                userName = name,
                userPhone = phone,
                lat = lat,
                lng = lng,
                triggerType = triggerType
            )
            _uiEvents.emit(UiEvent.ShowToast("ALERT RECEIVED: ESP32 Wearable $triggerType detected!"))
        }
    }

    fun resolveAlert(alertId: String, notes: String) {
        viewModelScope.launch {
            val currentUser = (authState.value as? AuthState.Success)?.user
            val resolverName = currentUser?.name ?: "Responder HQ"
            databaseService.resolveSOS(alertId, resolverName, notes)
            _uiEvents.emit(UiEvent.ShowToast("Alert successfully resolved."))
        }
    }

    // --- DEVICE BONDING ---

    fun bondDevice(
        name: String, 
        mac: String, 
        deviceId: String = "esp32-" + java.util.UUID.randomUUID().toString().take(8),
        firmware: String = "v1.2.4-esp32",
        battery: Int = 100,
        signal: Int = -67,
        health: String = "EXCELLENT"
    ) {
        viewModelScope.launch {
            val currentUser = (authState.value as? AuthState.Success)?.user
            val uid = currentUser?.uid ?: "anonymous"
            val newDevice = Device(
                deviceId = deviceId,
                userId = uid,
                deviceName = name,
                status = "CONNECTED",
                batteryLevel = battery,
                macAddress = mac,
                lastSync = System.currentTimeMillis(),
                firmwareVersion = firmware,
                signalStrength = signal,
                deviceHealth = health
            )
            databaseService.updateDevice(newDevice)
            _uiEvents.emit(UiEvent.ShowToast("ESP32 Wearable bound successfully!"))
        }
    }

    fun renameDevice(deviceId: String, newName: String) {
        viewModelScope.launch {
            try {
                databaseService.renameDevice(deviceId, newName)
                _uiEvents.emit(UiEvent.ShowToast("Device renamed successfully!"))
            } catch (e: Exception) {
                _uiEvents.emit(UiEvent.ShowToast("Failed to rename device: ${e.localizedMessage}"))
            }
        }
    }

    fun unbondDevice(deviceId: String) {
        viewModelScope.launch {
            databaseService.deleteDevice(deviceId)
            _uiEvents.emit(UiEvent.ShowToast("Wearable device disconnected."))
        }
    }

    // --- EMERGENCY CONTACT OPERATIONS ---

    fun saveEmergencyContact(contact: EmergencyContact) {
        viewModelScope.launch {
            try {
                databaseService.saveContact(contact)
                _uiEvents.emit(UiEvent.ShowToast("Emergency contact saved successfully!"))
            } catch (e: Exception) {
                _uiEvents.emit(UiEvent.ShowToast(e.localizedMessage ?: "Failed to save contact"))
            }
        }
    }

    fun deleteEmergencyContact(contactId: String) {
        viewModelScope.launch {
            try {
                databaseService.deleteContact(contactId)
                _uiEvents.emit(UiEvent.ShowToast("Emergency contact deleted."))
            } catch (e: Exception) {
                _uiEvents.emit(UiEvent.ShowToast(e.localizedMessage ?: "Failed to delete contact"))
            }
        }
    }

    // --- LOCATION TRACKING OPERATIONS ---

    fun startLocationTracking() {
        val uid = (authState.value as? AuthState.Success)?.user?.uid ?: "anonymous"
        locationService.startLocationTracking(uid)
    }

    fun stopLocationTracking() {
        locationService.stopLocationTracking()
    }

    fun toggleLocationSimulation(enabled: Boolean) {
        locationService.setSimulationMode(enabled)
    }

    fun saveFavoritePlace(name: String, lat: Double, lng: Double, type: String) {
        locationService.saveFavoritePlace(name, lat, lng, type)
    }

    fun deleteFavoritePlace(id: String) {
        locationService.deleteFavoritePlace(id)
    }

    fun updateMapOptions(mode: String, trafficEnabled: Boolean) {
        locationService.updateMapOptions(mode, trafficEnabled)
    }

    fun resetDistance() {
        locationService.resetDistance()
    }

    fun searchLocation(query: String) {
        val result = locationService.searchCoordinatesForQuery(query)
        if (result != null) {
            locationService.updateCurrentLocationManually(result.first, result.second)
            viewModelScope.launch {
                _uiEvents.emit(UiEvent.ShowToast("Moved map focus to: $query"))
            }
        } else {
            viewModelScope.launch {
                _uiEvents.emit(UiEvent.ShowToast("No locations found for: '$query'"))
            }
        }
    }
}
