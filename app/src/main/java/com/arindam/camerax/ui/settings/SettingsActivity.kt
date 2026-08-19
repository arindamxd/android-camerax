package com.arindam.camerax.ui.settings

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import com.arindam.camerax.ui.theme.AppTheme
import com.arindam.camerax.util.theme.applyEdgeToEdgeBarsForNightMode

/**
 * AppCompat host so [com.arindam.camerax.util.theme.NightMode] / `AppCompatDelegate`
 * restyles Light/Dark/System.
 * Preference rows live in [SettingsCatalog].
 */
class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        applyEdgeToEdgeBarsForNightMode()
        super.onCreate(savedInstanceState)
        setContent {
            AppTheme {
                SettingsScreen(onBack = { finish() })
            }
        }
    }
}
