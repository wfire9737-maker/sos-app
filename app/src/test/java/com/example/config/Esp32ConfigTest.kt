package com.example.config

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Esp32ConfigTest {

    @Test
    fun testEsp32BaseUrl() {
        assertEquals("http://10.63.183.90:8080", Esp32Config.ESP32_BASE_URL)
    }

    @Test
    fun testPollIntervalMs() {
        assertEquals(800L, Esp32Config.POLL_INTERVAL_MS)
    }

    @Test
    fun testGetStatusUrl() {
        val url = Esp32Config.getStatusUrl()
        val expectedPrefix = "${Esp32Config.ESP32_BASE_URL}/status?t="

        assertTrue("URL should start with correct prefix", url.startsWith(expectedPrefix))

        val timestampStr = url.substringAfter("?t=")
        assertTrue("Timestamp should be a valid number", timestampStr.toLongOrNull() != null)
    }

    @Test
    fun testGetResetUrl() {
        val url = Esp32Config.getResetUrl()
        val expectedPrefix = "${Esp32Config.ESP32_BASE_URL}/reset?t="

        assertTrue("URL should start with correct prefix", url.startsWith(expectedPrefix))

        val timestampStr = url.substringAfter("?t=")
        assertTrue("Timestamp should be a valid number", timestampStr.toLongOrNull() != null)
    }
}
