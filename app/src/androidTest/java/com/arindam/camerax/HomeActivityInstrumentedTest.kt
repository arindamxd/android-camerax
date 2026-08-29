package com.arindam.camerax

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.arindam.camerax.support.InstrumentedTestPermissions
import com.arindam.camerax.ui.home.HomeActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Instrumented smoke tests for the main camera host activity. */
@RunWith(AndroidJUnit4::class)
class HomeActivityInstrumentedTest {

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    @Test
    fun launchesCameraChromeWhenPermissionsGranted() {
        ActivityScenario.launch(HomeActivity::class.java).use {
            InstrumentedTestPermissions.ensureCamera()
            composeTestRule.waitForIdle()
            composeTestRule.waitUntil(timeoutMillis = 20_000) {
                composeTestRule
                    .onAllNodesWithContentDescription("Settings", useUnmergedTree = true)
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }
            composeTestRule.onNodeWithContentDescription("Settings").assertIsDisplayed()
            composeTestRule.onNodeWithContentDescription("Gallery").assertIsDisplayed()
        }
    }
}
