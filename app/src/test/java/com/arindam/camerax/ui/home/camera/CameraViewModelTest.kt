package com.arindam.camerax.ui.home.camera

import androidx.lifecycle.SavedStateHandle
import com.arindam.camerax.di.AppDispatchers
import com.arindam.camerax.di.cameraInteractors
import com.arindam.camerax.domain.model.CameraLens
import com.arindam.camerax.domain.model.CameraMode
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
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

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
}
