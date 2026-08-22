package com.arindam.camerax.domain.repository

import com.arindam.camerax.domain.model.CameraBindConfig
import com.arindam.camerax.domain.model.CameraBindResult
import com.arindam.camerax.domain.model.CameraHost
import com.arindam.camerax.domain.model.CameraLens
import com.arindam.camerax.domain.model.EffectMode
import com.arindam.camerax.domain.model.ExposurePriority
import com.arindam.camerax.domain.model.FlashMode
import com.arindam.camerax.domain.model.LowLightBoost
import com.arindam.camerax.domain.model.NightScene
import com.arindam.camerax.domain.model.RecordingEvent
import com.arindam.camerax.domain.model.ZoomInfo
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import android.graphics.Bitmap
import java.io.File

/**
 * CameraX session without CameraX / Compose types. UI and use cases depend on this, not on
 * [com.arindam.camerax.data.camera.CameraSession].
 */
interface CameraRepository {

    val nightScene: StateFlow<NightScene>
    val lowLightBoost: StateFlow<LowLightBoost>
    val recordingEvents: SharedFlow<RecordingEvent>
    val effectFrame: StateFlow<Bitmap?>

    suspend fun bind(host: CameraHost, config: CameraBindConfig): CameraBindResult

    suspend fun capturePhoto(
        outputDirectory: File,
        lens: CameraLens,
        effect: EffectMode,
        motionPhoto: Boolean
    ): Result<File>

    fun startRecording(
        outputDirectory: File,
        muted: Boolean,
        persistent: Boolean = false
    ): Result<File>

    fun pauseRecording()
    fun resumeRecording()
    fun stopRecording()
    fun muteRecording(muted: Boolean)
    fun setFlash(mode: FlashMode)
    fun setLowLightBoost(enabled: Boolean)
    fun setZoomRatio(ratio: Float): ZoomInfo?
    fun tapToFocus(x: Float, y: Float)
    fun setEffect(type: EffectMode)
    fun setTargetRotation(rotation: Int)
    fun setExposure(priority: ExposurePriority, iso: Int, shutterNanos: Long)
    fun setExposureCompensation(index: Int)
    fun release()
}

/**
 * App pictures directory and MediaStore publish. Panorama stitch lives here so CameraX stays
 * out of still-processing.
 */
interface MediaRepository {
    fun picturesDirectory(): File
    fun latest(directory: File = picturesDirectory()): File?
    fun list(directory: File = picturesDirectory()): List<File>
    fun delete(file: File): Boolean
    fun stitchPanorama(frames: List<File>, outputDirectory: File): Result<File>
    fun publish(file: File): Result<Unit>
}
