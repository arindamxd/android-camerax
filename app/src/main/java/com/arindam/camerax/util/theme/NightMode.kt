package com.arindam.camerax.util.theme

import androidx.appcompat.app.AppCompatDelegate

/**
 * Created by Arindam Karmakar on 21/04/20.
 */

enum class NightMode(val value: Int) {
    OFF(AppCompatDelegate.MODE_NIGHT_NO),
    ON(AppCompatDelegate.MODE_NIGHT_YES),
    SYSTEM(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
}
