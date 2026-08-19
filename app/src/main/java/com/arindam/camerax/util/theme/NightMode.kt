package com.arindam.camerax.util.theme

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
