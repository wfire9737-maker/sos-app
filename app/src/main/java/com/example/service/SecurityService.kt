package com.example.service

import android.content.Context
import android.content.SharedPreferences
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
            Log.e("SecurityService", "Failed to initialize EncryptedSharedPreferences, falling back to standard prefs", e)
            prefs = context.getSharedPreferences("guardian_fallback_prefs", Context.MODE_PRIVATE)
        }
        securePrefs = prefs!!
    }

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
