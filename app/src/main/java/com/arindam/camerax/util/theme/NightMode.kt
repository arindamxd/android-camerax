package com.arindam.camerax.util.theme

import android.app.Activity
import android.content.res.Configuration
import android.graphics.Color
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatDelegate

/**
 * Light / Dark / System. Values match Settings `pref_key_theme` (`off` / `on` / `system`).
 * Call [applyPref] from [com.arindam.camerax.CameraX] and from Settings; AppCompat recreates.
 */
enum class NightMode(val value: Int) {
    OFF(AppCompatDelegate.MODE_NIGHT_NO),
    ON(AppCompatDelegate.MODE_NIGHT_YES),
    SYSTEM(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);

    companion object {
        fun fromPref(value: String?): NightMode =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: ON

        fun applyPref(value: String) {
            AppCompatDelegate.setDefaultNightMode(fromPref(value).value)
        }
    }
}

/** True when this activity's configuration is night. */
fun Activity.isNightMode(): Boolean =
    resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK ==
        Configuration.UI_MODE_NIGHT_YES

/**
 * Transparent system bars. [lightIcons] true draws white status/nav icons (for dark canvases
 * such as the live viewfinder). False draws dark icons (light theme).
 */
fun ComponentActivity.applyEdgeToEdgeBars(lightIcons: Boolean) {
    val transparent = Color.TRANSPARENT
    val style = if (lightIcons) {
        SystemBarStyle.dark(transparent)
    } else {
        SystemBarStyle.light(transparent, transparent)
    }
    enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
}

/** Status/nav icon contrast follows Light / Dark / System. */
fun ComponentActivity.applyEdgeToEdgeBarsForNightMode() {
    applyEdgeToEdgeBars(lightIcons = isNightMode())
}
