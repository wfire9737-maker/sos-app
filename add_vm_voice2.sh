cat app/src/main/java/com/example/ui/GuardianViewModel.kt | sed '/fun setCriticalAlarmsEnabled(enabled: Boolean) {/i \
    fun setVoiceSosEnabled(enabled: Boolean) {\
        _voiceSosEnabled.value = enabled\
        try {\
            application.getSharedPreferences("smart_sos_settings", Context.MODE_PRIVATE)\
                .edit()\
                .putBoolean("voice_sos_enabled", enabled)\
                .apply()\
        } catch (e: Exception) {\
            Log.e("GuardianViewModel", "Failed to save voice_sos_enabled: ${e.message}")\
        }\
    }\
\
    fun setVoiceSosPhrase(phrase: String) {\
        _voiceSosPhrase.value = phrase\
        try {\
            application.getSharedPreferences("smart_sos_settings", Context.MODE_PRIVATE)\
                .edit()\
                .putString("voice_sos_phrase", phrase)\
                .apply()\
        } catch (e: Exception) {\
            Log.e("GuardianViewModel", "Failed to save voice_sos_phrase: ${e.message}")\
        }\
    }\
' > tmp_vm_voice2.kt
mv tmp_vm_voice2.kt app/src/main/java/com/example/ui/GuardianViewModel.kt
