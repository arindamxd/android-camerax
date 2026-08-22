package com.arindam.camerax.data.camera

import android.Manifest
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.util.Size
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CaptureResult
import android.hardware.camera2.TotalCaptureResult
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.Surface
import androidx.annotation.MainThread
import androidx.annotation.OptIn
import androidx.camera.camera2.interop.Camera2CameraControl
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.camera2.interop.Camera2Interop
import androidx.camera.camera2.interop.CaptureRequestOptions
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.Camera
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.ConcurrentCamera.SingleCameraConfig
import androidx.camera.core.DynamicRange
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.LowLightBoostState
import androidx.camera.core.MirrorMode
import androidx.camera.core.Preview
import androidx.camera.core.SessionConfig
import androidx.camera.core.UseCase
import androidx.camera.core.UseCaseGroup
import androidx.camera.core.featuregroup.GroupableFeature
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.extensions.ExtensionsManager
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.ExperimentalPersistentRecording
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.HighSpeedVideoSessionConfig
import androidx.camera.video.PendingRecording
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
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import com.arindam.camerax.domain.model.BoundSession
import com.arindam.camerax.domain.model.BoundSessionKind
import com.arindam.camerax.domain.model.CameraBindConfig
import com.arindam.camerax.domain.model.CameraBindResult
import com.arindam.camerax.domain.model.CameraExtension
import com.arindam.camerax.domain.model.CameraHost
import com.arindam.camerax.domain.model.CameraLens
import com.arindam.camerax.domain.model.CaptureAspect
import com.arindam.camerax.domain.model.EffectMode
import com.arindam.camerax.domain.model.ExposureLimits
import com.arindam.camerax.domain.model.ExposurePriority
import com.arindam.camerax.domain.model.FlashMode
import com.arindam.camerax.domain.model.LowLightBoost
import com.arindam.camerax.domain.model.NightScene
import com.arindam.camerax.domain.model.PhysicalZoom
import com.arindam.camerax.domain.model.RecordingEvent
import com.arindam.camerax.domain.model.StillFormat
import com.arindam.camerax.domain.model.VideoHdrRange
import com.arindam.camerax.domain.model.ZoomInfo
import com.arindam.camerax.domain.repository.CameraRepository
import com.arindam.camerax.util.commons.Constants
import com.arindam.camerax.util.log.Logger
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
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
 * CameraX implementation of [com.arindam.camerax.domain.repository.CameraRepository].
 * Bind, capture, record, zoom, AE, extensions, Dual, and slo-mo live here. UI must not
 * call this class; go through use cases from [com.arindam.camerax.di.AppContainer].
 */
class CameraSession(private val context: Context) : CameraRepository {

    private val cameraExecutor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    private var cameraProvider: ProcessCameraProvider? = null
    private var extensionsManager: ExtensionsManager? = null
    private var camera: Camera? = null
    private var preview: Preview? = null
    private var imageCapture: ImageCapture? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var recording: Recording? = null
    private var highSpeedSession = false
    private var imageAnalysis: ImageAnalysis? = null
    private var colorAnalyzer: ColorEffectAnalyzer? = null
    private val analysisExecutor = Executors.newSingleThreadExecutor()
    private val _effectFrame = MutableStateFlow<Bitmap?>(null)
    override val effectFrame: StateFlow<Bitmap?> = _effectFrame.asStateFlow()
    private var boundPreviewView: PreviewView? = null
    private var boundPipPreviewView: PreviewView? = null
    private var frontMirrorEnabled = true
    private var pipPreview: Preview? = null
    private val _nightScene = MutableStateFlow(NightScene.UNKNOWN)
    override val nightScene: StateFlow<NightScene> = _nightScene.asStateFlow()
    private val _lowLightBoost = MutableStateFlow(LowLightBoost.OFF)
    override val lowLightBoost: StateFlow<LowLightBoost> = _lowLightBoost.asStateFlow()
    private val _recordingEvents = MutableSharedFlow<RecordingEvent>(extraBufferCapacity = 16)
    override val recordingEvents: SharedFlow<RecordingEvent> = _recordingEvents.asSharedFlow()
    private var lowLightBoostLiveData: LiveData<Int>? = null
    private val lowLightBoostObserver = Observer<Int> { value ->
        _lowLightBoost.value = when (value) {
            LowLightBoostState.ACTIVE -> LowLightBoost.ACTIVE
            LowLightBoostState.INACTIVE -> LowLightBoost.INACTIVE
            else -> LowLightBoost.OFF
        }
    }
    private var flashMode: FlashMode = FlashMode.OFF
    private var lowLightBoostRequested = true
    private var stillFormat: StillFormat = StillFormat.JPEG
    private var motionStill: File? = null
    private var motionVideo: File? = null
    private var motionOnSaved: ((File) -> Unit)? = null
    private var motionOnError: ((String) -> Unit)? = null
    private var motionAwaitingVideo = false
    private var previewBoosted = false
    private var userExposureIndex: Int? = null
    private var fps60Active = false
    private var lastBoundKind = BoundSessionKind.STANDARD
    private var lastStillsOnlyFallback = false
    private var lastBoundExtension = CameraExtension.NONE
    private var lastRawFullSensor = false

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
    @OptIn(ExperimentalCamera2Interop::class)
    override suspend fun bind(host: CameraHost, config: CameraBindConfig): CameraBindResult {
        val previewHost = host as? PreviewViewHost
            ?: throw IllegalArgumentException("Unsupported camera host")
        val lifecycleOwner = previewHost.lifecycleOwner
        val previewView = previewHost.previewView
        boundPreviewView = previewView
        boundPipPreviewView = previewHost.pipPreviewView
        initialize()
        val provider = cameraProvider ?: throw IllegalStateException("Camera provider missing")
        if (config.retainRecording && canRetainRecording()) {
            return rebindWhileRecording(previewHost, config)
        }
        provider.unbindAll()
        stopColorAnalysis()
        highSpeedSession = false
        imageCapture = null
        videoCapture = null
        preview = null
        pipPreview = null

        previewView.implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        previewView.scaleType = if (config.slowMotion || config.captureAspect == CaptureAspect.FULL) {
            PreviewView.ScaleType.FILL_CENTER
        } else {
            PreviewView.ScaleType.FIT_CENTER
        }
        val rotation = previewView.display?.rotation ?: Surface.ROTATION_0

        if (config.concurrent) {
            if (!isDualCameraSupported(context, provider) || previewHost.pipPreviewView == null) {
                throw IllegalStateException("Dual camera is not supported on this device")
            }
            return bindConcurrent(provider, previewHost, config, rotation)
        }

        val requestedSelector = selectorFor(config)
        val requestedInfo = runCatching { provider.getCameraInfo(requestedSelector) }.getOrNull()
        val baseSelector = if (requestedInfo != null) {
            requestedSelector
        } else {
            if (!config.cameraId.isNullOrBlank()) {
                Logger.warning(TAG, "Falling back from camera ${config.cameraId} to default lens")
            }
            config.lens.toSelector()
        }
        val manager = extensionsManager
        val stillInfo = requestedInfo
            ?: runCatching { provider.getCameraInfo(baseSelector) }.getOrNull()
        val useRaw = config.rawCapture && !config.slowMotion &&
            stillInfo != null && stillInfo.supportsRawJpeg()
        val useExtension = !useRaw &&
            !config.slowMotion &&
            config.extension != CameraExtension.NONE &&
            config.cameraId == null &&
            manager != null &&
            manager.isExtensionAvailable(baseSelector, config.extension.toExtensionMode())
        val selector = if (useExtension) {
            manager.getExtensionEnabledCameraSelector(baseSelector, config.extension.toExtensionMode())
        } else {
            baseSelector
        }

        if (config.slowMotion) {
            stillFormat = StillFormat.JPEG
            val bound = bindHighSpeed(provider, lifecycleOwner, selector, previewView, rotation, config)
            if (bound != null) {
                camera = bound.camera
                highSpeedSession = true
                return finishBind(
                    config = config,
                    slowMotionSupported = true,
                    slowMotionFps = bound.fps
                )
            }
            Logger.warning(TAG, "High-speed bind failed; falling back to standard session")
        }

        val previewBuilder = Preview.Builder().setTargetRotation(rotation)
        config.captureAspect.toResolutionSelector()?.let { previewBuilder.setResolutionSelector(it) }
        val cameraInfo = stillInfo
        val wantStab = config.videoStabilization && !useExtension
        val previewStab = wantStab && isPreviewStabilizationSupported(cameraInfo)
        val videoStab = wantStab && isVideoStabilizationSupported(cameraInfo)
        previewBuilder.setPreviewStabilizationEnabled(previewStab)
        attachNightModeMonitor(previewBuilder)
        val liveEffects = config.liveEffects && !useRaw
        val preview = previewBuilder.build().also { it.surfaceProvider = previewView.surfaceProvider }
        this.preview = preview

        val captureBuilder = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .setFlashMode(config.flash.toImageCaptureMode())
            .setTargetRotation(rotation)
        val useFullSensor = useRaw &&
            config.rawFullSensor &&
            stillInfo.supportsFullSensorRaw(context) == true
        lastRawFullSensor = useFullSensor
        lastBoundExtension = if (useExtension) config.extension else CameraExtension.NONE
        lastBoundKind = BoundSessionKind.STANDARD
        lastStillsOnlyFallback = false
        if (useFullSensor) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Camera2Interop.Extender(captureBuilder).setCaptureRequestOption(
                    CaptureRequest.SENSOR_PIXEL_MODE,
                    CameraMetadata.SENSOR_PIXEL_MODE_MAXIMUM_RESOLUTION
                )
            }
            captureBuilder.setResolutionSelector(
                ResolutionSelector.Builder()
                    .setResolutionStrategy(ResolutionStrategy.HIGHEST_AVAILABLE_STRATEGY)
                    .build()
            )
        } else {
            config.captureAspect.toResolutionSelector()?.let { captureBuilder.setResolutionSelector(it) }
        }
        val resolved = resolveStillOutput(
            info = runCatching { provider.getCameraInfo(selector) }.getOrNull()
                ?: stillInfo
                ?: provider.getCameraInfo(baseSelector),
            ultraHdr = config.ultraHdr && !useRaw && !liveEffects,
            rawCapture = useRaw
        )
        stillFormat = resolved.second
        if (resolved.first != ImageCapture.OUTPUT_FORMAT_JPEG) {
            captureBuilder.setOutputFormat(resolved.first)
        }
        val capture = captureBuilder.build()
        imageCapture = capture

        val recorder = Recorder.Builder()
            .setQualitySelector(config.videoQuality.toQualitySelector())
            .build()
        val videoRange = resolveVideoHdrRange(cameraInfo, config)
        val video = buildVideoCapture(recorder, videoStab, videoRange, config.frontMirror)
        videoCapture = video

        val includeVideo = !useExtension && !liveEffects
        val analysis = if (liveEffects) {
            buildColorAnalysis(rotation, config.effect)
        } else {
            stopColorAnalysis()
            null
        }
        val wantFps60 = config.videoFps60 && includeVideo && !useExtension

        camera = bindWithFallback(
            provider = provider,
            lifecycleOwner = lifecycleOwner,
            selector = selector,
            preview = preview,
            imageCapture = capture,
            videoCapture = if (includeVideo) video else null,
            imageAnalysis = analysis,
            fps60 = wantFps60
        )
        if (!includeVideo) videoCapture = null

        return finishBind(
            config = config,
            slowMotionSupported = if (config.slowMotion) {
                false
            } else {
                isHighSpeedSupported(camera?.cameraInfo)
            },
            slowMotionFps = 0
        )
    }

    override suspend fun capturePhoto(
        outputDirectory: File,
        lens: CameraLens,
        effect: EffectMode,
        motionPhoto: Boolean
    ): Result<File> = suspendCancellableCoroutine { continuation ->
        val onSaved: (File) -> Unit = { file ->
            if (continuation.isActive) continuation.resume(Result.success(file))
        }
        val onError: (String) -> Unit = { message ->
            if (continuation.isActive) {
                continuation.resume(Result.failure(IllegalStateException(message)))
            }
        }
        if (motionPhoto && stillFormat != StillFormat.RAW_JPEG &&
            stillFormat != StillFormat.JPEG_ULTRA_HDR &&
            stillFormat != StillFormat.HEIC_ULTRA_HDR &&
            videoCapture != null
        ) {
            captureMotionPhoto(outputDirectory, lens, effect, onSaved, onError)
        } else {
            takeStill(outputDirectory, lens, effect, onSaved, onError)
        }
    }

    override fun startRecording(
        outputDirectory: File,
        muted: Boolean,
        persistent: Boolean
    ): Result<File> = beginRecording(
        outputDirectory = outputDirectory,
        muted = muted,
        persistent = persistent,
        emitEvents = true
    )

    private fun beginRecording(
        outputDirectory: File,
        muted: Boolean,
        persistent: Boolean,
        emitEvents: Boolean
    ): Result<File> {
        val capture = videoCapture ?: return Result.failure(
            IllegalStateException("Video capture is not available with this effect")
        )
        recording?.stop()
        val videoFile = createFile(outputDirectory, Constants.FILE.VIDEO_EXTENSION)
        val output = FileOutputOptions.Builder(videoFile).build()
        val pending = persistentPending(
            capture.output.prepareRecording(context, output),
            persist = persistent
        )
        val withAudio = shouldEnableRecordingAudio(
            highSpeedSession = highSpeedSession,
            recordAudioGranted = PermissionChecker.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PermissionChecker.PERMISSION_GRANTED
        )
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
            if (emitEvents) _recordingEvents.tryEmit(domain)
        }
        if (withAudio && muted) {
            recording?.mute(true)
        }
        return Result.success(videoFile)
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
        flashMode = mode
        imageCapture?.flashMode = mode.toImageCaptureMode()
        applyTorch(mode)
        applyLowLightBoost()
    }

    override fun setLowLightBoost(enabled: Boolean) {
        lowLightBoostRequested = enabled
        applyLowLightBoost()
    }

    override fun setZoomRatio(ratio: Float): ZoomInfo? {
        val state = camera?.cameraInfo?.zoomState?.value ?: return null
        val clamped = ratio.coerceIn(state.minZoomRatio, state.maxZoomRatio)
        camera?.cameraControl?.setZoomRatio(clamped)
        return ZoomInfo(
            ratio = clamped,
            min = state.minZoomRatio,
            max = state.maxZoomRatio
        )
    }

    override fun tapToFocus(x: Float, y: Float) {
        val previewView = boundPreviewView ?: return
        val mappedX = if (previewView.scaleX < 0f) {
            previewView.width - x
        } else {
            x
        }
        val factory = previewView.meteringPointFactory
        val point = factory.createPoint(mappedX, y)
        val action = FocusMeteringAction.Builder(
            point,
            FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE
        ).setAutoCancelDuration(3, TimeUnit.SECONDS).build()
        camera?.cameraControl?.startFocusAndMetering(action)
    }

    override fun setEffect(type: EffectMode) {
        colorAnalyzer?.effect = type
    }

    override fun setTargetRotation(rotation: Int) {
        preview?.targetRotation = rotation
        imageCapture?.targetRotation = rotation
        videoCapture?.targetRotation = rotation
        imageAnalysis?.targetRotation = rotation
        pipPreview?.targetRotation = rotation
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

    override fun setExposureCompensation(index: Int) {
        val exposure = camera?.cameraInfo?.exposureState ?: return
        if (!exposure.isExposureCompensationSupported) return
        userExposureIndex = index
        val range = exposure.exposureCompensationRange
        camera?.cameraControl?.setExposureCompensationIndex(index.coerceIn(range.lower, range.upper))
    }

    override fun unbind() {
        recording?.stop()
        recording = null
        stopRecordingService()
        camera?.cameraControl?.enableTorch(false)
        cameraProvider?.unbindAll()
        stopColorAnalysis()
        camera = null
        preview = null
        pipPreview = null
        imageCapture = null
        videoCapture = null
        boundPreviewView = null
        boundPipPreviewView = null
        highSpeedSession = false
        fps60Active = false
        previewBoosted = false
        userExposureIndex = null
        stopWatchingLowLightBoost()
        _lowLightBoost.value = LowLightBoost.OFF
        _nightScene.value = NightScene.UNKNOWN
        clearMotionCapture()
    }

    override fun release() {
        unbind()
        // Keep capture/analysis executors alive: AppContainer owns this session for the process.
    }

    @OptIn(ExperimentalCamera2Interop::class)
    private fun selectorFor(config: CameraBindConfig): CameraSelector {
        val id = config.cameraId
        if (id.isNullOrBlank()) return config.lens.toSelector()
        return CameraSelector.Builder()
            .addCameraFilter { infos ->
                val match = infos.filter { Camera2CameraInfo.from(it).cameraId == id }
                if (match.isEmpty()) {
                    Logger.warning(TAG, "Camera id $id is not independently bindable")
                }
                match
            }
            .build()
    }

    @OptIn(ExperimentalCamera2Interop::class)
    private fun discoverPhysicalZooms(facing: Int): List<PhysicalZoom> {
        val provider = cameraProvider ?: return emptyList()
        val infos = provider.availableCameraInfos.filter { it.lensFacing == facing }
        if (infos.size < 2) return emptyList()
        val labeled = infos.mapNotNull { info ->
            val camera2 = Camera2CameraInfo.from(info)
            val focals = camera2.getCameraCharacteristic(
                CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS
            ) ?: return@mapNotNull null
            val sensor = camera2.getCameraCharacteristic(
                CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE
            )
            val minFocal = focals.minOrNull() ?: return@mapNotNull null
            val equivalent = if (sensor != null && sensor.width > 0f) {
                36f * minFocal / sensor.width
            } else {
                minFocal * 7f
            }
            val label = when {
                equivalent < 20f -> 0.5f
                equivalent < 40f -> 1f
                equivalent < 70f -> 2f
                else -> 5f
            }
            PhysicalZoom(cameraId = camera2.cameraId, label = label) to equivalent
        }
        return labeled
            .groupBy { it.first.label }
            .map { (_, group) ->
                val target = when (group.first().first.label) {
                    0.5f -> 16f
                    1f -> 28f
                    2f -> 55f
                    else -> 100f
                }
                group.minBy { kotlin.math.abs(it.second - target) }.first
            }
            .sortedBy { it.label }
    }

    private fun applyLowLightPreviewBoost(scene: NightScene) {
        if (userExposureIndex != null) return
        if (lowLightBoostRequested && camera?.cameraInfo?.isLowLightBoostSupported == true) return
        val exposure = camera?.cameraInfo?.exposureState ?: return
        if (!exposure.isExposureCompensationSupported) return
        val range = exposure.exposureCompensationRange
        if (scene == NightScene.RECOMMENDED && !previewBoosted && range.upper > 0) {
            camera?.cameraControl?.setExposureCompensationIndex(
                2.coerceAtMost(range.upper)
            )
            previewBoosted = true
        } else if (scene != NightScene.RECOMMENDED && previewBoosted) {
            camera?.cameraControl?.setExposureCompensationIndex(0)
            previewBoosted = false
        }
    }

    private fun applyTorch(mode: FlashMode) {
        camera?.cameraControl?.enableTorch(mode == FlashMode.TORCH)
    }

    private fun takeStill(
        outputDirectory: File,
        lens: CameraLens,
        effect: EffectMode,
        onSaved: (File) -> Unit,
        onError: (String) -> Unit
    ) {
        val capture = imageCapture ?: return onError("Camera is not ready")
        if (stillFormat == StillFormat.RAW_JPEG) {
            takeRawJpeg(capture, outputDirectory, lens, onSaved, onError)
            return
        }
        val extension = if (stillFormat == StillFormat.HEIC_ULTRA_HDR) {
            Constants.FILE.HEIC_EXTENSION
        } else {
            Constants.FILE.PHOTO_EXTENSION
        }
        val photoFile = createFile(outputDirectory, extension)
        val mirrorOutput = shouldMirrorFrontOutput(lens)
        val metadata = ImageCapture.Metadata().apply {
            isReversedHorizontal = mirrorOutput && stillFormat != StillFormat.JPEG
        }
        val options = ImageCapture.OutputFileOptions.Builder(photoFile)
            .setMetadata(metadata)
            .build()
        capture.takePicture(
            options,
            cameraExecutor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val file = output.savedUri?.toFile() ?: photoFile
                    val preserveHdr = stillFormat != StillFormat.JPEG
                    val processed = if (preserveHdr) {
                        file
                    } else {
                        applyStillOutput(file, effect, mirrorOutput)
                    }
                    onSaved(processed)
                }

                override fun onError(exception: ImageCaptureException) {
                    Logger.error(TAG, "Photo capture failed: ${exception.message}")
                    onError(exception.message ?: "Photo capture failed")
                }
            }
        )
    }

    private fun takeRawJpeg(
        capture: ImageCapture,
        outputDirectory: File,
        lens: CameraLens,
        onSaved: (File) -> Unit,
        onError: (String) -> Unit
    ) {
        val stamp = SimpleDateFormat(Constants.FILE.FILENAME_FORMAT, Locale.US)
            .format(System.currentTimeMillis())
        val jpegFile = File(outputDirectory, stamp + Constants.FILE.PHOTO_EXTENSION)
        val dngFile = File(outputDirectory, stamp + Constants.FILE.DNG_EXTENSION)
        val metadata = ImageCapture.Metadata().apply {
            isReversedHorizontal = shouldMirrorFrontOutput(lens)
        }
        val jpegOptions = ImageCapture.OutputFileOptions.Builder(jpegFile)
            .setMetadata(metadata)
            .build()
        val dngOptions = ImageCapture.OutputFileOptions.Builder(dngFile)
            .setMetadata(metadata)
            .build()
        var pendingJpeg: File? = null
        var pendingDng: File? = null
        capture.takePicture(
            dngOptions,
            jpegOptions,
            cameraExecutor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val file = output.savedUri?.toFile()
                    when (output.imageFormat) {
                        ImageFormat.JPEG -> pendingJpeg = file ?: jpegFile
                        else -> pendingDng = file ?: dngFile
                    }
                    val jpeg = pendingJpeg
                    if (jpeg != null && pendingDng != null) onSaved(jpeg)
                }

                override fun onError(exception: ImageCaptureException) {
                    Logger.error(TAG, "RAW capture failed: ${exception.message}")
                    onError(exception.message ?: "RAW capture failed")
                }
            }
        )
    }

    private fun captureMotionPhoto(
        outputDirectory: File,
        lens: CameraLens,
        effect: EffectMode,
        onSaved: (File) -> Unit,
        onError: (String) -> Unit
    ) {
        motionStill = null
        motionOnSaved = onSaved
        motionOnError = onError
        motionAwaitingVideo = true
        val videoFile = beginRecording(
            outputDirectory = outputDirectory,
            muted = true,
            persistent = false,
            emitEvents = false
        ).getOrElse { error ->
            motionAwaitingVideo = false
            motionVideo = null
            Logger.warning(TAG, "Motion video failed: ${error.message}")
            finishMotionIfReady()
            null
        }
        if (videoFile == null) {
            motionAwaitingVideo = false
            takeStill(outputDirectory, lens, effect, onSaved, onError)
            return
        }
        motionVideo = videoFile
        takeStill(
            outputDirectory = outputDirectory,
            lens = lens,
            effect = effect,
            onSaved = { file ->
                motionStill = file
                finishMotionIfReady()
            },
            onError = { message ->
                clearMotionCapture()
                onError(message)
            }
        )
        mainHandler.postDelayed({ stopRecording() }, MOTION_DURATION_MS)
    }

    private fun finishMotionIfReady() {
        val still = motionStill ?: return
        if (motionAwaitingVideo) return
        val video = motionVideo
        val onSaved = motionOnSaved ?: return
        clearMotionCapture()
        cameraExecutor.execute {
            val saved = if (video != null && video.exists() && video.length() > 0L) {
                runCatching {
                    val muxed = File(still.parentFile, still.nameWithoutExtension + "_motion.jpg")
                    MotionPhotoMuxer.mux(still, video, muxed)
                    still.delete()
                    video.delete()
                    muxed
                }.getOrElse { error ->
                    Logger.error(TAG, "Motion mux failed: ${error.message}")
                    still
                }
            } else {
                still
            }
            mainHandler.post { onSaved(saved) }
        }
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
                    applyLowLightPreviewBoost(scene)
                }
            }
        )
    }

    @OptIn(ExperimentalCamera2Interop::class)
    private fun exposureLimits(): ExposureLimits {
        val bound = camera ?: return ExposureLimits()
        val exposure = bound.cameraInfo.exposureState
        val evSupported = exposure.isExposureCompensationSupported
        val evRange = exposure.exposureCompensationRange
        val evMin = if (evSupported) evRange.lower else 0
        val evMax = if (evSupported) evRange.upper else 0
        val evStep = if (evSupported) exposure.exposureCompensationStep.toFloat() else 0f
        if (Build.VERSION.SDK_INT < 36) {
            return ExposureLimits(
                evSupported = evSupported,
                evMin = evMin,
                evMax = evMax,
                evStep = evStep
            )
        }
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
            supportedPriorities = priorities,
            evSupported = evSupported,
            evMin = evMin,
            evMax = evMax,
            evStep = evStep
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

    private fun buildColorAnalysis(rotation: Int, initial: EffectMode): ImageAnalysis {
        val analyzer = ColorEffectAnalyzer { bitmap -> _effectFrame.value = bitmap }.also {
            it.effect = initial
            colorAnalyzer = it
        }
        return ImageAnalysis.Builder()
            .setTargetRotation(rotation)
            .setResolutionSelector(
                ResolutionSelector.Builder()
                    .setResolutionStrategy(
                        ResolutionStrategy(
                            Size(1280, 720),
                            ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
                        )
                    )
                    .build()
            )
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also { analysis ->
                analysis.setAnalyzer(analysisExecutor, analyzer)
                imageAnalysis = analysis
            }
    }

    private fun stopColorAnalysis() {
        imageAnalysis?.clearAnalyzer()
        imageAnalysis = null
        colorAnalyzer = null
        _effectFrame.value = null
    }

    private fun canRetainRecording(): Boolean =
        recording != null && videoCapture != null && preview != null && !highSpeedSession

    @OptIn(ExperimentalPersistentRecording::class)
    private fun persistentPending(pending: PendingRecording, persist: Boolean): PendingRecording {
        if (!persist || highSpeedSession || motionAwaitingVideo) return pending
        return pending.asPersistentRecording()
    }

    private fun rebindWhileRecording(
        host: PreviewViewHost,
        config: CameraBindConfig
    ): CameraBindResult {
        val lifecycleOwner = host.lifecycleOwner
        val previewView = host.previewView
        boundPreviewView = previewView
        boundPipPreviewView = host.pipPreviewView
        val provider = cameraProvider ?: throw IllegalStateException("Camera provider missing")
        val previewUseCase = preview ?: throw IllegalStateException("Preview missing")
        val video = videoCapture ?: throw IllegalStateException("Video capture missing")
        previewUseCase.surfaceProvider = previewView.surfaceProvider
        val selector = selectorFor(config)
        provider.unbindAll()
        camera = try {
            provider.bindToLifecycle(lifecycleOwner, selector, previewUseCase, video)
        } catch (error: Exception) {
            Logger.warning(TAG, "Flip while recording failed: ${error.message}")
            throw error
        }
        return finishBind(
            config = config,
            slowMotionSupported = isHighSpeedSupported(camera?.cameraInfo),
            slowMotionFps = 0
        )
    }

    private fun bindWithFallback(
        provider: ProcessCameraProvider,
        lifecycleOwner: LifecycleOwner,
        selector: CameraSelector,
        preview: Preview?,
        imageCapture: ImageCapture,
        videoCapture: VideoCapture<Recorder>?,
        imageAnalysis: ImageAnalysis?,
        fps60: Boolean = false
    ): Camera {
        val attempts = mutableListOf<List<UseCase>>()
        if (imageAnalysis != null) {
            attempts += listOfNotNull(preview, imageCapture, imageAnalysis)
            attempts += listOf(imageCapture, imageAnalysis)
        } else {
            attempts += listOfNotNull(preview, imageCapture, videoCapture)
            attempts += listOfNotNull(preview, imageCapture)
        }
        var lastError: Exception? = null
        for (useCases in attempts) {
            try {
                provider.unbindAll()
                val bound = bindSession(
                    provider = provider,
                    lifecycleOwner = lifecycleOwner,
                    selector = selector,
                    useCases = useCases,
                    fps60 = fps60 && videoCapture != null && useCases.contains(videoCapture)
                )
                this.videoCapture = videoCapture?.takeIf { capture -> useCases.contains(capture) }
                this.imageCapture = imageCapture.takeIf { capture -> useCases.contains(capture) }
                this.preview = preview?.takeIf { useCases.contains(it) }
                lastStillsOnlyFallback = videoCapture != null &&
                    !useCases.contains(videoCapture)
                if (imageAnalysis == null || !useCases.contains(imageAnalysis)) {
                    stopColorAnalysis()
                }
                Logger.debug(TAG, "Bound use cases: ${useCases.map { it.javaClass.simpleName }}")
                return bound
            } catch (error: Exception) {
                lastError = error
                Logger.warning(TAG, "Bind attempt failed: ${error.message}")
            }
        }
        throw lastError ?: IllegalStateException("Unable to bind camera")
    }

    private fun bindSession(
        provider: ProcessCameraProvider,
        lifecycleOwner: LifecycleOwner,
        selector: CameraSelector,
        useCases: List<UseCase>,
        fps60: Boolean
    ): Camera {
        if (fps60) {
            try {
                val builder = SessionConfig.Builder(*useCases.toTypedArray())
                    .setRequiredFeatureGroup(GroupableFeature.FPS_60)
                val bound = provider.bindToLifecycle(lifecycleOwner, selector, builder.build())
                fps60Active = true
                return bound
            } catch (error: Exception) {
                Logger.warning(TAG, "60 fps feature group bind failed: ${error.message}")
            }
        }
        fps60Active = false
        return provider.bindToLifecycle(lifecycleOwner, selector, *useCases.toTypedArray())
    }

    private fun bindConcurrent(
        provider: ProcessCameraProvider,
        host: PreviewViewHost,
        config: CameraBindConfig,
        rotation: Int
    ): CameraBindResult {
        val pipView = host.pipPreviewView ?: throw IllegalStateException("Dual preview missing")
        val group = provider.concurrentFrontBackGroup()
            ?: throw IllegalStateException("Dual camera is not supported on this device")
        pipView.implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        pipView.scaleType = PreviewView.ScaleType.FILL_CENTER
        val streams = concurrentStreamSelector()
        val primary = Preview.Builder()
            .setTargetRotation(rotation)
            .setResolutionSelector(streams)
            .build()
            .also { it.surfaceProvider = host.previewView.surfaceProvider }
        val secondary = Preview.Builder()
            .setTargetRotation(rotation)
            .setResolutionSelector(streams)
            .build()
            .also { it.surfaceProvider = pipView.surfaceProvider }
        preview = primary
        pipPreview = secondary
        videoCapture = null
        val capture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .setFlashMode(config.flash.toImageCaptureMode())
            .setTargetRotation(rotation)
            .build()
        val bindPair: (Boolean) -> androidx.camera.core.ConcurrentCamera = { includeStill ->
            provider.bindToLifecycle(
                group.map { info ->
                    val useCases = UseCaseGroup.Builder().apply {
                        if (info.lensFacing == CameraSelector.LENS_FACING_BACK) {
                            addUseCase(primary)
                            if (includeStill) addUseCase(capture)
                        } else {
                            addUseCase(secondary)
                        }
                    }.build()
                    SingleCameraConfig(info.toConcurrentSelector(), useCases, host.lifecycleOwner)
                }
            )
        }
        val concurrent = try {
            imageCapture = capture
            bindPair(true)
        } catch (error: Exception) {
            Logger.warning(TAG, "Dual still bind failed, preview only: ${error.message}")
            imageCapture = null
            provider.unbindAll()
            bindPair(false)
        }
        camera = concurrent.cameras.firstOrNull { bound ->
            bound.cameraInfo.lensFacing == CameraSelector.LENS_FACING_BACK
        } ?: concurrent.cameras.first()
        fps60Active = false
        stillFormat = StillFormat.JPEG
        lastBoundKind = BoundSessionKind.CONCURRENT
        lastBoundExtension = CameraExtension.NONE
        lastRawFullSensor = false
        lastStillsOnlyFallback = imageCapture == null
        Logger.debug(
            TAG,
            "Bound Dual PreviewViews: ${concurrent.cameras.map { bound -> bound.cameraInfo.lensFacing }}"
        )
        return finishBind(
            config = config,
            slowMotionSupported = isHighSpeedSupported(camera?.cameraInfo),
            slowMotionFps = 0
        )
    }

    @OptIn(ExperimentalCamera2Interop::class)
    private fun CameraInfo.toConcurrentSelector(): CameraSelector {
        val id = Camera2CameraInfo.from(this).cameraId
        val facing = lensFacing
        val builder = CameraSelector.Builder().addCameraFilter { infos ->
            infos.filter { info -> Camera2CameraInfo.from(info).cameraId == id }
        }
        if (facing != CameraSelector.LENS_FACING_UNKNOWN) {
            builder.requireLensFacing(facing)
        }
        return builder.build()
    }

    private fun concurrentStreamSelector(): ResolutionSelector =
        ResolutionSelector.Builder()
            .setResolutionStrategy(
                ResolutionStrategy(
                    Size(1280, 720),
                    ResolutionStrategy.FALLBACK_RULE_CLOSEST_LOWER_THEN_HIGHER
                )
            )
            .build()

    private fun bindHighSpeed(
        provider: ProcessCameraProvider,
        lifecycleOwner: LifecycleOwner,
        selector: CameraSelector,
        previewView: PreviewView,
        rotation: Int,
        config: CameraBindConfig
    ): HighSpeedBind? {
        val cameraInfo = runCatching { provider.getCameraInfo(selector) }.getOrNull() ?: return null
        val capabilities = Recorder.getHighSpeedVideoCapabilities(cameraInfo) ?: return null
        val supported = capabilities.getSupportedQualities(DynamicRange.SDR)
        if (supported.isEmpty()) return null
        val requested = config.slowMotionQuality.toQuality()
        val quality = (listOf(requested) + supported).firstOrNull { it in supported } ?: return null
        val previewUseCase = Preview.Builder()
            .setTargetRotation(rotation)
            .build()
            .also { it.surfaceProvider = previewView.surfaceProvider }
        val recorder = Recorder.Builder()
            .setQualitySelector(QualitySelector.from(quality))
            .build()
        val video = VideoCapture.Builder(recorder)
            .setMirrorMode(videoMirrorMode(config.frontMirror))
            .build()
        val probe = HighSpeedVideoSessionConfig(video, previewUseCase)
        val ranges = cameraInfo.getSupportedFrameRateRanges(probe).highSpeedFrameRateRanges()
        if (ranges.isEmpty()) return null
        val requestedFps = config.slowMotionRate.fps
        val fpsRange = if (requestedFps > 0) {
            ranges.firstOrNull { it.upper == requestedFps }
                ?: ranges.maxByOrNull { it.upper }
        } else {
            ranges.maxByOrNull { it.upper }
        } ?: return null
        val session = HighSpeedVideoSessionConfig(
            videoCapture = video,
            preview = previewUseCase,
            frameRateRange = fpsRange,
            isSlowMotionEnabled = true
        )
        return try {
            provider.unbindAll()
            val bound = provider.bindToLifecycle(lifecycleOwner, selector, session)
            this.preview = previewUseCase
            this.videoCapture = video
            this.imageCapture = null
            lastBoundKind = BoundSessionKind.HIGH_SPEED
            lastBoundExtension = CameraExtension.NONE
            lastRawFullSensor = false
            lastStillsOnlyFallback = false
            HighSpeedBind(camera = bound, fps = fpsRange.upper)
        } catch (error: Exception) {
            Logger.warning(TAG, "High-speed session bind failed: ${error.message}")
            null
        }
    }

    private fun isHighSpeedSupported(info: CameraInfo?): Boolean {
        if (info == null) return false
        return info.cachedSlowMotion()?.available == true
    }

    private fun resolveVideoHdrRange(info: CameraInfo?, config: CameraBindConfig): DynamicRange {
        if (config.slowMotion || info == null) return DynamicRange.SDR
        val requested = config.videoHdrRange.toDynamicRange()
        val supported = runCatching {
            Recorder.getVideoCapabilities(info).supportedDynamicRanges
        }.getOrDefault(emptySet())
        return if (requested in supported) requested else DynamicRange.SDR
    }

    private fun isPreviewStabilizationSupported(info: CameraInfo?): Boolean {
        if (info == null) return false
        return runCatching {
            Preview.getPreviewCapabilities(info).isStabilizationSupported
        }.getOrDefault(false)
    }

    private fun isVideoStabilizationSupported(info: CameraInfo?): Boolean {
        if (info == null) return false
        return runCatching {
            Recorder.getVideoCapabilities(info).isStabilizationSupported
        }.getOrDefault(false)
    }

    private fun buildVideoCapture(
        recorder: Recorder,
        stabilize: Boolean,
        range: DynamicRange = DynamicRange.SDR,
        frontMirror: Boolean
    ): VideoCapture<Recorder> = VideoCapture.Builder(recorder)
        .setVideoStabilizationEnabled(stabilize)
        .setDynamicRange(range)
        .setMirrorMode(videoMirrorMode(frontMirror))
        .build()

    private fun videoMirrorMode(frontMirror: Boolean): Int =
        if (frontMirror) MirrorMode.MIRROR_MODE_ON_FRONT_ONLY else MirrorMode.MIRROR_MODE_OFF

    private fun shouldMirrorFrontOutput(lens: CameraLens): Boolean =
        frontMirrorEnabled && lens == CameraLens.FRONT

    private fun applyViewfinderMirror(config: CameraBindConfig) {
        applyViewfinderMirror(
            boundPreviewView,
            frontCamera = config.lens == CameraLens.FRONT && !config.concurrent,
            frontMirror = config.frontMirror
        )
        if (config.concurrent) {
            applyViewfinderMirror(
                boundPipPreviewView,
                frontCamera = true,
                frontMirror = config.frontMirror
            )
        } else {
            boundPipPreviewView?.scaleX = 1f
        }
    }

    private fun applyViewfinderMirror(
        previewView: PreviewView?,
        frontCamera: Boolean,
        frontMirror: Boolean
    ) {
        previewView?.scaleX = if (frontCamera && !frontMirror) -1f else 1f
    }

    private fun toBindResult(
        config: CameraBindConfig,
        slowMotionSupported: Boolean,
        slowMotionFps: Int
    ): CameraBindResult {
        val zoom = camera?.cameraInfo?.zoomState?.value
        val facing = if (config.lens == CameraLens.FRONT) {
            CameraSelector.LENS_FACING_FRONT
        } else {
            CameraSelector.LENS_FACING_BACK
        }
        val info = camera?.cameraInfo
        return CameraBindResult(
            hasFlash = camera?.cameraInfo?.hasFlashUnit() == true,
            minZoom = zoom?.minZoomRatio ?: 1f,
            maxZoom = zoom?.maxZoomRatio ?: 1f,
            zoomRatio = zoom?.zoomRatio ?: 1f,
            videoAvailable = videoCapture != null,
            supportedExtensions = supportedExtensions(config.lens.toSelector()),
            stillFormat = stillFormat,
            ultraHdrEnabled = stillFormat == StillFormat.JPEG_ULTRA_HDR ||
                stillFormat == StillFormat.HEIC_ULTRA_HDR,
            nightIndicatorSupported = Build.VERSION.SDK_INT >= 36,
            exposureLimits = exposureLimits(),
            physicalZooms = discoverPhysicalZooms(facing),
            boundCameraId = runCatching {
                camera?.let { Camera2CameraInfo.from(it.cameraInfo).cameraId }
            }.getOrNull(),
            slowMotionSupported = slowMotionSupported,
            slowMotionFps = slowMotionFps,
            videoStabilizationSupported = isPreviewStabilizationSupported(info) ||
                isVideoStabilizationSupported(info),
            videoStabilizationActive = preview?.isPreviewStabilizationEnabled == true,
            lowLightBoostSupported = info?.isLowLightBoostSupported == true,
            videoHdrRange = videoCapture?.dynamicRange?.toVideoHdrRange() ?: VideoHdrRange.SDR,
            concurrentSupported = cameraProvider?.let { provider ->
                isDualCameraSupported(context, provider)
            } == true,
            videoFps60Supported = info?.cachedVideoFps60() == true,
            videoFps60Active = fps60Active,
            session = BoundSession(
                kind = lastBoundKind,
                preview = preview != null,
                stills = imageCapture != null,
                video = videoCapture != null,
                analysis = imageAnalysis != null,
                stillsOnlyFallback = lastStillsOnlyFallback,
                cameraId = runCatching {
                    camera?.let { Camera2CameraInfo.from(it.cameraInfo).cameraId }
                }.getOrNull(),
                lens = config.lens,
                extension = lastBoundExtension,
                stillFormat = stillFormat,
                videoHdr = videoCapture?.dynamicRange?.toVideoHdrRange() ?: VideoHdrRange.SDR,
                videoFps60 = fps60Active,
                videoStabilization = preview?.isPreviewStabilizationEnabled == true,
                rawFullSensor = lastRawFullSensor
            )
        )
    }

    private fun finishBind(
        config: CameraBindConfig,
        slowMotionSupported: Boolean,
        slowMotionFps: Int
    ): CameraBindResult {
        flashMode = config.flash
        frontMirrorEnabled = config.frontMirror
        lowLightBoostRequested = config.lowLightBoost
        applyViewfinderMirror(config)
        applyTorch(config.flash)
        watchLowLightBoost()
        applyLowLightBoost()
        return toBindResult(config, slowMotionSupported, slowMotionFps)
    }

    private fun watchLowLightBoost() {
        stopWatchingLowLightBoost()
        val live = camera?.cameraInfo?.lowLightBoostState ?: run {
            _lowLightBoost.value = LowLightBoost.OFF
            return
        }
        lowLightBoostLiveData = live
        live.observeForever(lowLightBoostObserver)
        _lowLightBoost.value = when (live.value) {
            LowLightBoostState.ACTIVE -> LowLightBoost.ACTIVE
            LowLightBoostState.INACTIVE -> LowLightBoost.INACTIVE
            else -> LowLightBoost.OFF
        }
    }

    private fun stopWatchingLowLightBoost() {
        lowLightBoostLiveData?.removeObserver(lowLightBoostObserver)
        lowLightBoostLiveData = null
    }

    private fun applyLowLightBoost() {
        val cam = camera ?: return
        val supported = cam.cameraInfo.isLowLightBoostSupported
        val enable = lowLightBoostRequested &&
            supported &&
            !highSpeedSession &&
            flashMode != FlashMode.TORCH
        if (!supported && !enable) {
            _lowLightBoost.value = LowLightBoost.OFF
            return
        }
        val future = cam.cameraControl.enableLowLightBoostAsync(enable)
        future.addListener({
            runCatching { future.get() }.onFailure { error ->
                Logger.warning(TAG, "Low light boost failed: ${error.message}")
            }
        }, cameraExecutor)
    }

    private data class HighSpeedBind(val camera: Camera, val fps: Int)

    private fun applyStillOutput(file: File, type: EffectMode, mirror: Boolean): File {
        if (type == EffectMode.NONE && !mirror) return file
        return try {
            val original = BitmapFactory.decodeFile(file.absolutePath) ?: return file
            val flipped = if (mirror) original.flippedHorizontally() else original
            val processed = ColorEffects.applyToBitmap(flipped, type)
            FileOutputStream(file).use { stream ->
                processed.compress(android.graphics.Bitmap.CompressFormat.JPEG, 95, stream)
            }
            if (processed !== flipped) processed.recycle()
            if (flipped !== original) flipped.recycle()
            original.recycle()
            file
        } catch (error: Throwable) {
            Logger.error(TAG, "Still effect failed: ${error.message}", error)
            file
        }
    }

    private fun Bitmap.flippedHorizontally(): Bitmap {
        val matrix = Matrix().apply { preScale(-1f, 1f) }
        return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
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
        }, cameraExecutor)
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
            }, cameraExecutor)
        }

    companion object {
        private const val TAG = "CameraSession"
        private const val MOTION_DURATION_MS = 1_500L
    }
}
