package com.arindam.camerax.data.camera

import android.os.Build
import com.arindam.camerax.testing.RobolectricPermissions
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class RecordingForegroundServiceTest {

    @Test
    @Config(sdk = [Build.VERSION_CODES.UPSIDE_DOWN_CAKE])
    fun onCreate_withoutMicPermission_doesNotCrash() {
        RobolectricPermissions.revokeRecordAudio()
        Robolectric.buildService(RecordingForegroundService::class.java)
            .create()
            .get()
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.UPSIDE_DOWN_CAKE])
    fun onCreate_withMicPermission_doesNotCrash() {
        RobolectricPermissions.grantRecordAudio()
        Robolectric.buildService(RecordingForegroundService::class.java)
            .create()
            .get()
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.TIRAMISU])
    fun onCreate_preUpsideDownCake_doesNotCrashWithoutMicPermission() {
        RobolectricPermissions.revokeRecordAudio()
        Robolectric.buildService(RecordingForegroundService::class.java)
            .create()
            .get()
    }
}
