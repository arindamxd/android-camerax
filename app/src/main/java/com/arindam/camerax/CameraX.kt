package com.arindam.camerax

import android.app.Application
import android.os.Build
import android.os.StrictMode
import android.os.strictmode.DiskReadViolation
import android.os.strictmode.DiskWriteViolation
import android.os.strictmode.Violation
import android.util.Log
import androidx.preference.PreferenceManager
import com.arindam.camerax.di.AppContainer
import com.arindam.camerax.util.debug.StrictModeFilters
import com.arindam.camerax.util.theme.NightMode
import java.util.concurrent.Executors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Application (composition root host). Play Store CameraX sample (`com.arindam.camerax`).
 *
 * Layers: `ui` → `domain` ← `data`. The composition root is [com.arindam.camerax.di.AppContainer].
 * Apply saved Light/Dark/System from [com.arindam.camerax.util.theme.NightMode] at process start.
 */
class CameraX : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        // Prefs + container touch disk once at startup; enable StrictMode after so that load and
        // OEM binder noise do not drown real main-thread I/O caught later in the session.
        container = AppContainer(this)
        CoroutineScope(container.dispatchers.io).launch {
            container.mediaRepository.picturesDirectory()
        }
        handleDayNightTheme()
        enableStrictMode()
    }

    private fun handleDayNightTheme() {
        val preferences = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        NightMode.applyPref(
            preferences.getString(
                getString(R.string.pref_key_theme),
                getString(R.string.pref_key_theme_default)
            ) ?: getString(R.string.pref_key_theme_default)
        )
    }

    private fun enableStrictMode() {
        if (!BuildConfig.DEBUG) return
        val threadBuilder = StrictMode.ThreadPolicy.Builder()
            .detectDiskReads()
            .detectDiskWrites()
            .detectNetwork()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            // Log only app-owned violations. ColorOS/OxygenOS (and similar) do disk I/O inside
            // system_server during Binder (edge-to-edge, permission dialogs, pause/resume); that
            // is attributed to us via readAndHandleBinderCallViolations and cannot be fixed here.
            threadBuilder.penaltyListener(Executors.newSingleThreadExecutor()) { violation ->
                if (!isPlatformBinderDiskNoise(violation)) {
                    Log.d(STRICT_MODE_TAG, Log.getStackTraceString(violation))
                }
            }
        } else {
            threadBuilder.penaltyLog()
        }
        StrictMode.setThreadPolicy(threadBuilder.build())
        StrictMode.setVmPolicy(
            StrictMode.VmPolicy.Builder()
                .detectLeakedClosableObjects()
                .detectActivityLeaks()
                .penaltyLog()
                .build()
        )
    }

    companion object {
        private const val STRICT_MODE_TAG = "StrictMode"

        private fun isPlatformBinderDiskNoise(violation: Violation): Boolean {
            if (violation !is DiskReadViolation && violation !is DiskWriteViolation) return false
            return StrictModeFilters.shouldSuppressDiskViolation(violation.stackTrace)
        }
    }
}
