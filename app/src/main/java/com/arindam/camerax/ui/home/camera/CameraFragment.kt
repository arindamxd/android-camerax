package com.arindam.camerax.ui.home.camera

import android.content.Intent
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.viewModels
import com.arindam.camerax.CameraX
import com.arindam.camerax.ui.base.BaseFragmentCompose
import com.arindam.camerax.ui.settings.SettingsActivity
import com.arindam.camerax.ui.theme.AppTheme

/**
 * Hosts the camera viewfinder. Presentation only; camera work goes through use cases.
 */
class CameraFragment : BaseFragmentCompose() {

    private val viewModel: CameraViewModel by viewModels {
        val app = requireActivity().application as CameraX
        CameraViewModelFactory(app.container.cameraInteractors)
    }

    override fun setComposeView(view: ComposeView) = view.setContent {
        AppTheme {
            CameraScreen(
                outputDirectory = getOutputFileDirectory(),
                viewModel = viewModel,
                onGalleryClicked = {
                    if (isDirectoryNotEmpty()) {
                        navigate(
                            CameraFragmentDirections.actionCameraToGallery(
                                getOutputFileDirectory().absolutePath
                            )
                        )
                    }
                },
                onSettingsClicked = {
                    startActivity(Intent(requireContext(), SettingsActivity::class.java))
                }
            )
        }
    }

    override fun onResume() {
        super.onResume()
        if (!hasPermissions()) {
            navigate(CameraFragmentDirections.actionCameraToPermissions())
        }
    }
}
