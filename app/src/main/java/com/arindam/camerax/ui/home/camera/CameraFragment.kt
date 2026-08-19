package com.arindam.camerax.ui.home.camera

import android.app.Activity
import android.content.ClipData
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.compose.ui.platform.ComposeView
import androidx.core.content.FileProvider
import androidx.fragment.app.viewModels
import androidx.preference.PreferenceManager
import com.arindam.camerax.BuildConfig
import com.arindam.camerax.CameraX
import com.arindam.camerax.R
import com.arindam.camerax.data.camera.MotionPhotoMuxer
import com.arindam.camerax.domain.model.CaptureAspect
import com.arindam.camerax.domain.model.SlowMotionRate
import com.arindam.camerax.domain.model.VideoHdrRange
import com.arindam.camerax.domain.model.VideoQuality
import com.arindam.camerax.ui.base.BaseFragmentCompose
import com.arindam.camerax.ui.settings.SettingsActivity
import com.arindam.camerax.ui.theme.AppTheme
import java.io.File

/**
 * Hosts the camera viewfinder. Presentation only; camera work goes through use cases.
 */
class CameraFragment : BaseFragmentCompose() {

    private val viewModel: CameraViewModel by viewModels {
        val app = requireActivity().application as CameraX
        CameraViewModelFactory(app.container.cameraInteractors, app)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.applyLaunchIntent(requireActivity().intent)
    }

    override fun setComposeView(view: ComposeView) = view.setContent {
        AppTheme(isDarkTheme = true) {
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
                },
                onExternalCaptureReady = { file -> deliverExternalCapture(file) }
            )
        }
    }

    override fun onResume() {
        super.onResume()
        val prefs = PreferenceManager.getDefaultSharedPreferences(requireContext())
        viewModel.applyCapturePreferences(
            confirmEnabled = prefs.getBoolean(getString(R.string.pref_key_capture_confirm), false),
            aspect = CaptureAspect.fromPref(prefs.getString(getString(R.string.pref_key_capture_aspect), null)),
            quality = VideoQuality.fromPref(prefs.getString(getString(R.string.pref_key_video_quality), null)),
            videoHdrRange = VideoHdrRange.fromPref(
                prefs.getString(getString(R.string.pref_key_video_hdr), null)
            ),
            videoStabilization = prefs.getBoolean(getString(R.string.pref_key_video_stabilization), true),
            slowMotionQuality = VideoQuality.fromPref(
                prefs.getString(getString(R.string.pref_key_slow_motion_quality), null)
            ),
            slowMotionRate = SlowMotionRate.fromPref(
                prefs.getString(getString(R.string.pref_key_slow_motion_fps), null)
            ),
            ultraHdr = prefs.getBoolean(getString(R.string.pref_key_ultra_hdr), true),
            rawCapture = prefs.getBoolean(getString(R.string.pref_key_raw_capture), false),
            rawFullSensor = prefs.getBoolean(getString(R.string.pref_key_raw_full_sensor), false),
            flipWhileRecording = prefs.getBoolean(
                getString(R.string.pref_key_flip_while_recording),
                false
            ),
            lowLightBoost = prefs.getBoolean(getString(R.string.pref_key_low_light_boost), true),
            videoFps60 = prefs.getBoolean(getString(R.string.pref_key_video_fps_60), false)
        )
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
