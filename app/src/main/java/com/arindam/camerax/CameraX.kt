package com.arindam.camerax

import android.app.Application
import android.os.Build
import android.os.StrictMode
import androidx.preference.PreferenceManager
import com.arindam.camerax.di.AppContainer
import com.arindam.camerax.util.debug.StrictModePenalty
import com.arindam.camerax.util.theme.NightMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * App: Application / composition-root host for the Play Store CameraX app (`com.arindam.camerax`).
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
            StrictModePenalty.install(threadBuilder)
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

}
