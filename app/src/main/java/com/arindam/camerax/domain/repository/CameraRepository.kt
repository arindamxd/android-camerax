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
 * Domain: CameraX session without CameraX / Compose types. UI and use cases depend on this, not on
 * [com.arindam.camerax.data.camera.CameraSession].
 */
interface CameraRepository {

    /** OEM night-scene indicator (API 36+); drives auto Night extension when enabled. */
    val nightScene: StateFlow<NightScene>

    /** Low-light boost state from the bound camera. */
    val lowLightBoost: StateFlow<LowLightBoost>

    /** Recording lifecycle events (status, pause, finalize). */
    val recordingEvents: SharedFlow<RecordingEvent>

    /** Latest analyzed frame in Effects mode; null when not in Effects. */
    val effectFrame: StateFlow<Bitmap?>

    /** Binds preview, capture, and video use cases for [config]. May rebind the provider. */
    suspend fun bind(host: CameraHost, config: CameraBindConfig): CameraBindResult

    /** Captures a still (or motion photo when [motionPhoto] is true) into [outputDirectory]. */
    suspend fun capturePhoto(
        outputDirectory: File,
        lens: CameraLens,
        effect: EffectMode,
        motionPhoto: Boolean
    ): Result<File>

    /** Starts video recording; returns the output file on success. */
    fun startRecording(
        outputDirectory: File,
        muted: Boolean,
        persistent: Boolean = false
    ): Result<File>

    fun pauseRecording()
    fun resumeRecording()
    fun stopRecording()
    fun muteRecording(muted: Boolean)

    /** Updates flash / torch without rebinding. */
    fun setFlash(mode: FlashMode)

    /** Enables low-light boost when supported; no rebind. */
    fun setLowLightBoost(enabled: Boolean)

    /** Sets zoom ratio; returns updated [ZoomInfo] when available. */
    fun setZoomRatio(ratio: Float): ZoomInfo?

    /** Tap-to-focus at normalized view coordinates. */
    fun tapToFocus(x: Float, y: Float)

    /** Updates live / still color-matrix effect without rebinding. */
    fun setEffect(type: EffectMode)

    /** Updates target rotation for capture; no rebind. */
    fun setTargetRotation(rotation: Int)

    /** Hybrid AE priority (ISO / shutter) when supported. */
    fun setExposure(priority: ExposurePriority, iso: Int, shutterNanos: Long)

    /** Exposure compensation index; no rebind. */
    fun setExposureCompensation(index: Int)

    fun unbind()
    fun release()
}

/**
 * Domain: app pictures directory and MediaStore publish. Panorama stitch lives here so CameraX
 * stays out of still-processing. List / delete / stitch / publish are suspend and must run off
 * the main thread in the data layer.
 */
interface MediaRepository {

    /** App-scoped pictures directory; may touch disk on first call — warm from a background thread. */
    fun picturesDirectory(): File

    /** Newest capture in [directory], preferring JPEG over companion DNG. */
    suspend fun latest(directory: File = picturesDirectory()): File?

    /** All gallery items in [directory], newest first. */
    suspend fun list(directory: File = picturesDirectory()): List<File>

    suspend fun delete(file: File): Boolean

    suspend fun stitchPanorama(frames: List<File>, outputDirectory: File): Result<File>

    /** Copies [file] into DCIM/CameraX via MediaStore. */
    suspend fun publish(file: File): Result<Unit>
}
