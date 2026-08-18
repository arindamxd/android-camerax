package com.arindam.camerax.data.camera

import android.content.Context
import androidx.camera.core.DynamicRange
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.HighSpeedVideoSessionConfig
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.VideoCapture
import androidx.core.content.ContextCompat
import com.arindam.camerax.domain.model.VideoHdrRange
import com.arindam.camerax.domain.model.VideoQuality
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

suspend fun supportedVideoQualities(context: Context): List<VideoQuality> {
    val provider: ProcessCameraProvider? = suspendCoroutine { continuation ->
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            continuation.resume(runCatching { future.get() }.getOrNull())
        }, ContextCompat.getMainExecutor(context))
    }
    if (provider == null) return VideoQuality.entries
    val found = linkedSetOf<VideoQuality>()
    provider.availableCameraInfos.forEach { info ->
        val fromCapabilities = runCatching {
            val capabilities = Recorder.getVideoCapabilities(info)
            capabilities.getSupportedDynamicRanges().flatMap { range ->
                capabilities.getSupportedQualities(range)
            }
        }.getOrDefault(emptyList())
        val fromSelector = runCatching {
            QualitySelector.getSupportedQualities(info)
        }.getOrDefault(emptyList())
        (fromCapabilities + fromSelector).mapNotNull { quality -> quality.toVideoQuality() }
            .forEach { found += it }
    }
    val ordered = VideoQuality.entries.filter { it in found }
    return ordered.ifEmpty { VideoQuality.entries }
}

suspend fun supportedVideoHdrRanges(context: Context): List<VideoHdrRange> {
    val provider: ProcessCameraProvider? = suspendCoroutine { continuation ->
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            continuation.resume(runCatching { future.get() }.getOrNull())
        }, ContextCompat.getMainExecutor(context))
    }
    if (provider == null) return listOf(VideoHdrRange.SDR)
    val found = linkedSetOf<VideoHdrRange>()
    provider.availableCameraInfos.forEach { info ->
        val ranges = runCatching {
            Recorder.getVideoCapabilities(info).supportedDynamicRanges
        }.getOrDefault(emptySet())
        ranges.mapNotNull { range -> range.toVideoHdrRange() }.forEach { found += it }
    }
    val ordered = VideoHdrRange.entries.filter { it in found }
    return if (VideoHdrRange.SDR in ordered) ordered else listOf(VideoHdrRange.SDR) + ordered
}

fun Quality.toVideoQuality(): VideoQuality? = when (this) {
    Quality.SD -> VideoQuality.SD
    Quality.HD -> VideoQuality.HD
    Quality.FHD -> VideoQuality.FHD
    Quality.UHD -> VideoQuality.UHD
    else -> null
}

data class SlowMotionOptions(
    val qualities: List<VideoQuality> = emptyList(),
    val frameRates: List<Int> = emptyList()
) {
    val available: Boolean get() = qualities.isNotEmpty() && frameRates.isNotEmpty()
}

suspend fun slowMotionOptions(context: Context): SlowMotionOptions {
    val provider: ProcessCameraProvider? = suspendCoroutine { continuation ->
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            continuation.resume(runCatching { future.get() }.getOrNull())
        }, ContextCompat.getMainExecutor(context))
    }
    if (provider == null) return SlowMotionOptions()
    val qualities = linkedSetOf<VideoQuality>()
    val frameRates = linkedSetOf<Int>()
    provider.availableCameraInfos.forEach { info ->
        val capabilities = runCatching {
            Recorder.getHighSpeedVideoCapabilities(info)
        }.getOrNull() ?: return@forEach
        val hsQualities = capabilities.getSupportedQualities(DynamicRange.SDR)
        hsQualities.mapNotNull { quality -> quality.toVideoQuality() }.forEach { qualities += it }
        if (hsQualities.isEmpty()) return@forEach
        runCatching {
            val recorder = Recorder.Builder()
                .setQualitySelector(QualitySelector.fromOrderedList(hsQualities))
                .build()
            val video = VideoCapture.withOutput(recorder)
            val preview = Preview.Builder().build()
            val probe = HighSpeedVideoSessionConfig.Builder(video)
                .setPreview(preview)
                .setSlowMotionEnabled(true)
                .build()
            info.getSupportedFrameRateRanges(probe).forEach { range -> frameRates += range.upper }
        }
    }
    return SlowMotionOptions(
        qualities = VideoQuality.entries.filter { it in qualities },
        frameRates = frameRates.sorted()
    )
}

suspend fun isVideoStabilizationSupported(context: Context): Boolean {
    val provider: ProcessCameraProvider? = suspendCoroutine { continuation ->
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            continuation.resume(runCatching { future.get() }.getOrNull())
        }, ContextCompat.getMainExecutor(context))
    }
    if (provider == null) return false
    return provider.availableCameraInfos.any { info ->
        val preview = runCatching {
            Preview.getPreviewCapabilities(info).isStabilizationSupported
        }.getOrDefault(false)
        val video = runCatching {
            Recorder.getVideoCapabilities(info).isStabilizationSupported
        }.getOrDefault(false)
        preview || video
    }
}

suspend fun isRawCaptureSupported(context: Context): Boolean {
    val provider: ProcessCameraProvider? = suspendCoroutine { continuation ->
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            continuation.resume(runCatching { future.get() }.getOrNull())
        }, ContextCompat.getMainExecutor(context))
    }
    if (provider == null) return false
    return provider.availableCameraInfos.any { info -> info.supportsRawJpeg() }
}

suspend fun isFullSensorRawSupported(context: Context): Boolean {
    val provider: ProcessCameraProvider? = suspendCoroutine { continuation ->
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            continuation.resume(runCatching { future.get() }.getOrNull())
        }, ContextCompat.getMainExecutor(context))
    }
    if (provider == null) return false
    return provider.availableCameraInfos.any { info -> info.supportsFullSensorRaw(context) }
}

suspend fun isUltraHdrSupported(context: Context): Boolean {
    val provider: ProcessCameraProvider? = suspendCoroutine { continuation ->
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            continuation.resume(runCatching { future.get() }.getOrNull())
        }, ContextCompat.getMainExecutor(context))
    }
    if (provider == null) return false
    return provider.availableCameraInfos.any { info -> info.supportsUltraHdr() }
}

suspend fun isLowLightBoostSupported(context: Context): Boolean {
    val provider: ProcessCameraProvider? = suspendCoroutine { continuation ->
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            continuation.resume(runCatching { future.get() }.getOrNull())
        }, ContextCompat.getMainExecutor(context))
    }
    if (provider == null) return false
    return provider.availableCameraInfos.any { info -> info.isLowLightBoostSupported }
}
