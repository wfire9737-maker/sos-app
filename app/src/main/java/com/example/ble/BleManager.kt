package com.example.ble

import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger

/**
 * Discrete representation of a hardware SOS push button event from the ESP32 (GPIO 4).
 * Each press produces an independent event.
 */
data class SosButtonEvent(
    val eventId: String,
    val rawPayload: String,
    val timestamp: Long = System.currentTimeMillis(),
    val totalEventsCount: Int = 1,
    val hardwareGpsLocation: HardwareGpsLocation? = null
)

@SuppressLint("MissingPermission")
@Suppress("DEPRECATION", "MissingPermission")
class BleManager(private val context: Context) {

    enum class BleState {
        DISCONNECTED,
        SCANNING,
        CONNECTING,
        DISCOVERING_SERVICES,
        SUBSCRIBING_STATUS_NOTIFICATIONS,
        SUBSCRIBING_GPS_NOTIFICATIONS,
        SUBSCRIBING_MPU_NOTIFICATIONS,
        READING_BATTERY,
        READING_GPS,
        READING_MPU,
        CONNECTED,
        READY,
        DEVICE_NOT_FOUND,
        CONNECTION_FAILED,
        ERROR
    }

    private val _connectionState = MutableStateFlow(BleState.DISCONNECTED)
    val connectionState: StateFlow<BleState> = _connectionState.asStateFlow()

    private val _deviceName = MutableStateFlow<String?>(BleProtocol.DEVICE_NAME)
    val deviceName: StateFlow<String?> = _deviceName.asStateFlow()

    private val _deviceMac = MutableStateFlow<String?>("00:00:00:00:00:00")
    val deviceMac: StateFlow<String?> = _deviceMac.asStateFlow()

    // Status Characteristic & Payload
    private val _statusString = MutableStateFlow<String?>("Not received yet")
    val statusString: StateFlow<String?> = _statusString.asStateFlow()

    // Battery Characteristic & Payload
    private val _batteryDisplay = MutableStateFlow("Unavailable")
    val batteryDisplay: StateFlow<String> = _batteryDisplay.asStateFlow()

    private val _batteryLevel = MutableStateFlow<Int?>(null)
    val batteryLevel: StateFlow<Int?> = _batteryLevel.asStateFlow()

    // Real Hardware NEO-6M GPS State & Coordinates
    private val _hardwareGpsState = MutableStateFlow<HardwareGpsState>(HardwareGpsState.Unavailable)
    val hardwareGpsState: StateFlow<HardwareGpsState> = _hardwareGpsState.asStateFlow()

    private val _latestHardwareGpsLocation = MutableStateFlow<HardwareGpsLocation?>(null)
    val latestHardwareGpsLocation: StateFlow<HardwareGpsLocation?> = _latestHardwareGpsLocation.asStateFlow()

    private val _gpsRawString = MutableStateFlow<String?>("Waiting for data...")
    val gpsRawString: StateFlow<String?> = _gpsRawString.asStateFlow()

    private val _lastGpsTimestamp = MutableStateFlow(0L)
    val lastGpsTimestamp: StateFlow<Long> = _lastGpsTimestamp.asStateFlow()

    // Real Hardware MPU6050 Accelerometer / Gyroscope State & Motion Processor
    val motionProcessor = MotionProcessor()
    val motionState: StateFlow<MotionState> = motionProcessor.motionState
    val mpuRecentReadings: StateFlow<List<Mpu6050Reading>> = motionProcessor.recentReadings

    private val _mpuCharacteristicFound = MutableStateFlow(false)
    val mpuCharacteristicFound: StateFlow<Boolean> = _mpuCharacteristicFound.asStateFlow()

    private val _mpuNotificationSubscribed = MutableStateFlow(false)
    val mpuNotificationSubscribed: StateFlow<Boolean> = _mpuNotificationSubscribed.asStateFlow()

    private val _latestMpuReading = MutableStateFlow<Mpu6050Reading?>(null)
    val latestMpuReading: StateFlow<Mpu6050Reading?> = _latestMpuReading.asStateFlow()

    private val _mpuRawString = MutableStateFlow<String?>("Waiting for MPU6050 data...")
    val mpuRawString: StateFlow<String?> = _mpuRawString.asStateFlow()

    private val _lastMpuTimestamp = MutableStateFlow(0L)
    val lastMpuTimestamp: StateFlow<Long> = _lastMpuTimestamp.asStateFlow()

    private val _mpuHardwareState = MutableStateFlow<MpuHardwareState>(MpuHardwareState.Unavailable)
    val mpuHardwareState: StateFlow<MpuHardwareState> = _mpuHardwareState.asStateFlow()

    private val _rssi = MutableStateFlow<Int?>(null)
    val rssi: StateFlow<Int?> = _rssi.asStateFlow()

    // SharedFlow stream for discrete button events - ensures consecutive repeated presses are never ignored
    private val _sosEvents = MutableSharedFlow<SosButtonEvent>(extraBufferCapacity = 64)
    val sosEvents: SharedFlow<SosButtonEvent> = _sosEvents.asSharedFlow()

    private val _lastSosEvent = MutableStateFlow<SosButtonEvent?>(null)
    val lastSosEvent: StateFlow<SosButtonEvent?> = _lastSosEvent.asStateFlow()

    private val _sosEventList = MutableStateFlow<List<SosButtonEvent>>(emptyList())
    val sosEventList: StateFlow<List<SosButtonEvent>> = _sosEventList.asStateFlow()

    private val _sosEventCount = MutableStateFlow(0)
    val sosEventCount: StateFlow<Int> = _sosEventCount.asStateFlow()

    private val _sosEvent = MutableStateFlow(false)
    val sosEvent: StateFlow<Boolean> = _sosEvent.asStateFlow()

    private val _lastStatusTimestamp = MutableStateFlow(0L)
    val lastStatusTimestamp: StateFlow<Long> = _lastStatusTimestamp.asStateFlow()

    private val _lastErrorMessage = MutableStateFlow<String?>(null)
    val lastErrorMessage: StateFlow<String?> = _lastErrorMessage.asStateFlow()

    private val _serviceFound = MutableStateFlow(false)
    val serviceFound: StateFlow<Boolean> = _serviceFound.asStateFlow()

    private val _statusNotificationSubscribed = MutableStateFlow(false)
    val statusNotificationSubscribed: StateFlow<Boolean> = _statusNotificationSubscribed.asStateFlow()

    private val _gpsNotificationSubscribed = MutableStateFlow(false)
    val gpsNotificationSubscribed: StateFlow<Boolean> = _gpsNotificationSubscribed.asStateFlow()

    private val _notificationSubscribed = MutableStateFlow(false)
    val notificationSubscribed: StateFlow<Boolean> = _notificationSubscribed.asStateFlow()

    private val _gpsCharacteristicFound = MutableStateFlow(false)
    val gpsCharacteristicFound: StateFlow<Boolean> = _gpsCharacteristicFound.asStateFlow()

    private val internalEventCounter = AtomicInteger(0)
    private var lastProcessedEventId: String? = null
    private var lastProcessedTimestamp: Long = 0L

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter
    private val scanner: BluetoothLeScanner? = bluetoothAdapter?.bluetoothLeScanner

    private var gatt: BluetoothGatt? = null
    private var isScanning = false
    private var autoReconnectEnabled = true
    private var reconnectAttempt = 0
    private val handler = Handler(Looper.getMainLooper())

    private val reconnectRunnable = Runnable {
        if (autoReconnectEnabled && _connectionState.value != BleState.CONNECTED && _connectionState.value != BleState.READY && !isScanning) {
            Log.d("BleManager", "BLE: reconnecting")
            scanAndConnect()
        }
    }

    private val scanTimeoutRunnable = Runnable {
        if (isScanning) {
            stopScan()
            if (_connectionState.value == BleState.SCANNING) {
                _connectionState.value = BleState.DEVICE_NOT_FOUND
                _lastErrorMessage.value = "ESP32 not found within scan window."
                Log.d("BleManager", "BLE: scan timed out, ESP32 not found")
                scheduleReconnect()
            }
        }
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            result?.device?.let { device ->
                try {
                    val name = device.name ?: result.scanRecord?.deviceName
                    if (name != null && name.contains(BleProtocol.DEVICE_NAME, ignoreCase = true)) {
                        Log.d("BleManager", "BLE: device discovered")
                        stopScan()
                        connectToDevice(device)
                    }
                } catch (e: SecurityException) {
                    Log.e("BleManager", "SecurityException in scanCallback", e)
                }
            }
        }

        override fun onScanFailed(errorCode: Int) {
            isScanning = false
            _connectionState.value = BleState.CONNECTION_FAILED
            _lastErrorMessage.value = "Scan failed with error code: $errorCode"
            Log.e("BleManager", "BLE: scan failed code $errorCode")
            scheduleReconnect()
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            try {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    if (newState == BluetoothProfile.STATE_CONNECTED) {
                        Log.d("BleManager", "BLE: GATT connected")
                        reconnectAttempt = 0
                        _deviceName.value = gatt.device.name ?: BleProtocol.DEVICE_NAME
                        _deviceMac.value = gatt.device.address ?: "00:00:00:00:00:00"
                        _connectionState.value = BleState.DISCOVERING_SERVICES
                        _lastErrorMessage.value = null
                        handler.post {
                            try {
                                gatt.discoverServices()
                            } catch (e: SecurityException) {
                                Log.e("BleManager", "SecurityException discovering services", e)
                            }
                        }
                    } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                        Log.d("BleManager", "BLE: disconnected")
                        handleDisconnection("Device disconnected")
                    }
                } else {
                    Log.d("BleManager", "BLE: disconnected (GATT Error status: $status)")
                    handleDisconnection("GATT Error status: $status")
                }
            } catch (e: SecurityException) {
                Log.e("BleManager", "Missing BLUETOOTH_CONNECT permission", e)
                handleDisconnection("Missing Bluetooth Connect permission")
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            try {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    Log.d("BleManager", "BLE: services discovered")
                    val service = gatt.getService(BleProtocol.SERVICE_UUID)
                    if (service == null) {
                        Log.w("BleManager", "BLE: Required service ${BleProtocol.SERVICE_UUID} not found")
                        _lastErrorMessage.value = "Required service ${BleProtocol.SERVICE_UUID} not found on device."
                        _connectionState.value = BleState.CONNECTION_FAILED
                        disconnectGattInternal()
                        scheduleReconnect()
                        return
                    }

                    _serviceFound.value = true
                    val statusChar = service.getCharacteristic(BleProtocol.STATUS_CHARACTERISTIC_UUID)
                    val batChar = service.getCharacteristic(BleProtocol.BATTERY_CHARACTERISTIC_UUID)
                    val gpsChar = service.getCharacteristic(BleProtocol.GPS_CHARACTERISTIC_UUID)
                    val mpuChar = service.getCharacteristic(BleProtocol.MPU6050_CHARACTERISTIC_UUID)

                    if (statusChar == null) {
                        Log.w("BleManager", "BLE: Status characteristic not found")
                        _lastErrorMessage.value = "Status characteristic not found."
                        _connectionState.value = BleState.CONNECTION_FAILED
                        disconnectGattInternal()
                        scheduleReconnect()
                        return
                    }

                    Log.d("BleManager", "BLE: status characteristic found")

                    if (gpsChar != null) {
                        _gpsCharacteristicFound.value = true
                        Log.d("BleManager", "BLE: GPS characteristic found")
                    } else {
                        _gpsCharacteristicFound.value = false
                        Log.w("BleManager", "BLE: GPS characteristic ${BleProtocol.GPS_CHARACTERISTIC_UUID} not found")
                    }

                    if (mpuChar != null) {
                        _mpuCharacteristicFound.value = true
                        _mpuHardwareState.value = MpuHardwareState.Connecting
                        Log.d("BleManager", "BLE: MPU characteristic found")
                    } else {
                        _mpuCharacteristicFound.value = false
                        _mpuHardwareState.value = MpuHardwareState.Error("MPU6050 unavailable")
                        Log.w("BleManager", "BLE: MPU characteristic ${BleProtocol.MPU6050_CHARACTERISTIC_UUID} not found - MPU6050 unavailable")
                    }

                    // Initialization Sequence Step 1: Enable status notifications locally & write CCCD
                    step1EnableStatusNotification(gatt, statusChar)
                } else {
                    _lastErrorMessage.value = "Service discovery failed with status $status"
                    _connectionState.value = BleState.CONNECTION_FAILED
                    disconnectGattInternal()
                    scheduleReconnect()
                }
            } catch (e: SecurityException) {
                Log.e("BleManager", "Missing BLUETOOTH_CONNECT permission", e)
            }
        }

        override fun onDescriptorWrite(gatt: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
            val charUuid = descriptor.characteristic?.uuid
            val descUuid = descriptor.uuid

            if (charUuid == BleProtocol.STATUS_CHARACTERISTIC_UUID || (descUuid == BleProtocol.CLIENT_CHARACTERISTIC_CONFIG_UUID && _connectionState.value == BleState.SUBSCRIBING_STATUS_NOTIFICATIONS)) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    _statusNotificationSubscribed.value = true
                    Log.d("BleManager", "BLE: status CCCD enabled")
                } else {
                    Log.w("BleManager", "BLE: Status CCCD write failed with status $status")
                }

                // Initialization Sequence Step 2: Enable GPS notifications & write CCCD
                val service = gatt.getService(BleProtocol.SERVICE_UUID)
                val gpsChar = service?.getCharacteristic(BleProtocol.GPS_CHARACTERISTIC_UUID)
                if (gpsChar != null) {
                    step2EnableGpsNotification(gatt, gpsChar)
                } else {
                    advanceAfterGpsNotification(gatt)
                }
                return
            }

            if (charUuid == BleProtocol.GPS_CHARACTERISTIC_UUID || (descUuid == BleProtocol.CLIENT_CHARACTERISTIC_CONFIG_UUID && _connectionState.value == BleState.SUBSCRIBING_GPS_NOTIFICATIONS)) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    _gpsNotificationSubscribed.value = true
                    Log.d("BleManager", "BLE: GPS CCCD enabled")
                } else {
                    Log.w("BleManager", "BLE: GPS CCCD write failed with status $status")
                }

                // Initialization Sequence Step 3: Enable MPU notifications & write CCCD
                advanceAfterGpsNotification(gatt)
                return
            }

            if (charUuid == BleProtocol.MPU6050_CHARACTERISTIC_UUID || (descUuid == BleProtocol.CLIENT_CHARACTERISTIC_CONFIG_UUID && _connectionState.value == BleState.SUBSCRIBING_MPU_NOTIFICATIONS)) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    _mpuNotificationSubscribed.value = true
                    _notificationSubscribed.value = true
                    Log.d("BleManager", "BLE: MPU CCCD enabled")
                } else {
                    Log.w("BleManager", "BLE: MPU CCCD write failed with status $status")
                }

                // Initialization Sequence Step 4: Read Battery characteristic
                advanceAfterMpuNotification(gatt)
                return
            }
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            val uuid = characteristic.uuid
            val value = characteristic.value ?: byteArrayOf()

            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (uuid == BleProtocol.BATTERY_CHARACTERISTIC_UUID) {
                    val batText = parseBatteryValue(value)
                    _batteryDisplay.value = batText
                    _batteryLevel.value = batText.replace("%", "").toIntOrNull()

                    // Initialization Sequence Step 5: Read initial GPS value
                    val service = gatt.getService(BleProtocol.SERVICE_UUID)
                    val gpsChar = service?.getCharacteristic(BleProtocol.GPS_CHARACTERISTIC_UUID)
                    step5ReadGps(gatt, gpsChar)
                    return
                } else if (uuid == BleProtocol.GPS_CHARACTERISTIC_UUID) {
                    handleGpsNotification(value)
                    // Initialization Sequence Step 6: Read initial MPU value
                    val service = gatt.getService(BleProtocol.SERVICE_UUID)
                    val mpuChar = service?.getCharacteristic(BleProtocol.MPU6050_CHARACTERISTIC_UUID)
                    step6ReadMpu(gatt, mpuChar)
                    return
                } else if (uuid == BleProtocol.MPU6050_CHARACTERISTIC_UUID) {
                    handleMpuNotification(value)
                } else if (uuid == BleProtocol.STATUS_CHARACTERISTIC_UUID) {
                    val text = parseStatusValue(value)
                    _statusString.value = text
                    _lastStatusTimestamp.value = System.currentTimeMillis()
                }
            } else {
                Log.w("BleManager", "Characteristic read failed for $uuid with status $status")
            }

            // Finish setup and mark READY
            finishConnectionSetup(gatt)
        }

        // For Android API < 33
        @Deprecated("Deprecated in Java")
        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            val value = characteristic.value ?: byteArrayOf()
            handleCharacteristicNotification(characteristic.uuid, value)
        }

        // For Android API >= 33 (Tiramisu+)
        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, value: ByteArray) {
            handleCharacteristicNotification(characteristic.uuid, value)
        }

        override fun onReadRemoteRssi(gatt: BluetoothGatt, rssi: Int, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                _rssi.value = rssi
            }
        }
    }

    private fun step1EnableStatusNotification(gatt: BluetoothGatt, statusChar: BluetoothGattCharacteristic) {
        handler.post {
            try {
                Log.d("BleManager", "BLE: enabling status notifications")
                _connectionState.value = BleState.SUBSCRIBING_STATUS_NOTIFICATIONS
                gatt.setCharacteristicNotification(statusChar, true)

                val descriptor = statusChar.getDescriptor(BleProtocol.CLIENT_CHARACTERISTIC_CONFIG_UUID)
                    ?: statusChar.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"))

                if (descriptor != null) {
                    writeDescriptorSafely(gatt, descriptor)
                } else {
                    Log.w("BleManager", "BLE: CCCD descriptor not found on status characteristic")
                    advanceAfterStatusNotification(gatt)
                }
            } catch (e: SecurityException) {
                Log.e("BleManager", "SecurityException enabling status notification", e)
            }
        }
    }

    private fun advanceAfterStatusNotification(gatt: BluetoothGatt) {
        val service = gatt.getService(BleProtocol.SERVICE_UUID)
        val gpsChar = service?.getCharacteristic(BleProtocol.GPS_CHARACTERISTIC_UUID)
        if (gpsChar != null) {
            step2EnableGpsNotification(gatt, gpsChar)
        } else {
            advanceAfterGpsNotification(gatt)
        }
    }

    private fun step2EnableGpsNotification(gatt: BluetoothGatt, gpsChar: BluetoothGattCharacteristic) {
        handler.post {
            try {
                Log.d("BleManager", "BLE: enabling GPS notifications")
                _connectionState.value = BleState.SUBSCRIBING_GPS_NOTIFICATIONS
                gatt.setCharacteristicNotification(gpsChar, true)

                val descriptor = gpsChar.getDescriptor(BleProtocol.CLIENT_CHARACTERISTIC_CONFIG_UUID)
                    ?: gpsChar.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"))

                if (descriptor != null) {
                    writeDescriptorSafely(gatt, descriptor)
                } else {
                    Log.w("BleManager", "BLE: CCCD descriptor not found on GPS characteristic")
                    advanceAfterGpsNotification(gatt)
                }
            } catch (e: SecurityException) {
                Log.e("BleManager", "SecurityException enabling GPS notification", e)
            }
        }
    }

    private fun advanceAfterGpsNotification(gatt: BluetoothGatt) {
        val service = gatt.getService(BleProtocol.SERVICE_UUID)
        val mpuChar = service?.getCharacteristic(BleProtocol.MPU6050_CHARACTERISTIC_UUID)
        if (mpuChar != null) {
            step3EnableMpuNotification(gatt, mpuChar)
        } else {
            advanceAfterMpuNotification(gatt)
        }
    }

    private fun step3EnableMpuNotification(gatt: BluetoothGatt, mpuChar: BluetoothGattCharacteristic) {
        handler.post {
            try {
                Log.d("BleManager", "BLE: enabling MPU notifications")
                _connectionState.value = BleState.SUBSCRIBING_MPU_NOTIFICATIONS
                gatt.setCharacteristicNotification(mpuChar, true)

                val descriptor = mpuChar.getDescriptor(BleProtocol.CLIENT_CHARACTERISTIC_CONFIG_UUID)
                    ?: mpuChar.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"))

                if (descriptor != null) {
                    writeDescriptorSafely(gatt, descriptor)
                } else {
                    Log.w("BleManager", "BLE: CCCD descriptor not found on MPU characteristic")
                    advanceAfterMpuNotification(gatt)
                }
            } catch (e: SecurityException) {
                Log.e("BleManager", "SecurityException enabling MPU notification", e)
            }
        }
    }

    private fun advanceAfterMpuNotification(gatt: BluetoothGatt) {
        val service = gatt.getService(BleProtocol.SERVICE_UUID)
        val batChar = service?.getCharacteristic(BleProtocol.BATTERY_CHARACTERISTIC_UUID)
        step4ReadBattery(gatt, batChar)
    }

    private fun step4ReadBattery(gatt: BluetoothGatt, batChar: BluetoothGattCharacteristic?) {
        if (batChar != null) {
            handler.post {
                try {
                    _connectionState.value = BleState.READING_BATTERY
                    gatt.readCharacteristic(batChar)
                } catch (e: SecurityException) {
                    Log.e("BleManager", "Error reading battery char", e)
                    finishConnectionSetup(gatt)
                }
            }
        } else {
            finishConnectionSetup(gatt)
        }
    }

    private fun step5ReadGps(gatt: BluetoothGatt, gpsChar: BluetoothGattCharacteristic?) {
        if (gpsChar != null) {
            handler.post {
                try {
                    _connectionState.value = BleState.READING_GPS
                    gatt.readCharacteristic(gpsChar)
                } catch (e: SecurityException) {
                    Log.e("BleManager", "Error reading GPS char", e)
                    finishConnectionSetup(gatt)
                }
            }
        } else {
            finishConnectionSetup(gatt)
        }
    }

    private fun step6ReadMpu(gatt: BluetoothGatt, mpuChar: BluetoothGattCharacteristic?) {
        if (mpuChar != null) {
            handler.post {
                try {
                    _connectionState.value = BleState.READING_MPU
                    gatt.readCharacteristic(mpuChar)
                } catch (e: SecurityException) {
                    Log.e("BleManager", "Error reading MPU char", e)
                    finishConnectionSetup(gatt)
                }
            }
        } else {
            finishConnectionSetup(gatt)
        }
    }

    private fun writeDescriptorSafely(gatt: BluetoothGatt, descriptor: BluetoothGattDescriptor) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                gatt.writeDescriptor(descriptor, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
            } else {
                descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                gatt.writeDescriptor(descriptor)
            }
        } catch (e: SecurityException) {
            Log.e("BleManager", "SecurityException writing CCCD descriptor", e)
        }
    }

    private fun handleCharacteristicNotification(uuid: UUID, value: ByteArray) {
        when (uuid) {
            BleProtocol.STATUS_CHARACTERISTIC_UUID -> {
                handleStatusNotification(value)
            }
            BleProtocol.GPS_CHARACTERISTIC_UUID -> {
                handleGpsNotification(value)
            }
            BleProtocol.MPU6050_CHARACTERISTIC_UUID -> {
                handleMpuNotification(value)
            }
            BleProtocol.BATTERY_CHARACTERISTIC_UUID -> {
                val batText = parseBatteryValue(value)
                _batteryDisplay.value = batText
                _batteryLevel.value = batText.replace("%", "").toIntOrNull()
            }
        }
    }

    private fun handleMpuNotification(value: ByteArray) {
        val text = parseStatusValue(value)
        _mpuRawString.value = text
        _lastMpuTimestamp.value = System.currentTimeMillis()

        val reading = Mpu6050Reading.parse(text)
        if (reading != null) {
            _latestMpuReading.value = reading
            _mpuHardwareState.value = MpuHardwareState.Receiving(reading, motionProcessor.motionState.value)
            // Offload motion processing asynchronously to MotionProcessor outside GATT callback
            motionProcessor.onNewReading(reading)
        } else {
            Log.w("BleManager", "MPU_BLE: Invalid or malformed MPU6050 packet: $text")
        }
    }

    private fun handleStatusNotification(value: ByteArray) {
        val text = parseStatusValue(value)
        _statusString.value = text
        _lastStatusTimestamp.value = System.currentTimeMillis()

        Log.d("BleManager", "BLE: received $text")

        // Every notification starting with or containing SOS_TRIGGERED is a discrete button event
        if (text.startsWith("SOS_TRIGGERED", ignoreCase = true) || text.contains("SOS_TRIGGERED", ignoreCase = true)) {
            val rawEventId = if (text.contains(":")) {
                text.substringAfter(":").trim()
            } else {
                "${internalEventCounter.get() + 1}"
            }

            val now = System.currentTimeMillis()
            // Prevent processing identical eventId if duplicate packet fired within 500ms
            if (lastProcessedEventId == rawEventId && (now - lastProcessedTimestamp) < 500) {
                Log.d("BleManager", "BLE: duplicate packet for event $rawEventId ignored")
                return
            }

            lastProcessedEventId = rawEventId
            lastProcessedTimestamp = now

            val count = internalEventCounter.incrementAndGet()
            Log.d("BleManager", "SOS: physical button event $rawEventId")

            val currentGps = _latestHardwareGpsLocation.value
            val isGpsValid = _hardwareGpsState.value is HardwareGpsState.ValidLocation && currentGps != null

            if (isGpsValid) {
                Log.d("BleManager", "SOS: using latest NEO-6M location")
            } else {
                Log.d("BleManager", "SOS: NEO-6M location unavailable")
            }

            val event = SosButtonEvent(
                eventId = rawEventId,
                rawPayload = text,
                timestamp = now,
                totalEventsCount = count,
                hardwareGpsLocation = if (isGpsValid) currentGps else null
            )

            _lastSosEvent.value = event
            _sosEventCount.value = count
            _sosEvent.value = true

            val currentList = _sosEventList.value.toMutableList()
            currentList.add(0, event)
            _sosEventList.value = currentList.take(50)

            // Emit to SharedFlow without deduplication so consecutive presses always trigger
            _sosEvents.tryEmit(event)
        }
    }

    private fun handleGpsNotification(value: ByteArray) {
        val text = parseStatusValue(value)
        _gpsRawString.value = text
        _lastGpsTimestamp.value = System.currentTimeMillis()

        Log.d("BleManager", "GPS_BLE: received $text")

        val clean = text.trim()

        if (clean.equals("NO_FIX", ignoreCase = true) || clean.contains("NO_FIX", ignoreCase = true)) {
            _hardwareGpsState.value = HardwareGpsState.WaitingForFix
            Log.d("BleManager", "GPS_BLE: waiting for GPS fix")
            return
        }

        if (clean.contains("LAT:", ignoreCase = true) && clean.contains("LON:", ignoreCase = true)) {
            try {
                val latStr = clean.substringAfter("LAT:", "").substringBefore(",", "").trim()
                val lonStr = clean.substringAfter("LON:", "").trim()

                val lat = latStr.toDoubleOrNull()
                val lon = lonStr.toDoubleOrNull()

                if (lat != null && lon != null && lat in -90.0..90.0 && lon in -180.0..180.0) {
                    val location = HardwareGpsLocation(
                        latitude = lat,
                        longitude = lon,
                        receivedAt = System.currentTimeMillis(),
                        rawPayload = text,
                        locationSource = "ESP32_NEO6M"
                    )
                    _latestHardwareGpsLocation.value = location
                    _hardwareGpsState.value = HardwareGpsState.ValidLocation(location)

                    Log.d("BleManager", "GPS_BLE: valid GPS fix")
                    Log.d("BleManager", "GPS_BLE: latitude = $lat")
                    Log.d("BleManager", "GPS_BLE: longitude = $lon")
                } else {
                    Log.w("BleManager", "GPS_BLE: invalid coordinates range lat=$latStr, lon=$lonStr in payload: $text")
                }
            } catch (e: Exception) {
                Log.w("BleManager", "GPS_BLE: malformed GPS data: $text", e)
            }
        } else {
            Log.d("BleManager", "GPS_BLE: unparsed payload: $text")
        }
    }

    private fun finishConnectionSetup(gatt: BluetoothGatt) {
        _connectionState.value = BleState.CONNECTED
        _lastErrorMessage.value = null
        _notificationSubscribed.value = _statusNotificationSubscribed.value || _gpsNotificationSubscribed.value || _mpuNotificationSubscribed.value
        Log.d("BleManager", "BLE: monitoring active (Status, GPS, MPU6050)")
        readRssiPeriodically()
    }

    private fun handleDisconnection(reason: String) {
        val wasConnected = _connectionState.value == BleState.CONNECTED || _connectionState.value == BleState.READY
        cleanGatt()
        _lastErrorMessage.value = reason
        _connectionState.value = if (wasConnected) BleState.DISCONNECTED else BleState.CONNECTION_FAILED
        _notificationSubscribed.value = false
        _statusNotificationSubscribed.value = false
        _gpsNotificationSubscribed.value = false
        _mpuNotificationSubscribed.value = false
        _serviceFound.value = false
        _statusString.value = "Disconnected"
        _hardwareGpsState.value = HardwareGpsState.Unavailable
        _mpuHardwareState.value = MpuHardwareState.Unavailable
        _mpuRawString.value = "MPU6050 unavailable / disconnected"
        motionProcessor.reset()
        scheduleReconnect()
    }

    private fun scheduleReconnect() {
        if (!autoReconnectEnabled) return
        handler.removeCallbacks(reconnectRunnable)
        val delayMs = (2000L * (1 shl (reconnectAttempt.coerceAtMost(3)))).coerceIn(2000L, 15000L)
        reconnectAttempt++
        Log.d("BleManager", "BLE: scheduling reconnect in ${delayMs}ms (attempt $reconnectAttempt)")
        handler.postDelayed(reconnectRunnable, delayMs)
    }

    private fun parseStatusValue(bytes: ByteArray?): String {
        if (bytes == null || bytes.isEmpty()) return "EMPTY"
        return try {
            val str = String(bytes, Charsets.UTF_8).trim()
            if (str.isNotBlank()) str else "0x" + bytes.joinToString("") { "%02X".format(it) }
        } catch (e: Exception) {
            "0x" + bytes.joinToString("") { "%02X".format(it) }
        }
    }

    private fun parseBatteryValue(bytes: ByteArray?): String {
        if (bytes == null || bytes.isEmpty()) return "Unavailable"
        val text = try {
            String(bytes, Charsets.UTF_8).trim()
        } catch (e: Exception) { "" }

        if (text.equals("UNAVAILABLE", ignoreCase = true) || text.contains("UNAVAILABLE", ignoreCase = true)) {
            return "Unavailable"
        }
        val cleanNum = text.replace("%", "").trim().toIntOrNull()
        if (cleanNum != null) {
            return "$cleanNum%"
        }
        if (bytes.size == 1 && bytes[0].toInt() in 0..100) {
            return "${bytes[0].toInt()}%"
        }
        return if (text.isNotBlank()) text else "Unavailable"
    }

    fun isBluetoothEnabled(): Boolean {
        return try {
            bluetoothAdapter?.isEnabled == true
        } catch (e: SecurityException) {
            Log.e("BleManager", "Bluetooth permission denied", e)
            false
        }
    }

    fun startMonitoring() {
        autoReconnectEnabled = true
        reconnectAttempt = 0
        scanAndConnect()
    }

    fun scanAndConnect() {
        try {
            if (!isBluetoothEnabled()) {
                _lastErrorMessage.value = "Bluetooth is disabled"
                return
            }
            if (isScanning || _connectionState.value == BleState.CONNECTED || _connectionState.value == BleState.READY) return

            autoReconnectEnabled = true
            handler.removeCallbacks(reconnectRunnable)

            Log.d("BleManager", "BLE: scanning for ${BleProtocol.DEVICE_NAME}")

            // Stop and clean any previous GATT connection before starting fresh scan
            cleanGatt()

            _connectionState.value = BleState.SCANNING
            _lastErrorMessage.value = null
            _serviceFound.value = false
            _statusNotificationSubscribed.value = false
            _gpsNotificationSubscribed.value = false
            _notificationSubscribed.value = false
            _gpsCharacteristicFound.value = false
            isScanning = true

            val filter = ScanFilter.Builder().build()
            val settings = ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build()

            scanner?.startScan(listOf(filter), settings, scanCallback)

            handler.removeCallbacks(scanTimeoutRunnable)
            handler.postDelayed(scanTimeoutRunnable, 15000)
        } catch (e: SecurityException) {
            Log.e("BleManager", "Missing BLUETOOTH_SCAN permission", e)
            _connectionState.value = BleState.CONNECTION_FAILED
            _lastErrorMessage.value = "Bluetooth scan permission missing"
        }
    }

    fun stopScan() {
        try {
            handler.removeCallbacks(scanTimeoutRunnable)
            if (isScanning) {
                scanner?.stopScan(scanCallback)
                isScanning = false
            }
        } catch (e: SecurityException) {
            Log.e("BleManager", "Missing BLUETOOTH_SCAN permission", e)
        }
    }

    private fun connectToDevice(device: BluetoothDevice) {
        try {
            _connectionState.value = BleState.CONNECTING
            Log.d("BleManager", "BLE: connecting")
            cleanGatt()
            gatt = device.connectGatt(context, false, gattCallback)
        } catch (e: SecurityException) {
            Log.e("BleManager", "Missing BLUETOOTH_CONNECT permission", e)
            _connectionState.value = BleState.CONNECTION_FAILED
            _lastErrorMessage.value = "Bluetooth connect permission missing"
            scheduleReconnect()
        }
    }

    private fun readRssiPeriodically() {
        handler.postDelayed(object : Runnable {
            override fun run() {
                try {
                    if (_connectionState.value == BleState.CONNECTED || _connectionState.value == BleState.READY) {
                        gatt?.readRemoteRssi()
                        handler.postDelayed(this, 5000)
                    }
                } catch (e: SecurityException) {
                    Log.e("BleManager", "SecurityException reading RSSI", e)
                }
            }
        }, 2000)
    }

    private fun cleanGatt() {
        disconnectGattInternal()
    }

    private fun disconnectGattInternal() {
        try {
            gatt?.disconnect()
            gatt?.close()
        } catch (e: SecurityException) {
            Log.e("BleManager", "SecurityException closing GATT", e)
        } finally {
            gatt = null
            _rssi.value = null
        }
    }

    fun disconnect() {
        autoReconnectEnabled = false
        handler.removeCallbacks(reconnectRunnable)
        stopScan()
        cleanGatt()
        _connectionState.value = BleState.DISCONNECTED
        _batteryDisplay.value = "Unavailable"
        _batteryLevel.value = null
        _statusString.value = "Disconnected"
        _serviceFound.value = false
        _statusNotificationSubscribed.value = false
        _gpsNotificationSubscribed.value = false
        _mpuNotificationSubscribed.value = false
        _notificationSubscribed.value = false
        _hardwareGpsState.value = HardwareGpsState.Unavailable
        _mpuHardwareState.value = MpuHardwareState.Unavailable
        _mpuRawString.value = "MPU6050 unavailable / disconnected"
        motionProcessor.reset()
        Log.d("BleManager", "BLE: disconnected")
    }

    fun resetSosState() {
        _sosEvent.value = false
    }

    fun sendCommand(command: ByteArray) {
        try {
            if (_connectionState.value == BleState.CONNECTED || _connectionState.value == BleState.READY) {
                // Command sender if needed
            }
        } catch (e: SecurityException) {
            Log.e("BleManager", "Missing BLUETOOTH_CONNECT permission", e)
        }
    }

    fun sendCommand(command: String) {
        sendCommand(command.toByteArray())
    }
}
