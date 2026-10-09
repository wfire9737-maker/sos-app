package com.example.service

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import javax.inject.Inject
import javax.inject.Singleton
import android.util.Log

@Singleton
class SecurityService @Inject constructor(context: Context) {
    
    private var securePrefs: SharedPreferences

    init {
        var prefs: SharedPreferences? = null
        val isRobolectric = Build.FINGERPRINT?.contains("robolectric", ignoreCase = true) == true
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            prefs = EncryptedSharedPreferences.create(
                context,
                "secure_guardian_prefs",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            if (isRobolectric) {
                Log.w("SecurityService", "Robolectric environment detected, using fallback prefs for unit tests", e)
                prefs = context.getSharedPreferences("guardian_test_prefs", Context.MODE_PRIVATE)
            } else {
                Log.e("SecurityService", "Failed to initialize EncryptedSharedPreferences securely", e)
                throw SecurityException("Failed to initialize secure storage for PIN", e)
            }
        }
        securePrefs = prefs!!
    }

    fun saveEmergencyPin(pin: String) {
        securePrefs.edit().putString("EMERGENCY_PIN", pin).apply()
    }

    fun getEmergencyPin(): String {
        return securePrefs.getString("EMERGENCY_PIN", "") ?: ""
    }

    fun verifyEmergencyPin(pin: String): Boolean {
        val expected = getEmergencyPin()
        if (expected.isEmpty()) return false
        return expected == pin
    }
}


