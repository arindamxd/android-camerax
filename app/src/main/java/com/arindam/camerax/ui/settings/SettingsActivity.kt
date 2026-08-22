package com.arindam.camerax.ui.settings

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arindam.camerax.CameraX
import com.arindam.camerax.R
import com.arindam.camerax.ui.theme.AppTheme
import com.arindam.camerax.util.theme.applyEdgeToEdgeBarsForNightMode

/**
 * AppCompat host so [com.arindam.camerax.util.theme.NightMode] / `AppCompatDelegate`
 * restyles Light/Dark/System.
 * Preference rows live in [SettingsCatalog].
 */
class SettingsActivity : AppCompatActivity() {

    private val viewModel: SettingsViewModel by viewModels {
        val app = application as CameraX
        SettingsViewModelFactory(app.container, getString(R.string.app_version))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        applyEdgeToEdgeBarsForNightMode()
        super.onCreate(savedInstanceState)
        setContent {
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            AppTheme {
                SettingsScreen(
                    onBack = { finish() },
                    features = state.features,
                    versionLabel = state.versionLabel
                )
            }
        }
    }
}
