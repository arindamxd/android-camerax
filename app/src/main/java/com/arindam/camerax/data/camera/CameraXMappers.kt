package com.arindam.camerax.data.camera

import android.content.Context
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import androidx.annotation.OptIn
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.DynamicRange
import androidx.camera.core.ImageCapture
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.extensions.ExtensionMode
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.VideoRecordEvent
import androidx.lifecycle.LifecycleOwner
import androidx.camera.view.PreviewView
import com.arindam.camerax.domain.model.CameraExtension
import com.arindam.camerax.domain.model.CameraHost
import com.arindam.camerax.domain.model.CameraLens
import com.arindam.camerax.domain.model.CaptureAspect
import com.arindam.camerax.domain.model.FlashMode
import com.arindam.camerax.domain.model.RecordingEvent
import com.arindam.camerax.domain.model.StillFormat
import com.arindam.camerax.domain.model.VideoHdrRange
import com.arindam.camerax.domain.model.VideoQuality

/**
 * Data: adapts CameraX [PreviewView] to domain [CameraHost] for [BindCamera].
 * The ViewModel may construct [PreviewViewHost]; it must not construct [CameraSession].
 */
class PreviewViewHost(
    val lifecycleOwner: LifecycleOwner,
    val previewView: PreviewView,
    val pipPreviewView: PreviewView? = null
) : CameraHost

fun CameraLens.toSelector(): CameraSelector = when (this) {
    CameraLens.BACK -> CameraSelector.DEFAULT_BACK_CAMERA
    CameraLens.FRONT -> CameraSelector.DEFAULT_FRONT_CAMERA
}

fun FlashMode.toImageCaptureMode(): Int = when (this) {
    FlashMode.OFF, FlashMode.TORCH -> ImageCapture.FLASH_MODE_OFF
    FlashMode.ON -> ImageCapture.FLASH_MODE_ON
    FlashMode.AUTO -> ImageCapture.FLASH_MODE_AUTO
}

fun CameraExtension.toExtensionMode(): Int = when (this) {
    CameraExtension.NONE -> ExtensionMode.NONE
    CameraExtension.HDR -> ExtensionMode.HDR
    CameraExtension.NIGHT -> ExtensionMode.NIGHT
    CameraExtension.PORTRAIT -> ExtensionMode.BOKEH
    CameraExtension.BEAUTY -> ExtensionMode.FACE_RETOUCH
}

fun CaptureAspect.toResolutionSelector(): ResolutionSelector? = when (this) {
    CaptureAspect.FULL -> null
    CaptureAspect.RATIO_4_3 -> ResolutionSelector.Builder()
        .setAspectRatioStrategy(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY)
        .build()
    CaptureAspect.RATIO_16_9 -> ResolutionSelector.Builder()
        .setAspectRatioStrategy(AspectRatioStrategy.RATIO_16_9_FALLBACK_AUTO_STRATEGY)
        .build()
}

fun VideoQuality.toQuality(): Quality = when (this) {
    VideoQuality.SD -> Quality.SD
    VideoQuality.HD -> Quality.HD
    VideoQuality.FHD -> Quality.FHD
    VideoQuality.UHD -> Quality.UHD
}

fun VideoQuality.toQualitySelector(): QualitySelector {
    return QualitySelector.from(toQuality(), FallbackStrategy.lowerQualityOrHigherThan(Quality.SD))
}

fun VideoHdrRange.toDynamicRange(): DynamicRange = when (this) {
    VideoHdrRange.SDR -> DynamicRange.SDR
    VideoHdrRange.HLG10 -> DynamicRange.HLG_10_BIT
    VideoHdrRange.HDR10 -> DynamicRange.HDR10_10_BIT
    VideoHdrRange.HDR10_PLUS -> DynamicRange.HDR10_PLUS_10_BIT
    VideoHdrRange.DOLBY_VISION -> DynamicRange.DOLBY_VISION_10_BIT
}

fun DynamicRange.toVideoHdrRange(): VideoHdrRange? = when (this) {
    DynamicRange.SDR -> VideoHdrRange.SDR
    DynamicRange.HLG_10_BIT -> VideoHdrRange.HLG10
    DynamicRange.HDR10_10_BIT -> VideoHdrRange.HDR10
    DynamicRange.HDR10_PLUS_10_BIT -> VideoHdrRange.HDR10_PLUS
    DynamicRange.DOLBY_VISION_10_BIT -> VideoHdrRange.DOLBY_VISION
    else -> null
}

fun heicUltraHdrOutputFormat(): Int? =
    ImageCapture::class.java.fields
        .firstOrNull { it.name.contains("HEIC", ignoreCase = true) }
        ?.let { runCatching { it.getInt(null) }.getOrNull() }

fun CameraInfo.supportedStillFormats(): Set<Int> = runCatching {
    ImageCapture.getImageCaptureCapabilities(this).supportedOutputFormats
}.getOrDefault(emptySet())

fun CameraInfo.supportsUltraHdr(): Boolean =
    ImageCapture.OUTPUT_FORMAT_JPEG_ULTRA_HDR in supportedStillFormats() ||
        supportsHeicUltraHdr()

fun CameraInfo.supportsJpegUltraHdr(): Boolean =
    ImageCapture.OUTPUT_FORMAT_JPEG_ULTRA_HDR in supportedStillFormats()

fun CameraInfo.supportsHeicUltraHdr(): Boolean {
    val heic = heicUltraHdrOutputFormat() ?: return false
    return heic in supportedStillFormats()
}

fun CameraInfo.supportsRawJpeg(): Boolean =
    ImageCapture.OUTPUT_FORMAT_RAW_JPEG in supportedStillFormats()

@OptIn(ExperimentalCamera2Interop::class)
fun CameraInfo.supportsFullSensorRaw(context: Context): Boolean {
    val camera2 = runCatching { Camera2CameraInfo.from(this) }.getOrNull() ?: return false
    val manager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
    val logical = runCatching { manager?.getCameraCharacteristics(camera2.cameraId) }.getOrNull()
    val caps = logical?.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
        ?: camera2.getCameraCharacteristic(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
    if (caps?.contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_ULTRA_HIGH_RESOLUTION_SENSOR) == true) {
        return true
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val maxMap = logical?.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP_MAXIMUM_RESOLUTION)
            ?: camera2.getCameraCharacteristic(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP_MAXIMUM_RESOLUTION)
        if (maxMap?.getOutputSizes(ImageFormat.RAW_SENSOR)?.isNotEmpty() == true) return true
    }
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P || logical == null || manager == null) {
        return false
    }
    return logical.physicalCameraIds.any { physicalId ->
        runCatching {
            val physical = manager.getCameraCharacteristics(physicalId)
            val physicalCaps = physical.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
            val ultra = physicalCaps?.contains(
                CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_ULTRA_HIGH_RESOLUTION_SENSOR
            ) == true
            val maxSize = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                physical.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP_MAXIMUM_RESOLUTION)
                    ?.getOutputSizes(ImageFormat.RAW_SENSOR)
                    ?.isNotEmpty() == true
            } else {
                false
            }
            ultra || maxSize
        }.getOrDefault(false)
    }
}

fun resolveStillOutput(
    info: CameraInfo,
    ultraHdr: Boolean,
    rawCapture: Boolean
): Pair<Int, StillFormat> {
    val supported = info.supportedStillFormats()
    if (rawCapture && ImageCapture.OUTPUT_FORMAT_RAW_JPEG in supported) {
        return ImageCapture.OUTPUT_FORMAT_RAW_JPEG to StillFormat.RAW_JPEG
    }
    if (!ultraHdr) return ImageCapture.OUTPUT_FORMAT_JPEG to StillFormat.JPEG
    val heic = heicUltraHdrOutputFormat()
    if (heic != null && heic in supported) return heic to StillFormat.HEIC_ULTRA_HDR
    if (ImageCapture.OUTPUT_FORMAT_JPEG_ULTRA_HDR in supported) {
        return ImageCapture.OUTPUT_FORMAT_JPEG_ULTRA_HDR to StillFormat.JPEG_ULTRA_HDR
    }
    return ImageCapture.OUTPUT_FORMAT_JPEG to StillFormat.JPEG
}

fun VideoRecordEvent.toDomain(): RecordingEvent? = when (this) {
    is VideoRecordEvent.Status -> RecordingEvent.Status(recordingStats.recordedDurationNanos)
    is VideoRecordEvent.Pause -> RecordingEvent.Paused
    is VideoRecordEvent.Resume -> RecordingEvent.Resumed
    is VideoRecordEvent.Finalize -> RecordingEvent.Finalized(success = !hasError())
    else -> null
}
