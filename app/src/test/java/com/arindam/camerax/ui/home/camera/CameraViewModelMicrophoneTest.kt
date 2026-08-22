package com.arindam.camerax.ui.home.camera

import androidx.camera.view.PreviewView
import androidx.lifecycle.SavedStateHandle
import com.arindam.camerax.testing.RobolectricPermissions
import com.arindam.camerax.di.AppDispatchers
import com.arindam.camerax.di.cameraInteractors
import com.arindam.camerax.domain.model.CameraMode
import com.arindam.camerax.domain.model.CaptureSettings
import com.arindam.camerax.domain.model.DeviceCaptureFeatures
import com.arindam.camerax.domain.model.RecordingEvent
import com.arindam.camerax.domain.model.SlowMotionOptions
import com.arindam.camerax.domain.model.VideoQuality
import com.arindam.camerax.testing.FakeCameraRepository
import com.arindam.camerax.testing.FakeDeviceFeaturesRepository
import com.arindam.camerax.testing.FakeMediaRepository
import com.arindam.camerax.testing.FakeSettingsRepository
import com.arindam.camerax.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class CameraViewModelMicrophoneTest {

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
    fun toggleMute_noOpWhenPermissionDenied() {
        val vm = viewModel(
            initial = CameraUiState(
                mode = CameraMode.VIDEO,
                microphonePermissionGranted = false,
                isMuted = true
            )
        )
        vm.toggleMute()
        assertTrue(vm.uiState.value.isMuted)
        assertEquals(0, camera.muteRecordingCalls)
    }

    @Test
    fun toggleMute_noOpInSlowMotionMode() {
        val vm = viewModel(
            device = DeviceCaptureFeatures(
                slowMotion = SlowMotionOptions(
                    qualities = listOf(VideoQuality.FHD),
                    frameRates = listOf(120)
                )
            ),
            initial = CameraUiState(
                mode = CameraMode.SLOW_MOTION,
                microphonePermissionGranted = true,
                isMuted = false
            )
        )
        vm.toggleMute()
        assertFalse(vm.uiState.value.isMuted)
        assertEquals(0, camera.muteRecordingCalls)
    }

    @Test
    fun toggleMute_updatesStateAndRepositoryWhileRecording() {
        val vm = viewModel(
            initial = CameraUiState(
                mode = CameraMode.VIDEO,
                microphonePermissionGranted = true,
                isMuted = false,
                isRecording = true
            )
        )
        vm.toggleMute()
        assertTrue(vm.uiState.value.isMuted)
        assertEquals(true, camera.lastMuteRecording)
    }

    @Test
    fun updateMicrophonePermission_preservesMutedWhileRecordingWhenGranted() {
        settings.settings = CaptureSettings(recordMuted = false)
        val vm = viewModel(
            initial = CameraUiState(
                mode = CameraMode.VIDEO,
                isRecording = true,
                isMuted = true,
                microphonePermissionGranted = true
            )
        )
        vm.updateMicrophonePermission(granted = true)
        assertTrue(vm.uiState.value.isMuted)
    }

    @Test
    fun updateMicrophonePermission_restoresRecordMutedDefaultWhenGrantedAndIdle() {
        settings.settings = CaptureSettings(recordMuted = false)
        val vm = viewModel(
            initial = CameraUiState(
                mode = CameraMode.VIDEO,
                isMuted = true,
                microphonePermissionGranted = false
            )
        )
        vm.updateMicrophonePermission(granted = true)
        assertFalse(vm.uiState.value.isMuted)
    }

    @Test
    fun updateMicrophonePermission_keepsMutedWhenGrantedAndRecordMutedDefault() {
        settings.settings = CaptureSettings(recordMuted = true)
        val vm = viewModel(
            initial = CameraUiState(
                mode = CameraMode.VIDEO,
                isMuted = true,
                microphonePermissionGranted = false
            )
        )
        vm.updateMicrophonePermission(granted = true)
        assertTrue(vm.uiState.value.isMuted)
    }

    @Test
    fun applyCaptureSettings_forcesMutedWhenPermissionDenied() {
        settings.settings = CaptureSettings(recordMuted = false)
        val vm = viewModel(
            initial = CameraUiState(
                mode = CameraMode.VIDEO,
                isMuted = false,
                microphonePermissionGranted = false
            )
        )
        vm.applyCaptureSettings(CaptureSettings(recordMuted = false))
        assertTrue(vm.uiState.value.isMuted)
    }

    @Test
    fun applyCaptureSettings_respectsRecordMutedWhenPermissionGranted() {
        val vm = viewModel(
            initial = CameraUiState(
                mode = CameraMode.VIDEO,
                microphonePermissionGranted = true,
                isMuted = false
            )
        )
        vm.applyCaptureSettings(CaptureSettings(recordMuted = true))
        assertTrue(vm.uiState.value.isMuted)
    }

    @Test
    fun applyCaptureSettings_doesNotChangeMutedWhileRecording() {
        val vm = viewModel(
            initial = CameraUiState(
                mode = CameraMode.VIDEO,
                isRecording = true,
                isMuted = true,
                microphonePermissionGranted = true
            )
        )
        vm.applyCaptureSettings(CaptureSettings(recordMuted = false))
        assertTrue(vm.uiState.value.isMuted)
    }

    @Test
    fun onMicControlClicked_noOpWhenNotRecording() {
        var requested = false
        val vm = viewModel(
            initial = CameraUiState(
                mode = CameraMode.VIDEO,
                microphonePermissionGranted = false
            )
        )
        vm.onMicControlClicked { requested = true }
        assertFalse(requested)
    }

    @Test
    fun onMicControlClicked_requestsPermissionWhileRecordingWhenDenied() {
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
    fun onMicrophonePermissionResult_denied_keepsMuted() {
        val vm = viewModel(
            initial = CameraUiState(
                mode = CameraMode.VIDEO,
                microphonePermissionGranted = false,
                isMuted = true
            )
        )
        vm.onMicrophonePermissionResult(granted = false)
        assertFalse(vm.uiState.value.microphonePermissionGranted)
        assertTrue(vm.uiState.value.isMuted)
    }

    @Test
    fun onMicrophonePermissionResult_granted_unmutesWhenNotRecordMutedByDefault() {
        settings.settings = CaptureSettings(recordMuted = false)
        val vm = viewModel(
            initial = CameraUiState(
                mode = CameraMode.VIDEO,
                microphonePermissionGranted = false,
                isMuted = true
            )
        )
        vm.onMicrophonePermissionResult(granted = true)
        assertTrue(vm.uiState.value.microphonePermissionGranted)
        assertFalse(vm.uiState.value.isMuted)
    }

    @Test
    fun onMicrophonePermissionResult_granted_keepsMutedWhenRecordMutedByDefault() {
        settings.settings = CaptureSettings(recordMuted = true)
        val vm = viewModel(
            initial = CameraUiState(
                mode = CameraMode.VIDEO,
                microphonePermissionGranted = false,
                isMuted = true
            )
        )
        vm.onMicrophonePermissionResult(granted = true)
        assertTrue(vm.uiState.value.isMuted)
    }

    @Test
    fun onMicrophonePermissionResult_granted_unmutesDuringRecordingIfWasMutedFromDenied() {
        settings.settings = CaptureSettings(recordMuted = false)
        val vm = viewModel(
            initial = CameraUiState(
                mode = CameraMode.VIDEO,
                microphonePermissionGranted = false,
                isMuted = true,
                isRecording = true
            )
        )
        vm.onMicrophonePermissionResult(granted = true)
        assertFalse(vm.uiState.value.isMuted)
        assertEquals(false, camera.lastMuteRecording)
    }

    @Test
    fun onShutter_startsRecordingMutedWhenMicDenied() {
        val context = RobolectricPermissions.applicationContext()
        camera.recordingResult = Result.success(File("clip.mp4"))
        val vm = viewModel(
            initial = CameraUiState(
                mode = CameraMode.VIDEO,
                microphonePermissionGranted = false,
                isMuted = true
            )
        )
        vm.setOutputDirectory(File("out"))
        vm.onShutter(PreviewView(context))
        assertEquals(true, camera.lastRecordingMuted)
        assertTrue(vm.uiState.value.isRecording)
    }

    @Test
    fun onShutter_startsRecordingUnmutedWhenMicGrantedAndNotMuted() {
        val context = RobolectricPermissions.applicationContext()
        camera.recordingResult = Result.success(File("clip.mp4"))
        settings.settings = CaptureSettings(recordMuted = false)
        val vm = viewModel(
            initial = CameraUiState(
                mode = CameraMode.VIDEO,
                microphonePermissionGranted = true,
                isMuted = false
            )
        )
        vm.setOutputDirectory(File("out"))
        vm.onShutter(PreviewView(context))
        assertEquals(false, camera.lastRecordingMuted)
    }

    @Test
    fun onShutter_startsRecordingMutedWhenMicGrantedButUserMuted() {
        val context = RobolectricPermissions.applicationContext()
        camera.recordingResult = Result.success(File("clip.mp4"))
        settings.settings = CaptureSettings(recordMuted = true)
        val vm = viewModel(
            initial = CameraUiState(
                mode = CameraMode.VIDEO,
                microphonePermissionGranted = true
            )
        )
        vm.setOutputDirectory(File("out"))
        vm.onShutter(PreviewView(context))
        assertEquals(true, camera.lastRecordingMuted)
    }

    @Test
    fun discardActiveCapture_stopsRecordingWithoutPublishing() = runTest {
        val context = RobolectricPermissions.applicationContext()
        val clip = File.createTempFile("clip", ".mp4").also { it.writeText("video") }
        camera.recordingResult = Result.success(clip)
        val vm = viewModel(
            initial = CameraUiState(
                mode = CameraMode.VIDEO,
                microphonePermissionGranted = true,
                isMuted = true
            )
        )
        vm.setOutputDirectory(File("out"))
        vm.onShutter(PreviewView(context))
        assertTrue(vm.uiState.value.isRecording)

        vm.discardActiveCapture()
        assertEquals(1, camera.stopRecordingCalls)
        camera.emitRecording(RecordingEvent.Finalized(success = true))
        advanceUntilIdle()

        assertFalse(vm.uiState.value.isRecording)
        assertTrue(media.published.isEmpty())
        assertFalse(clip.exists())
    }

    @Test
    fun saveActiveCapture_stopsRecordingAndPublishes() = runTest {
        val context = RobolectricPermissions.applicationContext()
        val clip = File.createTempFile("clip", ".mp4").also { it.writeText("video") }
        camera.recordingResult = Result.success(clip)
        val vm = viewModel(
            initial = CameraUiState(
                mode = CameraMode.VIDEO,
                microphonePermissionGranted = true,
                isMuted = true
            )
        )
        vm.setOutputDirectory(File("out"))
        vm.onShutter(PreviewView(context))
        assertTrue(vm.uiState.value.isRecording)

        vm.saveActiveCapture()
        assertEquals(1, camera.stopRecordingCalls)
        camera.emitRecording(RecordingEvent.Finalized(success = true))
        advanceUntilIdle()

        assertFalse(vm.uiState.value.isRecording)
        assertEquals(listOf(clip), media.published)
    }
}
