package com.example.service

import android.content.Context
import com.example.model.AIAnalysisModel
import com.example.model.AISensorReading
import kotlinx.coroutines.flow.StateFlow

class AIProvider(
    private val context: Context,
    val aiService: AIService
) {
    val analysisLogs: StateFlow<List<AIAnalysisModel>> = aiService.analysisLogs
    val currentLiveReading: StateFlow<AISensorReading> = aiService.currentLiveReading
    val currentLiveAnalysis: StateFlow<AIAnalysisModel?> = aiService.currentLiveAnalysis

    fun getLogs(): List<AIAnalysisModel> {
        return aiService.analysisLogs.value
    }

    fun startSimulation(pattern: String) {
        aiService.startSensorStreamingSimulation(pattern)
    }

    fun stopSimulation() {
        aiService.stopSimulation()
    }

    fun addLog(log: AIAnalysisModel) {
        aiService.addAnalysisLog(log)
    }
}
