package com.arindam.camerax.util.permission

import com.arindam.camerax.testing.RobolectricPermissions
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MicrophonePermissionTest {

    @Test
    fun isGranted_falseWhenPermissionNotGranted() {
        RobolectricPermissions.revokeRecordAudio()
        assertFalse(MicrophonePermission.isGranted(RobolectricPermissions.applicationContext()))
    }

    @Test
    fun isGranted_trueWhenPermissionGranted() {
        RobolectricPermissions.grantRecordAudio()
        assertTrue(MicrophonePermission.isGranted(RobolectricPermissions.applicationContext()))
    }
}
