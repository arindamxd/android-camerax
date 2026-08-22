package com.arindam.camerax.testing

import com.arindam.camerax.domain.model.CameraBindConfig
import com.arindam.camerax.domain.model.CameraBindResult
import com.arindam.camerax.domain.model.CameraHost
import com.arindam.camerax.domain.model.CameraLens
import com.arindam.camerax.domain.model.ColorFilterType
import com.arindam.camerax.domain.model.ExposurePriority
import com.arindam.camerax.domain.model.FlashMode
import com.arindam.camerax.domain.model.LowLightBoost
import com.arindam.camerax.domain.model.NightScene
import com.arindam.camerax.domain.model.RecordingEvent
import com.arindam.camerax.domain.model.ZoomInfo
import com.arindam.camerax.domain.repository.CameraRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

/** In-memory [CameraRepository] for unit tests. */
class FakeCameraRepository : CameraRepository {

    override val nightScene: StateFlow<NightScene> = MutableStateFlow(NightScene.UNKNOWN).asStateFlow()
    override val lowLightBoost: StateFlow<LowLightBoost> =
        MutableStateFlow(LowLightBoost.OFF).asStateFlow()
    private val _recordingEvents = MutableSharedFlow<RecordingEvent>(extraBufferCapacity = 16)
    override val recordingEvents: SharedFlow<RecordingEvent> = _recordingEvents.asSharedFlow()

    var lastFlash: FlashMode? = null
    var lastBindConfig: CameraBindConfig? = null
    var captureResult: Result<File> = Result.failure(IllegalStateException("No capture"))
    var recordingResult: Result<File> = Result.failure(IllegalStateException("No recording"))
    var released = false

    override suspend fun bind(host: CameraHost, config: CameraBindConfig): CameraBindResult {
        lastBindConfig = config
        return CameraBindResult(
            hasFlash = true,
            minZoom = 1f,
            maxZoom = 10f,
            zoomRatio = 1f,
            videoAvailable = true,
            supportedExtensions = emptySet()
        )
    }

    override suspend fun capturePhoto(
        outputDirectory: File,
        lens: CameraLens,
        colorFilter: ColorFilterType,
        motionPhoto: Boolean
    ): Result<File> = captureResult

    override fun startRecording(
        outputDirectory: File,
        muted: Boolean,
        persistent: Boolean
    ): Result<File> = recordingResult

    override fun pauseRecording() = Unit
    override fun resumeRecording() = Unit
    override fun stopRecording() = Unit
    override fun muteRecording(muted: Boolean) = Unit
    override fun setFlash(mode: FlashMode) {
        lastFlash = mode
    }
    override fun setLowLightBoost(enabled: Boolean) = Unit
    override fun setZoomRatio(ratio: Float): ZoomInfo? = ZoomInfo(ratio, 1f, 10f)
    override fun tapToFocus(x: Float, y: Float) = Unit
    override fun setColorFilter(type: ColorFilterType) = Unit
    override fun setTargetRotation(rotation: Int) = Unit
    override fun setExposure(priority: ExposurePriority, iso: Int, shutterNanos: Long) = Unit
    override fun setExposureCompensation(index: Int) = Unit
    override fun release() {
        released = true
    }

    suspend fun emitRecording(event: RecordingEvent) {
        _recordingEvents.emit(event)
    }
}
