package com.arindam.camerax.testing

import com.arindam.camerax.domain.model.CaptureSettings
import com.arindam.camerax.domain.model.DeviceCaptureFeatures
import com.arindam.camerax.domain.repository.DeviceFeaturesRepository
import com.arindam.camerax.domain.repository.SettingsRepository

class FakeSettingsRepository(
    var settings: CaptureSettings = CaptureSettings()
) : SettingsRepository {
    override fun loadCaptureSettings(): CaptureSettings = settings
}

class FakeDeviceFeaturesRepository(
    var features: DeviceCaptureFeatures = DeviceCaptureFeatures()
) : DeviceFeaturesRepository {
    override suspend fun probe(): DeviceCaptureFeatures = features
}
