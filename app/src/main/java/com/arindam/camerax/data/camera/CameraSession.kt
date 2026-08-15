package com.arindam.camerax.data.camera

import android.Manifest
import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.Rect
import android.graphics.RectF
import android.os.Handler
import android.os.HandlerThread
import android.view.Surface
import androidx.annotation.MainThread
import androidx.camera.core.Camera
import androidx.camera.core.CameraEffect
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.core.UseCase
import androidx.camera.core.UseCaseGroup
import androidx.camera.effects.OverlayEffect
import androidx.camera.extensions.ExtensionsManager
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.mlkit.vision.MlKitAnalyzer
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import androidx.core.net.toFile
import androidx.lifecycle.LifecycleOwner
import com.arindam.camerax.domain.model.CameraBindConfig
import com.arindam.camerax.domain.model.CameraBindResult
import com.arindam.camerax.domain.model.CameraExtension
import com.arindam.camerax.domain.model.CameraHost
import com.arindam.camerax.domain.model.CameraLens
import com.arindam.camerax.domain.model.ColorFilterType
import com.arindam.camerax.domain.model.FlashMode
import com.arindam.camerax.domain.model.RecordingEvent
import com.arindam.camerax.domain.model.ZoomInfo
import com.arindam.camerax.domain.repository.CameraRepository
import com.arindam.camerax.util.commons.Constants
import com.arindam.camerax.util.log.Logger
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * CameraX implementation of [CameraRepository].
 */
class CameraSession(private val context: Context) : CameraRepository {

    private val cameraExecutor = Executors.newSingleThreadExecutor()
    private val overlayThread = HandlerThread("CameraXOverlay").apply { start() }
    private val overlayHandler = Handler(overlayThread.looper)
    private val facePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = 0xFFF9AA33.toInt()
        strokeWidth = 6f
    }

    private var cameraProvider: ProcessCameraProvider? = null
    private var extensionsManager: ExtensionsManager? = null
    private var camera: Camera? = null
    private var imageCapture: ImageCapture? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var recording: Recording? = null
    private var overlayEffect: OverlayEffect? = null
    private var colorProcessor: ColorFilterProcessor? = null
    private var faceDetector: FaceDetector? = null
    private var boundPreviewView: PreviewView? = null
    @Volatile private var latestFaces: List<Rect> = emptyList()

    suspend fun initialize() {
        if (cameraProvider != null) return
        cameraProvider = awaitProvider()
        extensionsManager = awaitExtensions(cameraProvider!!)
    }

    fun supportedExtensions(selector: CameraSelector): Set<CameraExtension> {
        val manager = extensionsManager ?: return emptySet()
        return CameraExtension.entries.filter { extension ->
            extension != CameraExtension.NONE &&
                manager.isExtensionAvailable(selector, extension.toExtensionMode())
        }.toSet()
    }

    @MainThread
    override suspend fun bind(host: CameraHost, config: CameraBindConfig): CameraBindResult {
        val previewHost = host as? PreviewViewHost
            ?: throw IllegalArgumentException("Unsupported camera host")
        val lifecycleOwner = previewHost.lifecycleOwner
        val previewView = previewHost.previewView
        boundPreviewView = previewView
        initialize()
        val provider = cameraProvider ?: throw IllegalStateException("Camera provider missing")
        provider.unbindAll()
        releaseEffects()

        previewView.implementationMode = PreviewView.ImplementationMode.PERFORMANCE
        previewView.scaleType = PreviewView.ScaleType.FILL_CENTER
        val rotation = previewView.display?.rotation ?: Surface.ROTATION_0

        val baseSelector = config.lens.toSelector()
        val manager = extensionsManager
        val useExtension = config.extension != CameraExtension.NONE &&
            manager?.isExtensionAvailable(baseSelector, config.extension.toExtensionMode()) == true
        val selector = if (useExtension) {
            manager!!.getExtensionEnabledCameraSelector(baseSelector, config.extension.toExtensionMode())
        } else {
            baseSelector
        }

        val preview = Preview.Builder()
            .setTargetRotation(rotation)
            .build()
            .also { it.surfaceProvider = previewView.surfaceProvider }

        val capture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .setFlashMode(config.flash.toImageCaptureMode())
            .setTargetRotation(rotation)
            .build()
        imageCapture = capture

        val recorder = Recorder.Builder()
            .setQualitySelector(
                QualitySelector.from(
                    Quality.FHD,
                    FallbackStrategy.lowerQualityOrHigherThan(Quality.HD)
                )
            )
            .build()
        val video = VideoCapture.withOutput(recorder)
        videoCapture = video

        val analysis = if (config.faceDetection && !useExtension) {
            ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setTargetRotation(rotation)
                .build()
                .also { setupFaceAnalyzer(it) }
        } else {
            null
        }

        val includeVideo = !useExtension
        val includeAnalysis = analysis != null
        val effects = buildEffects(
            colorFilter = config.colorFilter,
            faceDetection = config.faceDetection && !useExtension,
            includeVideo = includeVideo
        )

        camera = bindWithFallback(
            provider = provider,
            lifecycleOwner = lifecycleOwner,
            selector = selector,
            preview = preview,
            imageCapture = capture,
            videoCapture = if (includeVideo) video else null,
            imageAnalysis = if (includeAnalysis) analysis else null,
            effects = effects
        )
        if (!includeVideo) videoCapture = null

        applyTorch(config.flash)
        val zoom = camera?.cameraInfo?.zoomState?.value
        return CameraBindResult(
            hasFlash = camera?.cameraInfo?.hasFlashUnit() == true,
            minZoom = zoom?.minZoomRatio ?: 1f,
            maxZoom = zoom?.maxZoomRatio ?: 1f,
            zoomRatio = zoom?.zoomRatio ?: 1f,
            videoAvailable = videoCapture != null,
            supportedExtensions = supportedExtensions(baseSelector)
        )
    }

    override fun capturePhoto(
        outputDirectory: File,
        lens: CameraLens,
        colorFilter: ColorFilterType,
        onSaved: (File) -> Unit,
        onError: (String) -> Unit
    ) {
        val capture = imageCapture ?: return onError("Camera is not ready")
        val photoFile = createFile(outputDirectory, Constants.FILE.PHOTO_EXTENSION)
        val metadata = ImageCapture.Metadata().apply {
            isReversedHorizontal = lens == CameraLens.FRONT
        }
        val options = ImageCapture.OutputFileOptions.Builder(photoFile)
            .setMetadata(metadata)
            .build()
        capture.takePicture(
            options,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val file = output.savedUri?.toFile() ?: photoFile
                    val processed = applyStillFilter(file, colorFilter)
                    onSaved(processed)
                }

                override fun onError(exception: ImageCaptureException) {
                    Logger.error(TAG, "Photo capture failed: ${exception.message}")
                    onError(exception.message ?: "Photo capture failed")
                }
            }
        )
    }

    override fun startRecording(
        outputDirectory: File,
        muted: Boolean,
        onEvent: (RecordingEvent) -> Unit,
        onError: (String) -> Unit
    ): File? {
        val capture = videoCapture ?: run {
            onError("Video capture is not available with this effect")
            return null
        }
        recording?.stop()
        val videoFile = createFile(outputDirectory, Constants.FILE.VIDEO_EXTENSION)
        val output = FileOutputOptions.Builder(videoFile).build()
        val pending = capture.output.prepareRecording(context, output)
        val withAudio = PermissionChecker.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PermissionChecker.PERMISSION_GRANTED
        val active = if (withAudio) pending.withAudioEnabled() else pending
        recording = active.start(ContextCompat.getMainExecutor(context)) { event ->
            event.toDomain()?.let(onEvent)
        }
        if (withAudio && muted) {
            recording?.mute(true)
        }
        return videoFile
    }

    override fun pauseRecording() {
        recording?.pause()
    }

    override fun resumeRecording() {
        recording?.resume()
    }

    override fun stopRecording() {
        recording?.stop()
        recording = null
    }

    override fun muteRecording(muted: Boolean) {
        recording?.mute(muted)
    }

    override fun setFlash(mode: FlashMode) {
        imageCapture?.flashMode = mode.toImageCaptureMode()
        applyTorch(mode)
    }

    override fun setZoomRatio(ratio: Float): ZoomInfo? {
        val state = camera?.cameraInfo?.zoomState?.value ?: return null
        camera?.cameraControl?.setZoomRatio(ratio.coerceIn(state.minZoomRatio, state.maxZoomRatio))
        val updated = camera?.cameraInfo?.zoomState?.value ?: state
        return ZoomInfo(
            ratio = updated.zoomRatio,
            min = updated.minZoomRatio,
            max = updated.maxZoomRatio
        )
    }

    override fun tapToFocus(x: Float, y: Float) {
        val previewView = boundPreviewView ?: return
        val factory = previewView.meteringPointFactory
        val point = factory.createPoint(x, y)
        val action = FocusMeteringAction.Builder(
            point,
            FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE
        ).setAutoCancelDuration(3, TimeUnit.SECONDS).build()
        camera?.cameraControl?.startFocusAndMetering(action)
    }

    override fun setColorFilter(type: ColorFilterType) {
        colorProcessor?.colorMatrix = ColorFilters.glMatrix(type)
    }

    override fun release() {
        recording?.stop()
        recording = null
        cameraProvider?.unbindAll()
        releaseEffects()
        faceDetector?.close()
        faceDetector = null
        cameraExecutor.shutdown()
        overlayThread.quitSafely()
        colorProcessor?.release()
        colorProcessor = null
    }

    private fun applyTorch(mode: FlashMode) {
        camera?.cameraControl?.enableTorch(mode == FlashMode.TORCH)
    }

    private fun setupFaceAnalyzer(analysis: ImageAnalysis) {
        val detector = faceDetector ?: FaceDetection.getClient(
            FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
                .build()
        ).also { faceDetector = it }
        analysis.setAnalyzer(
            cameraExecutor,
            MlKitAnalyzer(
                listOf(detector),
                ImageAnalysis.COORDINATE_SYSTEM_SENSOR,
                cameraExecutor
            ) { result ->
                latestFaces = result.getValue(detector)?.map { it.boundingBox } ?: emptyList()
            }
        )
    }

    private fun buildEffects(
        colorFilter: ColorFilterType,
        faceDetection: Boolean,
        includeVideo: Boolean
    ): List<CameraEffect> {
        val targets = CameraEffect.PREVIEW or if (includeVideo) CameraEffect.VIDEO_CAPTURE else 0
        val effects = mutableListOf<CameraEffect>()
        if (colorFilter != ColorFilterType.NONE) {
            val processor = ColorFilterProcessor().also { colorProcessor = it }
            processor.colorMatrix = ColorFilters.glMatrix(colorFilter)
            effects += processor.asCameraEffect(targets)
        }
        if (faceDetection) {
            val overlay = OverlayEffect(
                targets,
                0,
                overlayHandler
            ) { error -> Logger.error(TAG, "Overlay error: ${error.message}") }
            overlay.setOnDrawListener { frame ->
                val canvas = frame.overlayCanvas
                canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
                canvas.save()
                canvas.setMatrix(frame.sensorToBufferTransform)
                latestFaces.forEach { rect ->
                    canvas.drawRoundRect(RectF(rect), 24f, 24f, facePaint)
                }
                canvas.restore()
                true
            }
            overlayEffect = overlay
            effects += overlay
        }
        return effects
    }

    private fun bindWithFallback(
        provider: ProcessCameraProvider,
        lifecycleOwner: LifecycleOwner,
        selector: CameraSelector,
        preview: Preview,
        imageCapture: ImageCapture,
        videoCapture: VideoCapture<Recorder>?,
        imageAnalysis: ImageAnalysis?,
        effects: List<CameraEffect>
    ): Camera {
        val attempts: List<List<UseCase>> = listOfNotNull(
            listOfNotNull(preview, imageCapture, videoCapture, imageAnalysis),
            listOfNotNull(preview, imageCapture, videoCapture),
            listOfNotNull(preview, imageCapture, imageAnalysis),
            listOf(preview, imageCapture)
        ).distinct()
        var lastError: Exception? = null
        for (useCases in attempts) {
            try {
                provider.unbindAll()
                val includeEffects = effects.isNotEmpty()
                val bound = if (includeEffects) {
                    try {
                        val group = UseCaseGroup.Builder().apply {
                            useCases.forEach { addUseCase(it) }
                            effects.forEach { addEffect(it) }
                        }.build()
                        provider.bindToLifecycle(lifecycleOwner, selector, group)
                    } catch (error: Exception) {
                        Logger.warning(TAG, "Bind with effects failed: ${error.message}")
                        provider.unbindAll()
                        provider.bindToLifecycle(lifecycleOwner, selector, *useCases.toTypedArray())
                    }
                } else {
                    provider.bindToLifecycle(lifecycleOwner, selector, *useCases.toTypedArray())
                }
                this.videoCapture = videoCapture?.takeIf { capture -> useCases.contains(capture) }
                Logger.debug(TAG, "Bound use cases: ${useCases.map { it.javaClass.simpleName }}")
                return bound
            } catch (error: Exception) {
                lastError = error
                Logger.warning(TAG, "Bind attempt failed: ${error.message}")
            }
        }
        throw lastError ?: IllegalStateException("Unable to bind camera")
    }

    private fun releaseEffects() {
        overlayEffect?.clearOnDrawListener()
        overlayEffect?.close()
        overlayEffect = null
        colorProcessor?.release()
        colorProcessor = null
        latestFaces = emptyList()
    }

    private fun applyStillFilter(file: File, type: ColorFilterType): File {
        if (type == ColorFilterType.NONE) return file
        return try {
            val original = BitmapFactory.decodeFile(file.absolutePath) ?: return file
            val filtered = ColorFilters.applyToBitmap(original, type)
            FileOutputStream(file).use { stream ->
                filtered.compress(android.graphics.Bitmap.CompressFormat.JPEG, 95, stream)
            }
            if (filtered !== original) filtered.recycle()
            original.recycle()
            file
        } catch (error: Exception) {
            Logger.error(TAG, "Still filter failed: ${error.message}")
            file
        }
    }

    private fun createFile(baseFolder: File, extension: String): File {
        val name = SimpleDateFormat(Constants.FILE.FILENAME_FORMAT, Locale.US)
            .format(System.currentTimeMillis())
        return File(baseFolder, name + extension)
    }

    private suspend fun awaitProvider(): ProcessCameraProvider = suspendCoroutine { continuation ->
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            try {
                continuation.resume(future.get())
            } catch (error: Exception) {
                continuation.resumeWithException(error)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    private suspend fun awaitExtensions(provider: ProcessCameraProvider): ExtensionsManager =
        suspendCoroutine { continuation ->
            val future = ExtensionsManager.getInstanceAsync(context, provider)
            future.addListener({
                try {
                    continuation.resume(future.get())
                } catch (error: Exception) {
                    continuation.resumeWithException(error)
                }
            }, ContextCompat.getMainExecutor(context))
        }

    companion object {
        private const val TAG = "CameraSession"
    }
}
