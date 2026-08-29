package com.example.service

import android.content.Context
import android.content.Intent
import com.example.data.local.dao.EmergencyContactDao
import com.example.data.local.dao.SosHistoryDao
import com.example.model.EmergencyModel
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE)
class EmergencyServiceTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var emergencyService: EmergencyService

    private val mockContext: Context = mock()
    private val mockFirestore: FirebaseFirestore = mock()
    private val mockLocationService: LocationService = mock()
    private val mockNotificationService: NotificationService = mock()
    private val mockDatabaseService: DatabaseService = mock()
    private val mockSosHistoryDao: SosHistoryDao = mock()
    private val mockContactDao: EmergencyContactDao = mock()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        // Mock StateFlows where needed, e.g. for DatabaseService contacts if requested
        whenever(mockDatabaseService.contacts).thenReturn(MutableStateFlow(emptyList()))

        emergencyService = EmergencyService(
            context = mockContext,
            firestore = mockFirestore,
            locationService = mockLocationService,
            notificationService = mockNotificationService,
            databaseService = mockDatabaseService,
            sosHistoryDao = mockSosHistoryDao,
            contactDao = mockContactDao
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `isEmergencyActive returns false initially`() {
        assertFalse(emergencyService.isEmergencyActive())
    }

    @Test
    fun `isEmergencyActive returns true when startEmergency is called`() = runTest {
        // We start an emergency and verify it's active immediately (countdown job starts)
        emergencyService.startEmergency(
            userId = "test_user_id",
            userName = "Test User",
            userPhone = "1234567890",
            triggerType = "MANUAL",
            deviceId = "TEST-DEVICE-01",
            customLat = 10.0,
            customLng = 20.0
        )

        assertTrue(emergencyService.isEmergencyActive())
    }

    @Test
    fun `isEmergencyActive returns false after cancelling emergency`() = runTest {
        // 1. Start emergency
        emergencyService.startEmergency(
            userId = "test_user_id",
            userName = "Test User",
            userPhone = "1234567890",
            triggerType = "MANUAL",
            deviceId = "TEST-DEVICE-01",
            customLat = 10.0,
            customLng = 20.0
        )
        assertTrue(emergencyService.isEmergencyActive())

        // 2. Cancel it with a correct pin
        emergencyService.cancelEmergencyWithPin("1234", "1234")

        // 3. Should no longer be active
        assertFalse(emergencyService.isEmergencyActive())
    }
}
