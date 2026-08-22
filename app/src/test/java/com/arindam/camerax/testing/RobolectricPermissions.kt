package com.arindam.camerax.testing

import android.content.Context
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf

object RobolectricPermissions {

    fun applicationContext(): Context = RuntimeEnvironment.getApplication()

    fun grantRecordAudio() {
        shadowOf(RuntimeEnvironment.getApplication()).grantPermissions(
            android.Manifest.permission.RECORD_AUDIO
        )
    }

    fun revokeRecordAudio() {
        shadowOf(RuntimeEnvironment.getApplication()).denyPermissions(
            android.Manifest.permission.RECORD_AUDIO
        )
    }
}
