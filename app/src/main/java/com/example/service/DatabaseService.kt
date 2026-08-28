package com.example.service

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.model.Alert
import com.example.model.Device
import com.example.model.EmergencyContact
import com.example.model.DeveloperLog
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject

import com.example.data.local.dao.EmergencyContactDao
import com.example.data.local.entity.EmergencyContactEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class DatabaseService(private val context: Context, private val authService: AuthService? = null, private val contactDao: EmergencyContactDao? = null) {
    private var firestore: FirebaseFirestore? = null

    val firestoreInstance: FirebaseFirestore? get() = firestore

    // Alerts State
    private val _alerts = MutableStateFlow<List<Alert>>(emptyList())
    val alerts: StateFlow<List<Alert>> = _alerts.asStateFlow()

    // Connected Wearables State
    private val _devices = MutableStateFlow<List<Device>>(emptyList())
    val devices: StateFlow<List<Device>> = _devices.asStateFlow()

    // Emergency Contacts State
    private val _contacts = MutableStateFlow<List<EmergencyContact>>(emptyList())
    val contacts: StateFlow<List<EmergencyContact>> = _contacts.asStateFlow()

    // Developer Diagnostics Logs
    private val _developerLogs = MutableStateFlow<List<DeveloperLog>>(listOf(
        DeveloperLog(event = "System Diagnostics Initialized", status = "SUCCESS")
    ))
    val developerLogs: StateFlow<List<DeveloperLog>> = _developerLogs.asStateFlow()

    fun addDeveloperLog(event: String, status: String) {
        val currentLogs = _developerLogs.value.toMutableList()
        currentLogs.add(0, DeveloperLog(event = event, status = status))
        _developerLogs.value = currentLogs.take(100)
    }

    fun clearDeveloperLogs() {
        _developerLogs.value = emptyList()
    }

    // Network State
    val isOfflineMode = MutableStateFlow(false)
    val isSlowNetwork = MutableStateFlow(false)


    private val sharedPrefs: SharedPreferences = context.getSharedPreferences("guardian_sos_database", Context.MODE_PRIVATE)
    private var firestoreListener: ListenerRegistration? = null
    private var contactsListener: ListenerRegistration? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    val isDemoMode: Boolean
        get() = firestore == null

    init {
        initializeFirestore()
        loadData()
    }

    private fun initializeFirestore() {
        try {
            if (FirebaseApp.getApps(context).isNotEmpty()) {
                val fs = FirebaseFirestore.getInstance()
                val settings = com.google.firebase.firestore.FirebaseFirestoreSettings.Builder()
                    .setLocalCacheSettings(com.google.firebase.firestore.PersistentCacheSettings.newBuilder().build())
                    .build()
                fs.firestoreSettings = settings
                firestore = fs
                Log.d("DatabaseService", "Firestore bound successfully with offline persistence!")
            }
        } catch (e: Exception) {
            firestore = null
            Log.w("DatabaseService", "Firestore not available, falling back to offline SQLite/Pref model: ${e.message}")
        }
    }

    private fun loadData() {
        val fs = firestore
        if (fs != null) {
            // Setup real-time Firestore synchronization for Alerts
            firestoreListener?.remove()
            firestoreListener = fs.collection("alerts")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(100)
                .addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        Log.e("DatabaseService", "Listen failed.", e)
                        loadLocalAlerts() // Fallback
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val alertList = mutableListOf<Alert>()
                        for (doc in snapshot) {
                            alertList.add(Alert.fromMap(doc.data))
                        }
                        _alerts.value = alertList
                    }
                }

            // Load devices
            fs.collection("devices")
                .addSnapshotListener { snapshot, e ->
                    if (snapshot != null) {
                        val deviceList = mutableListOf<Device>()
                        for (doc in snapshot) {
                            deviceList.add(Device.fromMap(doc.data))
                        }
                        _devices.value = deviceList
                    }
                }

            // Sync Contacts and Settings with current User
            if (authService != null) {
                serviceScope.launch {
                    authService.authState.collect { state ->
                        contactsListener?.remove()
                        if (state is com.example.service.AuthState.Success) {
                            val uid = state.user.uid
                            contactsListener = fs.collection("users").document(uid).collection("contacts")
                                .addSnapshotListener { snapshot, e ->
                                    if (e != null) {
                                        Log.e("DatabaseService", "Contacts listen failed.", e)
                                        loadLocalContacts()
                                        return@addSnapshotListener
                                    }
                                    if (snapshot != null) {
                                        val list = mutableListOf<EmergencyContact>()
                                        for (doc in snapshot) {
                                            list.add(EmergencyContact.fromMap(doc.data))
                                        }
                                        _contacts.value = list.sortedWith(compareBy({ it.priority }, { it.name }))
                                    }
                                }
                        } else {
                            loadLocalContacts()
                        }
                    }
                }
            } else {
                contactsListener?.remove()
                contactsListener = fs.collection("contacts")
                    .addSnapshotListener { snapshot, e ->
                        if (e != null) {
                            Log.e("DatabaseService", "Contacts listen failed.", e)
                            loadLocalContacts()
                            return@addSnapshotListener
                        }
                        if (snapshot != null) {
                            val list = mutableListOf<EmergencyContact>()
                            for (doc in snapshot) {
                                list.add(EmergencyContact.fromMap(doc.data))
                            }
                            _contacts.value = list.sortedWith(compareBy({ it.priority }, { it.name }))
                        }
                    }
            }
        } else {
            // Load from persistent local JSON
            loadLocalAlerts()
            loadLocalDevices()
            loadLocalContacts()
        }
    }

    private suspend fun <T> runWithRetry(times: Int = 3, block: suspend () -> T): T {
        if (isOfflineMode.value) throw Exception("Offline Mode: Network Unavailable")
        if (isSlowNetwork.value) {
            kotlinx.coroutines.delay(2000L)
        }
        var exception: Exception? = null
        for (attempt in 1..times) {
            try {
                return block()
            } catch (e: Exception) {
                exception = e
                Log.w("DatabaseService", "Firestore operation failed (attempt $attempt/$times): ${e.message}")
                if (attempt < times) {
                    kotlinx.coroutines.delay(1000L * attempt)
                }
            }
        }
        throw exception ?: Exception("Failed database operation after $times attempts")
    }

    // --- ALERTS OPERATIONS ---

    suspend fun triggerSOS(userId: String, userName: String, userPhone: String, lat: Double, lng: Double, triggerType: String): Alert {
        val alertId = "alert-" + java.util.UUID.randomUUID().toString().take(8)
        val newAlert = Alert(
            id = alertId,
            userId = userId,
            userName = userName,
            userPhone = userPhone,
            latitude = lat,
            longitude = lng,
            status = "ACTIVE",
            triggerType = triggerType,
            timestamp = System.currentTimeMillis()
        )

        val fs = firestore
        if (fs != null) {
            try {
                runWithRetry {
                    fs.collection("alerts").document(alertId).set(newAlert.toMap()).await()
                }
            } catch (e: Exception) {
                Log.e("DatabaseService", "Failed to upload SOS alert after multiple attempts, saving locally: ${e.message}")
                saveAlertLocally(newAlert)
            }
        } else {
            saveAlertLocally(newAlert)
        }
        return newAlert
    }

    suspend fun resolveSOS(alertId: String, resolvedBy: String, notes: String) {
        val fs = firestore
        if (fs != null) {
            try {
                runWithRetry {
                    fs.collection("alerts").document(alertId).update(
                        mapOf(
                            "status" to "RESOLVED",
                            "resolvedAt" to System.currentTimeMillis(),
                            "resolvedBy" to resolvedBy,
                            "notes" to notes
                        )
                    ).await()
                }
            } catch (e: Exception) {
                Log.e("DatabaseService", "Failed to resolve SOS on Firestore after multiple attempts: ${e.message}")
                resolveAlertLocally(alertId, resolvedBy, notes)
            }
        } else {
            resolveAlertLocally(alertId, resolvedBy, notes)
        }
    }


    suspend fun uploadTestSOS(): Alert {
        val testAlert = Alert(
            id = "TEST-" + java.util.UUID.randomUUID().toString(),
            userId = "test_user",
            userName = "Test User",
            userPhone = "555-0000",
            latitude = 40.7128,
            longitude = -74.0060,
            status = "ACTIVE",
            triggerType = "MANUAL_TEST",
            timestamp = System.currentTimeMillis()
        )
        return runWithRetry {
            firestore?.collection("alerts")?.document(testAlert.id)?.set(testAlert.toMap())?.await()
            val list = _alerts.value.toMutableList()
            list.add(0, testAlert)
            _alerts.value = list
            saveAlertsListLocally(list)
            testAlert
        }
    }

    suspend fun downloadTestData() {
        runWithRetry {
            val result = firestore?.collection("alerts")?.whereEqualTo("userId", "test_user")?.get()?.await()
            result?.let {
                val list = _alerts.value.toMutableList()
                for (doc in it.documents) {
                    val alert = Alert.fromMap(doc.data ?: emptyMap())
                    if (list.none { existing -> existing.id == alert.id }) {
                        list.add(alert)
                    }
                }
                _alerts.value = list.sortedByDescending { a -> a.timestamp }
                saveAlertsListLocally(_alerts.value)
            }
        }
    }

    suspend fun deleteTestRecords() {
        runWithRetry {
            val result = firestore?.collection("alerts")?.whereEqualTo("userId", "test_user")?.get()?.await()
            result?.let {
                for (doc in it.documents) {
                    firestore?.collection("alerts")?.document(doc.id)?.delete()?.await()
                }
                val list = _alerts.value.filter { a -> a.userId != "test_user" }
                _alerts.value = list
                saveAlertsListLocally(list)
            }
        }
    }

    private fun loadLocalAlerts() {
        val alertsJson = sharedPrefs.getString("alerts_list", null)
        if (alertsJson != null) {
            try {
                val array = JSONArray(alertsJson)
                val alertList = mutableListOf<Alert>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    alertList.add(alertFromJsonObject(obj))
                }
                _alerts.value = alertList
            } catch (e: Exception) {
                Log.e("DatabaseService", "Error parsing cached alerts", e)
                _alerts.value = emptyList()
            }
        } else {
            _alerts.value = emptyList()
        }
    }

    

    private fun saveAlertLocally(alert: Alert) {
        val currentList = _alerts.value.toMutableList()
        // Check if exists, replace, or insert at head
        val index = currentList.indexOfFirst { it.id == alert.id }
        if (index >= 0) {
            currentList[index] = alert
        } else {
            currentList.add(0, alert)
        }
        saveAlertsListLocally(currentList)
    }

    private fun resolveAlertLocally(alertId: String, resolvedBy: String, notes: String) {
        val currentList = _alerts.value.map {
            if (it.id == alertId) {
                it.copy(
                    status = "RESOLVED",
                    resolvedAt = System.currentTimeMillis(),
                    resolvedBy = resolvedBy,
                    notes = notes
                )
            } else {
                it
            }
        }
        saveAlertsListLocally(currentList)
    }

    private fun saveAlertsListLocally(list: List<Alert>) {
        _alerts.value = list
        try {
            val array = JSONArray()
            for (alert in list) {
                array.put(alertToJsonObject(alert))
            }
            sharedPrefs.edit().putString("alerts_list", array.toString()).apply()
        } catch (e: Exception) {
            Log.e("DatabaseService", "Failed to cache alerts list", e)
        }
    }

    // --- DEVICES OPERATIONS (ESP32) ---

    suspend fun registerDevice(userId: String, name: String, mac: String): Device {
        val deviceId = "esp32-" + java.util.UUID.randomUUID().toString().take(8)
        val newDevice = Device(
            deviceId = deviceId,
            userId = userId,
            deviceName = name,
            status = "CONNECTED",
            batteryLevel = 98,
            macAddress = mac,
            lastSync = System.currentTimeMillis()
        )

        val fs = firestore
        if (fs != null) {
            try {
                runWithRetry {
                    fs.collection("devices").document(deviceId).set(newDevice.toMap()).await()
                }
            } catch (e: Exception) {
                Log.e("DatabaseService", "Failed to save device in Firestore after multiple attempts, saving locally: ${e.message}")
                saveDeviceLocally(newDevice)
            }
        } else {
            saveDeviceLocally(newDevice)
        }
        return newDevice
    }

    suspend fun deleteDevice(deviceId: String) {
        val fs = firestore
        if (fs != null) {
            try {
                runWithRetry {
                    fs.collection("devices").document(deviceId).delete().await()
                }
            } catch (e: Exception) {
                Log.e("DatabaseService", "Failed to remove device from Firestore after multiple attempts, updating locally: ${e.message}")
                removeDeviceLocally(deviceId)
            }
        } else {
            removeDeviceLocally(deviceId)
        }
    }

    private fun loadLocalDevices() {
        val devicesJson = sharedPrefs.getString("devices_list", null)
        if (devicesJson != null) {
            try {
                val array = JSONArray(devicesJson)
                val deviceList = mutableListOf<Device>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    deviceList.add(deviceFromJsonObject(obj))
                }
                _devices.value = deviceList
            } catch (e: Exception) {
                preloadDemoDevices()
            }
        } else {
            preloadDemoDevices()
        }
    }

    private fun preloadDemoDevices() {
        val demoDevices = emptyList<Device>()
        saveDevicesListLocally(demoDevices)
    }

    private fun saveDeviceLocally(device: Device) {
        val currentList = _devices.value.toMutableList()
        val index = currentList.indexOfFirst { it.deviceId == device.deviceId }
        if (index >= 0) {
            currentList[index] = device
        } else {
            currentList.add(device)
        }
        saveDevicesListLocally(currentList)
    }

    private fun removeDeviceLocally(deviceId: String) {
        val currentList = _devices.value.filter { it.deviceId != deviceId }
        saveDevicesListLocally(currentList)
    }

    private fun saveDevicesListLocally(list: List<Device>) {
        _devices.value = list
        try {
            val array = JSONArray()
            for (device in list) {
                array.put(deviceToJsonObject(device))
            }
            sharedPrefs.edit().putString("devices_list", array.toString()).apply()
        } catch (e: Exception) {
            Log.e("DatabaseService", "Failed to cache devices list", e)
        }
    }

    // --- JSON PARSING HELPERS ---

    private fun alertToJsonObject(alert: Alert): JSONObject {
        val obj = JSONObject()
        obj.put("id", alert.id)
        obj.put("userId", alert.userId)
        obj.put("userName", alert.userName)
        obj.put("userPhone", alert.userPhone)
        obj.put("latitude", alert.latitude)
        obj.put("longitude", alert.longitude)
        obj.put("status", alert.status)
        obj.put("triggerType", alert.triggerType)
        obj.put("timestamp", alert.timestamp)
        obj.put("resolvedAt", alert.resolvedAt)
        obj.put("resolvedBy", alert.resolvedBy)
        obj.put("notes", alert.notes)
        return obj
    }

    private fun alertFromJsonObject(obj: JSONObject): Alert {
        return Alert(
            id = obj.optString("id"),
            userId = obj.optString("userId"),
            userName = obj.optString("userName"),
            userPhone = obj.optString("userPhone"),
            latitude = obj.optDouble("latitude", 0.0),
            longitude = obj.optDouble("longitude", 0.0),
            status = obj.optString("status", "ACTIVE"),
            triggerType = obj.optString("triggerType", "MANUAL"),
            timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
            resolvedAt = obj.optLong("resolvedAt", 0L),
            resolvedBy = obj.optString("resolvedBy"),
            notes = obj.optString("notes")
        )
    }

    private fun deviceToJsonObject(device: Device): JSONObject {
        val obj = JSONObject()
        obj.put("deviceId", device.deviceId)
        obj.put("userId", device.userId)
        obj.put("deviceName", device.deviceName)
        obj.put("status", device.status)
        obj.put("batteryLevel", device.batteryLevel)
        obj.put("macAddress", device.macAddress)
        obj.put("lastSync", device.lastSync)
        obj.put("firmwareVersion", device.firmwareVersion)
        obj.put("signalStrength", device.signalStrength)
        obj.put("deviceHealth", device.deviceHealth)
        
        // New Device Monitoring Fields:
        obj.put("isCharging", device.isCharging)
        obj.put("wifiSignal", device.wifiSignal)
        obj.put("bluetoothStatus", device.bluetoothStatus)
        obj.put("gpsStatus", device.gpsStatus)
        obj.put("deviceTemperature", device.deviceTemperature.toDouble())
        obj.put("uptimeSeconds", device.uptimeSeconds)
        obj.put("memoryUsagePercent", device.memoryUsagePercent)
        obj.put("cpuUsagePercent", device.cpuUsagePercent)
        obj.put("healthScore", device.healthScore)
        obj.put("connectionStatus", device.connectionStatus)

        // GPS & MPU6050:
        obj.put("latitude", device.latitude)
        obj.put("longitude", device.longitude)
        obj.put("accelX", device.accelX.toDouble())
        obj.put("accelY", device.accelY.toDouble())
        obj.put("accelZ", device.accelZ.toDouble())
        obj.put("gyroX", device.gyroX.toDouble())
        obj.put("gyroY", device.gyroY.toDouble())
        obj.put("gyroZ", device.gyroZ.toDouble())
        return obj
    }

    private fun deviceFromJsonObject(obj: JSONObject): Device {
        return Device(
            deviceId = obj.optString("deviceId"),
            userId = obj.optString("userId"),
            deviceName = obj.optString("deviceName", "Guardian Band v1"),
            status = obj.optString("status", "DISCONNECTED"),
            batteryLevel = obj.optInt("batteryLevel", 100),
            macAddress = obj.optString("macAddress", "00:00:00:00:00:00"),
            lastSync = obj.optLong("lastSync", System.currentTimeMillis()),
            firmwareVersion = obj.optString("firmwareVersion", "v1.2.4-esp32"),
            signalStrength = obj.optInt("signalStrength", -67),
            deviceHealth = obj.optString("deviceHealth", "EXCELLENT"),
            
            // New Device Monitoring Fields:
            isCharging = obj.optBoolean("isCharging", false),
            wifiSignal = obj.optInt("wifiSignal", -55),
            bluetoothStatus = obj.optString("bluetoothStatus", "CONNECTED"),
            gpsStatus = obj.optString("gpsStatus", "LOCKED"),
            deviceTemperature = obj.optDouble("deviceTemperature", 36.5).toFloat(),
            uptimeSeconds = obj.optLong("uptimeSeconds", 3600L),
            memoryUsagePercent = obj.optInt("memoryUsagePercent", 42),
            cpuUsagePercent = obj.optInt("cpuUsagePercent", 18),
            healthScore = obj.optInt("healthScore", 98),
            connectionStatus = obj.optString("connectionStatus", "ONLINE"),

            // GPS & MPU6050:
            latitude = obj.optDouble("latitude", 0.0),
            longitude = obj.optDouble("longitude", 0.0),
            accelX = obj.optDouble("accelX", 0.05).toFloat(),
            accelY = obj.optDouble("accelY", -0.02).toFloat(),
            accelZ = obj.optDouble("accelZ", 0.98).toFloat(),
            gyroX = obj.optDouble("gyroX", 0.1).toFloat(),
            gyroY = obj.optDouble("gyroY", -0.1).toFloat(),
            gyroZ = obj.optDouble("gyroZ", 0.2).toFloat()
        )
    }

    suspend fun updateDevice(device: Device): Device {
        val fs = firestore
        if (fs != null) {
            try {
                runWithRetry {
                    fs.collection("devices").document(device.deviceId).set(device.toMap()).await()
                }
            } catch (e: Exception) {
                Log.e("DatabaseService", "Failed to update device on Firestore after multiple attempts, updating locally: ${e.message}")
                saveDeviceLocally(device)
            }
        } else {
            saveDeviceLocally(device)
        }
        return device
    }

    suspend fun renameDevice(deviceId: String, newName: String) {
        val currentDevice = _devices.value.find { it.deviceId == deviceId }
        if (currentDevice != null) {
            val updated = currentDevice.copy(deviceName = newName)
            updateDevice(updated)
        }
    }

    // --- EMERGENCY CONTACTS OPERATIONS ---

    suspend fun saveContact(contact: EmergencyContact): EmergencyContact {
        val finalContact = if (contact.id.isBlank()) {
            contact.copy(id = "contact-" + java.util.UUID.randomUUID().toString().take(8))
        } else {
            contact
        }

        val fs = firestore
        if (fs != null) {
            try {
                runWithRetry {
                    var documentRef = fs.collection("contacts").document(finalContact.id)
                    if (authService != null) {
                        val state = authService.authState.value
                        if (state is com.example.service.AuthState.Success) {
                            documentRef = fs.collection("users").document(state.user.uid)
                                .collection("contacts").document(finalContact.id)
                        }
                    }
                    documentRef.set(finalContact.toMap()).await()
                }
            } catch (e: Exception) {
                Log.e("DatabaseService", "Failed to save contact on Firestore after multiple attempts, saving locally: ${e.message}")
                saveContactLocally(finalContact)
            }
        } else {
            saveContactLocally(finalContact)
        }

        return finalContact
    }

        // --- USER SETTINGS OPERATIONS ---
    fun saveUserSetting(key: String, value: Any) {
        val fs = firestore
        val authState = authService?.authState?.value
        
        // Save locally first
        try {
            val prefs = context.getSharedPreferences("smart_sos_settings", Context.MODE_PRIVATE).edit()
            when (value) {
                is Boolean -> prefs.putBoolean(key, value)
                is String -> prefs.putString(key, value)
                is Int -> prefs.putInt(key, value)
                is Float -> prefs.putFloat(key, value)
                is Long -> prefs.putLong(key, value)
            }
            prefs.apply()
        } catch (e: Exception) {
            Log.e("DatabaseService", "Failed to save setting locally: ${e.message}")
        }
        
        // Save to Firestore
        if (fs != null && authState is com.example.service.AuthState.Success) {
            serviceScope.launch {
                try {
                    val updates = mapOf(key to value)
                    fs.collection("users").document(authState.user.uid)
                        .collection("settings").document("preferences")
                        .set(updates, com.google.firebase.firestore.SetOptions.merge())
                        .await()
                } catch (e: Exception) {
                    Log.e("DatabaseService", "Failed to save setting to Firestore: ${e.message}")
                }
            }
        }
    }

    suspend fun deleteContact(contactId: String) {
        val fs = firestore
        if (fs != null) {
            try {
                runWithRetry {
                    var documentRef = fs.collection("contacts").document(contactId)
                    if (authService != null) {
                        val state = authService.authState.value
                        if (state is com.example.service.AuthState.Success) {
                            documentRef = fs.collection("users").document(state.user.uid)
                                .collection("contacts").document(contactId)
                        }
                    }
                    documentRef.delete().await()
                }
            } catch (e: Exception) {
                Log.e("DatabaseService", "Failed to delete contact from Firestore after multiple attempts, updating locally: ${e.message}")
                removeContactLocally(contactId)
            }
        } else {
            removeContactLocally(contactId)
        }
    }

    private fun loadLocalContacts() {
        val contactsJson = sharedPrefs.getString("contacts_list", null)
        if (contactsJson != null) {
            try {
                val array = JSONArray(contactsJson)
                val list = mutableListOf<EmergencyContact>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(EmergencyContact.fromJsonObject(obj))
                }
                _contacts.value = list.sortedWith(compareBy({ it.priority }, { it.name }))
            } catch (e: Exception) {
                _contacts.value = emptyList()
            }
        } else {
            _contacts.value = emptyList()
        }
    }

    

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private fun saveContactLocally(contact: EmergencyContact) {
        val currentList = _contacts.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == contact.id }
        if (index >= 0) {
            currentList[index] = contact
        } else {
            currentList.add(contact)
        }
        saveContactsListLocally(currentList)
        
        // Save to Room
        scope.launch {
            try {
                val entity = EmergencyContactEntity(
                    contactId = contact.id,
                    uid = contact.userId,
                    name = contact.name,
                    phone = contact.phone,
                    relationship = contact.relationship,
                    priority = contact.priority
                )
                contactDao?.insertContact(entity)
            } catch (e: Exception) {
                Log.e("DatabaseService", "Failed to save contact to Room", e)
            }
        }
    }

    private fun removeContactLocally(contactId: String) {
        val currentList = _contacts.value.filter { it.id != contactId }
        saveContactsListLocally(currentList)
        
        scope.launch {
            try {
                contactDao?.deleteContact(contactId)
            } catch (e: Exception) {
                Log.e("DatabaseService", "Failed to delete contact from Room", e)
            }
        }
    }

    private fun saveContactsListLocally(list: List<EmergencyContact>) {
        val sorted = list.sortedWith(compareBy({ it.priority }, { it.name }))
        _contacts.value = sorted
        try {
            val array = JSONArray()
            for (contact in sorted) {
                array.put(contact.toJsonObject())
            }
            sharedPrefs.edit().putString("contacts_list", array.toString()).apply()
        } catch (e: Exception) {
            Log.e("DatabaseService", "Failed to cache contacts list", e)
        }
    }
}
