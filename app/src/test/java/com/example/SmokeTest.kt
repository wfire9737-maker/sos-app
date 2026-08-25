package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [33], manifest = Config.NONE)
class SmokeTest {
    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun testAppRendersWithoutCrashing() {
        val activityController = org.robolectric.Robolectric.buildActivity(MainActivity::class.java).setup()
        org.robolectric.shadows.ShadowLooper.idleMainLooper()
        println("App rendered successfully.")
    }
}
