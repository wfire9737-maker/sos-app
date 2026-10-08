package com.example.model

data class SyncResult(
    val success: Boolean,
    val syncedCategories: List<String> = emptyList(),
    val failedCategories: List<String> = emptyList(),
    val message: String = ""
)
