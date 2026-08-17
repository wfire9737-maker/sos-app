package com.example.config

object Esp32Config {
    const val ESP32_BASE_URL = "http://10.63.183.90:8080"
    const val POLL_INTERVAL_MS = 800L

    fun getStatusUrl(): String = "$ESP32_BASE_URL/status?t=${System.currentTimeMillis()}"
    fun getResetUrl(): String = "$ESP32_BASE_URL/reset?t=${System.currentTimeMillis()}"
}
