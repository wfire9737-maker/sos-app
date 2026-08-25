import re

with open('app/src/main/java/com/example/service/DatabaseService.kt', 'r') as f:
    content = f.read()

# I will replace the contacts listener to observe user auth state instead.
new_load_data = """
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

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
"""

old_load_data_match = re.search(r'    private fun loadData\(\) \{.*?loadLocalContacts\(\)\n        \}\n    \}', content, re.DOTALL)
if old_load_data_match:
    content = content.replace(old_load_data_match.group(0), new_load_data.strip('\n'))

with open('app/src/main/java/com/example/service/DatabaseService.kt', 'w') as f:
    f.write(content)
