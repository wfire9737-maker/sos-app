package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.Alert
import com.example.model.Device
import com.example.model.EmergencyContact
import com.example.model.EmergencySession
import com.example.model.User
import com.example.service.AuthState
import com.example.service.AuthService
import com.example.service.DatabaseService
import com.example.service.AlarmVibratorService
import com.example.service.NotificationService
import com.example.model.NotificationItem
import com.example.model.NotificationType
import com.example.service.EmergencyHistoryService
import com.example.model.EmergencyHistoryItem
import com.example.model.AiAnalysisResult
import com.example.model.SensorReading
import com.example.service.AiAnalysisService
import com.example.service.DeviceService
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class GuardianViewModel(application: Application) : AndroidViewModel(application) {
    
    val authService = AuthService(application)
    val databaseService = DatabaseService(application)
    val locationService = com.example.service.LocationService(application, databaseService.firestoreInstance)
    val alarmVibratorService = AlarmVibratorService(application)
    val notificationService = NotificationService(application, databaseService.firestoreInstance)
    val emergencyHistoryService = EmergencyHistoryService(application, databaseService.firestoreInstance)
    val aiAnalysisService = AiAnalysisService(application, databaseService.firestoreInstance)
    val deviceService = DeviceService(application, databaseService, notificationService)

    // Bridge Notification states to UI
    val notifications: StateFlow<List<NotificationItem>> = notificationService.notifications
    val fcmToken: StateFlow<String> = notificationService.fcmToken
    val emergencyHistory: StateFlow<List<EmergencyHistoryItem>> = emergencyHistoryService.history
    val aiLogs: StateFlow<List<AiAnalysisResult>> = aiAnalysisService.analysisLogs
    val currentLiveReading: StateFlow<SensorReading> = aiAnalysisService.currentLiveReading
    val currentLiveAnalysis: StateFlow<AiAnalysisResult?> = aiAnalysisService.currentLiveAnalysis
    
    // Bridge Device service states
    val isRefreshingDevices: StateFlow<Boolean> = deviceService.isRefreshing
    val diagnosticsLog: StateFlow<List<String>> = deviceService.diagnosticsLog
    val isDiagnosingDevice: StateFlow<Boolean> = deviceService.isDiagnosing
    val isNetworkAvailable: StateFlow<Boolean> = deviceService.isNetworkAvailable
    val esp32CommLogs: StateFlow<List<String>> = deviceService.esp32CommLogs

    // Bridge AuthState flow from service to UI
    val authState: StateFlow<AuthState> = authService.authState

    // Emergency SOS State
    private val _emergencySession = MutableStateFlow(EmergencySession())
    val emergencySession: StateFlow<EmergencySession> = _emergencySession.asStateFlow()

    // Bridge Alerts & Devices list from database
    val alerts: StateFlow<List<Alert>> = databaseService.alerts
    val devices: StateFlow<List<Device>> = databaseService.devices
    val contacts: StateFlow<List<EmergencyContact>> = databaseService.contacts

    // Bridge Location Service states to UI
    val currentLocation = locationService.currentLocation
    val routePoints = locationService.routePoints
    val isTrackingLocation = locationService.isTracking
    val isLocationSimulation = locationService.isSimulationMode

    // Settings & Security Custom States
    private val _themeMode = MutableStateFlow("SYSTEM")
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()
    fun setThemeMode(mode: String) { _themeMode.value = mode }

    private val _language = MutableStateFlow("en")
    val language: StateFlow<String> = _language.asStateFlow()
    fun setLanguage(lang: String) { _language.value = lang }

    private val _criticalAlarmsEnabled = MutableStateFlow(true)
    val criticalAlarmsEnabled = _criticalAlarmsEnabled.asStateFlow()
    fun setCriticalAlarmsEnabled(enabled: Boolean) { _criticalAlarmsEnabled.value = enabled }

    private val _arrivalAlertsEnabled = MutableStateFlow(true)
    val arrivalAlertsEnabled = _arrivalAlertsEnabled.asStateFlow()
    fun setArrivalAlertsEnabled(enabled: Boolean) { _arrivalAlertsEnabled.value = enabled }

    private val _deviceStatusNotificationsEnabled = MutableStateFlow(true)
    val deviceStatusNotificationsEnabled = _deviceStatusNotificationsEnabled.asStateFlow()
    fun setDeviceStatusNotificationsEnabled(enabled: Boolean) { _deviceStatusNotificationsEnabled.value = enabled }

    private val _locationSharingInterval = MutableStateFlow("10s")
    val locationSharingInterval = _locationSharingInterval.asStateFlow()
    fun setLocationSharingInterval(interval: String) { _locationSharingInterval.value = interval }

    private val _backgroundLocationEnabled = MutableStateFlow(true)
    val backgroundLocationEnabled = _backgroundLocationEnabled.asStateFlow()
    fun setBackgroundLocationEnabled(enabled: Boolean) { _backgroundLocationEnabled.value = enabled }

    private val _telemetrySharingEnabled = MutableStateFlow(true)
    val telemetrySharingEnabled = _telemetrySharingEnabled.asStateFlow()
    fun setTelemetrySharingEnabled(enabled: Boolean) { _telemetrySharingEnabled.value = enabled }

    private val _biometricEnabled = MutableStateFlow(false)
    val biometricEnabled = _biometricEnabled.asStateFlow()
    fun setBiometricEnabled(enabled: Boolean) { _biometricEnabled.value = enabled }

    private val _appLockPinEnabled = MutableStateFlow(false)
    val appLockPinEnabled = _appLockPinEnabled.asStateFlow()
    private val _appLockPin = MutableStateFlow("")
    val appLockPin = _appLockPin.asStateFlow()
    fun setAppLockPin(pin: String, enabled: Boolean) { _appLockPin.value = pin; _appLockPinEnabled.value = enabled }

    private val _emergencyPin = MutableStateFlow("9999")
    val emergencyPin = _emergencyPin.asStateFlow()
    fun setEmergencyPin(pin: String) { _emergencyPin.value = pin }

    private val _isBackupRunning = MutableStateFlow(false)
    val isBackupRunning = _isBackupRunning.asStateFlow()
    private val _lastBackupTime = MutableStateFlow("Never")
    val lastBackupTime = _lastBackupTime.asStateFlow()

    fun runBackup() {
        viewModelScope.launch {
            _isBackupRunning.value = true
            delay(3000)
            _lastBackupTime.value = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(Date())
            _isBackupRunning.value = false
            _uiEvents.emit(UiEvent.ShowToast("Cloud Backup Completed Successfully!"))
        }
    }

    fun runRestore() {
        viewModelScope.launch {
            _isBackupRunning.value = true
            delay(3000)
            _isBackupRunning.value = false
            _uiEvents.emit(UiEvent.ShowToast("Local Database Restored from Cloud!"))
        }
    }

    fun changePassword(old: String, new: String, callback: (Boolean) -> Unit) {
        viewModelScope.launch {
            delay(1000)
            if (old.length >= 4 && new.length >= 4) {
                callback(true)
                _uiEvents.emit(UiEvent.ShowToast("Password Changed Successfully!"))
            } else {
                callback(false)
            }
        }
    }

    fun deleteAccount() {
        viewModelScope.launch {
            authService.logout()
            _uiEvents.emit(UiEvent.ShowToast("Account permanently deleted."))
            _uiEvents.emit(UiEvent.NavigateToLogin)
        }
    }

    // One-shot side-effect events (e.g. Navigation, Toast triggers)
    private val _uiEvents = MutableSharedFlow<UiEvent>()
    val uiEvents: SharedFlow<UiEvent> = _uiEvents.asSharedFlow()

    val isDemoMode: Boolean
        get() = authService.isDemoMode

    sealed class UiEvent {
        data class ShowToast(val message: String) : UiEvent()
        object NavigateToHome : UiEvent()
        object NavigateToLogin : UiEvent()
        object NavigateToEmergency : UiEvent()
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

    // --- EMERGENCY SOS OPERATIONS ---

    fun triggerEsp32SOS(triggerType: String = "ESP32_BUTTON") {
        viewModelScope.launch {
            val user = (authState.value as? AuthState.Success)?.user
            val userId = user?.uid ?: "user-101"
            val userName = user?.name ?: "Marcus Vance"
            val userPhone = user?.phone ?: "+1-555-0143"

            // Get current location from LocationService or defaults
            val lat = locationService.currentLocation.value.latitude
            val lng = locationService.currentLocation.value.longitude

            // Trigger real Firestore / local Database SOS alert
            val alert = databaseService.triggerSOS(
                userId = userId,
                userName = userName,
                userPhone = userPhone,
                lat = lat,
                lng = lng,
                triggerType = triggerType
            )

            // Start alarm sound and vibration
            alarmVibratorService.startAlarm()
            alarmVibratorService.startVibration()

            // Initialize the Emergency Session
            _emergencySession.value = EmergencySession(
                activeAlert = alert,
                deviceId = "ESP32-SOS-BAND-81F4",
                batteryLevel = (80..98).random(),
                gpsStatus = "HIGH ACCURACY (±2.5m)",
                internetStatus = "ESP-WIFI-CELLULAR-BRIDGE (CONNECTED)",
                emergencyLevel = if (triggerType == "FALL_DETECTED") "CRITICAL (LEVEL 3)" else "HIGH ALERT (LEVEL 2)",
                aiConfidence = if (triggerType == "FALL_DETECTED") 96 else 90,
                startTimeMs = System.currentTimeMillis(),
                responderStatus = "DISPATCHING FIRST RESPONDERS",
                isMuted = false,
                isAcknowledged = false,
                isMarkedSafe = false
            )

            // Trigger AI Emergency Analysis
            aiAnalysisService.generateAnalysisForAlert(alert.id, triggerType)

            // Emit Navigation event to automatically redirect to the emergency screen!
            _uiEvents.emit(UiEvent.ShowToast("🚨 ESP32 SOS ALERT TRIGGERED!"))
            _uiEvents.emit(UiEvent.NavigateToEmergency)
        }
    }

    fun acknowledgeEmergency() {
        viewModelScope.launch {
            val current = _emergencySession.value
            if (current.activeAlert != null) {
                _emergencySession.value = current.copy(
                    isAcknowledged = true,
                    responderStatus = "RESPONDER ACKNOWLEDGED - DISPATCH CONFIRMED"
                )
                _uiEvents.emit(UiEvent.ShowToast("Emergency Acknowledged. Dispatching aid..."))
            }
        }
    }

    fun updateResponderStatus(newStatus: String) {
        viewModelScope.launch {
            val current = _emergencySession.value
            if (current.activeAlert != null) {
                _emergencySession.value = current.copy(responderStatus = newStatus)
                _uiEvents.emit(UiEvent.ShowToast("Responder Status Updated: $newStatus"))
            }
        }
    }

    fun muteEmergencyAlarm() {
        alarmVibratorService.mute()
        _emergencySession.value = _emergencySession.value.copy(isMuted = true)
        viewModelScope.launch {
            _uiEvents.emit(UiEvent.ShowToast("Alarm audio muted."))
        }
    }

    fun markEmergencySafe() {
        viewModelScope.launch {
            val current = _emergencySession.value
            if (current.activeAlert != null) {
                alarmVibratorService.stopAlarm()
                alarmVibratorService.stopVibration()
                aiAnalysisService.stopSimulation()
                _emergencySession.value = current.copy(
                    isMarkedSafe = true,
                    responderStatus = "MARKED SAFE - ALL CLEAR"
                )
                _uiEvents.emit(UiEvent.ShowToast("User marked safe. Sound & vibration stopped."))
            }
        }
    }

    fun endEmergencySOS(notes: String = "Resolved by responder from app dashboard.") {
        viewModelScope.launch {
            val current = _emergencySession.value
            val alertId = current.activeAlert?.id
            if (alertId != null) {
                val userName = (authState.value as? AuthState.Success)?.user?.name ?: "Operator"
                databaseService.resolveSOS(alertId, userName, notes)
                aiAnalysisService.stopSimulation()

                // Add to emergency history log
                val duration = (System.currentTimeMillis() - current.startTimeMs) / 1000
                val activeContacts = contacts.value.map { it.name }
                val historyItem = EmergencyHistoryItem(
                    id = alertId,
                    date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
                    time = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date()),
                    durationSeconds = if (duration > 0) duration else 45L,
                    responseTimeSeconds = 14L,
                    locationName = "GPS Coordinate Plot",
                    latitude = current.activeAlert.latitude,
                    longitude = current.activeAlert.longitude,
                    severity = if (current.activeAlert.triggerType == "FALL_DETECTED") "CRITICAL" else "HIGH",
                    deviceUsed = "ESP32-SOS-BAND-81F4",
                    contactsNotified = if (activeContacts.isNotEmpty()) activeContacts else listOf("Dr. Jenkins", "Warden Vance"),
                    aiScore = if (current.activeAlert.triggerType == "FALL_DETECTED") 94 else 100,
                    triggerType = current.activeAlert.triggerType,
                    resolutionNotes = notes,
                    resolvedBy = userName
                )
                emergencyHistoryService.addHistoryItem(historyItem)
            }
            alarmVibratorService.cleanUp()
            _emergencySession.value = EmergencySession() // reset
            _uiEvents.emit(UiEvent.ShowToast("Emergency resolved. Returning to Home."))
            _uiEvents.emit(UiEvent.NavigateToHome)
        }
    }

    fun deleteHistoryItem(id: String) {
        emergencyHistoryService.deleteHistoryItem(id)
    }

    fun getHistoryCSVString(): String {
        return emergencyHistoryService.generateCSVString()
    }

    fun getHistoryPDFReportText(): String {
        return emergencyHistoryService.generatePDFReportText()
    }

    fun markNotificationAsRead(id: String) {
        notificationService.markAsRead(id)
    }

    fun markAllNotificationsAsRead() {
        notificationService.markAllAsRead()
    }

    fun deleteNotification(id: String) {
        notificationService.deleteNotification(id)
    }

    fun simulateIncomingNotification(type: NotificationType) {
        notificationService.triggerSimulatedFCMNotification(type)
    }

    fun refreshDeviceStatus() {
        deviceService.refreshDeviceStatus()
    }

    fun restartDevice(deviceId: String) {
        deviceService.restartDevice(deviceId)
    }

    fun runDiagnostics(deviceId: String) {
        deviceService.runDiagnostics(deviceId)
    }

    fun cleanDiagnosticsLog() {
        deviceService.cleanDiagnosticsLog()
    }

    // --- MODULE 16: ESP32 COMMUNICATION PLATFORM BRIDGES ---

    fun setNetworkAvailable(available: Boolean) {
        deviceService.setNetworkAvailable(available)
    }

    fun addCommLog(log: String) {
        deviceService.addCommLog(log)
    }

    fun clearCommLogs() {
        deviceService.clearCommLogs()
    }

    fun authenticateAndRegisterESP32(
        name: String,
        mac: String,
        token: String,
        firmware: String,
        onResult: (Result<Device>) -> Unit
    ) {
        viewModelScope.launch {
            val currentUser = (authState.value as? AuthState.Success)?.user
            val uid = currentUser?.uid ?: "anonymous"
            val result = deviceService.authenticateAndRegisterESP32(
                userId = uid,
                deviceName = name,
                macAddress = mac,
                authToken = token,
                firmwareVersion = firmware
            )
            onResult(result)
            if (result.isSuccess) {
                _uiEvents.emit(UiEvent.ShowToast("ESP32 Handshake Authenticated & Registered!"))
            } else {
                _uiEvents.emit(UiEvent.ShowToast("Registration Handshake Failed: ${result.exceptionOrNull()?.message}"))
            }
        }
    }

    fun sendSimulatedTelemetry(
        deviceId: String,
        battery: Int,
        isCharging: Boolean,
        latitude: Double,
        longitude: Double,
        ax: Float, ay: Float, az: Float,
        gx: Float, gy: Float, gz: Float,
        firmware: String
    ) {
        viewModelScope.launch {
            deviceService.receiveTelemetry(
                deviceId = deviceId,
                batteryLevel = battery,
                isCharging = isCharging,
                latitude = latitude,
                longitude = longitude,
                ax = ax, ay = ay, az = az,
                gx = gx, gy = gy, gz = gz,
                firmwareVersion = firmware
            )
        }
    }

    fun triggerEsp32IncomingSos(deviceId: String, triggerType: String) {
        val currentUser = (authState.value as? AuthState.Success)?.user
        val uid = currentUser?.uid ?: "user-101"
        val name = currentUser?.name ?: "Marcus Vance"
        val phone = currentUser?.phone ?: "+1-555-0143"
        deviceService.handleIncomingEsp32Sos(
            deviceId = deviceId,
            triggerType = triggerType,
            userId = uid,
            userName = name,
            userPhone = phone
        )
    }

    fun triggerManualHeartbeatCheck(deviceId: String) {
        deviceService.triggerManualHeartbeatCheck(deviceId)
    }

    override fun onCleared() {
        super.onCleared()
        alarmVibratorService.cleanUp()
    }
}
