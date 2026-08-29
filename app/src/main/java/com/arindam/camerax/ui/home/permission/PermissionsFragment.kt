package com.arindam.camerax.ui.home.permission

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import com.arindam.camerax.R
import com.arindam.camerax.ui.base.BaseFragmentCompose
import com.arindam.camerax.ui.theme.AppTheme
import com.arindam.camerax.util.theme.applyEdgeToEdgeBars
import com.arindam.camerax.util.commons.Constants.PERMISSIONS.RUNTIME_PERMISSIONS

/**
 * Presentation: request camera (and optional microphone) permissions, then open the camera fragment.
 */
class PermissionsFragment : BaseFragmentCompose() {

    private var showSettingsHint by mutableStateOf(false)
    private var microphoneDenied by mutableStateOf(false)
    private var autoRequested = false

    private val activityResultLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val cameraGranted = permissions[Manifest.permission.CAMERA] == true
        if (cameraGranted) {
            microphoneDenied = permissions[Manifest.permission.RECORD_AUDIO] != true
            if (microphoneDenied) {
                showToast(R.string.permission_screen_mic_denied)
            } else {
                showToast(R.string.permission_request_granted)
            }
            showSettingsHint = false
            openCamera()
        } else {
            microphoneDenied = false
            refreshSettingsHint()
            showToast(R.string.permission_request_denied)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (hasPermissions()) {
            openCamera()
        } else {
            refreshSettingsHint()
        }
    }

    override fun onResume() {
        super.onResume()
        requireActivity().applyEdgeToEdgeBars(lightIcons = true)
        if (hasPermissions()) {
            openCamera()
        } else {
            refreshSettingsHint()
        }
    }

    /** Re-evaluate after Settings or a deny — "Ask every time" can request again without stale UI. */
    private fun refreshSettingsHint() {
        if (hasPermissions()) return
        showSettingsHint = autoRequested &&
            !shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)
    }

    override fun setComposeView(view: ComposeView) = view.setContent {
        LaunchedEffect(Unit) {
            if (!autoRequested && !hasPermissions()) {
                autoRequested = true
                requestRuntimePermissions()
            }
        }
        AppTheme(isDarkTheme = true, applySystemBars = false) {
            PermissionsScreen(
                showSettingsHint = showSettingsHint,
                microphoneDenied = microphoneDenied,
                onRequestPermissions = ::requestRuntimePermissions,
                onOpenSettings = ::openAppSettings
            )
        }
    }

    private fun requestRuntimePermissions() {
        activityResultLauncher.launch(RUNTIME_PERMISSIONS)
    }

    private fun openAppSettings() {
        startActivity(
            Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", requireContext().packageName, null)
            )
        )
    }

    private fun openCamera() {
        navigate(
            PermissionsFragmentDirections.actionPermissionsToCamera(),
            fromDestinationId = R.id.permissionsFragment
        )
    }
}
