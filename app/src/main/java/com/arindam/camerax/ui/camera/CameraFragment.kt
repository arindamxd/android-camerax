package com.arindam.camerax.ui.camera

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.hardware.Camera.ACTION_NEW_PICTURE
import android.hardware.display.DisplayManager
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.HandlerThread
import android.util.DisplayMetrics
import android.view.KeyEvent
import android.view.View
import android.webkit.MimeTypeMap
import android.widget.ImageButton
import androidx.camera.core.*
import androidx.camera.core.ImageCapture.Metadata
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.net.toFile
import androidx.core.view.setPadding
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.arindam.camerax.R
import com.arindam.camerax.utils.analyzer.LuminosityAnalyzer
import com.arindam.camerax.ui.base.BaseFragment
import com.arindam.camerax.utils.ANIMATION_FAST_MILLIS
import com.arindam.camerax.utils.ANIMATION_SLOW_MILLIS
import com.arindam.camerax.utils.commons.Constants.EXTRAS.KEY_EVENT_ACTION
import com.arindam.camerax.utils.commons.Constants.EXTRAS.KEY_EVENT_EXTRA
import com.arindam.camerax.utils.commons.Constants.FILE.EXTENSION_WHITELIST
import com.arindam.camerax.utils.log.Logger
import com.arindam.camerax.utils.simulateClick
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Main fragment for this app. Implements all camera operations including:
 * - Viewfinder
 * - Photo taking
 * - Image analysis
 *
 * Created by Arindam Karmakar on 9/5/19.
 */

class CameraFragment : BaseFragment() {

    private lateinit var container: ConstraintLayout
    private lateinit var viewFinder: PreviewView
    private lateinit var outputDirectory: File
    private lateinit var broadcastManager: LocalBroadcastManager

    private var displayId = -1
    private var lensFacing = CameraSelector.LENS_FACING_BACK

    private var previewBuilder: Preview.Builder? = null
    private var preview: Preview? = null
    private var imageCaptureBuilder: ImageCapture.Builder? = null
    private var imageCapture: ImageCapture? = null

    private var imageAnalyzer: ImageAnalysis? = null
    private var camera: Camera? = null

    /** Declare worker thread at the class level so it can be reused after config changes */
    private val analyzerThread = HandlerThread("LuminosityAnalysis").apply { start() }

    /** Internal reference of the [DisplayManager] */
    private val displayManager by lazy {
        requireContext().getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
    }

    /** Blocking camera operations are performed using this executor */
    private lateinit var cameraExecutor: ExecutorService

    /** Volume down button receiver used to trigger shutter */
    private val volumeDownReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.getIntExtra(KEY_EVENT_EXTRA, KeyEvent.KEYCODE_UNKNOWN)) {

                // When the volume down button is pressed, simulate a shutter button click
                KeyEvent.KEYCODE_VOLUME_DOWN -> {
                    val shutter = container.findViewById<ImageButton>(R.id.camera_capture_button)
                    shutter.simulateClick()
                }
            }
        }
    }

    /**
     * We need a display listener for orientation changes that do not trigger a configuration
     * change, for example if we choose to override config change in manifest or for 180-degree
     * orientation changes.
     */
    private val displayListener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) = Unit
        override fun onDisplayRemoved(displayId: Int) = Unit
        override fun onDisplayChanged(displayId: Int) = view?.let { view ->
            if (displayId == this@CameraFragment.displayId) {
                Logger.error(TAG, "Rotation changed: ${view.display.rotation}")

                imageCapture?.targetRotation = view.display.rotation
                imageAnalyzer?.targetRotation = view.display.rotation
            }
        } ?: Unit
    }

    companion object {

        private const val TAG = "CameraX"

        private const val FILENAME = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val PHOTO_EXTENSION = ".jpg"
        private const val RATIO_4_3_VALUE = 4.0 / 3.0
        private const val RATIO_16_9_VALUE = 16.0 / 9.0
    }

    override fun provideLayout(): Int = R.layout.fragment_camera
    override fun provideView(): View? = null

    override fun setupView(view: View, savedInstanceState: Bundle?) {
        container = view as ConstraintLayout
        viewFinder = container.findViewById(R.id.view_finder)

        // Initialize our background executor
        cameraExecutor = Executors.newSingleThreadExecutor()
        broadcastManager = LocalBroadcastManager.getInstance(view.context)

        // Set up the intent filter that will receive events from our main activity
        val filter = IntentFilter().apply { addAction(KEY_EVENT_ACTION) }
        broadcastManager.registerReceiver(volumeDownReceiver, filter)

        // Every time the orientation of device changes, recompute layout
        displayManager.registerDisplayListener(displayListener, null)

        // Determine the output directory
        outputDirectory = getOutputFileDirectory()

        // Wait for the views to be properly laid out
        viewFinder.post {

            // Keep track of the display in which this view is attached
            displayId = viewFinder.display.displayId

            // Build UI controls
            updateCameraUi()

            // Bind all camera use cases
            bindCameraUseCases()
        }
    }

    override fun onResume() {
        super.onResume()
        // Make sure that all permissions are still present, since user could have removed them
        // while the app was on paused state
        if (!hasPermissions()) {
            navigate(R.id.fragment_container, CameraFragmentDirections.actionCameraToPermissions())
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()

        // Shut down our background executor
        cameraExecutor.shutdown()

        // Unregister the broadcast receivers and listeners
        broadcastManager.unregisterReceiver(volumeDownReceiver)
        displayManager.unregisterDisplayListener(displayListener)
    }

    /**
     * Inflate camera controls and update the UI manually upon config changes to avoid removing
     * and re-adding the view finder from the view hierarchy; this provides a seamless rotation
     * transition on devices that support it.
     *
     * NOTE: The flag is supported starting in Android 8 but there still is a small flash on the
     * screen for devices that run Android 9 or below.
     */
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        updateCameraUi()
    }

    /** Method used to re-draw the camera UI controls, called every time configuration changes */
    @SuppressLint("RestrictedApi")
    private fun updateCameraUi() {

        // Remove previous UI if any
        container.findViewById<ConstraintLayout>(R.id.camera_ui_container)?.let {
            container.removeView(it)
        }

        // Inflate a new view containing all UI for controlling the camera
        val controls = View.inflate(requireContext(), R.layout.camera_ui_container, container)

        // In the background, load latest photo taken (if any) for gallery thumbnail
        lifecycleScope.launch(Dispatchers.IO) {
            outputDirectory.listFiles { file ->
                EXTENSION_WHITELIST.contains(file.extension.toUpperCase(Locale.ROOT))
            }?.max()?.let {
                setGalleryThumbnail(Uri.fromFile(it))
            }
        }

        // Listener for button used to capture photo
        controls.findViewById<ImageButton>(R.id.camera_capture_button).setOnClickListener {

            // Get a stable reference of the modifiable image capture use case
            imageCapture?.let { imageCapture ->

                // Create output file to hold the image
                val photoFile = createFile(outputDirectory,
                    FILENAME,
                    PHOTO_EXTENSION
                )

                // Setup image capture metadata
                val metadata = Metadata().apply {

                    // Mirror image when using the front camera
                    isReversedHorizontal = lensFacing == CameraSelector.LENS_FACING_FRONT
                }

                // Create output options object which contains file + metadata
                val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile)
                    .setMetadata(metadata)
                    .build()

                // Setup image capture listener which is triggered after photo has been taken
                imageCapture.takePicture(outputOptions, cameraExecutor, object : ImageCapture.OnImageSavedCallback {

                        override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                            val savedUri = output.savedUri ?: Uri.fromFile(photoFile)
                            Logger.debug(TAG, "Photo capture succeeded: $savedUri")

                            // We can only change the foreground Drawable using API level 23+ API
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                // Update the gallery thumbnail with latest picture taken
                                setGalleryThumbnail(savedUri)
                            }

                            // Implicit broadcasts will be ignored for devices running API level >= 24
                            // so if you only target API level 24+ you can remove this statement
                            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                                requireActivity().sendBroadcast(Intent(ACTION_NEW_PICTURE, savedUri))
                            }

                            // If the folder selected is an external media directory, this is
                            // unnecessary but otherwise other apps will not be able to access our
                            // images unless we scan them using [MediaScannerConnection]
                            val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(savedUri.toFile().extension)
                            MediaScannerConnection.scanFile(context, arrayOf(savedUri.toString()), arrayOf(mimeType)) { _, uri ->
                                Logger.debug(TAG, "Image capture scanned into media store: $uri")
                            }
                        }

                        override fun onError(exception: ImageCaptureException) {
                            Logger.debug(TAG, "Photo capture failed: ${exception.message}")
                        }
                    })

                // We can only change the foreground Drawable using API level 23+ API
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

                    // Display flash animation to indicate that photo was captured
                    container.postDelayed({
                        container.foreground = ColorDrawable(Color.WHITE)
                        container.postDelayed({ container.foreground = null }, ANIMATION_FAST_MILLIS)
                    }, ANIMATION_SLOW_MILLIS)
                }
            }
        }

        // Listener for button used to switch cameras
        controls.findViewById<ImageButton>(R.id.camera_switch_button).setOnClickListener {

            lensFacing = if (CameraSelector.LENS_FACING_FRONT == lensFacing) {
                CameraSelector.LENS_FACING_BACK
            } else {
                CameraSelector.LENS_FACING_FRONT
            }

            // Re-bind use cases to update selected camera
            bindCameraUseCases()
        }

        // Listener for button used to view last photo
        controls.findViewById<ImageButton>(R.id.photo_view_button).setOnClickListener {
            if (isDirectoryNotEmpty()) {
                navigate(R.id.fragment_container,
                    CameraFragmentDirections.actionCameraToGallery(
                        outputDirectory.absolutePath
                    )
                )
            }
        }
    }

    /** Declare and bind preview, capture and analysis use cases */
    private fun bindCameraUseCases() {

        // Get screen metrics used to setup camera for full screen resolution
        val metrics = DisplayMetrics().also { viewFinder.display.getRealMetrics(it) }
        Logger.debug(TAG, "Screen metrics: ${metrics.widthPixels} x ${metrics.heightPixels}")

        val screenAspectRatio = aspectRatio(metrics.widthPixels, metrics.heightPixels)
        Logger.debug(TAG, "Preview aspect ratio: $screenAspectRatio")

        val rotation = viewFinder.display.rotation

        // Bind the CameraProvider to the LifeCycleOwner
        val cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

        cameraProviderFuture.addListener(Runnable {

            // CameraProvider
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Preview
            preview = Preview.Builder()
                // We request aspect ratio but no resolution
                .setTargetAspectRatio(screenAspectRatio)
                // Set initial target rotation
                .setTargetRotation(rotation)
                .build()

            // Attach the viewfinder's surface provider to preview use case
            preview?.setSurfaceProvider(viewFinder.createSurfaceProvider(camera?.cameraInfo))

            // ImageCapture
            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                // We request aspect ratio but no resolution to match preview config, but letting
                // CameraX optimize for whatever specific resolution best fits our use cases
                .setTargetAspectRatio(screenAspectRatio)
                // Set initial target rotation, we will have to call this again if rotation changes
                // during the lifecycle of this use case
                .setTargetRotation(rotation)
                .build()

            // ImageAnalysis
            imageAnalyzer = ImageAnalysis.Builder()
                // We request aspect ratio but no resolution
                .setTargetAspectRatio(screenAspectRatio)
                // Set initial target rotation, we will have to call this again if rotation changes
                // during the lifecycle of this use case
                .setTargetRotation(rotation)
                .build()
                // The analyzer can then be assigned to the instance
                .also {
                    it.setAnalyzer(cameraExecutor, LuminosityAnalyzer { luma ->
                        // Values returned from our analyzer are passed to the attached listener
                        // We log image analysis results here - you should do something useful
                        // instead!
                        Logger.debug(TAG, "Average luminosity: $luma")
                    })
                }

            // Must unbind the use-cases before rebinding them
            cameraProvider.unbindAll()

            try {

                // Create an Extender object which can be used to apply extension
                // configurations.
                /*val customCaptureExtender = BeautyImageCaptureExtender.create(imageCaptureBuilder)
                val customPreviewExtender = BeautyPreviewExtender.create(previewBuilder)

                // Query if extension is available (optional).
                if (customCaptureExtender.isExtensionAvailable(cameraSelector)) {
                    // Enable the extension if available.
                }
                customCaptureExtender.enableExtension(cameraSelector)
                customPreviewExtender.enableExtension(cameraSelector)*/

                // A variable number of use-cases can be passed here -
                // camera provides access to CameraControl & CameraInfo
                camera = /*if (BuildConfig.DEBUG) {
                    cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture,  imageAnalyzer)
                } else*/ cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)



                /*viewFinder.setOnTouchListener { _, event ->
                    if (event.action != MotionEvent.ACTION_UP) {
                        return@setOnTouchListener false
                    }

                    val factory = viewFinder.createMeteringPointFactory(cameraSelector)
                    val point = factory.createPoint(event.x, event.y)
                    val action = FocusMeteringAction.Builder(point).build()
                    camera?.cameraControl?.startFocusAndMetering(action)
                    return@setOnTouchListener true
                }



                val listener = object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                    override fun onScale(detector: ScaleGestureDetector): Boolean {
                        val currentZoomRatio: Float = camera?.cameraInfo?.zoomState?.value?.zoomRatio ?: 0F
                        val delta = detector.scaleFactor
                        camera?.cameraControl?.setZoomRatio(currentZoomRatio * delta)
                        return true
                    }
                }

                val scaleGestureDetector = ScaleGestureDetector(context, listener)

                viewFinder.setOnTouchListener { _, event ->
                    scaleGestureDetector.onTouchEvent(event)
                    return@setOnTouchListener true
                }



                zoomBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                        camera?.cameraControl?.setLinearZoom(progress / 100.toFloat())
                    }

                    override fun onStartTrackingTouch(seekBar: SeekBar?) {}

                    override fun onStopTrackingTouch(seekBar: SeekBar?) {}
                })*/


            } catch (exception: Exception) {
                Logger.debug(TAG, "Use case binding failed: ${exception.message}")
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    /**
     *  [androidx.camera.core.impl.ImageAnalysisConfig] requires enum value of
     *  [androidx.camera.core.AspectRatio]. Currently it has values of 4:3 & 16:9.
     *
     *  Detecting the most suitable ratio for dimensions provided in @params by counting absolute
     *  of preview ratio to one of the provided values.
     *
     *  @param width - preview width
     *  @param height - preview height
     *  @return suitable aspect ratio
     */
    private fun aspectRatio(width: Int, height: Int): Int {
        val previewRatio = max(width, height).toDouble() / min(width, height)
        return if (abs(previewRatio - RATIO_4_3_VALUE) <= abs(previewRatio - RATIO_16_9_VALUE)) AspectRatio.RATIO_4_3
        else AspectRatio.RATIO_16_9
    }

    private fun setGalleryThumbnail(uri: Uri) {
        // Reference of the view that holds the gallery thumbnail
        val thumbnail = container.findViewById<ImageButton>(R.id.photo_view_button)

        // Run the operations in the view's thread
        thumbnail.post {
            // Remove thumbnail padding
            thumbnail.setPadding(resources.getDimension(R.dimen.stroke_small).toInt())

            // Load thumbnail into circular button using Glide
            Glide.with(thumbnail)
                .load(uri)
                .apply(RequestOptions.circleCropTransform())
                .into(thumbnail)
        }
    }
}
