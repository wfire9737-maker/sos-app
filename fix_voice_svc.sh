cat app/src/main/java/com/example/service/VoiceSosService.kt | sed '/fun evaluateRecognizedText/c\
    fun evaluateRecognizedText(spokenText: String, confidence: Int, isPartial: Boolean = false) {\
        val smartSosPrefs = context.getSharedPreferences("smart_sos_settings", Context.MODE_PRIVATE)\
        val isVoiceSosEnabled = smartSosPrefs.getBoolean("voice_sos_enabled", false)\
        if (!isVoiceSosEnabled) return\
        val customPhrase = smartSosPrefs.getString("voice_sos_phrase", "Emergency SOS") ?: "Emergency SOS"\
        val text = spokenText.lowercase(java.util.Locale.ROOT).trim()\
        val target = customPhrase.lowercase(java.util.Locale.ROOT).trim()\
        if (text.contains(target)) {\
            val command = VoiceCommand.Sos(customPhrase)\
            _lastRecognizedCommand.value = command\
            _speechStatusMessage.value = "Recognized Command: \"$customPhrase\" (Emergency SOS)"\
            addActivationLog(customPhrase, confidence, 85f, true)\
            onVoiceCommandRecognized?.invoke(command, confidence)\
            onVoiceSosTriggered?.invoke(customPhrase, confidence)\
            return\
        }' > tmp_voice_svc.kt
