package com.example.model

data class DeveloperLog(
    val id: String = java.util.UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val event: String,
    val status: String
)
