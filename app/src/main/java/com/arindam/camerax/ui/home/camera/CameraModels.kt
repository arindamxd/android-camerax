package com.arindam.camerax.ui.home.camera

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.annotation.StringRes
import androidx.compose.ui.geometry.Offset
import com.arindam.camerax.R
import com.arindam.camerax.domain.model.CameraExtension
import com.arindam.camerax.domain.model.CameraLens
import com.arindam.camerax.domain.model.CameraMode
import com.arindam.camerax.domain.model.CameraModeCatalog
import com.arindam.camerax.domain.model.CaptureAction
import com.arindam.camerax.domain.model.CaptureAspect
import com.arindam.camerax.domain.model.ColorFilterType
import com.arindam.camerax.domain.model.ExposureLimits
import com.arindam.camerax.domain.model.ExposurePriority
import com.arindam.camerax.domain.model.FlashMode
import com.arindam.camerax.domain.model.NightScene
import com.arindam.camerax.domain.model.PhysicalZoom
import com.arindam.camerax.domain.model.SlowMotionRate
import com.arindam.camerax.domain.model.StillFormat
import com.arindam.camerax.domain.model.TimerMode
import com.arindam.camerax.domain.model.VideoHdrRange
import com.arindam.camerax.domain.model.VideoQuality
import java.io.File

/** String resource for the mode pager chip. Add a mapping when you add a [CameraMode]. */
val CameraMode.labelRes: Int
    @StringRes get() = when (this) {
        CameraMode.PHOTO -> R.string.mode_photo
        CameraMode.VIDEO -> R.string.mode_video
        CameraMode.SLOW_MOTION -> R.string.mode_slow_motion
        CameraMode.EFFECTS -> R.string.mode_effects
        CameraMode.PANORAMA -> R.string.mode_panorama
        CameraMode.DUAL -> R.string.mode_dual
    }

val FlashMode.labelRes: Int
    @StringRes get() = when (this) {
        FlashMode.OFF -> R.string.flash_off
        FlashMode.ON -> R.string.flash_on
        FlashMode.AUTO -> R.string.flash_auto
        FlashMode.TORCH -> R.string.flash_torch
    }

val TimerMode.labelRes: Int
    @StringRes get() = when (this) {
        TimerMode.OFF -> R.string.timer_off
        TimerMode.THREE -> R.string.timer_3
        TimerMode.TEN -> R.string.timer_10
    }

val CameraExtension.labelRes: Int
    @StringRes get() = when (this) {
        CameraExtension.NONE -> R.string.extension_none
        CameraExtension.HDR -> R.string.extension_hdr
        CameraExtension.NIGHT -> R.string.extension_night
        CameraExtension.PORTRAIT -> R.string.extension_portrait
        CameraExtension.BEAUTY -> R.string.extension_beauty
    }

val ExposurePriority.labelRes: Int
    @StringRes get() = when (this) {
        ExposurePriority.AUTO -> R.string.ae_auto
        ExposurePriority.ISO -> R.string.ae_iso
        ExposurePriority.SHUTTER -> R.string.ae_shutter
    }

val ColorFilterType.labelRes: Int
    @StringRes get() = when (this) {
        ColorFilterType.NONE -> R.string.filter_none
        ColorFilterType.MONO -> R.string.filter_mono
        ColorFilterType.INVERT -> R.string.filter_invert
        ColorFilterType.VINTAGE -> R.string.filter_vintage
        ColorFilterType.COOL -> R.string.filter_cool
        ColorFilterType.WARM -> R.string.filter_warm
        ColorFilterType.VIVID -> R.string.filter_vivid
        ColorFilterType.BRIGHT -> R.string.filter_bright
        ColorFilterType.CONTRAST -> R.string.filter_contrast
    }

/**
 * Presentation state for the live feed. Chrome flags are derived from
 * [com.arindam.camerax.domain.model.CameraModeCatalog]; do not scatter `if (mode == …)` in UI.
 */
data class CameraUiState(
    val mode: CameraMode = CameraMode.PHOTO,
    val lens: CameraLens = CameraLens.BACK,
    val flash: FlashMode = FlashMode.OFF,
    val timer: TimerMode = TimerMode.OFF,
    val gridEnabled: Boolean = false,
    val countdownRemaining: Int? = null,
    val zoomRatio: Float = 1f,
    val minZoom: Float = 1f,
    val maxZoom: Float = 1f,
    val hasFlash: Boolean = false,
    val isRecording: Boolean = false,
    val isPaused: Boolean = false,
    val isMuted: Boolean = false,
    val recordingNanos: Long = 0L,
    val thumbnail: File? = null,
    val extension: CameraExtension = CameraExtension.NONE,
    val supportedExtensions: Set<CameraExtension> = emptySet(),
    val colorFilter: ColorFilterType = ColorFilterType.NONE,
    val focusPoint: Offset? = null,
    val captureFlashToken: Int = 0,
    val bindRevision: Int = 0,
    val lockCaptureMode: Boolean = false,
    val nightScene: NightScene = NightScene.UNKNOWN,
    val autoNightActive: Boolean = false,
    val motionPhotoEnabled: Boolean = false,
    val motionCapturing: Boolean = false,
    val ultraHdrEnabled: Boolean = false,
    val stillFormat: StillFormat = StillFormat.JPEG,
    val captureAspect: CaptureAspect = CaptureAspect.FULL,
    val videoQuality: VideoQuality = VideoQuality.FHD,
    val videoHdrRange: VideoHdrRange = VideoHdrRange.SDR,
    val videoHdrBound: VideoHdrRange = VideoHdrRange.SDR,
    val videoStabilization: Boolean = true,
    val videoStabilizationActive: Boolean = false,
    val slowMotionQuality: VideoQuality = VideoQuality.FHD,
    val slowMotionRate: SlowMotionRate = SlowMotionRate.AUTO,
    val ultraHdr: Boolean = true,
    val rawCapture: Boolean = false,
    val rawFullSensor: Boolean = false,
    val exposurePriority: ExposurePriority = ExposurePriority.AUTO,
    val exposureLimits: ExposureLimits = ExposureLimits(),
    val iso: Int = 100,
    val shutterNanos: Long = 16_666_667L,
    val exposureCompensation: Int = 0,
    val panoramaActive: Boolean = false,
    val panoramaFrames: Int = 0,
    val cameraId: String? = null,
    val physicalZooms: List<PhysicalZoom> = emptyList(),
    val slowMotionSupported: Boolean = false,
    val slowMotionFps: Int = 0,
    val lowLightBoost: Boolean = true,
    val lowLightBoostSupported: Boolean = false,
    val lowLightBoostActive: Boolean = false,
    val flipWhileRecording: Boolean = false,
    val review: CaptureReview? = null,
    val concurrentSupported: Boolean = false,
    val videoFps60: Boolean = false,
    val videoFps60Supported: Boolean = false,
    val videoFps60Active: Boolean = false,
    val message: String? = null
) {
    val zoomChips: List<Float>
        get() {
            val physical = physicalZooms.map { it.label }
            if (physical.size >= 2) {
                return (physical + listOfNotNull(
                    0.5f.takeIf { minZoom <= 0.7f && physical.none { it <= 0.7f } },
                    1f.takeIf { 1f !in physical },
                    2f.takeIf { maxZoom >= 1.9f && 2f !in physical },
                    5f.takeIf { maxZoom >= 4.5f && 5f !in physical }
                )).distinct().sorted()
            }
            return buildList {
                if (minZoom <= 0.7f) add(0.5f)
                add(1f)
                if (maxZoom >= 1.9f) add(2f)
                if (maxZoom >= 4.5f) add(5f)
            }.distinct()
        }

    val activeZoomChip: Float
        get() {
            val bound = cameraId?.let { id ->
                physicalZooms.firstOrNull { it.cameraId == id }
            }
            // Physical cameras report CameraX 1.0 at their native FOV (0.5x / 2x).
            val equivalent = (bound?.label ?: 1f) * zoomRatio
            return zoomChips.minByOrNull { kotlin.math.abs(it - equivalent) } ?: equivalent
        }

    val visibleModes: List<CameraMode>
        get() = CameraModeCatalog.visibleModes(slowMotionSupported, concurrentSupported)

    private val profile get() = CameraModeCatalog.profile(mode)

    val showsFlash: Boolean
        get() = hasFlash && profile.showsFlash

    val showsTimer: Boolean
        get() = profile.showsTimer

    val showsGrid: Boolean
        get() = profile.showsGrid

    val showsMotion: Boolean
        get() = profile.showsMotion && profile.allowsMotionPhoto && !rawCapture

    val showsZoomChips: Boolean
        get() = profile.showsZoom

    val showsStillBadge: Boolean
        get() = profile.showsStillBadge &&
            (stillFormat == StillFormat.RAW_JPEG || ultraHdrEnabled)

    val showsVideoStatus: Boolean
        get() = profile.showsVideoStatus

    val showsExposureControls: Boolean
        get() = profile.showsExposure &&
            (exposureLimits.supportedPriorities.size >= 2 ||
                exposureLimits.evSupported)

    val showsFilters: Boolean
        get() = profile.showsFilters

    val showsPip: Boolean
        get() = profile.showsPip

    val showsNightHint: Boolean
        get() = profile.showsNightHint

    val showsLowLightBoost: Boolean
        get() = profile.showsLowLightBoost

    val allowsAudioMute: Boolean
        get() = profile.allowsAudioMute

    val captureAction: CaptureAction
        get() = profile.captureAction

    val showsFlipControl: Boolean
        get() = profile.showsFlip &&
            (!isRecording || (flipWhileRecording && profile.allowsPersistentRecording))

    val recordsVideo: Boolean
        get() = profile.captureAction == CaptureAction.VIDEO

    val showsSlowMotionFps: Boolean
        get() = profile.bindSlowMotion
}

data class CaptureReview(
    val file: File,
    val isVideo: Boolean,
    val isMotionPhoto: Boolean,
    val width: Int,
    val height: Int,
    val durationLabel: String? = null,
    @StringRes val formatLabelRes: Int? = null,
    val companions: List<File> = emptyList()
) {
    fun metadataLabel(formatText: String?): String {
        val size = if (width > 0 && height > 0) "$width × $height" else null
        return listOfNotNull(durationLabel, size, formatText).joinToString(" · ")
    }
}

enum class ExternalCaptureKind {
    NONE,
    IMAGE_CAPTURE,
    VIDEO_CAPTURE,
    MOTION_PHOTO,
    OPEN_PHOTO,
    OPEN_VIDEO
}

data class ExternalCaptureRequest(
    val kind: ExternalCaptureKind = ExternalCaptureKind.NONE,
    val outputUri: Uri? = null
) {
    val returnsResult: Boolean
        get() = kind == ExternalCaptureKind.IMAGE_CAPTURE ||
            kind == ExternalCaptureKind.VIDEO_CAPTURE ||
            kind == ExternalCaptureKind.MOTION_PHOTO

    companion object {
        fun from(intent: Intent): ExternalCaptureRequest {
            val output = extraOutputUri(intent)
            return when (intent.action) {
                MediaStore.ACTION_IMAGE_CAPTURE,
                MediaStore.ACTION_IMAGE_CAPTURE_SECURE ->
                    ExternalCaptureRequest(ExternalCaptureKind.IMAGE_CAPTURE, output)
                MediaStore.ACTION_VIDEO_CAPTURE ->
                    ExternalCaptureRequest(ExternalCaptureKind.VIDEO_CAPTURE, output)
                MOTION_PHOTO_CAPTURE,
                MOTION_PHOTO_CAPTURE_SECURE ->
                    ExternalCaptureRequest(ExternalCaptureKind.MOTION_PHOTO, output)
                MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA,
                MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA_SECURE ->
                    ExternalCaptureRequest(ExternalCaptureKind.OPEN_PHOTO)
                MediaStore.INTENT_ACTION_VIDEO_CAMERA ->
                    ExternalCaptureRequest(ExternalCaptureKind.OPEN_VIDEO)
                else -> ExternalCaptureRequest()
            }
        }

        private fun extraOutputUri(intent: Intent): Uri? {
            val extra = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(MediaStore.EXTRA_OUTPUT, Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(MediaStore.EXTRA_OUTPUT)
            }
            return extra ?: intent.clipData?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.uri
        }

        private const val MOTION_PHOTO_CAPTURE = "android.provider.action.MOTION_PHOTO_CAPTURE"
        private const val MOTION_PHOTO_CAPTURE_SECURE =
            "android.provider.action.MOTION_PHOTO_CAPTURE_SECURE"
    }
}

fun formatRecordingTime(nanos: Long): String {
    val totalSeconds = nanos / 1_000_000_000L
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}
