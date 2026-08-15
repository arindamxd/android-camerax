package com.arindam.camerax.domain.usecase

import com.arindam.camerax.domain.model.CameraBindConfig
import com.arindam.camerax.domain.model.CameraBindResult
import com.arindam.camerax.domain.model.CameraHost
import com.arindam.camerax.domain.model.CameraLens
import com.arindam.camerax.domain.model.ColorFilterType
import com.arindam.camerax.domain.model.FlashMode
import com.arindam.camerax.domain.model.RecordingEvent
import com.arindam.camerax.domain.model.ZoomInfo
import com.arindam.camerax.domain.repository.CameraRepository
import com.arindam.camerax.domain.repository.MediaRepository
import java.io.File

class BindCamera(private val repository: CameraRepository) {
    suspend operator fun invoke(host: CameraHost, config: CameraBindConfig): CameraBindResult =
        repository.bind(host, config)
}

class CapturePhoto(private val repository: CameraRepository) {
    operator fun invoke(
        outputDirectory: File,
        lens: CameraLens,
        colorFilter: ColorFilterType,
        onSaved: (File) -> Unit,
        onError: (String) -> Unit
    ) = repository.capturePhoto(outputDirectory, lens, colorFilter, onSaved, onError)
}

class StartRecording(private val repository: CameraRepository) {
    operator fun invoke(
        outputDirectory: File,
        muted: Boolean,
        onEvent: (RecordingEvent) -> Unit,
        onError: (String) -> Unit
    ): File? = repository.startRecording(outputDirectory, muted, onEvent, onError)
}

class PauseRecording(private val repository: CameraRepository) {
    operator fun invoke() = repository.pauseRecording()
}

class ResumeRecording(private val repository: CameraRepository) {
    operator fun invoke() = repository.resumeRecording()
}

class StopRecording(private val repository: CameraRepository) {
    operator fun invoke() = repository.stopRecording()
}

class MuteRecording(private val repository: CameraRepository) {
    operator fun invoke(muted: Boolean) = repository.muteRecording(muted)
}

class SetFlash(private val repository: CameraRepository) {
    operator fun invoke(mode: FlashMode) = repository.setFlash(mode)
}

class SetZoom(private val repository: CameraRepository) {
    operator fun invoke(ratio: Float): ZoomInfo? = repository.setZoomRatio(ratio)
}

class TapToFocus(private val repository: CameraRepository) {
    operator fun invoke(x: Float, y: Float) = repository.tapToFocus(x, y)
}

class SetColorFilter(private val repository: CameraRepository) {
    operator fun invoke(type: ColorFilterType) = repository.setColorFilter(type)
}

class ReleaseCamera(private val repository: CameraRepository) {
    operator fun invoke() = repository.release()
}

class GetLatestMedia(private val repository: MediaRepository) {
    operator fun invoke(directory: File): File? = repository.latest(directory)
}
