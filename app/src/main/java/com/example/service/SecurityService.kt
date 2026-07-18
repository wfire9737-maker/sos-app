package com.example.service

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecurityService @Inject constructor(context: Context) {
    
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val securePrefs = EncryptedSharedPreferences.create(
        context,
        "secure_guardian_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun saveEmergencyPin(pin: String) {
        securePrefs.edit().putString("EMERGENCY_PIN", pin).apply()
    }

    fun getEmergencyPin(): String {
        return securePrefs.getString("EMERGENCY_PIN", "9999") ?: "9999"
    }

    fun verifyEmergencyPin(pin: String): Boolean {
        return getEmergencyPin() == pin
    }
}
