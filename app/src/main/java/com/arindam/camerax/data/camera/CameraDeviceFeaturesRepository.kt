package com.arindam.camerax.data.camera

import android.content.Context
import com.arindam.camerax.domain.model.DeviceCaptureFeatures
import com.arindam.camerax.domain.repository.DeviceFeaturesRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Data: one CameraX provider fetch, cached for the process. Used by Settings and the mode pager.
 */
class CameraDeviceFeaturesRepository(
    context: Context
) : DeviceFeaturesRepository {
    private val appContext = context.applicationContext
    private val mutex = Mutex()
    @Volatile
    private var cached: DeviceCaptureFeatures? = null

    override suspend fun probe(): DeviceCaptureFeatures {
        cached?.let { return it }
        return mutex.withLock {
            cached ?: probeDeviceFeatures(appContext).also { cached = it }
        }
    }
}
