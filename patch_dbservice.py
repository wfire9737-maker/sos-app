import re

with open("app/src/main/java/com/example/service/DatabaseService.kt", "r") as f:
    content = f.read()

new_state = """    // Emergency Contacts State
    private val _contacts = MutableStateFlow<List<EmergencyContact>>(emptyList())
    val contacts: StateFlow<List<EmergencyContact>> = _contacts.asStateFlow()

    // Network Simulation State
    val isOfflineMode = MutableStateFlow(false)
    val isSlowNetwork = MutableStateFlow(false)
"""
content = content.replace("    // Emergency Contacts State\n    private val _contacts = MutableStateFlow<List<EmergencyContact>>(emptyList())\n    val contacts: StateFlow<List<EmergencyContact>> = _contacts.asStateFlow()", new_state)

new_run = """    private suspend fun <T> runWithRetry(times: Int = 3, block: suspend () -> T): T {
        if (isOfflineMode.value) throw Exception("Simulated Offline Mode: Network Unavailable")
        if (isSlowNetwork.value) {
            kotlinx.coroutines.delay(2000L)
        }
        var exception: Exception? = null"""
content = content.replace("    private suspend fun <T> runWithRetry(times: Int = 3, block: suspend () -> T): T {\n        var exception: Exception? = null", new_run)

# We also need a way to Delete Test Records (where user == "test_user" or something)
delete_test_records = """
    suspend fun uploadTestSOS(): Alert {
        val testAlert = Alert(
            id = "TEST-" + UUID.randomUUID().toString(),
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
                    firestore.collection("alerts").document(doc.id).delete().await()
                }
                val list = _alerts.value.filter { a -> a.userId != "test_user" }
                _alerts.value = list
                saveAlertsListLocally(list)
            }
        }
    }
"""

content = content.replace("    private fun loadLocalAlerts() {", delete_test_records + "\n    private fun loadLocalAlerts() {")

with open("app/src/main/java/com/example/service/DatabaseService.kt", "w") as f:
    f.write(content)
