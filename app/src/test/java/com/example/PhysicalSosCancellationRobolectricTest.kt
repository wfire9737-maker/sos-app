package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.service.DatabaseService
import com.example.service.EmergencyService
import com.example.service.LocationService
import com.example.service.NotificationService
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PhysicalSosCancellationRobolectricTest {

    private lateinit var context: Context
    private lateinit var emergencyService: EmergencyService

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        val dbService = DatabaseService(context)
        val locService = LocationService(context, null, null)
        val notifService = NotificationService(context, null, null)
        emergencyService = EmergencyService(
            context = context,
            firestore = null,
            locationService = locService,
            notificationService = notifService,
            databaseService = dbService
        )
    }

    @Test
    fun testPhysicalSosCountdownCancelledWithoutPin() = runBlocking {
        // Trigger a physical SOS countdown
        emergencyService.startEmergency(
            userId = "test-user-1",
            userName = "Test User",
            userPhone = "555-0100",
            triggerType = "PHYSICAL_BLE_BUTTON"
        )

        // Verify it is recognized as pending cancellation
        assertTrue("Physical SOS should be pending cancellation during countdown", emergencyService.isPhysicalSosPendingCancellation())
        assertTrue("Countdown should be active", emergencyService.isCountdownActive())

        // Cancel directly via cancelPendingPhysicalSos without PIN
        val cancelled = emergencyService.cancelPendingPhysicalSos()
        assertTrue("Physical SOS should cancel successfully during countdown without PIN", cancelled)

        // Verify countdown is no longer active
        assertFalse("Countdown should be stopped", emergencyService.isCountdownActive())
        assertFalse("Physical SOS should no longer be pending cancellation", emergencyService.isPhysicalSosPendingCancellation())
    }

    @Test
    fun testManualSosDoesNotCancelViaPhysicalSosHandler() = runBlocking {
        // Trigger a manual in-app SOS
        emergencyService.startEmergency(
            userId = "test-user-2",
            userName = "Manual User",
            userPhone = "555-0200",
            triggerType = "MANUAL"
        )

        // Verify physical cancellation returns false for manual SOS
        assertFalse("Manual SOS should NOT be marked as physical SOS pending cancellation", emergencyService.isPhysicalSosPendingCancellation())

        val physicalCancelResult = emergencyService.cancelPendingPhysicalSos()
        assertFalse("cancelPendingPhysicalSos should reject non-physical SOS", physicalCancelResult)

        // Clean up manual emergency using pin cancellation
        val pinCancel = emergencyService.cancelEmergencyWithPin(pin = "1234", expectedPin = "1234")
        assertTrue("Manual SOS should cancel with valid PIN", pinCancel)
    }

    @Test
    fun testSecondPhysicalSosCancelFailsWhenCountdownExpiredOrNotInCountdown() = runBlocking {
        // When no emergency is active
        val result = emergencyService.cancelPendingPhysicalSos()
        assertFalse("cancelPendingPhysicalSos should return false when no emergency is active", result)
    }
}
