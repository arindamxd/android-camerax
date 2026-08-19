package com.arindam.camerax

import android.app.Application
import androidx.preference.PreferenceManager
import com.arindam.camerax.data.local.Preferences
import com.arindam.camerax.di.AppContainer
import com.arindam.camerax.util.theme.NightMode

/**
 * Created by Arindam Karmakar on 17/04/20.
 */

class CameraX : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        Preferences.init(this)
        handleDayNightTheme()
    }

    /* Handle Theme */
    private fun handleDayNightTheme() {
        val preferences = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        NightMode.applyPref(
            preferences.getString(
                getString(R.string.pref_key_theme),
                getString(R.string.pref_key_theme_default)
            ) ?: getString(R.string.pref_key_theme_default)
        )
    }
}
