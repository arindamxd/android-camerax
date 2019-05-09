package com.arindam.camerax

import android.Manifest
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.ActivityTestRule
import androidx.test.rule.GrantPermissionRule
import com.arindam.camerax.activities.MainActivity
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Created by Arindam Karmakar on 9/5/19.
 */
@RunWith(AndroidJUnit4::class)
class MainInstrumentedTest {

    @get:Rule
    val permissionRule = GrantPermissionRule.grant(
        Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO
    )

    @get:Rule
    val activityRule: ActivityTestRule<MainActivity> = ActivityTestRule(MainActivity::class.java)

    @Test
    fun useAppContext() {
        // Context of the app under test
        val context = ApplicationProvider.getApplicationContext() as Context
        assertEquals("com.arindam.camerax", context.packageName)
    }
}
