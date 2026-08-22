package com.arindam.camerax.ui.settings

import android.Manifest
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.core.app.ActivityCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arindam.camerax.CameraX
import com.arindam.camerax.R
import com.arindam.camerax.ui.theme.AppTheme
import com.arindam.camerax.util.commons.Constants.PERMISSIONS.MICROPHONE_PERMISSION
import com.arindam.camerax.util.display.Toaster
import com.arindam.camerax.util.permission.MicrophonePermission
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

    private val micPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.refreshMicrophonePermission(this)
        if (granted) return@registerForActivityResult
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, MICROPHONE_PERMISSION)) {
            Toaster.show(this, getString(R.string.permission_mic_denied))
        } else {
            Toaster.show(this, getString(R.string.permission_mic_settings))
            MicrophonePermission.openAppSettings(this)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        applyEdgeToEdgeBarsForNightMode()
        super.onCreate(savedInstanceState)
        viewModel.refreshMicrophonePermission(this)
        setContent {
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            AppTheme {
                SettingsScreen(
                    onBack = { finish() },
                    features = state.features,
                    versionLabel = state.versionLabel,
                    microphonePermissionGranted = state.microphonePermissionGranted,
                    onRequestMicrophonePermission = ::requestMicrophonePermission
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshMicrophonePermission(this)
    }

    private fun requestMicrophonePermission() {
        if (MicrophonePermission.isGranted(this)) {
            viewModel.refreshMicrophonePermission(this)
            return
        }
        micPermissionLauncher.launch(MICROPHONE_PERMISSION)
    }
}
