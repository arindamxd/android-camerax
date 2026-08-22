package com.arindam.camerax.data.camera

/**
 * Device capability probes for Settings and mode availability (qualities, HDR, slo-mo, 60 fps).
 */

import android.content.Context
import android.content.pm.PackageManager
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
import com.arindam.camerax.domain.model.DeviceCaptureFeatures
import com.arindam.camerax.domain.model.SlowMotionOptions
import com.arindam.camerax.domain.model.VideoHdrRange
import com.arindam.camerax.domain.model.VideoQuality
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/** Matches CameraX `HighSpeedVideoSessionConfig`: high-speed is at least 120 FPS. */
internal const val MIN_HIGH_SPEED_FPS = 120

private suspend fun awaitCameraProvider(context: Context): ProcessCameraProvider? =
    suspendCoroutine { continuation ->
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            continuation.resume(runCatching { future.get() }.getOrNull())
        }, ContextCompat.getMainExecutor(context))
    }

/** One CameraX provider fetch for Settings and the mode pager. */
suspend fun probeDeviceFeatures(context: Context): DeviceCaptureFeatures {
    val provider = awaitCameraProvider(context) ?: return DeviceCaptureFeatures()
    val infos = provider.availableCameraInfos
    val back = CameraSelector.DEFAULT_BACK_CAMERA.filter(infos).firstOrNull()
    val qualities = linkedSetOf<VideoQuality>()
    val hdr = linkedSetOf<VideoHdrRange>()
    infos.forEach { info ->
        runCatching {
            val capabilities = Recorder.getVideoCapabilities(info)
            capabilities.getSupportedDynamicRanges().flatMap { range ->
                capabilities.getSupportedQualities(range)
            }
        }.getOrDefault(emptyList()).mapNotNull { quality -> quality.toVideoQuality() }
            .forEach { qualities += it }
        runCatching {
            Recorder.getVideoCapabilities(info).supportedDynamicRanges
        }.getOrDefault(emptySet()).mapNotNull { range -> range.toVideoHdrRange() }
            .forEach { hdr += it }
    }
    val orderedQualities = VideoQuality.entries.filter { it in qualities }.ifEmpty { VideoQuality.entries }
    val orderedHdr = VideoHdrRange.entries.filter { it in hdr }.let { ordered ->
        if (VideoHdrRange.SDR in ordered) ordered else listOf(VideoHdrRange.SDR) + ordered
    }
    return DeviceCaptureFeatures(
        slowMotion = back?.highSpeedSlowMotionOptions() ?: SlowMotionOptions(),
        concurrent = isDualCameraSupported(context, provider),
        videoQualities = orderedQualities,
        videoHdrRanges = orderedHdr.ifEmpty { listOf(VideoHdrRange.SDR) },
        videoStabilization = infos.any { info ->
            runCatching { Recorder.getVideoCapabilities(info).isStabilizationSupported }.getOrDefault(false)
        },
        ultraHdr = infos.any { it.supportsUltraHdr() },
        rawCapture = infos.any { it.supportsRawJpeg() },
        fullSensorRaw = infos.any { it.supportsFullSensorRaw(context) },
        lowLightBoost = infos.any { it.isLowLightBoostSupported },
        videoFps60 = back?.supportsVideoFps60() == true
    )
}

/** Dual mode: system concurrent-camera feature plus a listed front+back pair. */
fun isDualCameraSupported(context: Context, provider: ProcessCameraProvider): Boolean =
    context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_CONCURRENT) &&
        provider.hasConcurrentFrontBack()

/** Dual needs a listed two-camera front+back pair, not merely a non-empty concurrent list. */
fun ProcessCameraProvider.hasConcurrentFrontBack(): Boolean =
    concurrentFrontBackGroup() != null

/** Advertised concurrent combination of exactly one back and one front camera. */
fun ProcessCameraProvider.concurrentFrontBackGroup(): List<CameraInfo>? =
    availableConcurrentCameraInfos.firstOrNull { group ->
        group.size == 2 &&
            group.any { info -> info.lensFacing == CameraSelector.LENS_FACING_BACK } &&
            group.any { info -> info.lensFacing == CameraSelector.LENS_FACING_FRONT }
    }

fun Quality.toVideoQuality(): VideoQuality? = when (this) {
    Quality.SD -> VideoQuality.SD
    Quality.HD -> VideoQuality.HD
    Quality.FHD -> VideoQuality.FHD
    Quality.UHD -> VideoQuality.UHD
    else -> null
}

/**
 * CameraX Slow-mo: [Recorder.getHighSpeedVideoCapabilities], SDR quality, then
 * [HighSpeedVideoSessionConfig] (preview + video) frame rates.
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

fun CameraInfo.supportsVideoFps60(): Boolean {
    val preview = Preview.Builder().build()
    val video = VideoCapture.withOutput(Recorder.Builder().build())
    val session = androidx.camera.core.SessionConfig.Builder(preview, video)
        .setRequiredFeatureGroup(androidx.camera.core.featuregroup.GroupableFeature.FPS_60)
        .build()
    return runCatching { isSessionConfigSupported(session) }.getOrDefault(false)
}
