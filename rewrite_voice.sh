cat app/src/main/java/com/example/service/VoiceSosService.kt | sed '/fun evaluateRecognizedText/,/        val cancelCommands = listOf(/c\
    fun evaluateRecognizedText(spokenText: String, confidence: Int, isPartial: Boolean = false) {\
        val text = spokenText.lowercase(java.util.Locale.ROOT).trim()\
        val smartSosPrefs = context.getSharedPreferences("smart_sos_settings", Context.MODE_PRIVATE)\
        val isVoiceSosEnabled = smartSosPrefs.getBoolean("voice_sos_enabled", false)\
        \
        if (isVoiceSosEnabled) {\
            val customPhrase = smartSosPrefs.getString("voice_sos_phrase", "Emergency SOS") ?: "Emergency SOS"\
            val target = customPhrase.lowercase(java.util.Locale.ROOT).trim()\
            \
            if (text.contains(target) && target.isNotBlank()) {\
                val command = VoiceCommand.Sos(customPhrase)\
                _lastRecognizedCommand.value = command\
                _speechStatusMessage.value = "Recognized Command: \"$customPhrase\" (Emergency SOS)"\
                addActivationLog(customPhrase, confidence, 85f, true)\
                onVoiceCommandRecognized?.invoke(command, confidence)\
                onVoiceSosTriggered?.invoke(customPhrase, confidence)\
                return\
            }\
        }\
\
        // 2. Cancellation Commands\
        val cancelCommands = listOf(' > tmp_voice2.kt
mv tmp_voice2.kt app/src/main/java/com/example/service/VoiceSosService.kt
