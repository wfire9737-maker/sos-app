package com.example.service

import android.content.Context
import com.example.model.HistoryModel
import kotlinx.coroutines.flow.StateFlow

class HistoryProvider(
    private val context: Context,
    val historyService: HistoryService
) {
    val history: StateFlow<List<HistoryModel>> = historyService.history

    fun getCompleteHistory(): List<HistoryModel> {
        return historyService.history.value
    }

    fun addHistoryRecord(item: HistoryModel) {
        historyService.addHistoryItem(item)
    }

    fun deleteHistoryRecord(id: String) {
        historyService.deleteHistoryItem(id)
    }

    fun exportToCSV(): String {
        return historyService.generateCSVString()
    }

    fun exportToPDF(): String {
        return historyService.generatePDFReportText()
    }
}
