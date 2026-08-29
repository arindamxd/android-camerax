package com.arindam.camerax

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.Lifecycle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.arindam.camerax.ui.settings.SettingsActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Instrumented Compose UI tests for the Settings screen. */
@RunWith(AndroidJUnit4::class)
class SettingsActivityInstrumentedTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<SettingsActivity>()

    @Test
    fun showsTitleAndCoreSections() {
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Settings").assertIsDisplayed()
        composeTestRule.onNodeWithText("THEME").assertIsDisplayed()
        composeTestRule.onNodeWithText("GENERAL").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Back").assertIsDisplayed()
    }

    @Test
    fun backButtonFinishesActivity() {
        val scenario = composeTestRule.activityRule.scenario
        composeTestRule.onNodeWithContentDescription("Back").performClick()
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            scenario.state == Lifecycle.State.DESTROYED
        }
    }
}
