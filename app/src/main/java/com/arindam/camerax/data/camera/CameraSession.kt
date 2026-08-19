package com.arindam.camerax.data.camera

import android.Manifest
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
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
import androidx.camera.core.CameraEffect
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.ConcurrentCamera.SingleCameraConfig
import androidx.camera.core.DynamicRange
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.LowLightBoostState
import androidx.camera.core.Preview
import androidx.camera.core.SessionConfig
import androidx.camera.core.UseCase
import androidx.camera.core.UseCaseGroup
import androidx.camera.core.featuregroup.GroupableFeature
import androidx.camera.media3.effect.Media3Effect
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
import com.arindam.camerax.domain.model.CameraBindConfig
import com.arindam.camerax.domain.model.CameraBindResult
import com.arindam.camerax.domain.model.CameraExtension
import com.arindam.camerax.domain.model.CameraHost
import com.arindam.camerax.domain.model.CameraLens
import com.arindam.camerax.domain.model.CaptureAspect
import com.arindam.camerax.domain.model.ColorFilterType
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
import com.arindam.camerax.domain.model.usesMedia3
import com.arindam.camerax.domain.repository.CameraRepository
import com.arindam.camerax.util.commons.Constants
import com.arindam.camerax.util.log.Logger
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
    private val mainHandler = Handler(Looper.getMainLooper())

    private var cameraProvider: ProcessCameraProvider? = null
    private var extensionsManager: ExtensionsManager? = null
    private var camera: Camera? = null
    private var preview: Preview? = null
    private var imageCapture: ImageCapture? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var recording: Recording? = null
    private var highSpeedSession = false
    private var colorProcessor: ColorFilterProcessor? = null
    private var media3Effect: Media3Effect? = null
    private var boundPreviewView: PreviewView? = null
    private var pipPreview: Preview? = null
    private val _nightScene = MutableStateFlow(NightScene.UNKNOWN)
    override val nightScene: StateFlow<NightScene> = _nightScene.asStateFlow()
    private val _lowLightBoost = MutableStateFlow(LowLightBoost.OFF)
    override val lowLightBoost: StateFlow<LowLightBoost> = _lowLightBoost.asStateFlow()
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
        initialize()
        val provider = cameraProvider ?: throw IllegalStateException("Camera provider missing")
        if (config.retainRecording && canRetainRecording()) {
            return rebindWhileRecording(previewHost, config)
        }
        provider.unbindAll()
        releaseEffects()
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

        val concurrentOk = config.concurrent &&
            provider.availableConcurrentCameraInfos.isNotEmpty() &&
            previewHost.pipPreviewView != null
        if (concurrentOk) {
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
        val useRaw = config.rawCapture && !config.slowMotion && stillInfo?.supportsRawJpeg() == true
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
        val preview = previewBuilder.build().also { it.surfaceProvider = previewView.surfaceProvider }
        this.preview = preview

        val captureBuilder = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .setFlashMode(config.flash.toImageCaptureMode())
            .setTargetRotation(rotation)
        val useFullSensor = useRaw &&
            config.rawFullSensor &&
            stillInfo?.supportsFullSensorRaw(context) == true
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
            ultraHdr = config.ultraHdr && !useRaw,
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
        val video = buildVideoCapture(recorder, videoStab, videoRange)
        videoCapture = video

        val includeVideo = !useExtension
        val effects = buildEffects(
            colorFilter = if (useRaw) ColorFilterType.NONE else config.colorFilter,
            includeVideo = includeVideo
        )
        val wantFps60 = config.videoFps60 && includeVideo && !useExtension

        camera = bindWithFallback(
            provider = provider,
            lifecycleOwner = lifecycleOwner,
            selector = selector,
            preview = preview,
            imageCapture = capture,
            videoCapture = if (includeVideo) video else null,
            effects = effects,
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

    override fun capturePhoto(
        outputDirectory: File,
        lens: CameraLens,
        colorFilter: ColorFilterType,
        motionPhoto: Boolean,
        onSaved: (File) -> Unit,
        onError: (String) -> Unit
    ) {
        if (motionPhoto && stillFormat != StillFormat.RAW_JPEG && videoCapture != null) {
            captureMotionPhoto(outputDirectory, lens, colorFilter, onSaved, onError)
            return
        }
        takeStill(outputDirectory, lens, colorFilter, onSaved, onError)
    }

    override fun startRecording(
        outputDirectory: File,
        muted: Boolean,
        persistent: Boolean,
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
        val pending = persistentPending(
            capture.output.prepareRecording(context, output),
            persist = persistent
        )
        val withAudio = !highSpeedSession &&
            PermissionChecker.checkSelfPermission(
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
        val factory = previewView.meteringPointFactory
        val point = factory.createPoint(x, y)
        val action = FocusMeteringAction.Builder(
            point,
            FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE
        ).setAutoCancelDuration(3, TimeUnit.SECONDS).build()
        camera?.cameraControl?.startFocusAndMetering(action)
    }

    override fun setColorFilter(type: ColorFilterType) {
        if (type.usesMedia3()) {
            media3Effect?.setEffects(type.media3Effects())
        } else {
            colorProcessor?.colorMatrix = ColorFilters.glMatrix(type)
        }
    }

    override fun setTargetRotation(rotation: Int) {
        preview?.targetRotation = rotation
        imageCapture?.targetRotation = rotation
        videoCapture?.targetRotation = rotation
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

    override fun release() {
        recording?.stop()
        recording = null
        stopRecordingService()
        cameraProvider?.unbindAll()
        releaseEffects()
        cameraExecutor.shutdown()
        colorProcessor?.release()
        colorProcessor = null
        _nightScene.value = NightScene.UNKNOWN
        stopWatchingLowLightBoost()
        _lowLightBoost.value = LowLightBoost.OFF
        previewBoosted = false
        clearMotionCapture()
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
        colorFilter: ColorFilterType,
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
            isReversedHorizontal = lens == CameraLens.FRONT
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
            ContextCompat.getMainExecutor(context),
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
            persistent = false,
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
        mainHandler.postDelayed({ stopRecording() }, MOTION_DURATION_MS)
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

    private fun buildEffects(
        colorFilter: ColorFilterType,
        includeVideo: Boolean
    ): List<CameraEffect> {
        val targets = CameraEffect.PREVIEW or if (includeVideo) CameraEffect.VIDEO_CAPTURE else 0
        val effects = mutableListOf<CameraEffect>()
        if (colorFilter.usesMedia3()) {
            val effect = Media3Effect(
                context,
                targets,
                ContextCompat.getMainExecutor(context)
            ) { error -> Logger.error(TAG, "Media3 effect error: ${error.message}") }
            effect.setEffects(colorFilter.media3Effects())
            media3Effect = effect
            effects += effect
        } else if (colorFilter != ColorFilterType.NONE) {
            val processor = ColorFilterProcessor().also { colorProcessor = it }
            processor.colorMatrix = ColorFilters.glMatrix(colorFilter)
            effects += processor.asCameraEffect(targets)
        }
        return effects
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
        preview: Preview,
        imageCapture: ImageCapture,
        videoCapture: VideoCapture<Recorder>?,
        effects: List<CameraEffect>,
        fps60: Boolean = false
    ): Camera {
        val attempts: List<List<UseCase>> = listOfNotNull(
            listOfNotNull(preview, imageCapture, videoCapture),
            listOf(preview, imageCapture)
        ).distinct()
        var lastError: Exception? = null
        for (useCases in attempts) {
            try {
                provider.unbindAll()
                val bound = bindSession(
                    provider = provider,
                    lifecycleOwner = lifecycleOwner,
                    selector = selector,
                    useCases = useCases,
                    effects = effects,
                    fps60 = fps60 && videoCapture != null && useCases.contains(videoCapture)
                )
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

    private fun bindSession(
        provider: ProcessCameraProvider,
        lifecycleOwner: LifecycleOwner,
        selector: CameraSelector,
        useCases: List<UseCase>,
        effects: List<CameraEffect>,
        fps60: Boolean
    ): Camera {
        if (fps60) {
            try {
                val builder = SessionConfig.Builder(*useCases.toTypedArray())
                    .setRequiredFeatureGroup(GroupableFeature.FPS_60)
                effects.forEach { builder.addEffect(it) }
                val bound = provider.bindToLifecycle(lifecycleOwner, selector, builder.build())
                fps60Active = true
                return bound
            } catch (error: Exception) {
                Logger.warning(TAG, "60 fps feature group bind failed: ${error.message}")
            }
        }
        fps60Active = false
        return if (effects.isNotEmpty()) {
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
    }

    private fun bindConcurrent(
        provider: ProcessCameraProvider,
        host: PreviewViewHost,
        config: CameraBindConfig,
        rotation: Int
    ): CameraBindResult {
        val pipView = host.pipPreviewView ?: throw IllegalStateException("Dual preview missing")
        pipView.implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        pipView.scaleType = PreviewView.ScaleType.FILL_CENTER
        val primary = Preview.Builder().setTargetRotation(rotation).build().also {
            it.surfaceProvider = host.previewView.surfaceProvider
        }
        val secondary = Preview.Builder().setTargetRotation(rotation).build().also {
            it.surfaceProvider = pipView.surfaceProvider
        }
        preview = primary
        pipPreview = secondary
        videoCapture = null
        val frontGroup = UseCaseGroup.Builder().addUseCase(secondary).build()
        val capture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .setFlashMode(config.flash.toImageCaptureMode())
            .setTargetRotation(rotation)
            .build()
        val withStill = UseCaseGroup.Builder().addUseCase(primary).addUseCase(capture).build()
        val previewOnly = UseCaseGroup.Builder().addUseCase(primary).build()
        val concurrent = try {
            imageCapture = capture
            provider.bindToLifecycle(
                listOf(
                    SingleCameraConfig(CameraSelector.DEFAULT_BACK_CAMERA, withStill, host.lifecycleOwner),
                    SingleCameraConfig(CameraSelector.DEFAULT_FRONT_CAMERA, frontGroup, host.lifecycleOwner)
                )
            )
        } catch (error: Exception) {
            Logger.warning(TAG, "Dual still bind failed, preview only: ${error.message}")
            imageCapture = null
            provider.unbindAll()
            provider.bindToLifecycle(
                listOf(
                    SingleCameraConfig(CameraSelector.DEFAULT_BACK_CAMERA, previewOnly, host.lifecycleOwner),
                    SingleCameraConfig(CameraSelector.DEFAULT_FRONT_CAMERA, frontGroup, host.lifecycleOwner)
                )
            )
        }
        camera = concurrent.cameras.firstOrNull { bound ->
            bound.cameraInfo.lensFacing == CameraSelector.LENS_FACING_BACK
        } ?: concurrent.cameras.first()
        fps60Active = false
        stillFormat = StillFormat.JPEG
        return finishBind(
            config = config,
            slowMotionSupported = isHighSpeedSupported(camera?.cameraInfo),
            slowMotionFps = 0
        )
    }

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
        val video = VideoCapture.withOutput(recorder)
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
            HighSpeedBind(camera = bound, fps = fpsRange.upper)
        } catch (error: Exception) {
            Logger.warning(TAG, "High-speed session bind failed: ${error.message}")
            null
        }
    }

    private fun isHighSpeedSupported(info: CameraInfo?): Boolean {
        if (info == null) return false
        return info.supportsHighSpeedSlowMotion()
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
        range: DynamicRange = DynamicRange.SDR
    ): VideoCapture<Recorder> = VideoCapture.Builder(recorder)
        .setVideoStabilizationEnabled(stabilize)
        .setDynamicRange(range)
        .build()

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
            concurrentSupported = cameraProvider?.availableConcurrentCameraInfos?.isNotEmpty() == true,
            videoFps60Supported = info?.supportsVideoFps60() == true,
            videoFps60Active = fps60Active
        )
    }

    private fun finishBind(
        config: CameraBindConfig,
        slowMotionSupported: Boolean,
        slowMotionFps: Int
    ): CameraBindResult {
        flashMode = config.flash
        lowLightBoostRequested = config.lowLightBoost
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
        }, ContextCompat.getMainExecutor(context))
    }

    private data class HighSpeedBind(val camera: Camera, val fps: Int)

    private fun releaseEffects() {
        colorProcessor?.release()
        colorProcessor = null
        media3Effect?.close()
        media3Effect = null
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
