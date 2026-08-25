import re

with open('app/src/main/java/com/example/service/VoiceSosService.kt', 'r') as f:
    content = f.read()

if 'import android.media.AudioManager' not in content:
    content = content.replace('import android.os.Bundle', 'import android.media.AudioManager\nimport android.os.Bundle')

# Rewrite startSpeechRecognition and stopSpeechRecognition
old_start_stop = re.search(r'    fun startSpeechRecognition\(context: Context\).*?fun evaluateRecognizedText', content, re.DOTALL).group(0)

new_start_stop = """    private var audioManager: AudioManager? = null
    private var isMuted = false

    private fun muteBeep(context: Context) {
        if (audioManager == null) {
            audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        }
        audioManager?.let { am ->
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                if (!am.isStreamMute(AudioManager.STREAM_MUSIC)) {
                    am.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_MUTE, 0)
                    isMuted = true
                }
            } else {
                @Suppress("DEPRECATION")
                am.setStreamMute(AudioManager.STREAM_MUSIC, true)
                isMuted = true
            }
        }
    }

    private fun unmuteBeep() {
        if (!isMuted) return
        audioManager?.let { am ->
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                am.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_UNMUTE, 0)
            } else {
                @Suppress("DEPRECATION")
                am.setStreamMute(AudioManager.STREAM_MUSIC, false)
            }
            isMuted = false
        }
    }

    /**
     * Start native Android SpeechRecognizer
     */
    fun startSpeechRecognition(context: Context) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            _speechStatusMessage.value = "Microphone permission required!"
            return
        }

        mainHandler.post {
            try {
                if (speechRecognizer == null) {
                    speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
                    speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                        override fun onReadyForSpeech(params: Bundle?) {
                            unmuteBeep()
                            _isSpeechRecognizerActive.value = true
                            _voiceState.value = "LISTENING"
                            _speechStatusMessage.value = "Listening for voice command... (Speak now)"
                        }
                        override fun onBeginningOfSpeech() {
                            _speechStatusMessage.value = "Listening to speech..."
                        }
                        override fun onRmsChanged(rmsdB: Float) {
                            val calculatedDb = (35f + rmsdB.coerceAtLeast(0f) * 4f).coerceIn(30f, 95f)
                            _micDecibels.value = calculatedDb
                        }
                        override fun onBufferReceived(buffer: ByteArray?) {}
                        override fun onEndOfSpeech() {
                            _voiceState.value = "PROCESSING_SPEECH"
                            _speechStatusMessage.value = "Processing command..."
                        }
                        override fun onError(error: Int) {
                            unmuteBeep()
                            val message = when (error) {
                                SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                                SpeechRecognizer.ERROR_CLIENT -> "Client error"
                                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Permission missing"
                                SpeechRecognizer.ERROR_NETWORK -> "Network error"
                                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                                SpeechRecognizer.ERROR_NO_MATCH -> "No speech recognized."
                                SpeechRecognizer.ERROR_SERVER -> "Server recognition error"
                                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input detected."
                                else -> "Speech recognition error ($error)"
                            }
                            _speechStatusMessage.value = message
                            _isSpeechRecognizerActive.value = false
                            _voiceState.value = "LISTENING"

                            if (isContinuousMode) {
                                // Do not recreate the whole service, just start listening again without a huge delay.
                                restartListening(context)
                            }
                        }
                        override fun onResults(results: Bundle?) {
                            unmuteBeep()
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
                                restartListening(context)
                            }
                        }
                        override fun onPartialResults(partialResults: Bundle?) {
                            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            val text = matches?.firstOrNull() ?: ""
                            if (text.isNotBlank()) {
                                _liveSpokenText.value = text
                                _speechStatusMessage.value = "Hearing: \"$text\""
                                evaluateRecognizedText(text, 90, isPartial = true)
                            }
                        }
                        override fun onEvent(eventType: Int, params: Bundle?) {}
                    })
                }

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                }
                
                _liveSpokenText.value = ""
                _speechStatusMessage.value = "Starting speech recognizer..."
                muteBeep(context)
                speechRecognizer?.startListening(intent)

            } catch (e: Exception) {
                unmuteBeep()
                Log.e("VoiceSosService", "Failed to start speech recognizer: ${e.message}")
                _speechStatusMessage.value = "Speech recognizer unavailable. Standard voice mode active."
                _isSpeechRecognizerActive.value = false
                
                // If it fails to start, destroy and try to recreate on next attempt
                speechRecognizer?.destroy()
                speechRecognizer = null
                
                if (isContinuousMode) {
                    mainHandler.postDelayed({
                        if (isContinuousMode) startSpeechRecognition(context)
                    }, 1000)
                }
            }
        }
    }
    
    private fun restartListening(context: Context) {
        if (!isContinuousMode) return
        mainHandler.post {
            try {
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                }
                muteBeep(context)
                speechRecognizer?.startListening(intent)
            } catch(e: Exception) {
                unmuteBeep()
                speechRecognizer?.destroy()
                speechRecognizer = null
                startSpeechRecognition(context)
            }
        }
    }

    /**
     * Stop native SpeechRecognizer
     */
    fun stopSpeechRecognition() {
        mainHandler.post {
            unmuteBeep()
            try {
                speechRecognizer?.stopListening()
                speechRecognizer?.destroy()
                speechRecognizer = null
            } catch (e: Exception) {
                Log.e("VoiceSosService", "Error stopping speech recognizer", e)
            }
            _isSpeechRecognizerActive.value = false
            _speechStatusMessage.value = "Speech listening stopped."
        }
    }

    fun evaluateRecognizedText"""

content = content.replace(old_start_stop, new_start_stop)

with open('app/src/main/java/com/example/service/VoiceSosService.kt', 'w') as f:
    f.write(content)
