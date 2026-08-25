import re

with open('app/src/main/java/com/example/service/VoiceSosService.kt', 'r') as f:
    content = f.read()

# I will rewrite evaluateRecognizedText
old_eval = re.search(r'    fun evaluateRecognizedText\(spokenText: String.*?(?=    fun processVoiceInput)', content, re.DOTALL).group(0)

new_eval = """    fun evaluateRecognizedText(spokenText: String, confidence: Int, isPartial: Boolean = false) {
        val text = spokenText.lowercase(java.util.Locale.ROOT).trim()
        val smartSosPrefs = context.getSharedPreferences("smart_sos_settings", Context.MODE_PRIVATE)
        val isVoiceSosEnabled = smartSosPrefs.getBoolean("voice_sos_enabled", false)
        
        if (isVoiceSosEnabled) {
            // Check all wake phrases
            val currentPhrases = _wakePhrases.value
            val matchedPhrase = currentPhrases.firstOrNull { text.contains(it.lowercase(java.util.Locale.ROOT).trim()) && it.isNotBlank() }
            
            if (matchedPhrase != null) {
                // If it's a stop command
                if (matchedPhrase.lowercase().contains("stop") || matchedPhrase.lowercase().contains("cancel")) {
                    val command = VoiceCommand.CancelSos(matchedPhrase)
                    _lastRecognizedCommand.value = command
                    _speechStatusMessage.value = "Recognized Command: \\"$matchedPhrase\\" (Emergency Cancelled)"
                    addActivationLog(matchedPhrase, confidence, 70f, true)
                    onVoiceCommandRecognized?.invoke(command, confidence)
                    return
                } else if (matchedPhrase.lowercase().contains("track location")) {
                    val command = VoiceCommand.TrackLocation(matchedPhrase)
                    _lastRecognizedCommand.value = command
                    _speechStatusMessage.value = "Recognized Command: \\"$matchedPhrase\\" (Location Tracking Started)"
                    addActivationLog(matchedPhrase, confidence, 65f, true)
                    onVoiceCommandRecognized?.invoke(command, confidence)
                    return
                } else {
                    val command = VoiceCommand.Sos(matchedPhrase)
                    _lastRecognizedCommand.value = command
                    _speechStatusMessage.value = "Recognized Command: \\"$matchedPhrase\\" (Emergency SOS)"
                    addActivationLog(matchedPhrase, confidence, 85f, true)
                    onVoiceCommandRecognized?.invoke(command, confidence)
                    onVoiceSosTriggered?.invoke(matchedPhrase, confidence)
                    return
                }
            }
        }

        // 4. Unknown / No direct action match
        if (!isPartial) {
            val command = VoiceCommand.Unknown(spokenText)
            _lastRecognizedCommand.value = command
            _speechStatusMessage.value = "Recognized: \\"$spokenText\\""
            addActivationLog(spokenText, confidence, 50f, false)
            onVoiceCommandRecognized?.invoke(command, confidence)
        }
    }
"""

content = content.replace(old_eval, new_eval)

with open('app/src/main/java/com/example/service/VoiceSosService.kt', 'w') as f:
    f.write(content)
