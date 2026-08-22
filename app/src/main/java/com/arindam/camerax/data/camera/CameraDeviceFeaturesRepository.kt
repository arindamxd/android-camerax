package com.arindam.camerax.data.camera

import android.content.Context
import com.arindam.camerax.domain.model.DeviceCaptureFeatures
import com.arindam.camerax.domain.repository.DeviceFeaturesRepository

class CameraDeviceFeaturesRepository(
    context: Context
) : DeviceFeaturesRepository {
    private val appContext = context.applicationContext
    override suspend fun probe(): DeviceCaptureFeatures = probeDeviceFeatures(appContext)
}
