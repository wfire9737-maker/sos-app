package com.example.service

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import java.util.UUID

data class VoiceActivationLog(
    val id: String = UUID.randomUUID().toString(),
    val phrase: String,
    val confidence: Int,
    val noiseFilteredDb: Float,
    val isActivated: Boolean,
    val timestampMs: Long = System.currentTimeMillis()
)

class VoiceSosService(
    private val context: Context
) {
    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val sharedPrefs: SharedPreferences = context.getSharedPreferences("guardian_sos_voice", Context.MODE_PRIVATE)

    // Speech states
    private val _isListening = MutableStateFlow(true)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _voiceState = MutableStateFlow("LISTENING") // "LISTENING", "PROCESSING_SPEECH", "MATCH_FOUND", "THRESHOLD_REJECTED"
    val voiceState: StateFlow<String> = _voiceState.asStateFlow()

    // Configurable Wake Phrases
    private val _wakePhrases = MutableStateFlow<List<String>>(listOf("Help me", "Emergency", "Save me"))
    val wakePhrases: StateFlow<List<String>> = _wakePhrases.asStateFlow()

    // Live Decibel Level (for visual wave visualization)
    private val _micDecibels = MutableStateFlow(42f)
    val micDecibels: StateFlow<Float> = _micDecibels.asStateFlow()

    // Configurable trigger confidence threshold
    private val _confidenceThreshold = MutableStateFlow(80) // default 80%
    val confidenceThreshold: StateFlow<Int> = _confidenceThreshold.asStateFlow()

    // Log list of all voice detection attempts
    private val _activationLogs = MutableStateFlow<List<VoiceActivationLog>>(emptyList())
    val activationLogs: StateFlow<List<VoiceActivationLog>> = _activationLogs.asStateFlow()

    // Trigger Callback for main SOS action
    var onVoiceSosTriggered: ((String, Int) -> Unit)? = null

    private var micPollerJob: Job? = null

    init {
        loadPhrasesAndLogs()
        startMicLevelSimulation()
    }

    private fun loadPhrasesAndLogs() {
        // Load custom wake words
        val phrasesJson = sharedPrefs.getString("wake_phrases", null)
        if (phrasesJson != null) {
            try {
                val arr = JSONArray(phrasesJson)
                val list = mutableListOf<String>()
                for (i in 0 until arr.length()) {
                    list.add(arr.getString(i))
                }
                _wakePhrases.value = list
            } catch (e: Exception) {
                Log.e("VoiceSosService", "Error parsing saved phrases", e)
            }
        }

        // Load logs
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

    private fun startMicLevelSimulation() {
        micPollerJob?.cancel()
        micPollerJob = serviceScope.launch {
            while (isActive) {
                if (_isListening.value) {
                    // Simulate voice decibel oscillation (ambient background noise 35dB - 55dB)
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

    /**
     * Simulate speech recognition input. This filters out background noise,
     * checks for matches against configured wake phrases, evaluates confidence,
     * and triggers the SOS flow if confidence matches or exceeds the threshold.
     */
    fun processVoiceInput(spokenText: String, inputConfidence: Int) {
        if (!_isListening.value) return

        serviceScope.launch {
            _voiceState.value = "PROCESSING_SPEECH"
            delay(1200) // Simulate offline neural net DSP delay

            val matchedPhrase = _wakePhrases.value.firstOrNull { phrase ->
                spokenText.contains(phrase, ignoreCase = true)
            }

            val meetsThreshold = matchedPhrase != null && inputConfidence >= _confidenceThreshold.value
            val simulatedDb = 72f // Spike indicating speech vs ambient

            if (meetsThreshold && matchedPhrase != null) {
                _voiceState.value = "MATCH_FOUND"
                addActivationLog(spokenText, inputConfidence, simulatedDb, true)
                delay(500)
                // Invoke callback on main thread/ViewModel
                withContext(Dispatchers.Main) {
                    onVoiceSosTriggered?.invoke(matchedPhrase, inputConfidence)
                }
                _voiceState.value = "LISTENING"
            } else {
                _voiceState.value = "THRESHOLD_REJECTED"
                addActivationLog(spokenText, inputConfidence, simulatedDb, false)
                delay(1500)
                _voiceState.value = "LISTENING"
            }
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
        micPollerJob?.cancel()
    }
}
