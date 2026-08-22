package com.arindam.camerax.domain.model

/**
 * Policy for one capture mode. Add a [CameraMode] value and a row in [CameraModeCatalog.profiles]
 * first. Then implement any new CameraX session type in `CameraSession` if bind flags are not enough.
 */
data class CameraModeProfile(
    val mode: CameraMode,
    val availability: ModeAvailability = ModeAvailability.ALWAYS,
    val fallback: CameraMode = CameraMode.PHOTO,
    val captureAction: CaptureAction = CaptureAction.STILL,
    val showsFlash: Boolean = true,
    val showsTimer: Boolean = false,
    val showsGrid: Boolean = true,
    val showsMotion: Boolean = false,
    val showsZoom: Boolean = true,
    val showsStillBadge: Boolean = false,
    val showsVideoStatus: Boolean = false,
    val showsExposure: Boolean = true,
    val showsFlip: Boolean = true,
    val showsEffects: Boolean = false,
    val showsPip: Boolean = false,
    val showsNightHint: Boolean = false,
    val showsLowLightBoost: Boolean = false,
    val showsTools: Boolean = false,
    val showsCaptureControls: Boolean = true,
    val allowsAudioMute: Boolean = true,
    val allowsNightAuto: Boolean = false,
    val allowsExtensions: Boolean = true,
    val allowsEffect: Boolean = true,
    val allowsMotionPhoto: Boolean = false,
    val allowsFps60: Boolean = false,
    val allowsPersistentRecording: Boolean = false,
    val bindSlowMotion: Boolean = false,
    val bindConcurrent: Boolean = false,
    val rebindOnEnter: Boolean = false,
    val clearsSessionExtras: Boolean = false
) {
    fun isAvailable(slowMotionSupported: Boolean, concurrentSupported: Boolean): Boolean =
        when (availability) {
            ModeAvailability.ALWAYS -> true
            ModeAvailability.HIGH_SPEED -> slowMotionSupported
            ModeAvailability.CONCURRENT -> concurrentSupported
        }
}

enum class ModeAvailability {
    ALWAYS,
    HIGH_SPEED,
    CONCURRENT
}

enum class CaptureAction {
    STILL,
    VIDEO,
    PANORAMA
}

fun CameraMode.profile(): CameraModeProfile = CameraModeCatalog.profile(this)

/**
 * Registry of pager modes. To add a mode: [CameraMode] value, a [CameraModeProfile] here,
 * then `CameraMode.labelRes`. Touch [com.arindam.camerax.data.camera.CameraSession] only
 * when bind flags (slow-motion / concurrent) are not enough.
 */
object CameraModeCatalog {
    val profiles: Map<CameraMode, CameraModeProfile> = listOf(
        CameraModeProfile(
            mode = CameraMode.PHOTO,
            showsTimer = true,
            showsMotion = true,
            showsStillBadge = true,
            showsNightHint = true,
            showsLowLightBoost = true,
            allowsNightAuto = true,
            allowsMotionPhoto = true
        ),
        CameraModeProfile(
            mode = CameraMode.VIDEO,
            captureAction = CaptureAction.VIDEO,
            showsVideoStatus = true,
            showsLowLightBoost = true,
            allowsExtensions = false,
            allowsNightAuto = false,
            allowsFps60 = true,
            allowsPersistentRecording = true
        ),
        CameraModeProfile(
            mode = CameraMode.SLOW_MOTION,
            availability = ModeAvailability.HIGH_SPEED,
            fallback = CameraMode.VIDEO,
            captureAction = CaptureAction.VIDEO,
            showsExposure = false,
            allowsAudioMute = false,
            allowsNightAuto = false,
            allowsExtensions = false,
            allowsEffect = false,
            allowsMotionPhoto = false,
            bindSlowMotion = true,
            rebindOnEnter = true,
            clearsSessionExtras = true
        ),
        CameraModeProfile(
            mode = CameraMode.EFFECTS,
            showsTimer = true,
            showsStillBadge = true,
            showsEffects = true,
            allowsExtensions = false,
            allowsNightAuto = false,
            rebindOnEnter = true
        ),
        CameraModeProfile(
            mode = CameraMode.PANORAMA,
            captureAction = CaptureAction.PANORAMA,
            showsFlash = false,
            showsZoom = false,
            showsExposure = false,
            allowsNightAuto = false,
            allowsExtensions = false,
            allowsEffect = false,
            allowsMotionPhoto = false
        ),
        CameraModeProfile(
            mode = CameraMode.DUAL,
            availability = ModeAvailability.CONCURRENT,
            fallback = CameraMode.PHOTO,
            showsGrid = true,
            showsZoom = false,
            showsExposure = false,
            showsFlip = false,
            showsPip = true,
            allowsNightAuto = false,
            allowsExtensions = false,
            allowsEffect = false,
            allowsMotionPhoto = false,
            bindConcurrent = true,
            rebindOnEnter = true,
            clearsSessionExtras = true
        ),
        CameraModeProfile(
            mode = CameraMode.OTHERS,
            showsFlash = false,
            showsGrid = false,
            showsZoom = false,
            showsExposure = false,
            showsFlip = false,
            showsTools = true,
            showsCaptureControls = false,
            allowsNightAuto = false,
            allowsExtensions = false,
            allowsEffect = false,
            allowsMotionPhoto = false
        )
    ).associateBy { it.mode }

    init {
        check(profiles.keys == CameraMode.entries.toSet()) {
            "CameraModeCatalog must declare a profile for every CameraMode"
        }
    }

    fun profile(mode: CameraMode): CameraModeProfile =
        profiles[mode] ?: error("Missing CameraModeProfile for $mode")

    fun visibleModes(
        slowMotionSupported: Boolean,
        concurrentSupported: Boolean
    ): List<CameraMode> = CameraMode.entries.filter { mode ->
        profile(mode).isAvailable(slowMotionSupported, concurrentSupported)
    }

    fun resolve(
        mode: CameraMode,
        slowMotionSupported: Boolean,
        concurrentSupported: Boolean
    ): CameraMode {
        val current = profile(mode)
        return if (current.isAvailable(slowMotionSupported, concurrentSupported)) {
            mode
        } else {
            current.fallback
        }
    }
}
