/**
 * ViewModel-facing use cases. Each class is a thin wrapper over [CameraRepository] or
 * [MediaRepository] so new camera features stay out of Compose and out of [CameraSession] call sites.
 */
package com.arindam.camerax.domain.usecase

import com.arindam.camerax.domain.model.CameraBindConfig
import com.arindam.camerax.domain.model.CameraBindResult
import com.arindam.camerax.domain.model.CameraHost
import com.arindam.camerax.domain.model.CameraLens
import com.arindam.camerax.domain.model.CaptureSettings
import com.arindam.camerax.domain.model.ColorFilterType
import com.arindam.camerax.domain.model.DeviceCaptureFeatures
import com.arindam.camerax.domain.model.ExposurePriority
import com.arindam.camerax.domain.model.FlashMode
import com.arindam.camerax.domain.model.LowLightBoost
import com.arindam.camerax.domain.model.NightScene
import com.arindam.camerax.domain.model.RecordingEvent
import com.arindam.camerax.domain.model.ZoomInfo
import com.arindam.camerax.domain.repository.CameraRepository
import com.arindam.camerax.domain.repository.DeviceFeaturesRepository
import com.arindam.camerax.domain.repository.MediaRepository
import com.arindam.camerax.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File

/** Bind preview and capture use cases for [CameraBindConfig]. */
class BindCamera(private val repository: CameraRepository) {
    suspend operator fun invoke(host: CameraHost, config: CameraBindConfig): CameraBindResult = repository.bind(host, config)
}

/** Still capture, including motion photo when [motionPhoto] is true. */
class CapturePhoto(private val repository: CameraRepository) {
    suspend operator fun invoke(
        outputDirectory: File,
        lens: CameraLens,
        colorFilter: ColorFilterType,
        motionPhoto: Boolean
    ): Result<File> = repository.capturePhoto(outputDirectory, lens, colorFilter, motionPhoto)
}

/** Start video. [persistent] keeps the clip across a lens flip when Settings allows it. */
class StartRecording(private val repository: CameraRepository) {
    operator fun invoke(
        outputDirectory: File,
        muted: Boolean,
        persistent: Boolean = false
    ): Result<File> = repository.startRecording(outputDirectory, muted, persistent)
}

/** Pause the active recording. */
class PauseRecording(private val repository: CameraRepository) {
    operator fun invoke() = repository.pauseRecording()
}

/** Resume a paused recording. */
class ResumeRecording(private val repository: CameraRepository) {
    operator fun invoke() = repository.resumeRecording()
}

/** Finalize the active recording. */
class StopRecording(private val repository: CameraRepository) {
    operator fun invoke() = repository.stopRecording()
}

/** Mute or unmute microphone on the active recording. */
class MuteRecording(private val repository: CameraRepository) {
    operator fun invoke(muted: Boolean) = repository.muteRecording(muted)
}

/** Flash / torch. Does not rebind. */
class SetFlash(private val repository: CameraRepository) {
    operator fun invoke(mode: FlashMode) = repository.setFlash(mode)
}

/** Low-light boost. Does not rebind. */
class SetLowLightBoost(private val repository: CameraRepository) {
    operator fun invoke(enabled: Boolean) = repository.setLowLightBoost(enabled)
}

/** Digital zoom. Does not rebind. */
class SetZoom(private val repository: CameraRepository) {
    operator fun invoke(ratio: Float): ZoomInfo? = repository.setZoomRatio(ratio)
}

/** Metering AF/AE at viewfinder normalized coordinates. */
class TapToFocus(private val repository: CameraRepository) {
    operator fun invoke(x: Float, y: Float) = repository.tapToFocus(x, y)
}

/** Live filter chip. Same pipeline does not rebind; matrix ↔ Media3 does. */
class SetColorFilter(private val repository: CameraRepository) {
    operator fun invoke(type: ColorFilterType) = repository.setColorFilter(type)
}

/** Large-screen rotation. Does not rebind. */
class SetTargetRotation(private val repository: CameraRepository) {
    operator fun invoke(rotation: Int) = repository.setTargetRotation(rotation)
}

/** Hybrid AE (ISO / shutter priority). Does not rebind. */
class SetExposure(private val repository: CameraRepository) {
    operator fun invoke(priority: ExposurePriority, iso: Int, shutterNanos: Long) = repository.setExposure(priority, iso, shutterNanos)
}

/** Exposure compensation index. Does not rebind. */
class SetExposureCompensation(private val repository: CameraRepository) {
    operator fun invoke(index: Int) = repository.setExposureCompensation(index)
}

/** Night-scene indicator for auto-switch. */
class ObserveNightScene(private val repository: CameraRepository) {
    operator fun invoke(): StateFlow<NightScene> = repository.nightScene
}

/** Low-light boost HUD state. */
class ObserveLowLightBoost(private val repository: CameraRepository) {
    operator fun invoke(): StateFlow<LowLightBoost> = repository.lowLightBoost
}

/** Video recording status / finalize events. */
class ObserveRecording(private val repository: CameraRepository) {
    operator fun invoke(): SharedFlow<RecordingEvent> = repository.recordingEvents
}

/** Unbind and drop CameraX handles. */
class ReleaseCamera(private val repository: CameraRepository) {
    operator fun invoke() = repository.release()
}

/** Latest file in the app pictures directory (gallery thumb). */
class GetLatestMedia(private val repository: MediaRepository) {
    operator fun invoke(directory: File = repository.picturesDirectory()): File? =
        repository.latest(directory)
}

/** App pictures directory (DCIM is publish-only). */
class PicturesDirectory(private val repository: MediaRepository) {
    operator fun invoke(): File = repository.picturesDirectory()
}

/** Files in the app pictures directory, newest first. */
class ListMedia(private val repository: MediaRepository) {
    operator fun invoke(directory: File = repository.picturesDirectory()): List<File> =
        repository.list(directory)
}

/** Delete a capture from the app pictures directory. */
class DeleteMedia(private val repository: MediaRepository) {
    operator fun invoke(file: File): Boolean = repository.delete(file)
}

/** Horizontal sweep stitch for [com.arindam.camerax.domain.model.CameraMode.PANORAMA]. */
class StitchPanorama(private val repository: MediaRepository) {
    operator fun invoke(frames: List<File>, outputDirectory: File): Result<File> =
        repository.stitchPanorama(frames, outputDirectory)
}

/** Copy into DCIM/CameraX so the system gallery lists the capture. */
class PublishMedia(private val repository: MediaRepository) {
    operator fun invoke(file: File): Result<Unit> = repository.publish(file)
}

/** Bind flags from Settings. */
class LoadCaptureSettings(private val repository: SettingsRepository) {
    operator fun invoke(): CaptureSettings = repository.loadCaptureSettings()
}

/** CameraX capability probe (one provider fetch). */
class ProbeDeviceFeatures(private val repository: DeviceFeaturesRepository) {
    suspend operator fun invoke(): DeviceCaptureFeatures = repository.probe()
}
