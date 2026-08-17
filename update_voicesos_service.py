import re

with open('app/src/main/java/com/example/service/VoiceSosService.kt', 'r') as f:
    content = f.read()

# Add isContinuousMode
content = content.replace('class VoiceSosService(\n    private val context: Context\n) {', 'class VoiceSosService(\n    private val context: Context\n) {\n    var isContinuousMode = false\n')

# Find onError and onResults and add restart logic
on_error_replacement = '''
                    override fun onError(error: Int) {
                        val message = when (error) {
                            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                            SpeechRecognizer.ERROR_CLIENT -> "Client error"
                            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Permission missing"
                            SpeechRecognizer.ERROR_NETWORK -> "Network error"
                            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                            SpeechRecognizer.ERROR_NO_MATCH -> "No speech recognized. Tap mic to try again."
                            SpeechRecognizer.ERROR_SERVER -> "Server recognition error"
                            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input detected."
                            else -> "Speech recognition error ($error)"
                        }
                        _speechStatusMessage.value = message
                        _isSpeechRecognizerActive.value = false
                        _voiceState.value = "LISTENING"
                        if (isContinuousMode) {
                            mainHandler.postDelayed({
                                if (isContinuousMode) startSpeechRecognition(context)
                            }, 500)
                        }
                    }
'''

on_results_replacement = '''
                    override fun onResults(results: Bundle?) {
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val text = matches?.firstOrNull() ?: ""
                        if (text.isNotBlank()) {
                            _liveSpokenText.value = text
                            evaluateRecognizedText(text, 95)
                        } else {
                            _speechStatusMessage.value = "No command recognized."
                        }
                        _isSpeechRecognizerActive.value = false
                        _voiceState.value = "LISTENING"
                        if (isContinuousMode) {
                            mainHandler.postDelayed({
                                if (isContinuousMode) startSpeechRecognition(context)
                            }, 500)
                        }
                    }
'''

content = re.sub(r'override fun onError\(error: Int\) \{.*?_voiceState\.value = "LISTENING"\s*\}', on_error_replacement.strip(), content, flags=re.DOTALL)
content = re.sub(r'override fun onResults\(results: Bundle\?\) \{.*?_voiceState\.value = "LISTENING"\s*\}', on_results_replacement.strip(), content, flags=re.DOTALL)

with open('app/src/main/java/com/example/service/VoiceSosService.kt', 'w') as f:
    f.write(content)
