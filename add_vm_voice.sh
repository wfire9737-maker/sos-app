cat app/src/main/java/com/example/ui/GuardianViewModel.kt | sed '/val sosSoundEnabled = _sosSoundEnabled.asStateFlow()/a \
    private val _voiceSosEnabled = MutableStateFlow(\
        try {\
            application.getSharedPreferences("smart_sos_settings", Context.MODE_PRIVATE)\
                .getBoolean("voice_sos_enabled", false)\
        } catch (e: Exception) {\
            false\
        }\
    )\
    val voiceSosEnabled = _voiceSosEnabled.asStateFlow()\
\
    private val _voiceSosPhrase = MutableStateFlow(\
        try {\
            application.getSharedPreferences("smart_sos_settings", Context.MODE_PRIVATE)\
                .getString("voice_sos_phrase", "Emergency SOS") ?: "Emergency SOS"\
        } catch (e: Exception) {\
            "Emergency SOS"\
        }\
    )\
    val voiceSosPhrase = _voiceSosPhrase.asStateFlow()' > tmp_vm_voice.kt
mv tmp_vm_voice.kt app/src/main/java/com/example/ui/GuardianViewModel.kt
