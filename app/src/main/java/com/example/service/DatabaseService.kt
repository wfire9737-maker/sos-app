package com.example.service

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.model.Alert
import com.example.model.Device
import com.example.model.EmergencyContact
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

class DatabaseService(private val context: Context) {
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

    private val sharedPrefs: SharedPreferences = context.getSharedPreferences("guardian_sos_database", Context.MODE_PRIVATE)
    private var firestoreListener: ListenerRegistration? = null
    private var contactsListener: ListenerRegistration? = null

    val isDemoMode: Boolean
        get() = firestore == null

    init {
        initializeFirestore()
        loadData()
    }

    private fun initializeFirestore() {
        try {
            if (FirebaseApp.getApps(context).isNotEmpty()) {
                firestore = FirebaseFirestore.getInstance()
                Log.d("DatabaseService", "Firestore bound successfully!")
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

            // Load and Sync Contacts
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
        } else {
            // Load from persistent local JSON
            loadLocalAlerts()
            loadLocalDevices()
            loadLocalContacts()
        }
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
                fs.collection("alerts").document(alertId).set(newAlert.toMap()).await()
            } catch (e: Exception) {
                Log.e("DatabaseService", "Failed to upload SOS alert, saving locally: ${e.message}")
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
                fs.collection("alerts").document(alertId).update(
                    mapOf(
                        "status" to "RESOLVED",
                        "resolvedAt" to System.currentTimeMillis(),
                        "resolvedBy" to resolvedBy,
                        "notes" to notes
                    )
                ).await()
            } catch (e: Exception) {
                Log.e("DatabaseService", "Failed to resolve SOS on Firestore: ${e.message}")
                resolveAlertLocally(alertId, resolvedBy, notes)
            }
        } else {
            resolveAlertLocally(alertId, resolvedBy, notes)
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
                preloadDemoAlerts()
            }
        } else {
            preloadDemoAlerts()
        }
    }

    private fun preloadDemoAlerts() {
        val demoAlerts = listOf(
            Alert(
                id = "demo-alert-1",
                userId = "user-101",
                userName = "Marcus Vance",
                userPhone = "+1-555-0143",
                latitude = 37.7749,
                longitude = -122.4194,
                status = "ACTIVE",
                triggerType = "FALL_DETECTED",
                timestamp = System.currentTimeMillis() - 480000 // 8m ago
            ),
            Alert(
                id = "demo-alert-2",
                userId = "user-102",
                userName = "Sophia Martinez",
                userPhone = "+1-555-0188",
                latitude = 37.7833,
                longitude = -122.4167,
                status = "RESOLVED",
                triggerType = "ESP32_BUTTON",
                timestamp = System.currentTimeMillis() - 3600000, // 1h ago
                resolvedAt = System.currentTimeMillis() - 3000000,
                resolvedBy = "Rescue Unit A",
                notes = "False alarm, button was clicked accidentally inside handbag."
            )
        )
        saveAlertsListLocally(demoAlerts)
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
                fs.collection("devices").document(deviceId).set(newDevice.toMap()).await()
            } catch (e: Exception) {
                Log.e("DatabaseService", "Failed to save device in Firestore, saving locally: ${e.message}")
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
                fs.collection("devices").document(deviceId).delete().await()
            } catch (e: Exception) {
                Log.e("DatabaseService", "Failed to remove device from Firestore, updating locally: ${e.message}")
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
        val demoDevices = listOf(
            Device(
                deviceId = "demo-esp32-safety-band",
                userId = "user-101",
                deviceName = "Guardian Wristband ESP32",
                status = "CONNECTED",
                batteryLevel = 84,
                macAddress = "30:AE:A4:07:0D:64",
                lastSync = System.currentTimeMillis() - 60000
            )
        )
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
            deviceHealth = obj.optString("deviceHealth", "EXCELLENT")
        )
    }

    suspend fun updateDevice(device: Device): Device {
        val fs = firestore
        if (fs != null) {
            try {
                fs.collection("devices").document(device.deviceId).set(device.toMap()).await()
            } catch (e: Exception) {
                Log.e("DatabaseService", "Failed to update device on Firestore, updating locally: ${e.message}")
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
                fs.collection("contacts").document(finalContact.id).set(finalContact.toMap()).await()
            } catch (e: Exception) {
                Log.e("DatabaseService", "Failed to save contact on Firestore, saving locally: ${e.message}")
                saveContactLocally(finalContact)
            }
        } else {
            saveContactLocally(finalContact)
        }
        return finalContact
    }

    suspend fun deleteContact(contactId: String) {
        val fs = firestore
        if (fs != null) {
            try {
                fs.collection("contacts").document(contactId).delete().await()
            } catch (e: Exception) {
                Log.e("DatabaseService", "Failed to delete contact from Firestore, updating locally: ${e.message}")
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
                preloadDemoContacts()
            }
        } else {
            preloadDemoContacts()
        }
    }

    private fun preloadDemoContacts() {
        val demoContacts = listOf(
            EmergencyContact(
                id = "demo-contact-1",
                userId = "demo-uid-123",
                name = "Dispatch Center HQ",
                phone = "911",
                relationship = "First Responders",
                priority = 1,
                notes = "24/7 emergency services routing dispatch.",
                avatarEmoji = "🚨"
            ),
            EmergencyContact(
                id = "demo-contact-2",
                userId = "demo-uid-123",
                name = "Dr. Elizabeth Vance",
                phone = "+1-555-0144",
                relationship = "Primary Physician",
                priority = 2,
                notes = "Cardiologist. Medical records access code: #CARD-8491.",
                avatarEmoji = "🩺"
            ),
            EmergencyContact(
                id = "demo-contact-3",
                userId = "demo-uid-123",
                name = "Marcus Vance",
                phone = "+1-555-0143",
                relationship = "Brother",
                priority = 2,
                notes = "Primary family contact. Holds backup keys to home.",
                avatarEmoji = "🏡"
            ),
            EmergencyContact(
                id = "demo-contact-4",
                userId = "demo-uid-123",
                name = "County Search & Rescue",
                phone = "+1-555-0199",
                relationship = "Support Unit",
                priority = 3,
                notes = "Secondary contact for wilderness dispatch coordinates.",
                avatarEmoji = "🌲"
            )
        )
        saveContactsListLocally(demoContacts)
    }

    private fun saveContactLocally(contact: EmergencyContact) {
        val currentList = _contacts.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == contact.id }
        if (index >= 0) {
            currentList[index] = contact
        } else {
            currentList.add(contact)
        }
        saveContactsListLocally(currentList)
    }

    private fun removeContactLocally(contactId: String) {
        val currentList = _contacts.value.filter { it.id != contactId }
        saveContactsListLocally(currentList)
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
