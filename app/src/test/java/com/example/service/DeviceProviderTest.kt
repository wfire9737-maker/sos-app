package com.example.service

import android.app.ActivityManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowActivityManager

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class DeviceProviderTest {

    private lateinit var context: Context
    private lateinit var deviceProvider: DeviceProvider

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        deviceProvider = DeviceProvider(context)
    }

    @Test
    fun getLocalMemoryUsagePercent_success() {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val shadowActivityManager = shadowOf(activityManager)

        val memoryInfo = ActivityManager.MemoryInfo()
        memoryInfo.totalMem = 1000L
        memoryInfo.availMem = 250L // 750 used
        shadowActivityManager.setMemoryInfo(memoryInfo)

        val percent = deviceProvider.getLocalMemoryUsagePercent()

        // 750 / 1000 = 75%
        assertEquals(75, percent)
    }

    @Test
    fun getLocalMemoryUsagePercent_exceptionReturnsFallback() {
        // Create an activity manager without memory info to trigger an exception, or pass context that throws an exception
        // An easy way to test exception path:
        // Create a custom context that throws exception when getting ACTIVITY_SERVICE
        // Let's create a proxy context that throws

        val contextThatThrows = object : android.content.ContextWrapper(context) {
            override fun getSystemService(name: String): Any {
                if (name == Context.ACTIVITY_SERVICE) {
                    throw RuntimeException("Simulated exception")
                }
                return super.getSystemService(name)
            }
        }

        val providerWithThrowingContext = DeviceProvider(contextThatThrows)
        val percent = providerWithThrowingContext.getLocalMemoryUsagePercent()

        // Expected fallback is 45
        assertEquals(45, percent)
    }
}
