package com.arindam.camerax.data.camera

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoRecordingAudioPolicyTest {

    @Test
    fun enablesAudioWhenPermissionGrantedAndNotHighSpeed() {
        assertTrue(shouldEnableRecordingAudio(highSpeedSession = false, recordAudioGranted = true))
    }

    @Test
    fun disablesAudioWhenPermissionDenied() {
        assertFalse(shouldEnableRecordingAudio(highSpeedSession = false, recordAudioGranted = false))
    }

    @Test
    fun disablesAudioDuringHighSpeedEvenWithPermission() {
        assertFalse(shouldEnableRecordingAudio(highSpeedSession = true, recordAudioGranted = true))
    }

    @Test
    fun disablesAudioDuringHighSpeedWithoutPermission() {
        assertFalse(shouldEnableRecordingAudio(highSpeedSession = true, recordAudioGranted = false))
    }
}
