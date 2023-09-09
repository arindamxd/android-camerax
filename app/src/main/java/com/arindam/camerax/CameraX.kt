package com.arindam.camerax

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.PreferenceManager
import com.arindam.camerax.data.local.Preferences
import com.arindam.camerax.util.theme.NightMode
import java.util.Locale

/**
 * Created by Arindam Karmakar on 17/04/20.
 */

class CameraX : Application() {

    override fun onCreate() {
        super.onCreate()

        Preferences.init(this)
        handleDayNightTheme()
    }

    /* Handle Theme */
    private fun handleDayNightTheme() {
        val preferences = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        preferences.getString(getString(R.string.pref_key_theme), getString(R.string.pref_key_theme_default))?.apply {
            val mode = NightMode.valueOf(this.uppercase(Locale.US))
            AppCompatDelegate.setDefaultNightMode(mode.value)
        }
    }
}
