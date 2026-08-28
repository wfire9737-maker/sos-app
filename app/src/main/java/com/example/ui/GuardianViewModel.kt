package com.example.ui
import android.content.Context

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import com.example.service.*
import com.example.model.*
import com.example.data.*
import com.example.repository.*
import com.example.ble.*
import android.location.Location

@HiltViewModel
class GuardianViewModel @Inject constructor(
    application: Application,
    private val authService: AuthService,
    private val databaseService: DatabaseService,
    private val locationService: LocationService,
    private val alarmVibratorService: AlarmVibratorService,
    private val notificationService: NotificationService,
    private val notificationProvider: NotificationProvider,
    private val historyService: HistoryService,
    private val historyProvider: HistoryProvider,
    private val aiAnalysisService: AiAnalysisService,
    val deviceService: DeviceService,
    private val fallDatabase: FallDatabase,
    private val fallRepository: FallRepository,
    val fallDetectionService: FallDetectionService,
    val voiceSosService: VoiceSosService,
    private val aiService: AIService,
    private val aiProvider: AIProvider,
    private val emergencyService: EmergencyService,
    private val emergencyProvider: EmergencyProvider,
    val safetyTimerService: SafetyTimerService,
    private val analyticsService: AnalyticsService,
    private val securityService: SecurityService,
    private val trustedPlacesService: TrustedPlacesService,
    private val settingsDataStore: SettingsDataStore
) : AndroidViewModel(application) {
    
    private val _uiEvents = MutableSharedFlow<UiEvent>()
    val uiEvents = _uiEvents.asSharedFlow()




    private val _voiceSosEnabled = MutableStateFlow(
        getApplication<android.app.Application>().getSharedPreferences("smart_sos_settings", android.content.Context.MODE_PRIVATE)
            .getBoolean("voice_sos_enabled", false)
    )
    val voiceSosEnabled = _voiceSosEnabled.asStateFlow()
    private val _voiceSosPhrase = MutableStateFlow("Emergency SOS")
    val voiceSosPhrase = _voiceSosPhrase.asStateFlow()

    init {


        // Start service if enabled on boot/init
        if (_voiceSosEnabled.value) {
            val intent = android.content.Intent(getApplication(), com.example.service.VoiceSosForegroundService::class.java)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                getApplication<android.app.Application>().startForegroundService(intent)
            } else {
                getApplication<android.app.Application>().startService(intent)
            }
        }
    }

    private val _isSirenPlaying = MutableStateFlow(false)
    val isSirenPlaying = _isSirenPlaying.asStateFlow()
    val countdown = emergencyService.countdown
    val notifications = notificationService.notifications
    val notificationsNew = notificationProvider.notifications
    val aiLogsNew = aiProvider.analysisLogs
    val currentLiveReadingNew = aiProvider.currentLiveReading
    val currentLiveAnalysisNew = aiProvider.currentLiveAnalysis
    val mpuReading = deviceService.bleManager.latestMpuReading
    val mpuHardwareState = deviceService.bleManager.mpuHardwareState
    val mpuMotionState = deviceService.bleManager.motionState
    val mpuRawString = deviceService.bleManager.mpuRawString
    val mpuRecentReadings = deviceService.bleManager.mpuRecentReadings
    val mpuCharacteristicFound = deviceService.bleManager.mpuCharacteristicFound
    val mpuNotificationSubscribed = deviceService.bleManager.mpuNotificationSubscribed
    val bleBatteryDisplay = deviceService.bleManager.batteryDisplay
    val bleBatteryLevel = deviceService.bleManager.batteryLevel
    val bleRssi = deviceService.bleManager.rssi

    val fallState = fallDetectionService.currentState
    val fallCountdown = fallDetectionService.countdownSeconds

    val isVoiceListening = voiceSosService.isListening
    val voiceState = voiceSosService.voiceState
    val wakePhrases = voiceSosService.wakePhrases
    val micDecibels = voiceSosService.micDecibels
    val voiceConfidenceThreshold = voiceSosService.confidenceThreshold
    val voiceActivationLogs = voiceSosService.activationLogs
    val isSpeechRecognizerActive = voiceSosService.isSpeechRecognizerActive
    val liveSpokenText = voiceSosService.liveSpokenText
    val speechStatusMessage = voiceSosService.speechStatusMessage
    val lastRecognizedCommand = voiceSosService.lastRecognizedCommand

    private val _voiceCommandConfirmation = MutableStateFlow<String?>(null)
    val voiceCommandConfirmation = _voiceCommandConfirmation.asStateFlow()
    val fcmToken = notificationService.fcmToken
    val emergencyHistory = historyService.history
    val aiLogs = aiAnalysisService.analysisLogs
    val currentLiveReading = aiAnalysisService.currentLiveReading
    val currentLiveAnalysis = aiAnalysisService.currentLiveAnalysis
    val isRefreshingDevices = deviceService.isRefreshing
    val isEsp32Connected = deviceService.isEsp32Connected
    val bleConnectionState = deviceService.bleManager.connectionState
    val activeEmergency = emergencyService.activeEmergency
    val diagnosticsLog = deviceService.diagnosticsLog
    val isDiagnosingDevice = deviceService.isDiagnosing
    val isNetworkAvailable = deviceService.isNetworkAvailable
    val trustedPlaces = trustedPlacesService.trustedPlaces
    val esp32CommLogs = deviceService.esp32CommLogs
    val authState = authService.authState

    private val _sosWorkflowState = MutableStateFlow(SosWorkflowState.IDLE)
    val sosWorkflowState = _sosWorkflowState.asStateFlow()

    private val _emergencySession = MutableStateFlow<EmergencySession?>(null)
    val emergencySession = _emergencySession.asStateFlow()
    val alerts = databaseService.alerts
    val devices = databaseService.devices
    val contacts = databaseService.contacts
    val currentLocation = locationService.currentLocation
    val routePoints = locationService.routePoints
    val isTrackingLocation = locationService.isTracking
    private val _themeMode = MutableStateFlow(getApplication<android.app.Application>().getSharedPreferences("smart_sos_settings", android.content.Context.MODE_PRIVATE).getString("theme_mode", "SYSTEM") ?: "SYSTEM")
    val themeMode = _themeMode.asStateFlow()
    private val _language = MutableStateFlow("en")
    val language = _language.asStateFlow()
    val criticalAlarmsEnabled = settingsDataStore.criticalAlarmsEnabledFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    private val _arrivalAlertsEnabled = MutableStateFlow(true)
    val arrivalAlertsEnabled = _arrivalAlertsEnabled.asStateFlow()

    private val _deviceStatusNotificationsEnabled = MutableStateFlow(true)
    val deviceStatusNotificationsEnabled = _deviceStatusNotificationsEnabled.asStateFlow()

    private val _locationSharingInterval = MutableStateFlow("10s")
    val locationSharingInterval = _locationSharingInterval.asStateFlow()

    private val _backgroundLocationEnabled = MutableStateFlow(true)
    val backgroundLocationEnabled = _backgroundLocationEnabled.asStateFlow()

    private val _telemetrySharingEnabled = MutableStateFlow(true)
    val telemetrySharingEnabled = _telemetrySharingEnabled.asStateFlow()

    private val _biometricEnabled = MutableStateFlow(false)
    val biometricEnabled = _biometricEnabled.asStateFlow()

    private val _appLockPinEnabled = MutableStateFlow(false)
    val appLockPinEnabled = _appLockPinEnabled.asStateFlow()

    private val _appLockPin = MutableStateFlow("")
    val appLockPin = _appLockPin.asStateFlow()

    private val _emergencyPin = MutableStateFlow("")
    val emergencyPin = _emergencyPin.asStateFlow()

    private val _isBackupRunning = MutableStateFlow(false)
    val isBackupRunning = _isBackupRunning.asStateFlow()

    private val _lastBackupTime = MutableStateFlow(0L)
    val lastBackupTime = _lastBackupTime.asStateFlow()

    val isGpsDisabled = locationService.isGpsDisabled
    val isWeakGps = locationService.isWeakGps
    val isOfflineMode = databaseService.isOfflineMode
    val isSlowNetwork = databaseService.isSlowNetwork
    val developerLogs = databaseService.developerLogs

    fun setDeveloperModeEnabled(enabled: Boolean) { }
    fun setSosSoundEnabled(enabled: Boolean) { viewModelScope.launch { settingsDataStore.setSosSoundEnabled(enabled) } }
    fun setSosVibrationEnabled(enabled: Boolean) { viewModelScope.launch { settingsDataStore.setSosVibrationEnabled(enabled) } }
    fun setThemeMode(mode: String) {
        _themeMode.value = mode
        val prefs = getApplication<android.app.Application>().getSharedPreferences("smart_sos_settings", android.content.Context.MODE_PRIVATE)
        prefs.edit().putString("theme_mode", mode).apply()
    }
    fun setLanguage(lang: String) { }
    fun toggleSirenAlarm() { }
    fun setVoiceSosEnabled(enabled: Boolean) {
        _voiceSosEnabled.value = enabled
        val smartSosPrefs = getApplication<android.app.Application>().getSharedPreferences("smart_sos_settings", android.content.Context.MODE_PRIVATE)
        smartSosPrefs.edit().putBoolean("voice_sos_enabled", enabled).apply()

        if (enabled) {
            val intent = android.content.Intent(getApplication(), com.example.service.VoiceSosForegroundService::class.java)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                getApplication<android.app.Application>().startForegroundService(intent)
            } else {
                getApplication<android.app.Application>().startService(intent)
            }
        } else {
            val intent = android.content.Intent(getApplication(), com.example.service.VoiceSosForegroundService::class.java).apply {
                action = "STOP"
            }
            getApplication<android.app.Application>().startService(intent)
        }
    }
    fun setVoiceSosPhrase(phrase: String) { }
    fun setCriticalAlarmsEnabled(enabled: Boolean) { viewModelScope.launch { settingsDataStore.setCriticalAlarmsEnabled(enabled) } }
    fun setArrivalAlertsEnabled(enabled: Boolean) { }
    fun setDeviceStatusNotificationsEnabled(enabled: Boolean) { }
    fun setLocationSharingInterval(interval: String) { }
    fun setBackgroundLocationEnabled(enabled: Boolean) { }
    fun setTelemetrySharingEnabled(enabled: Boolean) { }
    fun setBiometricEnabled(enabled: Boolean) { }
    fun setAppLockPin(pin: String, enabled: Boolean) { }
    fun setEmergencyPin(pin: String) { }
    fun runBackup() { }
    fun runRestore() { }
    fun changePassword(old: String, string: String, callback: (Boolean) -> Unit) { }
    fun deleteAccount() { }
    fun refreshPermissions(context: Context) { }
    fun isDemoMode(): Boolean = false
    fun loginUser(email: String, pass: String) {
        viewModelScope.launch {
            authService.login(email, pass)
        }
    }
    fun registerUser(name: String, email: String, phone: String, medical: String, contactName: String, contactPhone: String, pass: String) {
        viewModelScope.launch {
            val user = User(
                name = name,
                email = email,
                phone = phone,
                medicalInfo = medical,
                emergencyContactName = contactName,
                emergencyContactPhone = contactPhone
            )
            authService.register(user, pass)
        }
    }
    fun resetPassword(email: String) {
        viewModelScope.launch {
            authService.resetPassword(email)
        }
    }
    fun logout() {
        authService.logout()
    }
    fun updateUserProfile(updatedUser: User) {
        viewModelScope.launch {
            authService.updateProfile(updatedUser)
        }
    }
    fun checkSystemReadiness() { }
    fun triggerTimerSOS() { }
    fun triggerManualSOS(lat: Double? = null, lng: Double? = null) {
        emergencyProvider.triggerEmergency(
            triggerSource = "MANUAL_SOS",
            lat = lat,
            lng = lng
        )
    }
    fun triggerFallDetectedSOS() { }
    fun triggerVoiceSOS(matchedPhrase: String, confidence: Int) {
        emergencyProvider.triggerEmergency(
            triggerSource = "VOICE_SOS",
            lat = null,
            lng = null
        )
    }
    fun handleVoiceCommand(command: VoiceCommand, confidence: Int) { }
    fun startVoiceRecognition(context: Context) { }
    fun stopVoiceRecognition() { }
    fun clearVoiceCommandConfirmation() { }
    fun resolveAlert(alertId: String, notes: String) { }
    fun bondDevice(name: String, mac: String, deviceId: String = "", firmware: String, battery: Int, signal: Int, health: String) { }
    fun renameDevice(deviceId: String, newName: String) { }
    fun unbondDevice(deviceId: String) { }
    fun saveEmergencyContact(contact: EmergencyContact) = viewModelScope.launch { databaseService.saveContact(contact) }
    fun deleteEmergencyContact(contactId: String) = viewModelScope.launch { databaseService.deleteContact(contactId) }
    fun startLocationTracking() { }
    fun stopLocationTracking() { }
    fun saveFavoritePlace(name: String, lat: Double, lng: Double, type: String) { }
    fun deleteFavoritePlace(id: String) { }
    suspend fun searchCoordinates(query: String): Pair<Double, Double>? = null
    fun addTrustedPlace(place: TrustedPlace) { }
    fun updateTrustedPlace(place: TrustedPlace) { }
    fun deleteTrustedPlace(placeId: String) { }
    fun updateMapOptions(mode: String, trafficEnabled: Boolean) { }
    fun resetDistance() { }
    fun searchLocation(query: String) { }
    fun triggerEsp32SOS(triggerType: String) { }
    fun acknowledgeEmergency() { }
    fun updateResponderStatus(newStatus: String) { }
    fun muteEmergencyAlarm() { }
    fun markEmergencySafe() { }
    fun cancelEmergencyWithPin(pin: String, callback: (Boolean) -> Unit) { }
    fun endEmergencySOS(notes: String) { }
    fun deleteHistoryItem(id: String) { }
    fun markNotificationAsRead(id: String) { }
    fun markNotificationNewAsRead(id: String) { }
    fun markAllNotificationsAsRead() { }
    fun markAllNotificationsNewAsRead() { }
    fun deleteNotification(id: String) { }
    fun deleteNotificationNew(id: String) { }
    fun refreshDeviceStatus() { }
    fun restartDevice(deviceId: String) { }
    fun runDiagnostics(deviceId: String) { }
    fun cleanDiagnosticsLog() { }
    fun setNetworkAvailable(available: Boolean) { }
    fun addCommLog(log: String) { }
    fun clearCommLogs() { }
    fun authenticateAndRegisterESP32(name: String, mac: String, token: String, firmware: String, onResult: (Result<Device>) -> Unit) { }
    fun resetEsp32() { }
    fun setGpsDisabled(disabled: Boolean) { }
    fun setWeakGps(weak: Boolean) { }
    fun setOfflineMode(enabled: Boolean) { }
    fun setSlowNetwork(enabled: Boolean) { }
    fun uploadTestSOS() { }
    fun downloadTestData() { }
    fun addDeveloperLog(event: String, status: String) { }
    fun clearDeveloperLogs() { }
    fun deleteTestRecords() { }
    fun setCustomLocation(lat: Double, lng: Double) { }
    fun disconnectDevice(deviceId: String) { }
    fun connectDevice(deviceId: String) { }
    fun startEsp32Polling() { }
    fun stopEsp32Polling() { }
    fun triggerManualHeartbeatCheck(deviceId: String) { }
    sealed class UiEvent {
        data class ShowToast(val message: String) : UiEvent()
        object NavigateToLogin : UiEvent()
        object NavigateToHome : UiEvent()
        object NavigateToEmergency : UiEvent()
    }

    fun initiateEmergencySequence(triggerSource: String = "MANUAL_SOS", deviceId: String? = null, lat: Double? = null, lng: Double? = null, accuracy: Float? = null, altitude: Double? = null, speed: Float? = null, bearing: Float? = null) { }

    fun getHistoryCSVString(): String = ""
    fun getHistoryPDFReportText(): String = ""

    suspend fun getCurrentLocationOnce(): android.location.Location? = null

    val developerModeEnabled = settingsDataStore.developerModeFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val sosSoundEnabled = settingsDataStore.sosSoundEnabledFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val sosVibrationEnabled = settingsDataStore.sosVibrationEnabledFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val allFallEvents = fallRepository.allEvents.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList<com.example.model.FallEvent>())
    private val _permissionsState = MutableStateFlow(com.example.model.PermissionsState())
    val permissionsState = _permissionsState.asStateFlow()
}
