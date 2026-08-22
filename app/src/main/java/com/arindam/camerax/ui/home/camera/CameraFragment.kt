package com.arindam.camerax.ui.home.camera

import android.app.Activity
import android.content.ClipData
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.core.content.FileProvider
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arindam.camerax.BuildConfig
import com.arindam.camerax.CameraX
import com.arindam.camerax.R
import com.arindam.camerax.data.camera.MotionPhotoMuxer
import com.arindam.camerax.ui.base.BaseFragmentCompose
import com.arindam.camerax.ui.settings.SettingsActivity
import com.arindam.camerax.ui.theme.AppTheme
import com.arindam.camerax.util.theme.applyEdgeToEdgeBars
import com.arindam.camerax.util.theme.applyEdgeToEdgeBarsForNightMode
import java.io.File

/**
 * Hosts the camera viewfinder. Presentation only; camera work goes through use cases.
 */
class CameraFragment : BaseFragmentCompose() {

    private val viewModel: CameraViewModel by viewModels {
        val app = requireActivity().application as CameraX
        CameraViewModelFactory(app.container)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.applyLaunchIntent(requireActivity().intent)
    }

    override fun setComposeView(view: ComposeView) = view.setContent {
        val cameraState by viewModel.uiState.collectAsStateWithLifecycle()
        AppTheme(isDarkTheme = true, applySystemBars = cameraState.review == null) {
            CameraScreen(
                viewModel = viewModel,
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
        if (viewModel.uiState.value.review == null) {
            requireActivity().applyEdgeToEdgeBars(lightIcons = true)
        } else {
            requireActivity().applyEdgeToEdgeBarsForNightMode()
        }
        viewModel.syncHost()
        if (!hasPermissions()) {
            navigate(CameraFragmentDirections.actionCameraToPermissions())
        }
    }

    private fun deliverExternalCapture(file: File) {
        val activity = requireActivity()
        val request = ExternalCaptureRequest.from(activity.intent)
        val result = Intent()
        try {
            val outputUri = request.outputUri
            if (outputUri != null) {
                activity.contentResolver.openOutputStream(outputUri)?.use { output ->
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

    private fun grantResultUri(intent: Intent, uri: Uri) {
        intent.data = uri
        intent.clipData = ClipData.newRawUri("", uri)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    private fun thumbnailBitmap(file: File): Bitmap {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        val longest = maxOf(bounds.outWidth, bounds.outHeight).coerceAtLeast(1)
        val sample = (longest / THUMBNAIL_SIZE).coerceAtLeast(1)
        val bitmap = BitmapFactory.decodeFile(
            file.absolutePath,
            BitmapFactory.Options().apply { inSampleSize = sample }
        ) ?: error("Unable to decode capture")
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
