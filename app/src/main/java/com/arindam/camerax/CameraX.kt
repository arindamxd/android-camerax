package com.arindam.camerax

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.PreferenceManager
import com.arindam.camerax.utils.theme.NightMode
import java.util.*

/**
 * Created by Arindam Karmakar on 17/04/20.
 */

class CameraX : Application() {

    override fun onCreate() {
        super.onCreate()

        /* Handle Theme */
        val preferences = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        preferences.getString(getString(R.string.pref_key_theme), getString(R.string.pref_key_theme_default))?.apply {
            val mode = NightMode.valueOf(this.toUpperCase(Locale.US))
            AppCompatDelegate.setDefaultNightMode(mode.value)
        }
    }
}
