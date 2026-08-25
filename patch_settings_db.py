import re

with open('app/src/main/java/com/example/service/DatabaseService.kt', 'r') as f:
    content = f.read()

# I will add a saveUserSetting function
save_setting_func = """
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
"""

if 'fun saveUserSetting' not in content:
    content = content.replace('suspend fun deleteContact', save_setting_func.strip('\n') + '\n\n    suspend fun deleteContact')

# Now add a settings listener in loadData
settings_listener = """
                            fs.collection("users").document(uid).collection("settings").document("preferences")
                                .addSnapshotListener { snapshot, e ->
                                    if (e != null) return@addSnapshotListener
                                    if (snapshot != null && snapshot.exists()) {
                                        val prefs = context.getSharedPreferences("smart_sos_settings", Context.MODE_PRIVATE).edit()
                                        for ((key, value) in snapshot.data ?: emptyMap()) {
                                            when (value) {
                                                is Boolean -> prefs.putBoolean(key, value)
                                                is String -> prefs.putString(key, value)
                                                is Long -> prefs.putInt(key, value.toInt())
                                                is Double -> prefs.putFloat(key, value.toFloat())
                                            }
                                        }
                                        prefs.apply()
                                    }
                                }
"""

if 'collection("settings").document("preferences")' not in content:
    content = content.replace(
        'contactsListener = fs.collection("users").document(uid).collection("contacts")',
        settings_listener.strip('\n') + '\n\n                            contactsListener = fs.collection("users").document(uid).collection("contacts")'
    )

with open('app/src/main/java/com/example/service/DatabaseService.kt', 'w') as f:
    f.write(content)
