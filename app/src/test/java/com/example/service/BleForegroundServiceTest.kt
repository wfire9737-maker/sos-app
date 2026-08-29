package com.example.service

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class BleForegroundServiceTest {

    @Test
    fun `test start intent creation`() {
        val context = ApplicationProvider.getApplicationContext<Application>()

        BleForegroundService.start(context)

        val shadowApplication = shadowOf(context)
        val startedIntent = shadowApplication.nextStartedService

        assertNotNull("Expected a service to be started", startedIntent)
        assertEquals(BleForegroundService.ACTION_START_BLE_SERVICE, startedIntent!!.action)
        assertEquals(BleForegroundService::class.java.name, startedIntent.component?.className)
    }

    @Test
    fun `test stop intent creation`() {
        val context = ApplicationProvider.getApplicationContext<Application>()

        BleForegroundService.stop(context)

        val shadowApplication = shadowOf(context)
        val startedIntent = shadowApplication.nextStartedService

        assertNotNull("Expected a service to be started", startedIntent)
        assertEquals(BleForegroundService.ACTION_STOP_BLE_SERVICE, startedIntent!!.action)
        assertEquals(BleForegroundService::class.java.name, startedIntent.component?.className)
    }
}
