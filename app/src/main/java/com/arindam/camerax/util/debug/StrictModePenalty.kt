package com.arindam.camerax.util.debug

import android.os.Build
import android.os.StrictMode
import android.os.strictmode.DiskReadViolation
import android.os.strictmode.DiskWriteViolation
import android.os.strictmode.Violation
import android.util.Log
import androidx.annotation.RequiresApi
import java.util.concurrent.Executors

/** API 28+ [StrictMode.ThreadPolicy.Builder.penaltyListener] that filters OEM binder disk noise. */
@RequiresApi(Build.VERSION_CODES.P)
internal object StrictModePenalty {

    private const val TAG = "StrictMode"

    fun install(threadBuilder: StrictMode.ThreadPolicy.Builder) {
        threadBuilder.penaltyListener(Executors.newSingleThreadExecutor()) { violation ->
            if (!isPlatformBinderDiskNoise(violation)) {
                Log.d(TAG, Log.getStackTraceString(violation))
            }
        }
    }

    private fun isPlatformBinderDiskNoise(violation: Violation): Boolean {
        if (violation !is DiskReadViolation && violation !is DiskWriteViolation) return false
        return StrictModeFilters.shouldSuppressDiskViolation(violation.stackTrace)
    }
}
