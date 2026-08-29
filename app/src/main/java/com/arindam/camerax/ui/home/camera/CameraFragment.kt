package com.arindam.camerax.ui.home.camera

import android.app.Activity
import android.content.ClipData
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.ComposeView
import androidx.core.content.FileProvider
import androidx.core.app.ActivityCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.arindam.camerax.BuildConfig
import com.arindam.camerax.CameraX
import com.arindam.camerax.R
import com.arindam.camerax.data.camera.MotionPhotoMuxer
import com.arindam.camerax.data.camera.StillImageExif
import com.arindam.camerax.ui.base.BaseFragmentCompose
import com.arindam.camerax.ui.settings.SettingsActivity
import com.arindam.camerax.ui.theme.AppTheme
import com.arindam.camerax.util.commons.Constants.PERMISSIONS.MICROPHONE_PERMISSION
import com.arindam.camerax.util.permission.MicrophonePermission
import com.arindam.camerax.util.theme.applyEdgeToEdgeBars
import com.arindam.camerax.util.theme.applyEdgeToEdgeBarsForNightMode
import java.io.File
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Presentation: hosts the camera viewfinder. Camera work goes through use cases only.
 */
class CameraFragment : BaseFragmentCompose() {

    private val viewModel: CameraViewModel by viewModels {
        val app = requireActivity().application as CameraX
        CameraViewModelFactory(app.container)
    }

    private val micPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.onMicrophonePermissionResult(granted)
        if (granted) return@registerForActivityResult
        if (ActivityCompat.shouldShowRequestPermissionRationale(requireActivity(), MICROPHONE_PERMISSION)) {
            showToast(R.string.permission_mic_denied)
        } else {
            showToast(R.string.permission_mic_settings)
            MicrophonePermission.openAppSettings(requireContext())
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.applyLaunchIntent(requireActivity().intent)
    }

    override fun setComposeView(view: ComposeView) = view.setContent {
        val cameraState by viewModel.uiState.collectAsStateWithLifecycle()
        val followAppTheme = cameraState.showsTools || cameraState.review != null
        LaunchedEffect(cameraState.showsTools, cameraState.review) {
            if (cameraState.review != null || cameraState.showsTools) {
                requireActivity().applyEdgeToEdgeBarsForNightMode()
            } else {
                requireActivity().applyEdgeToEdgeBars(lightIcons = true)
            }
        }
        AppTheme(
            isDarkTheme = if (followAppTheme) isSystemInDarkTheme() else true,
            applySystemBars = cameraState.review == null
        ) {
            CameraScreen(
                viewModel = viewModel,
                onRequestMicrophonePermission = ::requestMicrophonePermission,
                onGalleryClicked = {
                    if (viewModel.hasGalleryItems()) {
                        navigate(
                            CameraFragmentDirections.actionCameraToGallery(
                                viewModel.picturesDirectory().absolutePath
                            )
                        )
                    }
                },
                onSettingsClicked = {
                    startActivity(Intent(requireContext(), SettingsActivity::class.java))
                },
                onExternalCaptureReady = { file -> deliverExternalCapture(file) }
            )
        }
    }

    override fun onResume() {
        super.onResume()
        if (viewModel.uiState.value.review != null || viewModel.uiState.value.showsTools) {
            requireActivity().applyEdgeToEdgeBarsForNightMode()
        } else {
            requireActivity().applyEdgeToEdgeBars(lightIcons = true)
        }
        viewModel.updateMicrophonePermission(MicrophonePermission.isGranted(requireContext()))
        viewModel.syncHost()
        if (!hasPermissions()) {
            navigate(
                CameraFragmentDirections.actionCameraToPermissions(),
                fromDestinationId = R.id.cameraFragment
            )
        }
    }

    private fun requestMicrophonePermission() {
        if (MicrophonePermission.isGranted(requireContext())) {
            viewModel.onMicrophonePermissionResult(true)
            return
        }
        micPermissionLauncher.launch(MICROPHONE_PERMISSION)
    }

    private fun deliverExternalCapture(file: File) {
        val activity = requireActivity()
        val request = ExternalCaptureRequest.from(activity.intent)
        val io = (activity.application as CameraX).container.dispatchers.io
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val result = withContext(io) { buildCaptureResult(file, request) }
                activity.setResult(Activity.RESULT_OK, result)
                activity.finish()
            } catch (error: Exception) {
                Toast.makeText(
                    requireContext(),
                    error.message ?: "Unable to return capture",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun buildCaptureResult(file: File, request: ExternalCaptureRequest): Intent {
        val result = Intent()
        val outputUri = request.outputUri
        if (outputUri != null) {
            requireActivity().contentResolver.openOutputStream(outputUri)?.use { output ->
                file.inputStream().use { input -> input.copyTo(output) }
            } ?: error("Unable to write capture output")
            grantResultUri(result, outputUri)
        } else if (
            (file.extension.equals("jpg", ignoreCase = true) ||
                file.extension.equals("jpeg", ignoreCase = true)) &&
            !MotionPhotoMuxer.isMotionPhoto(file)
        ) {
            result.putExtra("data", thumbnailBitmap(file))
        } else {
            val uri = FileProvider.getUriForFile(
                requireContext(),
                BuildConfig.APPLICATION_ID + ".provider",
                file
            )
            grantResultUri(result, uri)
        }
        return result
    }

    private fun grantResultUri(intent: Intent, uri: Uri) {
        intent.data = uri
        intent.clipData = ClipData.newRawUri("", uri)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    private fun thumbnailBitmap(file: File): Bitmap {
        val bitmap = StillImageExif.decodeSampled(file, THUMBNAIL_SIZE * 2)
            ?: error("Unable to decode capture")
        return if (bitmap.width <= THUMBNAIL_SIZE && bitmap.height <= THUMBNAIL_SIZE) {
            bitmap
        } else {
            val scale = THUMBNAIL_SIZE.toFloat() / maxOf(bitmap.width, bitmap.height)
            Bitmap.createScaledBitmap(
                bitmap,
                (bitmap.width * scale).toInt().coerceAtLeast(1),
                (bitmap.height * scale).toInt().coerceAtLeast(1),
                true
            ).also { if (it !== bitmap) bitmap.recycle() }
        }
    }

    companion object {
        private const val THUMBNAIL_SIZE = 256
    }
}
