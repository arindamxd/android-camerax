package com.arindam.camerax.util.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.appcompat.app.AppCompatDelegate

/**
 * Created by Arindam Karmakar on 21/04/20.
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

internal tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
