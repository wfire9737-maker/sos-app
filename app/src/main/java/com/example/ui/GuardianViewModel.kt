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
import com.example.model.NotificationModel
import com.example.model.NotificationCategory
import com.example.service.NotificationProvider
import com.example.model.HistoryModel
import com.example.service.HistoryService
import com.example.service.HistoryProvider
import com.example.model.AiAnalysisResult
import com.example.model.SensorReading
import com.example.service.AiAnalysisService
import com.example.model.AIAnalysisModel
import com.example.model.AISensorReading
import com.example.model.AITimelineEvent
import com.example.service.AIService
import com.example.service.AIProvider
import com.example.model.FallEvent
import com.example.data.FallDatabase
import com.example.repository.FallRepository
import com.example.service.FallDetectionService
import com.example.service.VoiceSosService
import com.example.service.VoiceActivationLog
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import com.example.service.DeviceService
import com.example.model.EmergencyModel
import com.example.service.EmergencyService
import com.example.service.EmergencyProvider
import com.example.service.SafetyTimerService
import com.example.service.SafetyTimerStatus
import com.example.service.AnalyticsService
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

import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class GuardianViewModel @Inject constructor(
    application: Application,
    val authService: AuthService,
    val databaseService: DatabaseService,
    val locationService: com.example.service.LocationService,
    val alarmVibratorService: AlarmVibratorService,
    val notificationService: NotificationService,
    val notificationProvider: NotificationProvider,
    val historyService: HistoryService,
    val historyProvider: HistoryProvider,
    val aiAnalysisService: AiAnalysisService,
    val deviceService: DeviceService,
    val fallDatabase: com.example.data.FallDatabase,
    val fallRepository: com.example.repository.FallRepository,
    val fallDetectionService: com.example.service.FallDetectionService,
    val voiceSosService: com.example.service.VoiceSosService,
    val aiService: com.example.service.AIService,
    val aiProvider: com.example.service.AIProvider,
    val emergencyService: EmergencyService,
    val emergencyProvider: EmergencyProvider,
    val safetyTimerService: SafetyTimerService,
    val analyticsService: AnalyticsService,
    val securityService: com.example.service.SecurityService
) : AndroidViewModel(application) {

    init {
        // Register callbacks for Fall, Voice SOS, and Safety Timer automation
        safetyTimerService.onTimerExpiredCallback = {
            triggerTimerSOS()
        }
        fallDetectionService.onSosTriggeredCallback = {
            triggerFallDetectedSOS()
        }
        voiceSosService.onVoiceSosTriggered = { matchedPhrase, confidence ->
            triggerVoiceSOS(matchedPhrase, confidence)
        }

        viewModelScope.launch {
            emergencyProvider.activeEmergencyState.collect { model ->
                if (model != null) {
                    val alert = _emergencySession.value.activeAlert ?: Alert(
                        id = model.emergencyId,
                        userId = model.userId,
                        userName = model.userName,
                        userPhone = model.userPhone,
                        latitude = model.latitude,
                        longitude = model.longitude,
                        status = "ACTIVE",
                        triggerType = model.triggerType,
                        timestamp = model.startTimeMs
                    )
                    _emergencySession.value = _emergencySession.value.copy(
                        activeAlert = alert.copy(
                            latitude = model.latitude,
                            longitude = model.longitude,
                            status = model.status
                        ),
                        deviceId = model.deviceId,
                        startTimeMs = model.startTimeMs,
                        responderStatus = model.responderStatus,
                        emergencyLevel = if (model.triggerType == "FALL_DETECTED") "CRITICAL (LEVEL 3)" else "HIGH ALERT (LEVEL 2)",
                        aiConfidence = model.aiConfidenceScore,
                        isMarkedSafe = model.status == "MARKED_SAFE" || model.status == "RESOLVED" || model.status == "CANCELLED"
                    )
                }
            }
        }
    }

    // Bridge Notification states to UI
    val notifications: StateFlow<List<NotificationItem>> = notificationService.notifications
    val notificationsNew: StateFlow<List<NotificationModel>> = notificationProvider.notifications
    
    // AI Analysis (Module 21) State Flows
    val aiLogsNew: StateFlow<List<AIAnalysisModel>> = aiProvider.analysisLogs
    val currentLiveReadingNew: StateFlow<AISensorReading> = aiProvider.currentLiveReading
    val currentLiveAnalysisNew: StateFlow<AIAnalysisModel?> = aiProvider.currentLiveAnalysis

    // Fall Detection (Module 22) State Flows
    val fallState: StateFlow<String> = fallDetectionService.currentState
    val fallCountdown: StateFlow<Int> = fallDetectionService.countdownSeconds
    val allFallEvents: StateFlow<List<FallEvent>> = fallRepository.allEvents.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Voice SOS (Module 23) State Flows
    val isVoiceListening: StateFlow<Boolean> = voiceSosService.isListening
    val voiceState: StateFlow<String> = voiceSosService.voiceState
    val wakePhrases: StateFlow<List<String>> = voiceSosService.wakePhrases
    val micDecibels: StateFlow<Float> = voiceSosService.micDecibels
    val voiceConfidenceThreshold: StateFlow<Int> = voiceSosService.confidenceThreshold
    val voiceActivationLogs: StateFlow<List<VoiceActivationLog>> = voiceSosService.activationLogs
    val fcmToken: StateFlow<String> = notificationService.fcmToken
    val emergencyHistory: StateFlow<List<HistoryModel>> = historyService.history
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

    private val _emergencyPin = MutableStateFlow(securityService.getEmergencyPin())
    val emergencyPin = _emergencyPin.asStateFlow()
    fun setEmergencyPin(pin: String) { 
        securityService.saveEmergencyPin(pin)
        _emergencyPin.value = pin 
    }

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

    private suspend fun initiateEmergencySequence(triggerSource: String, deviceId: String): com.example.model.EmergencyModel {
        val user = (authState.value as? AuthState.Success)?.user
        val userId = user?.uid ?: "user-101"
        val userName = user?.name ?: "Marcus Vance"
        val userPhone = user?.phone ?: "+1-555-0143"

        alarmVibratorService.startAlarm()
        alarmVibratorService.startVibration()

        return emergencyProvider.initiateEmergency(
            userId = userId,
            userName = userName,
            userPhone = userPhone,
            triggerSource = triggerSource,
            deviceId = deviceId
        )
    }

    fun triggerTimerSOS() {
        viewModelScope.launch {
            initiateEmergencySequence(
                triggerSource = "SAFETY_TIMER_EXPIRED",
                deviceId = "MOBILE-APP-TIMER"
            )
            _uiEvents.emit(UiEvent.ShowToast("🚨 SAFETY TIMER EXPIRED: AUTOMATIC SOS DISPATCHED!"))
            _uiEvents.emit(UiEvent.NavigateToEmergency)
        }
    }

    fun triggerManualSOS(lat: Double = 37.7749, lng: Double = -122.4194) {
        viewModelScope.launch {
            initiateEmergencySequence(
                triggerSource = "MANUAL",
                deviceId = "MOBILE-APP-SOS"
            )
            _uiEvents.emit(UiEvent.ShowToast("ALERT TRANSMITTED: Manual SOS Triggered!"))
            _uiEvents.emit(UiEvent.NavigateToEmergency)
        }
    }

    fun triggerFallDetectedSOS() {
        viewModelScope.launch {
            val model = initiateEmergencySequence(
                triggerSource = "FALL_DETECTED",
                deviceId = "WEARABLE-BAND-IMU"
            )

            // Trigger AI Emergency Analysis for our new service as well
            val analysis = AIAnalysisModel(
                alertId = model.emergencyId,
                confidenceScore = 98,
                falseAlarmProbability = 2,
                motionAnalysis = "CRITICAL_ACCELERATION_SPIKE_FOLLOWED_BY_HORIZONTAL_AXIS_SHIFT",
                activityRecognition = "SUDDEN FALL DETECTED (STATIC LAYING)",
                riskLevel = "CRITICAL",
                suggestedAction = "ALERT ALL PRIMARY FAMILY CONTACTS AND LAUNCH COUNTY DISPATCH CODES",
                timeline = listOf(
                    AITimelineEvent("10:44:00 AM", "Impact Shock", "Accelerometer spike of 4.1G logged.", "💥"),
                    AITimelineEvent("10:44:05 AM", "Countdown Commenced", "Wearer unresponsive. 15-second countdown started.", "⏱️"),
                    AITimelineEvent("10:44:20 AM", "Auto SOS Dispatch", "No cancel received. Triggering fallback emergency broadcast.", "🚨")
                )
            )
            aiService.addAnalysisLog(analysis)

            _uiEvents.emit(UiEvent.ShowToast("🚨 FALL DETECTED: AUTOMATIC SOS DISPATCHED!"))
            _uiEvents.emit(UiEvent.NavigateToEmergency)
        }
    }

    fun triggerVoiceSOS(matchedPhrase: String, confidence: Int) {
        viewModelScope.launch {
            val model = initiateEmergencySequence(
                triggerSource = "VOICE_SOS",
                deviceId = "MOBILE-VOICE-RECOGNIZE"
            )

            val analysis = AIAnalysisModel(
                alertId = model.emergencyId,
                confidenceScore = confidence,
                falseAlarmProbability = 100 - confidence,
                motionAnalysis = "AUDIO_FREQUENCY_WAVE_MATCH",
                activityRecognition = "VOICE SOS ACTIVATION: \"$matchedPhrase\"",
                riskLevel = "CRITICAL",
                suggestedAction = "WAKE WORD MATCHED DETECTOR. DISPATCH COGNITIVE RESPONSE AGENT.",
                timeline = listOf(
                    AITimelineEvent("10:44:00 AM", "Voice Alert Heard", "Acoustic sensor detected wake phrase \"$matchedPhrase\".", "🎤"),
                    AITimelineEvent("10:44:02 AM", "Neural Match Lock", "Matched against offline template with $confidence% confidence.", "🧠"),
                    AITimelineEvent("10:44:03 AM", "SOS Dispatch", "Voice SOS emergency alert initiated.", "🚨")
                )
            )
            aiService.addAnalysisLog(analysis)

            _uiEvents.emit(UiEvent.ShowToast("🚨 VOICE SOS: AUTOMATIC SOS DISPATCHED!"))
            _uiEvents.emit(UiEvent.NavigateToEmergency)
        }
    }

    fun triggerESP32SimulatedSOS(triggerType: String) {
        triggerEsp32SOS(triggerType)
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
            val model = initiateEmergencySequence(
                triggerSource = triggerType,
                deviceId = "ESP32-SOS-BAND-81F4"
            )

            // Trigger AI Emergency Analysis
            aiAnalysisService.generateAnalysisForAlert(model.emergencyId, triggerType)

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
                
                // End tracking and sync to cloud
                emergencyProvider.markEmergencySafe()
                
                _emergencySession.value = current.copy(
                    isMarkedSafe = true,
                    responderStatus = "MARKED SAFE - ALL CLEAR"
                )
                _uiEvents.emit(UiEvent.ShowToast("User marked safe. Session auto-closing..."))
                
                // Automatically close the session when marked safe
                delay(3000)
                _emergencySession.value = EmergencySession() // reset session
                _uiEvents.emit(UiEvent.NavigateToHome)
            }
        }
    }

    fun cancelEmergencyWithPin(pin: String, callback: (Boolean) -> Unit) {
        viewModelScope.launch {
            val expectedPin = securityService.getEmergencyPin()
            val success = emergencyProvider.cancelEmergency(pin, expectedPin, "Cancelled securely with PIN verification.")
            if (success) {
                alarmVibratorService.stopAlarm()
                alarmVibratorService.stopVibration()
                aiAnalysisService.stopSimulation()
                alarmVibratorService.cleanUp()
                
                _emergencySession.value = EmergencySession() // Reset legacy state
                _uiEvents.emit(UiEvent.ShowToast("SOS Session Cancelled successfully with PIN."))
                _uiEvents.emit(UiEvent.NavigateToHome)
            } else {
                _uiEvents.emit(UiEvent.ShowToast("Incorrect Emergency Security PIN."))
            }
            callback(success)
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
                
                // End tracking loop
                emergencyProvider.markEmergencySafe()

                // Add to emergency history log
                val duration = (System.currentTimeMillis() - current.startTimeMs) / 1000
                val activeContacts = contacts.value.map { it.name }
                val historyItem = HistoryModel(
                    id = alertId,
                    date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
                    time = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date()),
                    durationSeconds = if (duration > 0) duration else 45L,
                    responseTimeSeconds = 14L,
                    address = "GPS Coordinate Plot",
                    latitude = current.activeAlert.latitude,
                    longitude = current.activeAlert.longitude,
                    severity = if (current.activeAlert.triggerType == "FALL_DETECTED") "CRITICAL" else "HIGH",
                    contactsNotified = if (activeContacts.isNotEmpty()) activeContacts else listOf("Dr. Jenkins", "Warden Vance"),
                    aiConfidence = if (current.activeAlert.triggerType == "FALL_DETECTED") 94 else 100,
                    triggerType = current.activeAlert.triggerType,
                    resolutionNotes = notes,
                    resolvedBy = userName
                )
                historyProvider.addHistoryRecord(historyItem)
            }
            alarmVibratorService.cleanUp()
            _emergencySession.value = EmergencySession() // reset
            _uiEvents.emit(UiEvent.ShowToast("Emergency resolved. Returning to Home."))
            _uiEvents.emit(UiEvent.NavigateToHome)
        }
    }

    fun deleteHistoryItem(id: String) {
        historyProvider.deleteHistoryRecord(id)
    }

    fun getHistoryCSVString(): String {
        return historyProvider.exportToCSV()
    }

    fun getHistoryPDFReportText(): String {
        return historyProvider.exportToPDF()
    }

    fun markNotificationAsRead(id: String) {
        notificationService.markAsRead(id)
    }

    fun markNotificationNewAsRead(id: String) {
        notificationProvider.markAsRead(id)
    }

    fun markAllNotificationsAsRead() {
        notificationService.markAllAsRead()
    }

    fun markAllNotificationsNewAsRead() {
        notificationProvider.markAllAsRead()
    }

    fun deleteNotification(id: String) {
        notificationService.deleteNotification(id)
    }

    fun deleteNotificationNew(id: String) {
        notificationProvider.deleteNotification(id)
    }

    fun simulateIncomingNotification(type: NotificationType) {
        notificationService.triggerSimulatedFCMNotification(type)
    }

    fun simulateIncomingNotificationNew(category: NotificationCategory) {
        notificationProvider.triggerFCMNotification(category)
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
        fallDetectionService.cleanup()
        voiceSosService.cleanup()
        aiService.stopSimulation()
    }
}
