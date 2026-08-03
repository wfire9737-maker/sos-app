package com.example.service

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import java.util.Locale
import java.util.UUID

data class VoiceActivationLog(
    val id: String = UUID.randomUUID().toString(),
    val phrase: String,
    val confidence: Int,
    val noiseFilteredDb: Float,
    val isActivated: Boolean,
    val timestampMs: Long = System.currentTimeMillis()
)

sealed class VoiceCommand(val commandName: String) {
    data class Sos(val matchedPhrase: String) : VoiceCommand("SOS")
    data class CancelSos(val matchedPhrase: String) : VoiceCommand("CANCEL_SOS")
    data class TrackLocation(val matchedPhrase: String) : VoiceCommand("TRACK_LOCATION")
    data class Unknown(val spokenText: String) : VoiceCommand("UNKNOWN")
}

class VoiceSosService(
    private val context: Context
) {
    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val mainHandler = Handler(Looper.getMainLooper())
    private val sharedPrefs: SharedPreferences = context.getSharedPreferences("guardian_sos_voice", Context.MODE_PRIVATE)

    // Speech states
    private val _isListening = MutableStateFlow(true)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _voiceState = MutableStateFlow("LISTENING") // "LISTENING", "PROCESSING_SPEECH", "MATCH_FOUND", "THRESHOLD_REJECTED"
    val voiceState: StateFlow<String> = _voiceState.asStateFlow()

    // Native Speech Recognizer States
    private val _isSpeechRecognizerActive = MutableStateFlow(false)
    val isSpeechRecognizerActive: StateFlow<Boolean> = _isSpeechRecognizerActive.asStateFlow()

    private val _liveSpokenText = MutableStateFlow("")
    val liveSpokenText: StateFlow<String> = _liveSpokenText.asStateFlow()

    private val _speechStatusMessage = MutableStateFlow("Tap microphone to start listening")
    val speechStatusMessage: StateFlow<String> = _speechStatusMessage.asStateFlow()

    private val _lastRecognizedCommand = MutableStateFlow<VoiceCommand?>(null)
    val lastRecognizedCommand: StateFlow<VoiceCommand?> = _lastRecognizedCommand.asStateFlow()

    // Configurable Wake Phrases
    private val _wakePhrases = MutableStateFlow<List<String>>(
        listOf("Help", "Emergency", "SOS", "Send SOS", "Call for help", "I'm in danger", "Track my location", "Stop SOS", "Cancel SOS")
    )
    val wakePhrases: StateFlow<List<String>> = _wakePhrases.asStateFlow()

    // Live Decibel Level
    private val _micDecibels = MutableStateFlow(42f)
    val micDecibels: StateFlow<Float> = _micDecibels.asStateFlow()

    // Configurable trigger confidence threshold
    private val _confidenceThreshold = MutableStateFlow(75)
    val confidenceThreshold: StateFlow<Int> = _confidenceThreshold.asStateFlow()

    // Log list of all voice detection attempts
    private val _activationLogs = MutableStateFlow<List<VoiceActivationLog>>(emptyList())
    val activationLogs: StateFlow<List<VoiceActivationLog>> = _activationLogs.asStateFlow()

    // Callbacks
    var onVoiceSosTriggered: ((String, Int) -> Unit)? = null
    var onVoiceCommandRecognized: ((VoiceCommand, Int) -> Unit)? = null

    private var speechRecognizer: SpeechRecognizer? = null
    private var micPollerJob: Job? = null

    init {
        loadPhrasesAndLogs()
        startMicLevelSimulation()
    }

    private fun loadPhrasesAndLogs() {
        val defaultPhrases = listOf(
            "Help", "Emergency", "SOS", "Send SOS", "Call for help",
            "I'm in danger", "Track my location", "Stop SOS", "Cancel SOS"
        )
        val phrasesJson = sharedPrefs.getString("wake_phrases", null)
        if (phrasesJson != null) {
            try {
                val arr = JSONArray(phrasesJson)
                val list = mutableListOf<String>()
                for (i in 0 until arr.length()) {
                    list.add(arr.getString(i))
                }
                defaultPhrases.forEach { p ->
                    if (!list.contains(p)) list.add(p)
                }
                _wakePhrases.value = list
            } catch (e: Exception) {
                _wakePhrases.value = defaultPhrases
            }
        } else {
            _wakePhrases.value = defaultPhrases
        }

        val logsJson = sharedPrefs.getString("activation_logs", "[]") ?: "[]"
        try {
            val arr = JSONArray(logsJson)
            val list = mutableListOf<VoiceActivationLog>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    VoiceActivationLog(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        phrase = obj.optString("phrase", ""),
                        confidence = obj.optInt("confidence", 0),
                        noiseFilteredDb = obj.optDouble("db", 0.0).toFloat(),
                        isActivated = obj.optBoolean("isActivated", false),
                        timestampMs = obj.optLong("time", System.currentTimeMillis())
                    )
                )
            }
            _activationLogs.value = list
        } catch (e: Exception) {
            Log.e("VoiceSosService", "Error parsing activation logs", e)
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
                }

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                }

                speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
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
                    }

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

                _liveSpokenText.value = ""
                _speechStatusMessage.value = "Starting speech recognizer..."
                speechRecognizer?.startListening(intent)
            } catch (e: Exception) {
                Log.e("VoiceSosService", "Failed to start speech recognizer: ${e.message}")
                _speechStatusMessage.value = "Speech recognizer unavailable. Standard voice mode active."
                _isSpeechRecognizerActive.value = false
            }
        }
    }

    /**
     * Stop native SpeechRecognizer
     */
    fun stopSpeechRecognition() {
        mainHandler.post {
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

    /**
     * Evaluate recognized spoken text against predefined commands
     */
    fun evaluateRecognizedText(spokenText: String, confidence: Int, isPartial: Boolean = false) {
        val text = spokenText.lowercase(Locale.ROOT).trim()

        // 1. SOS Emergency Commands
        val sosCommands = listOf(
            "help", "emergency", "sos", "send sos", "call for help",
            "i'm in danger", "im in danger", "i am in danger", "in danger"
        )
        val matchedSos = sosCommands.firstOrNull { text.contains(it) }

        if (matchedSos != null) {
            val matchedPhraseClean = when {
                text.contains("send sos") -> "Send SOS"
                text.contains("call for help") -> "Call for help"
                text.contains("in danger") -> "I'm in danger"
                text.contains("emergency") -> "Emergency"
                text.contains("help") -> "Help"
                else -> "SOS"
            }
            val command = VoiceCommand.Sos(matchedPhraseClean)
            _lastRecognizedCommand.value = command
            _speechStatusMessage.value = "Recognized Command: \"$matchedPhraseClean\" (Emergency SOS)"
            addActivationLog(matchedPhraseClean, confidence, 85f, true)

            onVoiceCommandRecognized?.invoke(command, confidence)
            onVoiceSosTriggered?.invoke(matchedPhraseClean, confidence)
            return
        }

        // 2. Cancellation Commands
        val cancelCommands = listOf(
            "stop sos", "cancel sos", "stop emergency", "cancel emergency"
        )
        val matchedCancel = cancelCommands.firstOrNull { text.contains(it) }

        if (matchedCancel != null) {
            val matchedPhraseClean = if (text.contains("stop")) "Stop SOS" else "Cancel SOS"
            val command = VoiceCommand.CancelSos(matchedPhraseClean)
            _lastRecognizedCommand.value = command
            _speechStatusMessage.value = "Recognized Command: \"$matchedPhraseClean\" (Emergency Cancelled)"
            addActivationLog(matchedPhraseClean, confidence, 70f, true)

            onVoiceCommandRecognized?.invoke(command, confidence)
            return
        }

        // 3. Location Tracking Command
        val trackCommands = listOf(
            "track my location", "track location", "start location tracking", "track me"
        )
        val matchedTrack = trackCommands.firstOrNull { text.contains(it) }

        if (matchedTrack != null) {
            val matchedPhraseClean = "Track my location"
            val command = VoiceCommand.TrackLocation(matchedPhraseClean)
            _lastRecognizedCommand.value = command
            _speechStatusMessage.value = "Recognized Command: \"$matchedPhraseClean\" (Location Tracking Started)"
            addActivationLog(matchedPhraseClean, confidence, 65f, true)

            onVoiceCommandRecognized?.invoke(command, confidence)
            return
        }

        // 4. Unknown / No direct action match
        if (!isPartial) {
            val command = VoiceCommand.Unknown(spokenText)
            _lastRecognizedCommand.value = command
            _speechStatusMessage.value = "Recognized: \"$spokenText\""
            addActivationLog(spokenText, confidence, 50f, false)
            onVoiceCommandRecognized?.invoke(command, confidence)
        }
    }

    /**
     * Process voice input for simulation or external voice pipeline
     */
    fun processVoiceInput(spokenText: String, inputConfidence: Int) {
        if (!_isListening.value) return

        serviceScope.launch {
            _voiceState.value = "PROCESSING_SPEECH"
            delay(400)
            _liveSpokenText.value = spokenText
            evaluateRecognizedText(spokenText, inputConfidence)
            _voiceState.value = "LISTENING"
        }
    }

    private fun startMicLevelSimulation() {
        micPollerJob?.cancel()
        micPollerJob = serviceScope.launch {
            while (isActive) {
                if (_isListening.value || _isSpeechRecognizerActive.value) {
                    val base = 40f
                    val variance = (Math.sin(System.currentTimeMillis() * 0.002) * 12).toFloat()
                    val randomJitter = (Math.random() * 4 - 2).toFloat()
                    _micDecibels.value = base + variance + randomJitter
                }
                delay(120)
            }
        }
    }

    fun setConfidenceThreshold(threshold: Int) {
        _confidenceThreshold.value = threshold
        sharedPrefs.edit().putInt("confidence_threshold", threshold).apply()
    }

    fun addWakePhrase(phrase: String): Boolean {
        val trimmed = phrase.trim()
        if (trimmed.isEmpty()) return false
        val current = _wakePhrases.value.toMutableList()
        if (!current.contains(trimmed)) {
            current.add(trimmed)
            _wakePhrases.value = current
            savePhrases()
            return true
        }
        return false
    }

    fun removeWakePhrase(phrase: String) {
        val current = _wakePhrases.value.toMutableList()
        if (current.remove(phrase)) {
            _wakePhrases.value = current
            savePhrases()
        }
    }

    fun toggleListening(enabled: Boolean) {
        _isListening.value = enabled
        _voiceState.value = if (enabled) "LISTENING" else "DISABLED"
    }

    private fun savePhrases() {
        try {
            val arr = JSONArray(_wakePhrases.value)
            sharedPrefs.edit().putString("wake_phrases", arr.toString()).apply()
        } catch (e: Exception) {
            Log.e("VoiceSosService", "Error saving phrases", e)
        }
    }

    private fun saveLogs() {
        try {
            val arr = JSONArray()
            _activationLogs.value.forEach { log ->
                val obj = org.json.JSONObject()
                obj.put("id", log.id)
                obj.put("phrase", log.phrase)
                obj.put("confidence", log.confidence)
                obj.put("db", log.noiseFilteredDb)
                obj.put("isActivated", log.isActivated)
                obj.put("time", log.timestampMs)
                arr.put(obj)
            }
            sharedPrefs.edit().putString("activation_logs", arr.toString()).apply()
        } catch (e: Exception) {
            Log.e("VoiceSosService", "Error saving logs", e)
        }
    }

    private fun addActivationLog(phrase: String, confidence: Int, db: Float, activated: Boolean) {
        val newLog = VoiceActivationLog(
            phrase = phrase,
            confidence = confidence,
            noiseFilteredDb = db,
            isActivated = activated
        )
        val list = (_activationLogs.value + newLog).sortedByDescending { it.timestampMs }
        _activationLogs.value = list
        saveLogs()
    }

    fun clearLogs() {
        _activationLogs.value = emptyList()
        saveLogs()
    }

    fun cleanup() {
        stopSpeechRecognition()
        micPollerJob?.cancel()
    }
}
