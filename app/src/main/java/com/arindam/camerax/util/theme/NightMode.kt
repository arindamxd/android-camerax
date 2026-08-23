package com.arindam.camerax.util.theme

import android.app.Activity
import android.content.res.Configuration
import android.os.Build
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat

/**
 * Util: Light / Dark / System. Values match Settings `pref_key_theme` (`off` / `on` / `system`).
 * Call [applyPref] from [com.arindam.camerax.CameraX] and from Settings; AppCompat recreates.
 */
enum class NightMode(val value: Int) {

    OFF(AppCompatDelegate.MODE_NIGHT_NO),
    ON(AppCompatDelegate.MODE_NIGHT_YES),
    SYSTEM(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);

    companion object {
        fun fromPref(value: String?): NightMode = entries.firstOrNull {
            it.name.equals(value, ignoreCase = true)
        } ?: ON

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
 * Draw behind system bars on all API levels without the Android 15–deprecated
 * `Window.setStatusBarColor` / `setNavigationBarColor` /
 * `LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES` APIs that Play Console flags when
 * calling `androidx.activity.enableEdgeToEdge()`.
 *
 * [lightIcons] true draws white status/nav icons (for dark canvases such as the
 * live viewfinder). False draws dark icons (light theme).
 */
fun ComponentActivity.applyEdgeToEdgeBars(lightIcons: Boolean) {
    val window = window
    val decor = window.decorView
    WindowCompat.setDecorFitsSystemWindows(window, false)

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        window.attributes = window.attributes.apply {
            layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
        }
    }

    // API 29–34 only: status-bar contrast enforcement is deprecated on Android 15+.
    if (Build.VERSION.SDK_INT in Build.VERSION_CODES.Q until 35) {
        @Suppress("DEPRECATION")
        window.isStatusBarContrastEnforced = false
        window.isNavigationBarContrastEnforced = false
    }

    WindowInsetsControllerCompat(window, decor).apply {
        isAppearanceLightStatusBars = !lightIcons
        isAppearanceLightNavigationBars = !lightIcons
    }
}

/** Status/nav icon contrast follows Light / Dark / System. */
fun ComponentActivity.applyEdgeToEdgeBarsForNightMode() {
    applyEdgeToEdgeBars(lightIcons = isNightMode())
}
