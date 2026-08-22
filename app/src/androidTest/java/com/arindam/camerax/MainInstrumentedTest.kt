package com.arindam.camerax

import android.Manifest
import android.content.Context
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.arindam.camerax.ui.home.HomeActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented smoke test. Debug builds use the `.debug` applicationId suffix.
 */
@RunWith(AndroidJUnit4::class)
class MainInstrumentedTest {

    @get:Rule
    val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.CAMERA
    )

    @Test
    fun useAppContext() {
        val context = ApplicationProvider.getApplicationContext() as Context
        assertEquals("com.arindam.camerax.debug", context.packageName)
    }

    @Test
    fun homeActivity_launches() {
        ActivityScenario.launch(HomeActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                assertTrue(activity.packageName.endsWith(".debug") || activity.packageName == "com.arindam.camerax")
            }
        }
    }
}
