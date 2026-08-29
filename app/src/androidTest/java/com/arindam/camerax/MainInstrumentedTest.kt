package com.arindam.camerax

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * App-level instrumented smoke tests. Debug builds use the `.debug` applicationId suffix.
 *
 * Activity and Compose UI coverage lives in [HomeActivityInstrumentedTest] and
 * [SettingsActivityInstrumentedTest].
 */
@RunWith(AndroidJUnit4::class)
class MainInstrumentedTest {

    @Test
    fun useAppContext() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertTrue(
            context.packageName == "com.arindam.camerax.debug" ||
                context.packageName == "com.arindam.camerax"
        )
    }

    @Test
    fun applicationProvidesContainer() {
        val app = ApplicationProvider.getApplicationContext<CameraX>()
        assertNotNull(app.container)
    }
}
