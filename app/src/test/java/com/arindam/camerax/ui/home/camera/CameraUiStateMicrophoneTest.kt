package com.arindam.camerax.ui.home.camera

import com.arindam.camerax.domain.model.CameraMode
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CameraUiStateMicrophoneTest {

    @Test
    fun videoMode_showsMicControlAndAllowsMuteWhenPermissionGranted() {
        val state = CameraUiState(mode = CameraMode.VIDEO, microphonePermissionGranted = true)
        assertTrue(state.showsAudioMuteControl)
        assertTrue(state.allowsAudioMute)
    }

    @Test
    fun videoMode_showsMicControlButDisallowsMuteWhenPermissionDenied() {
        val state = CameraUiState(mode = CameraMode.VIDEO, microphonePermissionGranted = false)
        assertTrue(state.showsAudioMuteControl)
        assertFalse(state.allowsAudioMute)
    }

    @Test
    fun slowMotion_hidesMicControlRegardlessOfPermission() {
        val granted = CameraUiState(mode = CameraMode.SLOW_MOTION, microphonePermissionGranted = true)
        val denied = CameraUiState(mode = CameraMode.SLOW_MOTION, microphonePermissionGranted = false)
        assertFalse(granted.showsAudioMuteControl)
        assertFalse(denied.showsAudioMuteControl)
        assertFalse(granted.allowsAudioMute)
        assertFalse(denied.allowsAudioMute)
    }

    @Test
    fun photoMode_allowsAudioMuteOnlyWhenPermissionGranted() {
        val granted = CameraUiState(mode = CameraMode.PHOTO, microphonePermissionGranted = true)
        val denied = CameraUiState(mode = CameraMode.PHOTO, microphonePermissionGranted = false)
        assertTrue(granted.showsAudioMuteControl)
        assertTrue(granted.allowsAudioMute)
        assertTrue(denied.showsAudioMuteControl)
        assertFalse(denied.allowsAudioMute)
    }

    @Test
    fun dualMode_respectsMicrophonePermissionForMute() {
        val denied = CameraUiState(mode = CameraMode.DUAL, microphonePermissionGranted = false)
        assertFalse(denied.allowsAudioMute)
    }
}
