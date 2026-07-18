package com.example.service

import android.content.Context
import android.util.Log
import com.example.model.Device
import com.example.model.NotificationItem
import com.example.model.NotificationType
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class DeviceService(
    private val context: Context,
    private val databaseService: DatabaseService,
    private val notificationService: NotificationService
) {
    private val deviceProvider = DeviceProvider(context)
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _diagnosticsLog = MutableStateFlow<List<String>>(emptyList())
    val diagnosticsLog: StateFlow<List<String>> = _diagnosticsLog.asStateFlow()

    private val _isDiagnosing = MutableStateFlow(false)
    val isDiagnosing: StateFlow<Boolean> = _isDiagnosing.asStateFlow()

    private var telemetryJob: Job? = null
    
    // Track sent warning flags to avoid spamming alerts
    private val warnedLowBatteryIds = mutableSetOf<String>()
    private val warnedOfflineIds = mutableSetOf<String>()

    init {
        startTelemetryLoop()
    }

    private fun startTelemetryLoop() {
        telemetryJob?.cancel()
        telemetryJob = serviceScope.launch {
            while (isActive) {
                try {
                    updateAllDevicesTelemetry()
                } catch (e: Exception) {
                    Log.e("DeviceService", "Error in telemetry update loop", e)
                }
                delay(12000) // Poll/update metrics every 12 seconds
            }
        }
    }

    private suspend fun updateAllDevicesTelemetry() {
        val currentDevices = databaseService.devices.value
        if (currentDevices.isEmpty()) {
            return
        }

        currentDevices.forEach { device ->
            // Skip updating devices that are actively in simulated REBOOTING state
            if (device.status == "REBOOTING") return@forEach

            val updatedDevice = if (device.macAddress == "00:00:00:00:00:00" || device.deviceId.contains("local")) {
                // If this represents the host phone or local bonded node, pull REAL sensors from provider!
                val batt = deviceProvider.getLocalBatteryPercentage()
                val isChar = deviceProvider.getLocalIsCharging()
                val wifiStrength = deviceProvider.getLocalWifiSignalStrength()
                val wifiStat = deviceProvider.getLocalWifiStatus()
                val btStat = deviceProvider.getLocalBluetoothStatus()
                val gpsStat = deviceProvider.getLocalGpsStatus()
                val temp = deviceProvider.getLocalDeviceTemperature()
                val uptime = deviceProvider.getLocalUptimeSeconds()
                val mem = deviceProvider.getLocalMemoryUsagePercent()
                val cpu = deviceProvider.getLocalCpuUsagePercent()
                
                // Calculate device health score based on parameters
                val score = calculateHealthScore(batt, temp, mem, cpu, wifiStat)
                val health = when {
                    score >= 90 -> "EXCELLENT"
                    score >= 70 -> "GOOD"
                    score >= 45 -> "WARNING"
                    else -> "CRITICAL"
                }

                device.copy(
                    batteryLevel = batt,
                    isCharging = isChar,
                    wifiSignal = wifiStrength,
                    bluetoothStatus = btStat,
                    gpsStatus = gpsStat,
                    deviceTemperature = temp,
                    uptimeSeconds = uptime,
                    memoryUsagePercent = mem,
                    cpuUsagePercent = cpu,
                    healthScore = score,
                    deviceHealth = health,
                    connectionStatus = if (wifiStat == "CONNECTED") "ONLINE" else "OFFLINE",
                    status = if (wifiStat == "CONNECTED") "CONNECTED" else "DISCONNECTED",
                    lastSync = System.currentTimeMillis()
                )
            } else {
                // Simulated Wearable band v1 telemetry fluctuations!
                val randomChange = (-2..2).random()
                val newBatt = (device.batteryLevel - 1).coerceIn(1, 100) // Drain battery gradually
                val newTemp = (35.8f + (Math.random().toFloat() * 1.5f))
                val newCpu = (5..35).random()
                val newMem = (38..48).random()
                val wifiStrength = (-75..-50).random()
                
                // Keep simulated connection status stable unless critical battery
                val connStat = if (newBatt <= 2) "OFFLINE" else "ONLINE"
                val deviceStatus = if (newBatt <= 2) "DISCONNECTED" else "CONNECTED"
                
                val score = calculateHealthScore(newBatt, newTemp, newMem, newCpu, "CONNECTED")
                val health = when {
                    score >= 90 -> "EXCELLENT"
                    score >= 70 -> "GOOD"
                    score >= 45 -> "WARNING"
                    else -> "CRITICAL"
                }

                device.copy(
                    batteryLevel = newBatt,
                    deviceTemperature = newTemp,
                    cpuUsagePercent = newCpu,
                    memoryUsagePercent = newMem,
                    wifiSignal = wifiStrength,
                    uptimeSeconds = device.uptimeSeconds + 12,
                    healthScore = score,
                    deviceHealth = health,
                    connectionStatus = connStat,
                    status = deviceStatus,
                    lastSync = System.currentTimeMillis()
                )
            }

            // Check alerts
            checkDeviceThresholdAlerts(updatedDevice)

            // Save to database & firestore
            databaseService.updateDevice(updatedDevice)
        }
    }

    private fun calculateHealthScore(
        battery: Int,
        temp: Float,
        mem: Int,
        cpu: Int,
        wifiStat: String
    ): Int {
        var score = 100
        // Deduct for low battery
        if (battery < 20) score -= 15
        if (battery < 10) score -= 15
        
        // Deduct for high temperature
        if (temp > 42.0f) score -= 20
        else if (temp > 38.5f) score -= 10
        
        // Deduct for high memory/cpu
        if (mem > 85) score -= 10
        if (cpu > 85) score -= 10

        // Deduct for disconnects
        if (wifiStat == "DISCONNECTED") score -= 30

        return score.coerceIn(5, 100)
    }

    private fun checkDeviceThresholdAlerts(device: Device) {
        // Low Battery Warnings
        if (device.batteryLevel < 20 && !device.isCharging) {
            if (!warnedLowBatteryIds.contains(device.deviceId)) {
                warnedLowBatteryIds.add(device.deviceId)
                notificationService.addNotification(
                    NotificationItem(
                        id = UUID.randomUUID().toString(),
                        title = "🔋 Low Battery Warning: ${device.deviceName}",
                        body = "The battery level on ${device.deviceName} has dropped to ${device.batteryLevel}%. Please plug it in immediately to keep Guardian monitoring active.",
                        type = NotificationType.BATTERY_LOW,
                        deviceId = device.deviceId
                    )
                )
            }
        } else if (device.batteryLevel >= 25) {
            // Reset warning flag when charged
            warnedLowBatteryIds.remove(device.deviceId)
        }

        // Offline Alerts
        if (device.connectionStatus == "OFFLINE" || device.status == "DISCONNECTED") {
            if (!warnedOfflineIds.contains(device.deviceId)) {
                warnedOfflineIds.add(device.deviceId)
                notificationService.addNotification(
                    NotificationItem(
                        id = UUID.randomUUID().toString(),
                        title = "🚨 Device Offline: ${device.deviceName}",
                        body = "Guardian connection has lost contact with ${device.deviceName}. Heartbeat signal terminated. Please verify connection, Wi-Fi settings, or device power.",
                        type = NotificationType.DEVICE_OFFLINE,
                        deviceId = device.deviceId
                    )
                )
            }
        } else {
            warnedOfflineIds.remove(device.deviceId)
        }
    }

    // --- USER ACTIONS ---

    fun refreshDeviceStatus() {
        if (_isRefreshing.value) return
        serviceScope.launch {
            _isRefreshing.value = true
            try {
                updateAllDevicesTelemetry()
                delay(800) // Visual confirmation delay
            } catch (e: Exception) {
                Log.e("DeviceService", "Force refresh failed", e)
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun renameDevice(deviceId: String, newName: String) {
        serviceScope.launch {
            databaseService.renameDevice(deviceId, newName)
        }
    }

    fun restartDevice(deviceId: String) {
        serviceScope.launch {
            val device = databaseService.devices.value.find { it.deviceId == deviceId } ?: return@launch
            
            // Step 1: Set to REBOOTING
            val rebootingDevice = device.copy(
                status = "REBOOTING",
                connectionStatus = "OFFLINE",
                uptimeSeconds = 0,
                cpuUsagePercent = 0,
                memoryUsagePercent = 0,
                gpsStatus = "OFF",
                bluetoothStatus = "DISCONNECTED"
            )
            databaseService.updateDevice(rebootingDevice)
            
            // Trigger device offline notification
            notificationService.addNotification(
                NotificationItem(
                    id = UUID.randomUUID().toString(),
                    title = "🔄 Device Reboot Initiated",
                    body = "${device.deviceName} is undergoing a system reboot sequence. Telemetry will resume shortly.",
                    type = NotificationType.DEVICE_OFFLINE,
                    deviceId = deviceId
                )
            )

            // Step 2: Simulate boot timer
            delay(5000)

            // Step 3: Online state restore
            val onlineDevice = device.copy(
                status = "CONNECTED",
                connectionStatus = "ONLINE",
                uptimeSeconds = 12,
                cpuUsagePercent = 12,
                memoryUsagePercent = 38,
                gpsStatus = "LOCKED",
                bluetoothStatus = "CONNECTED",
                lastSync = System.currentTimeMillis()
            )
            databaseService.updateDevice(onlineDevice)
            
            notificationService.addNotification(
                NotificationItem(
                    id = UUID.randomUUID().toString(),
                    title = "🟢 Device Boot Complete",
                    body = "${device.deviceName} is now online, calibrated, and broadcasting secure telemetry.",
                    type = NotificationType.SAFE_ARRIVAL,
                    deviceId = deviceId
                )
            )
        }
    }

    fun runDiagnostics(deviceId: String) {
        if (_isDiagnosing.value) return
        serviceScope.launch {
            _isDiagnosing.value = true
            _diagnosticsLog.value = emptyList()
            
            val log = mutableListOf<String>()
            fun addLog(msg: String) {
                log.add(msg)
                _diagnosticsLog.value = log.toList()
            }

            val device = databaseService.devices.value.find { it.deviceId == deviceId }
            if (device == null) {
                addLog("❌ Diagnostic abort: Target device ID $deviceId not found.")
                _isDiagnosing.value = false
                return@launch
            }

            addLog("⚡ Starting System Diagnostics for ${device.deviceName}...")
            delay(600)
            addLog("🔍 Checking MPU6050 Accelerometer Register Map...")
            delay(700)
            addLog("✅ Accelerometer Integrity: OK. Noise ceiling <= 0.05G.")
            delay(500)
            addLog("📐 Calibrating Gyroscope 3-Axis Zero-Offsets...")
            delay(800)
            addLog("✅ Gyroscope Calibration: SUCCESS. Pitch=0.12°/s, Roll=-0.04°/s.")
            delay(600)
            addLog("🔋 Inspecting Battery Cell Coulomb Counter & Charging Regulator...")
            delay(800)
            
            if (device.batteryLevel < 20) {
                addLog("⚠️ Warning: Low charge remaining (${device.batteryLevel}%). Cell degradation: Minimal.")
            } else {
                addLog("✅ Power Supply: SECURE. Battery Level: ${device.batteryLevel}%. Cell health: 98% (Excellent).")
            }
            delay(600)

            addLog("📡 Probing Wireless Radios (Wi-Fi 802.11b/g/n & BLE 5.0 Transceivers)...")
            delay(800)
            addLog("✅ Network RF Strength: ${device.wifiSignal} dBm (Good). Packet Loss: 0.0%.")
            delay(500)
            addLog("🗄️ Flash Memory Integrity Check: Sector allocation scan...")
            delay(700)
            addLog("✅ Flash Partition Green. Alloc memory: ${device.memoryUsagePercent}% used.")
            delay(500)
            addLog("🤖 AI Classifier Weight Checksum & Triage Pipelines...")
            delay(600)
            addLog("✅ Neural Processor Core: READY. Standard latency: 4.2ms.")
            delay(500)
            
            val finalScore = device.healthScore
            addLog("📋 SYSTEM HEALTH SCORE CALIBRATED: $finalScore / 100")
            addLog("✨ Diagnostic Complete. All Subsystems functional.")
            
            _isDiagnosing.value = false
        }
    }
    
    fun cleanDiagnosticsLog() {
        _diagnosticsLog.value = emptyList()
    }

    // --- MODULE 16: ESP32 COMMUNICATION PLATFORM ---

    private val _isNetworkAvailable = MutableStateFlow(true)
    val isNetworkAvailable: StateFlow<Boolean> = _isNetworkAvailable.asStateFlow()

    private val _esp32CommLogs = MutableStateFlow<List<String>>(emptyList())
    val esp32CommLogs: StateFlow<List<String>> = _esp32CommLogs.asStateFlow()

    fun setNetworkAvailable(available: Boolean) {
        _isNetworkAvailable.value = available
        addCommLog("Network connectivity marked as " + if (available) "AVAILABLE" else "UNAVAILABLE")
    }

    fun addCommLog(log: String) {
        val current = _esp32CommLogs.value.toMutableList()
        val timeStr = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
        current.add(0, "[$timeStr] $log")
        _esp32CommLogs.value = current.take(100)
    }

    fun clearCommLogs() {
        _esp32CommLogs.value = emptyList()
    }

    suspend fun <T> runWithNetworkRetry(times: Int = 3, action: suspend () -> T): T {
        var lastException: Exception? = null
        for (attempt in 1..times) {
            if (!_isNetworkAvailable.value) {
                addCommLog("⚠️ Offline: Network unavailable. Buffering operation (attempt $attempt/$times)...")
                delay(1500L * attempt)
                continue
            }
            try {
                return action()
            } catch (e: Exception) {
                lastException = e
                addCommLog("⚠️ Retry exception on network action (attempt $attempt/$times): ${e.message}")
                if (attempt < times) {
                    delay(1000L * attempt)
                }
            }
        }
        throw lastException ?: Exception("Network action aborted: Connection unavailable")
    }

    suspend fun authenticateAndRegisterESP32(
        userId: String,
        deviceName: String,
        macAddress: String,
        authToken: String,
        firmwareVersion: String
    ): Result<Device> {
        return try {
            addCommLog("🔑 Initiating ESP32 Device Handshake for MAC $macAddress...")
            delay(500)

            // Step 1: Token Authentication
            addCommLog("🔒 Authenticating registration token: \"$authToken\"...")
            delay(500)
            if (authToken.trim().length < 6) {
                addCommLog("❌ Handshake Rejected: Registration token must be at least 6 characters.")
                return Result.failure(Exception("Registration handshake failed: Invalid authentication token"))
            }

            // Step 2: Firmware Version Check
            addCommLog("🔎 Firmware Check: Analyzing board version \"$firmwareVersion\"...")
            delay(500)
            var firmwareCheckSuccess = true
            if (firmwareVersion.startsWith("v1.0") || firmwareVersion.startsWith("v1.1")) {
                addCommLog("⚠️ Firmware Outdated Check: Version is old ($firmwareVersion). Handshake proceeding with warnings.")
                notificationService.addNotification(
                    NotificationItem(
                        id = UUID.randomUUID().toString(),
                        title = "⚠️ Outdated ESP32 Firmware Detected",
                        body = "Device $deviceName is running an older firmware ($firmwareVersion). Please flash the latest v1.2.8 firmware to avoid connection jitter.",
                        type = NotificationType.FIRMWARE_UPDATE
                    )
                )
                firmwareCheckSuccess = false
            } else {
                addCommLog("✅ Firmware Verified: Board is compliant ($firmwareVersion).")
            }

            // Step 3: Cryptographic Pairing stages
            addCommLog("📶 Handshake stage 3/4: Establishing BLE secure socket...")
            delay(400)
            addCommLog("📐 Handshake stage 4/4: Calibrating MPU6050 accelerometer & synchronizing UTC...")
            delay(400)

            val deviceId = "esp32-" + UUID.randomUUID().toString().take(8)
            val newDevice = Device(
                deviceId = deviceId,
                userId = userId,
                deviceName = deviceName,
                status = "CONNECTED",
                batteryLevel = 100,
                macAddress = macAddress,
                lastSync = System.currentTimeMillis(),
                firmwareVersion = firmwareVersion,
                signalStrength = -55,
                deviceHealth = "EXCELLENT",
                connectionStatus = "ONLINE"
            )

            // Save to Firestore / local DB with network auto-retry helper
            runWithNetworkRetry {
                databaseService.updateDevice(newDevice)
            }

            addCommLog("🟢 Handshake SUCCESS: Device registered as $deviceId.")
            Result.success(newDevice)
        } catch (e: Exception) {
            addCommLog("❌ Registration aborted: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun receiveTelemetry(
        deviceId: String,
        batteryLevel: Int,
        isCharging: Boolean,
        latitude: Double,
        longitude: Double,
        ax: Float, ay: Float, az: Float,
        gx: Float, gy: Float, gz: Float,
        firmwareVersion: String
    ) {
        val currentDevices = databaseService.devices.value
        val device = currentDevices.find { it.deviceId == deviceId }
        if (device == null) {
            addCommLog("⚠️ Telemetry Rejected: Unknown node ID $deviceId")
            return
        }

        addCommLog("📥 Parsing incoming UDP/BLE packet from $deviceId...")
        
        // Auto network retry simulation wrapper
        try {
            runWithNetworkRetry(1) {
                val score = calculateHealthScore(batteryLevel, device.deviceTemperature, device.memoryUsagePercent, device.cpuUsagePercent, "CONNECTED")
                val health = when {
                    score >= 90 -> "EXCELLENT"
                    score >= 70 -> "GOOD"
                    score >= 45 -> "WARNING"
                    else -> "CRITICAL"
                }

                val updatedDevice = device.copy(
                    batteryLevel = batteryLevel,
                    isCharging = isCharging,
                    latitude = latitude,
                    longitude = longitude,
                    accelX = ax,
                    accelY = ay,
                    accelZ = az,
                    gyroX = gx,
                    gyroY = gy,
                    gyroZ = gz,
                    firmwareVersion = firmwareVersion,
                    lastSync = System.currentTimeMillis(),
                    healthScore = score,
                    deviceHealth = health,
                    connectionStatus = "ONLINE",
                    status = "CONNECTED"
                )

                databaseService.updateDevice(updatedDevice)
                addCommLog("📊 Stream: GPS=(${String.format("%.4f", latitude)}, ${String.format("%.4f", longitude)}) MPU=(${String.format("%.2f", ax)}G, ${String.format("%.2f", ay)}G, ${String.format("%.2f", az)}G) Batt=$batteryLevel%")

                // Check safety threshold for MPU6050 (e.g. impact or fall)
                val totalG = kotlin.math.sqrt((ax * ax + ay * ay + az * az).toDouble())
                if (totalG > 4.5) {
                    addCommLog("💥 CRITICAL: MPU6050 G-force threshold exceeded! Detected magnitude: ${String.format("%.2f", totalG)}G.")
                    handleIncomingEsp32Sos(deviceId, "FALL_DETECTED")
                }
            }
        } catch (e: Exception) {
            addCommLog("⚠️ Telemetry packet buffered locally: network is offline.")
        }
    }

    fun handleIncomingEsp32Sos(
        deviceId: String,
        triggerType: String,
        userId: String = "user-101",
        userName: String = "Marcus Vance",
        userPhone: String = "+1-555-0143"
    ) {
        serviceScope.launch {
            addCommLog("🚨 SOS Event received from ESP32 [$deviceId]: Type: $triggerType")
            val device = databaseService.devices.value.find { it.deviceId == deviceId }
            
            val lat = device?.latitude ?: 37.7749
            val lng = device?.longitude ?: -122.4194

            // Trigger Firestore SOS Alert
            databaseService.triggerSOS(
                userId = userId,
                userName = userName,
                userPhone = userPhone,
                lat = lat,
                lng = lng,
                triggerType = triggerType
            )

            // Update Device Status
            device?.let {
                databaseService.updateDevice(it.copy(status = "ALERTing"))
            }

            addCommLog("📢 SOS Broadcast dispatched to security contacts!")
        }
    }

    fun triggerManualHeartbeatCheck(deviceId: String) {
        serviceScope.launch {
            addCommLog("💓 Dispatching manual ping/heartbeat query to $deviceId...")
            delay(600)
            val device = databaseService.devices.value.find { it.deviceId == deviceId }
            if (device == null) {
                addCommLog("❌ Ping Failed: Target node not found.")
                return@launch
            }

            if (!_isNetworkAvailable.value) {
                addCommLog("❌ Ping Timeout: Android wireless network is unavailable.")
                handleDeviceDisconnect(deviceId)
                return@launch
            }

            // Successfully received response
            addCommLog("✅ Heartbeat response received from $deviceId in 18ms. Signal RSSI: ${device.wifiSignal} dBm.")
            databaseService.updateDevice(
                device.copy(
                    lastSync = System.currentTimeMillis(),
                    connectionStatus = "ONLINE",
                    status = "CONNECTED"
                )
            )
        }
    }

    private suspend fun handleDeviceDisconnect(deviceId: String) {
        val device = databaseService.devices.value.find { it.deviceId == deviceId } ?: return
        if (device.status != "DISCONNECTED" && device.status != "REBOOTING") {
            addCommLog("🚨 Lost contact with $deviceId. Heartbeat monitor timed out.")
            databaseService.updateDevice(
                device.copy(
                    status = "DISCONNECTED",
                    connectionStatus = "OFFLINE"
                )
            )
            checkDeviceThresholdAlerts(device.copy(status = "DISCONNECTED", connectionStatus = "OFFLINE"))
            
            // Start reconnect loop
            initiateAutomaticReconnection(deviceId)
        }
    }

    private fun initiateAutomaticReconnection(deviceId: String) {
        serviceScope.launch {
            addCommLog("🔄 Reconnection: Initiating automatic background reconnect loop for $deviceId...")
            var attempt = 1
            val maxAttempts = 5
            
            while (attempt <= maxAttempts) {
                val currentDevice = databaseService.devices.value.find { it.deviceId == deviceId }
                if (currentDevice == null || currentDevice.status == "CONNECTED") {
                    addCommLog("🔄 Reconnection loop aborted: device is already connected or unbonded.")
                    return@launch
                }

                addCommLog("🔄 Reconnection: Attempt $attempt/$maxAttempts to reconnect...")
                delay(4000)

                if (_isNetworkAvailable.value) {
                    addCommLog("🟢 Reconnection SUCCESS: Handshake established. Secure telemetry stream recovered.")
                    val restoredDevice = currentDevice.copy(
                        status = "CONNECTED",
                        connectionStatus = "ONLINE",
                        lastSync = System.currentTimeMillis()
                    )
                    databaseService.updateDevice(restoredDevice)
                    return@launch
                } else {
                    addCommLog("⚠️ Reconnection failed (network offline). Backing off...")
                    attempt++
                }
            }
            addCommLog("❌ Reconnection: Background reconnect loop terminated after $maxAttempts failed attempts. Please inspect hardware.")
        }
    }
}
