package com.arindam.camerax

import android.app.Application
import androidx.preference.PreferenceManager
import com.arindam.camerax.di.AppContainer
import com.arindam.camerax.util.theme.NightMode

/**
 * Play Store CameraX sample (`com.arindam.camerax`).
 *
 * Layers: `ui` → `domain` ← `data`. The composition root is [com.arindam.camerax.di.AppContainer].
 * Apply saved Light/Dark/System from [com.arindam.camerax.util.theme.NightMode] at process start.
 */
class CameraX : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        handleDayNightTheme()
    }

    private fun handleDayNightTheme() {
        val preferences = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        NightMode.applyPref(preferences.getString(getString(R.string.pref_key_theme), getString(R.string.pref_key_theme_default)) ?: getString(R.string.pref_key_theme_default))
    }
}
