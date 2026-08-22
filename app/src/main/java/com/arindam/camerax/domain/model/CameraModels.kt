package com.arindam.camerax.domain.model

/**
 * Domain camera types (no CameraX, no Compose). UI maps these to strings and chrome;
 * [com.arindam.camerax.data.camera.CameraSession] maps them to CameraX builders.
 */

/** Pager capture mode. Chrome and bind flags live on [CameraModeProfile]. */
enum class CameraMode {
    PHOTO,
    VIDEO,
    SLOW_MOTION,
    EFFECTS,
    PANORAMA,
    DUAL,
    OTHERS
}

/** Facing lens for bind / flip. */
enum class CameraLens {
    BACK,
    FRONT;

    /** Switches between front and back. */
    fun toggle(): CameraLens = if (this == BACK) FRONT else BACK
}

/** Flash for stills; [TORCH] keeps the LED on during preview. */
enum class FlashMode {
    OFF,
    ON,
    AUTO,
    TORCH;

    fun next(): FlashMode = when (this) {
        OFF -> ON
        ON -> AUTO
        AUTO -> TORCH
        TORCH -> OFF
    }
}

/** Self-timer delay before still capture. */
enum class TimerMode(val seconds: Int) {
    OFF(0),
    THREE(3),
    TEN(10);

    fun next(): TimerMode = when (this) {
        OFF -> THREE
        THREE -> TEN
        TEN -> OFF
    }
}

/** OEM CameraX extension (HDR, Night, Portrait, Beauty). */
enum class CameraExtension {
    NONE,
    HDR,
    NIGHT,
    PORTRAIT,
    BEAUTY
}

/** Live and still ColorMatrix effect (Effects mode). */
enum class EffectMode {
    NONE,
    GRAYSCALE,
    INVERT,
    SEPIA,
    COOL,
    WARM,
    VIVID
}

/** Night-scene recommendation from the camera (API 36+). */
enum class NightScene {
    UNKNOWN,
    NOT_RECOMMENDED,
    RECOMMENDED
}

/** Hybrid AE priority mode (API 36+). */
enum class ExposurePriority {
    AUTO,
    ISO,
    SHUTTER
}

/** Still output format selected in Settings. */
enum class StillFormat {
    JPEG,
    JPEG_ULTRA_HDR,
    HEIC_ULTRA_HDR,
    RAW_JPEG
}

/** Still / preview aspect from Settings (`4_3` / `16_9` / `full`). */
enum class CaptureAspect(val prefValue: String) {
    RATIO_4_3("4_3"),
    RATIO_16_9("16_9"),
    FULL("full");

    companion object {
        fun fromPref(value: String?): CaptureAspect =
            entries.firstOrNull { it.prefValue == value } ?: FULL
    }
}

/** Recording quality from Settings (SD→UHD). */
enum class VideoQuality(val prefValue: String) {
    SD("sd"),
    HD("hd"),
    FHD("fhd"),
    UHD("uhd");

    companion object {
        fun fromPref(value: String?): VideoQuality =
            entries.firstOrNull { it.prefValue == value } ?: FHD
    }
}

/** Video dynamic range from Settings (SDR / HLG / HDR10 / …). */
enum class VideoHdrRange(val prefValue: String) {
    SDR("sdr"),
    HLG10("hlg10"),
    HDR10("hdr10"),
    HDR10_PLUS("hdr10_plus"),
    DOLBY_VISION("dolby_vision");

    companion object {
        fun fromPref(value: String?): VideoHdrRange =
            entries.firstOrNull { it.prefValue == value } ?: SDR
    }
}

/** High-speed capture fps from Settings (`auto` or a listed rate). */
enum class SlowMotionRate(val prefValue: String, val fps: Int) {
    AUTO("auto", 0),
    FPS_120("120", 120),
    FPS_240("240", 240),
    FPS_480("480", 480),
    FPS_960("960", 960);

    companion object {
        fun fromPref(value: String?): SlowMotionRate =
            entries.firstOrNull { it.prefValue == value } ?: AUTO

        fun forFrameRates(frameRates: List<Int>): List<SlowMotionRate> =
            listOf(AUTO) + entries.filter { rate -> rate.fps > 0 && rate.fps in frameRates }
    }
}

/** Exposure compensation range and step reported by the bound camera. */
data class ExposureLimits(
    val isoMin: Int = 50,
    val isoMax: Int = 3200,
    val shutterMinNanos: Long = 1_000_000L,
    val shutterMaxNanos: Long = 250_000_000L,
    val supportedPriorities: Set<ExposurePriority> = setOf(ExposurePriority.AUTO),
    val evSupported: Boolean = false,
    val evMin: Int = 0,
    val evMax: Int = 0,
    val evStep: Float = 0f
)

/** Normalized viewfinder tap point for AF/AE metering. */
data class FocusPoint(val x: Float, val y: Float)

/** Physical camera zoom chip (ultra-wide / wide / tele) for rebind by camera id. */
data class PhysicalZoom(
    val cameraId: String,
    val label: Float
)

/** Inputs for one [com.arindam.camerax.domain.repository.CameraRepository.bind] call. */
data class CameraBindConfig(
    val lens: CameraLens,
    val flash: FlashMode,
    val extension: CameraExtension,
    val effect: EffectMode,
    val liveEffects: Boolean = false,
    val cameraId: String? = null,
    val captureAspect: CaptureAspect = CaptureAspect.FULL,
    val videoQuality: VideoQuality = VideoQuality.FHD,
    val videoHdrRange: VideoHdrRange = VideoHdrRange.SDR,
    val slowMotion: Boolean = false,
    val slowMotionQuality: VideoQuality = VideoQuality.FHD,
    val slowMotionRate: SlowMotionRate = SlowMotionRate.AUTO,
    val videoStabilization: Boolean = true,
    val ultraHdr: Boolean = false,
    val rawCapture: Boolean = false,
    val rawFullSensor: Boolean = false,
    val lowLightBoost: Boolean = true,
    val retainRecording: Boolean = false,
    val concurrent: Boolean = false,
    val videoFps60: Boolean = false,
    val frontMirror: Boolean = true
)

/** What the session actually bound (HUD chips, zoom range, capabilities). */
data class CameraBindResult(
    val hasFlash: Boolean,
    val minZoom: Float,
    val maxZoom: Float,
    val zoomRatio: Float,
    val videoAvailable: Boolean,
    val supportedExtensions: Set<CameraExtension>,
    val stillFormat: StillFormat = StillFormat.JPEG,
    val ultraHdrEnabled: Boolean = false,
    val nightIndicatorSupported: Boolean = false,
    val exposureLimits: ExposureLimits = ExposureLimits(),
    val physicalZooms: List<PhysicalZoom> = emptyList(),
    val boundCameraId: String? = null,
    val slowMotionSupported: Boolean = false,
    val slowMotionFps: Int = 0,
    val videoStabilizationSupported: Boolean = false,
    val videoStabilizationActive: Boolean = false,
    val lowLightBoostSupported: Boolean = false,
    val videoHdrRange: VideoHdrRange = VideoHdrRange.SDR,
    val concurrentSupported: Boolean = false,
    val videoFps60Supported: Boolean = false,
    val videoFps60Active: Boolean = false,
    val session: BoundSession = BoundSession()
)

/** Use cases and format flags from the last successful bind. */
data class BoundSession(
    val kind: BoundSessionKind = BoundSessionKind.STANDARD,
    val preview: Boolean = true,
    val stills: Boolean = true,
    val video: Boolean = false,
    val analysis: Boolean = false,
    val stillsOnlyFallback: Boolean = false,
    val cameraId: String? = null,
    val lens: CameraLens = CameraLens.BACK,
    val extension: CameraExtension = CameraExtension.NONE,
    val stillFormat: StillFormat = StillFormat.JPEG,
    val videoHdr: VideoHdrRange = VideoHdrRange.SDR,
    val videoFps60: Boolean = false,
    val videoStabilization: Boolean = false,
    val rawFullSensor: Boolean = false
)

/** Which CameraX session graph is currently bound (still+video, high-speed, dual, …). */
enum class BoundSessionKind {
    STANDARD,
    HIGH_SPEED,
    CONCURRENT
}

/** Last bind plus pager mode, kept after Tools unbinds the preview. */
data class LastCameraSession(
    val mode: CameraMode,
    val bound: BoundSession
)

/** Low-light boost HUD state from [CameraRepository.lowLightBoost]. */
enum class LowLightBoost {
    OFF,
    INACTIVE,
    ACTIVE
}

/** Live zoom ratio and min/max from the bound camera. */
data class ZoomInfo(
    val ratio: Float,
    val min: Float,
    val max: Float
)

/** Domain: recording lifecycle events from [CameraRepository.recordingEvents]. */
sealed interface RecordingEvent {
    data class Status(val durationNanos: Long) : RecordingEvent
    data object Paused : RecordingEvent
    data object Resumed : RecordingEvent
    data class Finalized(val success: Boolean) : RecordingEvent
}

/** High-speed capture options for Slo-mo. Empty lists means the device cannot do it. */
data class SlowMotionOptions(
    val qualities: List<VideoQuality> = emptyList(),
    val frameRates: List<Int> = emptyList()
) {
    val available: Boolean get() = qualities.isNotEmpty() && frameRates.isNotEmpty()
}

/** Settings that rebind or change capture behavior. Loaded from app preferences. */
data class CaptureSettings(
    val confirmEnabled: Boolean = false,
    val aspect: CaptureAspect = CaptureAspect.FULL,
    val videoQuality: VideoQuality = VideoQuality.FHD,
    val videoHdrRange: VideoHdrRange = VideoHdrRange.SDR,
    val videoStabilization: Boolean = true,
    val slowMotionQuality: VideoQuality = VideoQuality.FHD,
    val slowMotionRate: SlowMotionRate = SlowMotionRate.AUTO,
    val ultraHdr: Boolean = false,
    val rawCapture: Boolean = false,
    val rawFullSensor: Boolean = false,
    val flipWhileRecording: Boolean = false,
    val recordMuted: Boolean = false,
    val lowLightBoost: Boolean = true,
    val videoFps60: Boolean = false,
    val frontMirror: Boolean = true,
    val galleryVideoAutoplay: Boolean = false
)

/** Device CameraX capabilities. Probed once per Settings open / camera start. */
data class DeviceCaptureFeatures(
    val slowMotion: SlowMotionOptions = SlowMotionOptions(),
    val concurrent: Boolean = false,
    val videoQualities: List<VideoQuality> = VideoQuality.entries,
    val videoHdrRanges: List<VideoHdrRange> = listOf(VideoHdrRange.SDR),
    val videoStabilization: Boolean = false,
    val ultraHdr: Boolean = false,
    val rawCapture: Boolean = false,
    val fullSensorRaw: Boolean = false,
    val lowLightBoost: Boolean = false,
    val videoFps60: Boolean = false,
    val jpegUltraHdr: Boolean = false,
    val heicUltraHdr: Boolean = false,
    val cameras: List<InstalledCamera> = emptyList(),
    val extensions: Set<CameraExtension> = emptySet()
)

/** One physical camera listed by CameraX for diagnostics. */
data class InstalledCamera(
    val id: String,
    val lens: CameraLens,
    val zoomLabel: Float? = null,
    val focalMm: Float? = null
)

/** Opaque preview/lifecycle handle. Data layer provides the CameraX implementation. */
interface CameraHost
