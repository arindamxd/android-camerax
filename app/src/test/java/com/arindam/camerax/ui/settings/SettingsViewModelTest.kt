package com.arindam.camerax.ui.settings

import com.arindam.camerax.testing.RobolectricPermissions
import com.arindam.camerax.di.AppDispatchers
import com.arindam.camerax.di.cameraInteractors
import com.arindam.camerax.domain.model.DeviceCaptureFeatures
import com.arindam.camerax.testing.FakeCameraRepository
import com.arindam.camerax.testing.FakeDeviceFeaturesRepository
import com.arindam.camerax.testing.FakeMediaRepository
import com.arindam.camerax.testing.FakeSettingsRepository
import com.arindam.camerax.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class SettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun viewModel(): SettingsViewModel = SettingsViewModel(
        cameraInteractors(
            FakeCameraRepository(),
            FakeMediaRepository(),
            FakeSettingsRepository(),
            FakeDeviceFeaturesRepository(DeviceCaptureFeatures(concurrent = true, videoFps60 = true))
        ),
        versionLabel = "1.7.0",
        dispatchers = AppDispatchers(
            main = mainDispatcherRule.dispatcher,
            default = mainDispatcherRule.dispatcher,
            io = mainDispatcherRule.dispatcher
        )
    )

    @Test
    fun probe_exposesDeviceFeatures() {
        val vm = viewModel()
        assertEquals(true, vm.uiState.value.features.concurrent)
        assertEquals(true, vm.uiState.value.features.videoFps60)
        assertEquals("1.7.0", vm.uiState.value.versionLabel)
    }

    @Test
    fun refreshMicrophonePermission_falseWhenNotGranted() {
        RobolectricPermissions.revokeRecordAudio()
        val context = RobolectricPermissions.applicationContext()
        val vm = viewModel()
        vm.refreshMicrophonePermission(context)
        assertFalse(vm.uiState.value.microphonePermissionGranted)
    }

    @Test
    fun refreshMicrophonePermission_trueWhenGranted() {
        RobolectricPermissions.grantRecordAudio()
        val context = RobolectricPermissions.applicationContext()
        val vm = viewModel()
        vm.refreshMicrophonePermission(context)
        assertTrue(vm.uiState.value.microphonePermissionGranted)
    }

    @Test
    fun refreshMicrophonePermission_updatesWhenPermissionRevoked() {
        RobolectricPermissions.grantRecordAudio()
        val context = RobolectricPermissions.applicationContext()
        val vm = viewModel()
        vm.refreshMicrophonePermission(context)
        assertTrue(vm.uiState.value.microphonePermissionGranted)
        RobolectricPermissions.revokeRecordAudio()
        vm.refreshMicrophonePermission(context)
        assertFalse(vm.uiState.value.microphonePermissionGranted)
    }
}
