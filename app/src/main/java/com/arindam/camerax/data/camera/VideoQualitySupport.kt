package com.arindam.camerax.data.camera

import android.content.Context
import android.util.Range
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
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

/** Matches CameraX `HighSpeedVideoSessionConfig`: high-speed is at least 120 FPS. */
internal const val MIN_HIGH_SPEED_FPS = 120

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

/**
 * CameraX Slow-mo sample (`camerax-slowmotion`): [Recorder.getHighSpeedVideoCapabilities],
 * SDR quality, then [HighSpeedVideoSessionConfig] (preview + video) frame rates.
 * Preview+video high-speed sessions only advertise **fixed** ranges (120/120, 240/240, …).
 * Variable AE ranges such as 30–120 are regular video, not slow motion.
 */
fun CameraInfo.highSpeedSlowMotionOptions(): SlowMotionOptions {
    val capabilities = runCatching {
        Recorder.getHighSpeedVideoCapabilities(this)
    }.getOrNull() ?: return SlowMotionOptions()
    val hsQualities = capabilities.getSupportedQualities(DynamicRange.SDR)
    val quality = hsQualities.firstOrNull() ?: return SlowMotionOptions()
    val qualities = hsQualities.mapNotNull { item -> item.toVideoQuality() }
    if (qualities.isEmpty()) return SlowMotionOptions()
    val frameRates = runCatching {
        val recorder = Recorder.Builder()
            .setQualitySelector(QualitySelector.from(quality))
            .build()
        val video = VideoCapture.withOutput(recorder)
        val preview = Preview.Builder().build()
        val probe = HighSpeedVideoSessionConfig(video, preview)
        getSupportedFrameRateRanges(probe).highSpeedFrameRateRanges().map { range -> range.upper }
    }.getOrDefault(emptyList())
    if (frameRates.isEmpty()) return SlowMotionOptions()
    return SlowMotionOptions(
        qualities = VideoQuality.entries.filter { it in qualities.toSet() },
        frameRates = frameRates.distinct().sorted()
    )
}

fun CameraInfo.supportsHighSpeedSlowMotion(): Boolean = highSpeedSlowMotionOptions().available

internal fun Collection<Range<Int>>.highSpeedFrameRateRanges(): List<Range<Int>> =
    filter { range -> range.lower == range.upper && range.upper >= MIN_HIGH_SPEED_FPS }
        .distinct()
        .sortedBy { it.upper }

suspend fun slowMotionOptions(context: Context): SlowMotionOptions {
    val provider: ProcessCameraProvider? = suspendCoroutine { continuation ->
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            continuation.resume(runCatching { future.get() }.getOrNull())
        }, ContextCompat.getMainExecutor(context))
    }
    if (provider == null) return SlowMotionOptions()
    val back = CameraSelector.DEFAULT_BACK_CAMERA
        .filter(provider.availableCameraInfos)
        .firstOrNull()
        ?: return SlowMotionOptions()
    return back.highSpeedSlowMotionOptions()
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
        runCatching {
            Recorder.getVideoCapabilities(info).isStabilizationSupported
        }.getOrDefault(false)
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
