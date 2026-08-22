package com.arindam.camerax.domain.repository

import com.arindam.camerax.domain.model.CaptureSettings
import com.arindam.camerax.domain.model.DeviceCaptureFeatures

/** App preferences for capture. UI must not read SharedPreferences for camera bind flags. */
interface SettingsRepository {
    fun loadCaptureSettings(): CaptureSettings
}

/** CameraX capability probe (qualities, slo-mo, Dual, Ultra HDR, …). */
interface DeviceFeaturesRepository {
    suspend fun probe(): DeviceCaptureFeatures
}
