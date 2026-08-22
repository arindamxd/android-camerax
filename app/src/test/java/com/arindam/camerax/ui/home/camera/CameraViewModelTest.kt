package com.arindam.camerax.ui.home.camera

import androidx.lifecycle.SavedStateHandle
import com.arindam.camerax.di.AppDispatchers
import com.arindam.camerax.di.cameraInteractors
import com.arindam.camerax.domain.model.CameraLens
import com.arindam.camerax.domain.model.CameraMode
import com.arindam.camerax.domain.model.CaptureAspect
import com.arindam.camerax.domain.model.CaptureSettings
import com.arindam.camerax.domain.model.DeviceCaptureFeatures
import com.arindam.camerax.domain.model.FlashMode
import com.arindam.camerax.domain.model.SlowMotionOptions
import com.arindam.camerax.domain.model.VideoQuality
import com.arindam.camerax.testing.FakeCameraRepository
import com.arindam.camerax.testing.FakeDeviceFeaturesRepository
import com.arindam.camerax.testing.FakeMediaRepository
import com.arindam.camerax.testing.FakeSettingsRepository
import com.arindam.camerax.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class CameraViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val camera = FakeCameraRepository()
    private val media = FakeMediaRepository()
    private val settings = FakeSettingsRepository()
    private val features = FakeDeviceFeaturesRepository()

    private fun viewModel(
        handle: SavedStateHandle = SavedStateHandle(),
        device: DeviceCaptureFeatures = DeviceCaptureFeatures(),
        initial: CameraUiState = CameraUiState()
    ): CameraViewModel {
        features.features = device
        return CameraViewModel(
            interactors = cameraInteractors(camera, media, settings, features),
            savedState = handle,
            dispatchers = AppDispatchers(
                main = mainDispatcherRule.dispatcher,
                default = mainDispatcherRule.dispatcher,
                io = mainDispatcherRule.dispatcher
            ),
            initialState = initial
        )
    }

    @Test
    fun setMode_hiddenSlowMotionIsIgnored() {
        val vm = viewModel()
        vm.setMode(CameraMode.SLOW_MOTION)
        assertEquals(CameraMode.PHOTO, vm.uiState.value.mode)
    }

    @Test
    fun setMode_slowMotionWhenSupported() {
        val vm = viewModel(
            device = DeviceCaptureFeatures(
                slowMotion = SlowMotionOptions(
                    qualities = listOf(VideoQuality.FHD),
                    frameRates = listOf(120)
                )
            )
        )
        vm.setMode(CameraMode.SLOW_MOTION)
        assertEquals(CameraMode.SLOW_MOTION, vm.uiState.value.mode)
    }

    @Test
    fun cycleFlash_updatesStateAndRepository() {
        val vm = viewModel(initial = CameraUiState(hasFlash = true))
        vm.cycleFlash()
        assertEquals(FlashMode.ON, vm.uiState.value.flash)
        assertEquals(FlashMode.ON, camera.lastFlash)
    }

    @Test
    fun consumeMessage_clearsBanner() {
        val vm = viewModel(initial = CameraUiState(message = "failed"))
        vm.consumeMessage()
        assertNull(vm.uiState.value.message)
    }

    @Test
    fun savedState_restoresModeLensFlash() {
        val handle = SavedStateHandle(
            mapOf(
                "camera_mode" to CameraMode.VIDEO.name,
                "camera_lens" to "FRONT",
                "camera_flash" to FlashMode.TORCH.name
            )
        )
        val vm = viewModel(handle = handle, initial = CameraUiState(hasFlash = true))
        assertEquals(CameraMode.VIDEO, vm.uiState.value.mode)
        assertEquals(CameraLens.FRONT, vm.uiState.value.lens)
        assertEquals(FlashMode.TORCH, vm.uiState.value.flash)
    }

    @Test
    fun setMode_persistsToSavedState() {
        val handle = SavedStateHandle()
        val vm = viewModel(handle = handle)
        vm.setMode(CameraMode.EFFECTS)
        assertEquals(CameraMode.EFFECTS.name, handle["camera_mode"])
    }

    @Test
    fun setMode_hiddenDualIsIgnored() {
        val vm = viewModel()
        vm.setMode(CameraMode.DUAL)
        assertEquals(CameraMode.PHOTO, vm.uiState.value.mode)
    }

    @Test
    fun setMode_dualWhenConcurrentSupported() {
        val vm = viewModel(device = DeviceCaptureFeatures(concurrent = true))
        vm.setMode(CameraMode.DUAL)
        assertEquals(CameraMode.DUAL, vm.uiState.value.mode)
    }

    @Test
    fun probe_hidesDualWhenConcurrentUnsupported() {
        val vm = viewModel(
            initial = CameraUiState(mode = CameraMode.DUAL, concurrentSupported = true)
        )
        assertEquals(CameraMode.PHOTO, vm.uiState.value.mode)
        assertEquals(false, vm.uiState.value.concurrentSupported)
        assertEquals(false, vm.uiState.value.visibleModes.contains(CameraMode.DUAL))
    }

    @Test
    fun setMode_othersOpensToolsHub() {
        val vm = viewModel()
        vm.setMode(CameraMode.OTHERS)
        assertEquals(CameraMode.OTHERS, vm.uiState.value.mode)
        assertEquals(true, vm.uiState.value.showsTools)
        assertEquals(false, vm.uiState.value.showsCaptureControls)
    }

    @Test
    fun exitTools_returnsToPreviousMode() {
        val vm = viewModel(initial = CameraUiState(mode = CameraMode.VIDEO))
        vm.setMode(CameraMode.OTHERS)
        vm.exitTools()
        assertEquals(CameraMode.VIDEO, vm.uiState.value.mode)
        assertEquals(false, vm.uiState.value.showsTools)
    }

    @Test
    fun exitTools_fromOthersHubDefaultsToPhoto() {
        val vm = viewModel()
        vm.setMode(CameraMode.OTHERS)
        vm.exitTools()
        assertEquals(CameraMode.PHOTO, vm.uiState.value.mode)
        assertEquals(false, vm.uiState.value.showsTools)
    }

    @Test
    fun hasGalleryItems_falseUntilThumbnailLoads() {
        val vm = viewModel()
        assertEquals(false, vm.hasGalleryItems())
    }

    @Test
    fun hasGalleryItems_trueWhenLatestMediaExists() {
        media.add(File("shot.jpg"))
        val vm = viewModel()
        assertEquals(true, vm.hasGalleryItems())
    }

    @Test
    fun cycleTimer_advancesWhileIdle() {
        val vm = viewModel()
        vm.cycleTimer()
        assertEquals(3, vm.uiState.value.timer.seconds)
    }

    @Test
    fun toggleGrid_flipsFlag() {
        val vm = viewModel()
        vm.toggleGrid()
        assertEquals(true, vm.uiState.value.gridEnabled)
        vm.toggleGrid()
        assertEquals(false, vm.uiState.value.gridEnabled)
    }

    @Test
    fun onMicControlClicked_requestsPermissionWhenDenied() {
        var requested = false
        val vm = viewModel(
            initial = CameraUiState(
                mode = CameraMode.VIDEO,
                microphonePermissionGranted = false,
                isRecording = true,
                isMuted = true
            )
        )
        vm.onMicControlClicked { requested = true }
        assertTrue(requested)
    }

    @Test
    fun onMicControlClicked_togglesWhenGranted() {
        var requested = false
        val vm = viewModel(
            initial = CameraUiState(
                mode = CameraMode.VIDEO,
                microphonePermissionGranted = true,
                isMuted = false,
                isRecording = true
            )
        )
        vm.onMicControlClicked { requested = true }
        assertFalse(requested)
        assertTrue(vm.uiState.value.isMuted)
    }

    @Test
    fun updateMicrophonePermission_forcesMutedWhenDenied() {
        settings.settings = CaptureSettings(recordMuted = false)
        val vm = viewModel(initial = CameraUiState(mode = CameraMode.VIDEO, isMuted = false))
        vm.updateMicrophonePermission(granted = false)
        assertEquals(false, vm.uiState.value.microphonePermissionGranted)
        assertEquals(true, vm.uiState.value.isMuted)
        assertEquals(false, vm.uiState.value.allowsAudioMute)
    }

    @Test
    fun applyCaptureSettings_updatesAspectAndRebind() {
        val vm = viewModel()
        val before = vm.uiState.value.bindRevision
        vm.applyCaptureSettings(
            CaptureSettings(aspect = CaptureAspect.RATIO_4_3)
        )
        assertEquals(CaptureAspect.RATIO_4_3, vm.uiState.value.captureAspect)
        assertEquals(before + 1, vm.uiState.value.bindRevision)
    }

    @Test
    fun discardActiveCapture_clearsPanoramaWithoutPublishing() {
        val vm = viewModel(
            initial = CameraUiState(
                mode = CameraMode.PANORAMA,
                panoramaActive = true,
                panoramaFrames = 4
            )
        )
        vm.discardActiveCapture()
        assertFalse(vm.uiState.value.panoramaActive)
        assertEquals(0, vm.uiState.value.panoramaFrames)
        assertTrue(media.published.isEmpty())
    }

    @Test
    fun saveActiveCapture_clearsEmptyPanoramaWithoutPublishing() {
        val vm = viewModel(
            initial = CameraUiState(
                mode = CameraMode.PANORAMA,
                panoramaActive = true,
                panoramaFrames = 0
            )
        )
        vm.saveActiveCapture()
        assertFalse(vm.uiState.value.panoramaActive)
        assertTrue(media.published.isEmpty())
    }

    @Test
    fun toggleMotionPhoto_blockedWhenUltraHdrOn() {
        settings.settings = CaptureSettings(ultraHdr = true)
        val vm = viewModel()
        assertTrue(vm.uiState.value.ultraHdr)
        vm.toggleMotionPhoto()
        assertFalse(vm.uiState.value.motionPhotoEnabled)
        assertEquals(
            com.arindam.camerax.R.string.motion_photo_disable_ultra_hdr,
            vm.uiState.value.messageRes
        )
    }

    @Test
    fun applyCaptureSettings_clearsMotionWhenUltraHdrEnabled() {
        val vm = viewModel(initial = CameraUiState(motionPhotoEnabled = true))
        vm.applyCaptureSettings(CaptureSettings(ultraHdr = true))
        assertFalse(vm.uiState.value.motionPhotoEnabled)
        assertTrue(vm.uiState.value.ultraHdr)
    }
}
