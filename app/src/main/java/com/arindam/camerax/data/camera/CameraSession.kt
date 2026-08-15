package com.arindam.camerax.data.camera

import android.Manifest
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.Rect
import android.graphics.RectF
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CaptureResult
import android.hardware.camera2.TotalCaptureResult
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.view.Surface
import androidx.annotation.MainThread
import androidx.annotation.OptIn
import androidx.camera.camera2.interop.Camera2CameraControl
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.camera2.interop.Camera2Interop
import androidx.camera.camera2.interop.CaptureRequestOptions
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.Camera
import androidx.camera.core.CameraEffect
import androidx.camera.core.CameraInfo
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
import com.arindam.camerax.domain.model.ExposureLimits
import com.arindam.camerax.domain.model.ExposurePriority
import com.arindam.camerax.domain.model.FlashMode
import com.arindam.camerax.domain.model.NightScene
import com.arindam.camerax.domain.model.RecordingEvent
import com.arindam.camerax.domain.model.StillFormat
import com.arindam.camerax.domain.model.ZoomInfo
import com.arindam.camerax.domain.repository.CameraRepository
import com.arindam.camerax.util.commons.Constants
import com.arindam.camerax.util.log.Logger
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    private var preview: Preview? = null
    private var imageCapture: ImageCapture? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var imageAnalysis: ImageAnalysis? = null
    private var recording: Recording? = null
    private var overlayEffect: OverlayEffect? = null
    private var colorProcessor: ColorFilterProcessor? = null
    private var faceDetector: FaceDetector? = null
    private var boundPreviewView: PreviewView? = null
    @Volatile private var latestFaces: List<Rect> = emptyList()
    private val _nightScene = MutableStateFlow(NightScene.UNKNOWN)
    override val nightScene: StateFlow<NightScene> = _nightScene.asStateFlow()
    private var stillFormat: StillFormat = StillFormat.JPEG
    private var motionStill: File? = null
    private var motionVideo: File? = null
    private var motionOnSaved: ((File) -> Unit)? = null
    private var motionOnError: ((String) -> Unit)? = null
    private var motionAwaitingVideo = false

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
            manager != null &&
            manager.isExtensionAvailable(baseSelector, config.extension.toExtensionMode())
        val selector = if (useExtension) {
            manager.getExtensionEnabledCameraSelector(baseSelector, config.extension.toExtensionMode())
        } else {
            baseSelector
        }

        val previewBuilder = Preview.Builder().setTargetRotation(rotation)
        attachNightModeMonitor(previewBuilder)
        val preview = previewBuilder.build().also { it.surfaceProvider = previewView.surfaceProvider }
        this.preview = preview

        val captureBuilder = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .setFlashMode(config.flash.toImageCaptureMode())
            .setTargetRotation(rotation)
        val resolved = resolveStillFormat(provider.getCameraInfo(selector))
        stillFormat = resolved.second
        if (resolved.first != ImageCapture.OUTPUT_FORMAT_JPEG) {
            captureBuilder.setOutputFormat(resolved.first)
        }
        val capture = captureBuilder.build()
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
        imageAnalysis = analysis

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
        val limits = exposureLimits()
        return CameraBindResult(
            hasFlash = camera?.cameraInfo?.hasFlashUnit() == true,
            minZoom = zoom?.minZoomRatio ?: 1f,
            maxZoom = zoom?.maxZoomRatio ?: 1f,
            zoomRatio = zoom?.zoomRatio ?: 1f,
            videoAvailable = videoCapture != null,
            supportedExtensions = supportedExtensions(baseSelector),
            stillFormat = stillFormat,
            ultraHdrEnabled = stillFormat != StillFormat.JPEG,
            nightIndicatorSupported = Build.VERSION.SDK_INT >= 36,
            exposureLimits = limits
        )
    }

    override fun capturePhoto(
        outputDirectory: File,
        lens: CameraLens,
        colorFilter: ColorFilterType,
        motionPhoto: Boolean,
        onSaved: (File) -> Unit,
        onError: (String) -> Unit
    ) {
        if (motionPhoto && videoCapture != null) {
            captureMotionPhoto(outputDirectory, lens, colorFilter, onSaved, onError)
            return
        }
        takeStill(outputDirectory, lens, colorFilter, onSaved, onError)
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
        startRecordingService()
        recording = active.start(ContextCompat.getMainExecutor(context)) { event ->
            val domain = event.toDomain() ?: return@start
            if (domain is RecordingEvent.Finalized) {
                stopRecordingService()
                if (motionAwaitingVideo) {
                    motionAwaitingVideo = false
                    if (!domain.success) motionVideo = null
                    finishMotionIfReady()
                }
            }
            onEvent(domain)
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

    override fun setTargetRotation(rotation: Int) {
        preview?.targetRotation = rotation
        imageCapture?.targetRotation = rotation
        videoCapture?.targetRotation = rotation
        imageAnalysis?.targetRotation = rotation
    }

    @OptIn(ExperimentalCamera2Interop::class)
    override fun setExposure(priority: ExposurePriority, iso: Int, shutterNanos: Long) {
        val control = camera?.cameraControl ?: return
        if (Build.VERSION.SDK_INT < 36) return
        val camera2 = Camera2CameraControl.from(control)
        if (priority == ExposurePriority.AUTO) {
            camera2.clearCaptureRequestOptions()
            return
        }
        val builder = CaptureRequestOptions.Builder()
            .setCaptureRequestOption(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AE_MODE_ON)
        when (priority) {
            ExposurePriority.ISO -> {
                builder.setCaptureRequestOption(
                    CaptureRequest.CONTROL_AE_PRIORITY_MODE,
                    CameraMetadata.CONTROL_AE_PRIORITY_MODE_SENSOR_SENSITIVITY_PRIORITY
                )
                builder.setCaptureRequestOption(CaptureRequest.SENSOR_SENSITIVITY, iso)
            }
            ExposurePriority.SHUTTER -> {
                builder.setCaptureRequestOption(
                    CaptureRequest.CONTROL_AE_PRIORITY_MODE,
                    CameraMetadata.CONTROL_AE_PRIORITY_MODE_SENSOR_EXPOSURE_TIME_PRIORITY
                )
                builder.setCaptureRequestOption(CaptureRequest.SENSOR_EXPOSURE_TIME, shutterNanos)
            }
            ExposurePriority.AUTO -> Unit
        }
        camera2.setCaptureRequestOptions(builder.build())
    }

    override fun release() {
        recording?.stop()
        recording = null
        stopRecordingService()
        cameraProvider?.unbindAll()
        releaseEffects()
        faceDetector?.close()
        faceDetector = null
        cameraExecutor.shutdown()
        overlayThread.quitSafely()
        colorProcessor?.release()
        colorProcessor = null
        _nightScene.value = NightScene.UNKNOWN
        clearMotionCapture()
    }

    private fun applyTorch(mode: FlashMode) {
        camera?.cameraControl?.enableTorch(mode == FlashMode.TORCH)
    }

    private fun takeStill(
        outputDirectory: File,
        lens: CameraLens,
        colorFilter: ColorFilterType,
        onSaved: (File) -> Unit,
        onError: (String) -> Unit
    ) {
        val capture = imageCapture ?: return onError("Camera is not ready")
        val extension = if (stillFormat == StillFormat.HEIC_ULTRA_HDR) {
            Constants.FILE.HEIC_EXTENSION
        } else {
            Constants.FILE.PHOTO_EXTENSION
        }
        val photoFile = createFile(outputDirectory, extension)
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
                    val preserveHdr = stillFormat != StillFormat.JPEG
                    val processed = if (preserveHdr) file else applyStillFilter(file, colorFilter)
                    onSaved(processed)
                }

                override fun onError(exception: ImageCaptureException) {
                    Logger.error(TAG, "Photo capture failed: ${exception.message}")
                    onError(exception.message ?: "Photo capture failed")
                }
            }
        )
    }

    private fun captureMotionPhoto(
        outputDirectory: File,
        lens: CameraLens,
        colorFilter: ColorFilterType,
        onSaved: (File) -> Unit,
        onError: (String) -> Unit
    ) {
        motionStill = null
        motionOnSaved = onSaved
        motionOnError = onError
        motionAwaitingVideo = true
        val videoFile = startRecording(
            outputDirectory = outputDirectory,
            muted = true,
            onEvent = {},
            onError = { message ->
                motionAwaitingVideo = false
                motionVideo = null
                Logger.warning(TAG, "Motion video failed: $message")
                finishMotionIfReady()
            }
        )
        if (videoFile == null) {
            motionAwaitingVideo = false
            takeStill(outputDirectory, lens, colorFilter, onSaved, onError)
            return
        }
        motionVideo = videoFile
        takeStill(
            outputDirectory = outputDirectory,
            lens = lens,
            colorFilter = colorFilter,
            onSaved = { file ->
                motionStill = file
                finishMotionIfReady()
            },
            onError = { message ->
                clearMotionCapture()
                onError(message)
            }
        )
        overlayHandler.postDelayed({ stopRecording() }, MOTION_DURATION_MS)
    }

    private fun finishMotionIfReady() {
        val still = motionStill ?: return
        if (motionAwaitingVideo) return
        val video = motionVideo
        val onSaved = motionOnSaved ?: return
        clearMotionCapture()
        if (video != null && video.exists() && video.length() > 0L) {
            try {
                val muxed = File(still.parentFile, still.nameWithoutExtension + "_motion.jpg")
                MotionPhotoMuxer.mux(still, video, muxed)
                still.delete()
                video.delete()
                onSaved(muxed)
                return
            } catch (error: Exception) {
                Logger.error(TAG, "Motion mux failed: ${error.message}")
            }
        }
        onSaved(still)
    }

    private fun clearMotionCapture() {
        motionStill = null
        motionVideo = null
        motionOnSaved = null
        motionOnError = null
        motionAwaitingVideo = false
    }

    @OptIn(ExperimentalCamera2Interop::class)
    private fun attachNightModeMonitor(builder: Preview.Builder) {
        if (Build.VERSION.SDK_INT < 36) return
        Camera2Interop.Extender(builder).setSessionCaptureCallback(
            object : CameraCaptureSession.CaptureCallback() {
                override fun onCaptureCompleted(
                    session: CameraCaptureSession,
                    request: CaptureRequest,
                    result: TotalCaptureResult
                ) {
                    val value = result.get(CaptureResult.EXTENSION_NIGHT_MODE_INDICATOR) ?: return
                    val scene = when (value) {
                        CameraMetadata.EXTENSION_NIGHT_MODE_INDICATOR_ON -> NightScene.RECOMMENDED
                        CameraMetadata.EXTENSION_NIGHT_MODE_INDICATOR_OFF -> NightScene.NOT_RECOMMENDED
                        else -> NightScene.UNKNOWN
                    }
                    if (_nightScene.value != scene) _nightScene.value = scene
                }
            }
        )
    }

    private fun resolveStillFormat(info: CameraInfo): Pair<Int, StillFormat> {
        val supported = runCatching {
            ImageCapture.getImageCaptureCapabilities(info).supportedOutputFormats
        }.getOrDefault(emptySet())
        val heic = heicUltraHdrFormat()
        if (heic != null && heic in supported) return heic to StillFormat.HEIC_ULTRA_HDR
        if (ImageCapture.OUTPUT_FORMAT_JPEG_ULTRA_HDR in supported) {
            return ImageCapture.OUTPUT_FORMAT_JPEG_ULTRA_HDR to StillFormat.JPEG_ULTRA_HDR
        }
        return ImageCapture.OUTPUT_FORMAT_JPEG to StillFormat.JPEG
    }

    private fun heicUltraHdrFormat(): Int? =
        ImageCapture::class.java.fields
            .firstOrNull { it.name.contains("HEIC", ignoreCase = true) }
            ?.let { runCatching { it.getInt(null) }.getOrNull() }

    @OptIn(ExperimentalCamera2Interop::class)
    private fun exposureLimits(): ExposureLimits {
        val bound = camera ?: return ExposureLimits()
        if (Build.VERSION.SDK_INT < 36) return ExposureLimits()
        val info = Camera2CameraInfo.from(bound.cameraInfo)
        val modes = info.getCameraCharacteristic(CameraCharacteristics.CONTROL_AE_AVAILABLE_PRIORITY_MODES)
        val iso = info.getCameraCharacteristic(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE)
        val shutter = info.getCameraCharacteristic(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE)
        val priorities = buildSet {
            add(ExposurePriority.AUTO)
            modes?.forEach { mode ->
                when (mode) {
                    CameraMetadata.CONTROL_AE_PRIORITY_MODE_SENSOR_SENSITIVITY_PRIORITY ->
                        add(ExposurePriority.ISO)
                    CameraMetadata.CONTROL_AE_PRIORITY_MODE_SENSOR_EXPOSURE_TIME_PRIORITY ->
                        add(ExposurePriority.SHUTTER)
                }
            }
        }
        return ExposureLimits(
            isoMin = iso?.lower ?: 50,
            isoMax = iso?.upper ?: 3200,
            shutterMinNanos = shutter?.lower ?: 1_000_000L,
            shutterMaxNanos = shutter?.upper ?: 250_000_000L,
            supportedPriorities = priorities
        )
    }

    private fun startRecordingService() {
        runCatching {
            ContextCompat.startForegroundService(
                context,
                Intent(context, RecordingForegroundService::class.java)
            )
        }.onFailure { error ->
            Logger.warning(TAG, "Unable to start recording service: ${error.message}")
        }
    }

    private fun stopRecordingService() {
        runCatching {
            context.stopService(Intent(context, RecordingForegroundService::class.java))
        }
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
        private const val MOTION_DURATION_MS = 1_500L
    }
}
