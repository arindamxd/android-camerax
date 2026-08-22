package com.arindam.camerax.domain.repository

import com.arindam.camerax.domain.model.CaptureSettings
import com.arindam.camerax.domain.model.DeviceCaptureFeatures

/**
 * Domain: app preferences for capture. UI must not read SharedPreferences for camera bind flags.
 */
interface SettingsRepository {
    /** Reads persisted capture toggles (aspect, HDR, stabilization, …) for bind and UI. */
    fun loadCaptureSettings(): CaptureSettings
}

/** Domain: CameraX capability probe (qualities, slo-mo, Dual, Ultra HDR, …). */
interface DeviceFeaturesRepository {
    /** Probes device once; call from a background dispatcher. */
    suspend fun probe(): DeviceCaptureFeatures
}
