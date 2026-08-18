package com.arindam.camerax.ui.home.camera

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.drawable.ColorDrawable
import android.media.MediaMetadataRetriever
import android.os.Build
import androidx.camera.view.PreviewView
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arindam.camerax.R
import com.arindam.camerax.data.camera.MotionPhotoMuxer
import com.arindam.camerax.data.camera.PreviewViewHost
import com.arindam.camerax.data.camera.slowMotionOptions
import com.arindam.camerax.di.CameraInteractors
import com.arindam.camerax.domain.model.CameraBindConfig
import com.arindam.camerax.domain.model.CameraExtension
import com.arindam.camerax.domain.model.CameraLens
import com.arindam.camerax.domain.model.CameraMode
import com.arindam.camerax.domain.model.CaptureAspect
import com.arindam.camerax.domain.model.ColorFilterType
import com.arindam.camerax.domain.model.ExposurePriority
import com.arindam.camerax.domain.model.LowLightBoost
import com.arindam.camerax.domain.model.NightScene
import com.arindam.camerax.domain.model.RecordingEvent
import com.arindam.camerax.domain.model.SlowMotionRate
import com.arindam.camerax.domain.model.StillFormat
import com.arindam.camerax.domain.model.VideoHdrRange
import com.arindam.camerax.domain.model.VideoQuality
import com.arindam.camerax.util.ANIMATION_FAST_MILLIS
import com.arindam.camerax.util.ANIMATION_SLOW_MILLIS
import com.arindam.camerax.util.commons.Constants
import com.arindam.camerax.util.log.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

class CameraViewModel(
    private val interactors: CameraInteractors,
    private val appContext: Context
) : ViewModel() {

    private val bindMutex = Mutex()
    private val _uiState = MutableStateFlow(CameraUiState())
    val uiState: StateFlow<CameraUiState> = _uiState.asStateFlow()
    private val _externalCaptureReady = MutableSharedFlow<File>(extraBufferCapacity = 1)
    val externalCaptureReady: SharedFlow<File> = _externalCaptureReady.asSharedFlow()

    private var outputDirectory: File? = null
    private var countdownJob: Job? = null
    private var focusJob: Job? = null
    private var pendingVideoFile: File? = null
    private var launchIntentApplied = false
    private var externalCapture = ExternalCaptureRequest()
    private var manualExtension = false
    private var captureConfirmEnabled = false
    private var flipWhileRecordingEnabled = false
    private val panoramaFrames = mutableListOf<File>()
    private var lastPanoramaYaw: Float? = null
    private var panoramaCaptureBusy = false

    init {
        viewModelScope.launch {
            interactors.observeNightScene().debounce(800).collect { scene ->
                _uiState.update { it.copy(nightScene = scene) }
                applyAutoNight(scene)
            }
        }
        viewModelScope.launch {
            interactors.observeLowLightBoost().collect { boost ->
                _uiState.update { it.copy(lowLightBoostActive = boost == LowLightBoost.ACTIVE) }
            }
        }
        viewModelScope.launch {
            val supported = slowMotionOptions(appContext).available
            _uiState.update { state ->
                state.copy(
                    slowMotionSupported = supported,
                    mode = if (state.mode == CameraMode.SLOW_MOTION && !supported) {
                        CameraMode.VIDEO
                    } else {
                        state.mode
                    }
                )
            }
        }
    }

    fun setOutputDirectory(directory: File) {
        outputDirectory = directory
        _uiState.update { it.copy(thumbnail = interactors.getLatestMedia(directory)) }
    }

    fun bind(lifecycleOwner: LifecycleOwner, previewView: PreviewView) {
        viewModelScope.launch {
            bindMutex.withLock {
                try {
                    val state = _uiState.value
                    val result = interactors.bindCamera(
                        host = PreviewViewHost(lifecycleOwner, previewView),
                        config = CameraBindConfig(
                            lens = state.lens,
                            flash = state.flash,
                            extension = state.extension,
                            colorFilter = state.colorFilter,
                            faceDetection = state.faceDetectionEnabled,
                            cameraId = state.cameraId,
                            captureAspect = state.captureAspect,
                            videoQuality = state.videoQuality,
                            videoHdrRange = state.videoHdrRange,
                            slowMotion = state.mode == CameraMode.SLOW_MOTION,
                            slowMotionQuality = state.slowMotionQuality,
                            slowMotionRate = state.slowMotionRate,
                            videoStabilization = state.videoStabilization,
                            ultraHdr = state.ultraHdr,
                            rawCapture = state.rawCapture,
                            rawFullSensor = state.rawFullSensor,
                            lowLightBoost = state.lowLightBoost,
                            retainRecording = flipWhileRecordingEnabled &&
                                state.isRecording &&
                                !state.motionCapturing &&
                                state.mode != CameraMode.SLOW_MOTION
                        )
                    )
                    _uiState.update {
                        it.copy(
                            hasFlash = result.hasFlash,
                            minZoom = result.minZoom,
                            maxZoom = result.maxZoom,
                            zoomRatio = result.zoomRatio,
                            supportedExtensions = result.supportedExtensions,
                            ultraHdrEnabled = result.ultraHdrEnabled,
                            stillFormat = result.stillFormat,
                            exposureLimits = result.exposureLimits,
                            iso = result.exposureLimits.isoMin.coerceAtLeast(100)
                                .coerceAtMost(result.exposureLimits.isoMax),
                            shutterNanos = 16_666_667L.coerceIn(
                                result.exposureLimits.shutterMinNanos,
                                result.exposureLimits.shutterMaxNanos
                            ),
                            exposureCompensation = if (result.exposureLimits.evSupported) {
                                it.exposureCompensation.coerceIn(
                                    result.exposureLimits.evMin,
                                    result.exposureLimits.evMax
                                )
                            } else {
                                0
                            },
                            physicalZooms = result.physicalZooms,
                            cameraId = result.boundCameraId ?: it.cameraId,
                            slowMotionSupported = result.slowMotionSupported &&
                                (it.slowMotionSupported || result.slowMotionFps > 0),
                            slowMotionFps = result.slowMotionFps,
                            mode = if (it.mode == CameraMode.SLOW_MOTION &&
                                !result.slowMotionSupported
                            ) {
                                CameraMode.VIDEO
                            } else {
                                it.mode
                            },
                            bindRevision = if (it.mode == CameraMode.SLOW_MOTION &&
                                !result.slowMotionSupported
                            ) {
                                it.bindRevision + 1
                            } else {
                                it.bindRevision
                            },
                            videoStabilizationActive = result.videoStabilizationActive,
                            lowLightBoostSupported = result.lowLightBoostSupported,
                            videoHdrBound = result.videoHdrRange,
                            message = null
                        )
                    }
                    interactors.setExposure(
                        _uiState.value.exposurePriority,
                        _uiState.value.iso,
                        _uiState.value.shutterNanos
                    )
                    interactors.setExposureCompensation(_uiState.value.exposureCompensation)
                } catch (error: Exception) {
                    Logger.error(TAG, "Bind failed: ${error.message}")
                    val state = _uiState.value
                    if (state.lens == CameraLens.FRONT) {
                        _uiState.update {
                            it.copy(
                                lens = CameraLens.BACK,
                                cameraId = null,
                                bindRevision = it.bindRevision + 1,
                                message = "Front camera unavailable"
                            )
                        }
                    } else {
                        _uiState.update {
                            it.copy(message = error.message ?: "Unable to start camera")
                        }
                    }
                }
            }
        }
    }

    fun applyLaunchIntent(intent: Intent) {
        if (launchIntentApplied) return
        launchIntentApplied = true
        externalCapture = ExternalCaptureRequest.from(intent)
        when (externalCapture.kind) {
            ExternalCaptureKind.IMAGE_CAPTURE,
            ExternalCaptureKind.OPEN_PHOTO -> _uiState.update {
                it.copy(
                    mode = CameraMode.PHOTO,
                    lockCaptureMode = externalCapture.returnsResult
                )
            }

            ExternalCaptureKind.VIDEO_CAPTURE,
            ExternalCaptureKind.OPEN_VIDEO -> _uiState.update {
                it.copy(
                    mode = CameraMode.VIDEO,
                    lockCaptureMode = externalCapture.returnsResult
                )
            }

            ExternalCaptureKind.MOTION_PHOTO -> _uiState.update {
                it.copy(
                    mode = CameraMode.PHOTO,
                    motionPhotoEnabled = true,
                    lockCaptureMode = true
                )
            }

            ExternalCaptureKind.NONE -> Unit
        }
    }

    fun updateTargetRotation(rotation: Int) {
        interactors.setTargetRotation(rotation)
    }

    fun onHostStopped() {
        if (_uiState.value.isRecording || _uiState.value.motionCapturing) stopRecording()
        if (_uiState.value.panoramaActive) finishPanorama()
    }

    fun setMode(mode: CameraMode) {
        if (_uiState.value.isRecording || _uiState.value.motionCapturing || _uiState.value.lockCaptureMode) return
        if (mode == CameraMode.SLOW_MOTION && !_uiState.value.slowMotionSupported) return
        if (_uiState.value.panoramaActive) finishPanorama()
        countdownJob?.cancel()
        val previous = _uiState.value.mode
        val recordsVideo = mode == CameraMode.VIDEO || mode == CameraMode.SLOW_MOTION
        val clearExtension = recordsVideo && _uiState.value.extension != CameraExtension.NONE
        val slowMotionChanged = previous == CameraMode.SLOW_MOTION || mode == CameraMode.SLOW_MOTION
        _uiState.update {
            it.copy(
                mode = mode,
                countdownRemaining = null,
                extension = if (clearExtension || mode == CameraMode.SLOW_MOTION) {
                    CameraExtension.NONE
                } else {
                    it.extension
                },
                autoNightActive = if (mode == CameraMode.SLOW_MOTION) false else it.autoNightActive,
                motionPhotoEnabled = if (mode == CameraMode.SLOW_MOTION) false else it.motionPhotoEnabled,
                faceDetectionEnabled = if (mode == CameraMode.SLOW_MOTION) false else it.faceDetectionEnabled,
                colorFilter = if (mode == CameraMode.SLOW_MOTION) {
                    ColorFilterType.NONE
                } else {
                    it.colorFilter
                },
                bindRevision = if (slowMotionChanged || clearExtension) {
                    it.bindRevision + 1
                } else {
                    it.bindRevision
                }
            )
        }
    }

    fun toggleLens() {
        val state = _uiState.value
        if (state.motionCapturing || state.panoramaActive) return
        if (state.isRecording &&
            (!flipWhileRecordingEnabled || state.mode != CameraMode.VIDEO)
        ) return
        _uiState.update {
            it.copy(
                lens = it.lens.toggle(),
                cameraId = null,
                bindRevision = it.bindRevision + 1
            )
        }
    }

    fun cycleFlash() {
        if (!_uiState.value.hasFlash) return
        val next = _uiState.value.flash.next()
        _uiState.update { it.copy(flash = next) }
        interactors.setFlash(next)
    }

    fun cycleTimer() {
        if (_uiState.value.isRecording) return
        _uiState.update { it.copy(timer = it.timer.next()) }
    }

    fun toggleGrid() {
        _uiState.update { it.copy(gridEnabled = !it.gridEnabled) }
    }

    fun setZoom(ratio: Float) {
        val physical = _uiState.value.physicalZooms.minByOrNull {
            kotlin.math.abs(it.label - ratio)
        }?.takeIf { kotlin.math.abs(it.label - ratio) < 0.12f }
        if (physical != null &&
            physical.cameraId != _uiState.value.cameraId &&
            !_uiState.value.isRecording
        ) {
            _uiState.update {
                it.copy(
                    cameraId = physical.cameraId,
                    zoomRatio = physical.label,
                    bindRevision = it.bindRevision + 1
                )
            }
            return
        }
        interactors.setZoom(ratio)?.let { zoom ->
            _uiState.update {
                it.copy(zoomRatio = zoom.ratio, minZoom = zoom.min, maxZoom = zoom.max)
            }
        }
    }

    fun pinchZoom(scale: Float) {
        setZoom(_uiState.value.zoomRatio * scale)
    }

    fun tapToFocus(previewView: PreviewView, offset: Offset) {
        interactors.tapToFocus(offset.x, offset.y)
        focusJob?.cancel()
        _uiState.update { it.copy(focusPoint = offset) }
        focusJob = viewModelScope.launch {
            delay(900)
            _uiState.update { it.copy(focusPoint = null) }
        }
    }

    fun setExtension(extension: CameraExtension) {
        if (_uiState.value.isRecording || _uiState.value.motionCapturing) return
        manualExtension = true
        _uiState.update {
            it.copy(
                extension = extension,
                autoNightActive = false,
                mode = CameraMode.EFFECTS,
                bindRevision = it.bindRevision + 1
            )
        }
    }

    fun toggleMotionPhoto() {
        if (_uiState.value.isRecording || _uiState.value.motionCapturing ||
            _uiState.value.lockCaptureMode || _uiState.value.rawCapture
        ) return
        val enabled = !_uiState.value.motionPhotoEnabled
        val dropExtension = enabled && _uiState.value.extension != CameraExtension.NONE
        if (dropExtension) manualExtension = false
        _uiState.update {
            it.copy(
                motionPhotoEnabled = enabled,
                extension = if (dropExtension) CameraExtension.NONE else it.extension,
                autoNightActive = if (dropExtension) false else it.autoNightActive,
                bindRevision = if (dropExtension) it.bindRevision + 1 else it.bindRevision
            )
        }
    }

    fun setExposurePriority(priority: ExposurePriority) {
        _uiState.update { it.copy(exposurePriority = priority) }
        interactors.setExposure(priority, _uiState.value.iso, _uiState.value.shutterNanos)
    }

    fun setIso(iso: Int) {
        val limits = _uiState.value.exposureLimits
        val value = iso.coerceIn(limits.isoMin, limits.isoMax)
        _uiState.update { it.copy(iso = value, exposurePriority = ExposurePriority.ISO) }
        interactors.setExposure(ExposurePriority.ISO, value, _uiState.value.shutterNanos)
    }

    fun setShutterNanos(nanos: Long) {
        val limits = _uiState.value.exposureLimits
        val value = nanos.coerceIn(limits.shutterMinNanos, limits.shutterMaxNanos)
        _uiState.update {
            it.copy(
                shutterNanos = value,
                exposurePriority = ExposurePriority.SHUTTER
            )
        }
        interactors.setExposure(ExposurePriority.SHUTTER, _uiState.value.iso, value)
    }

    fun setExposureCompensation(index: Int) {
        val limits = _uiState.value.exposureLimits
        val value = index.coerceIn(limits.evMin, limits.evMax)
        _uiState.update { it.copy(exposureCompensation = value) }
        interactors.setExposureCompensation(value)
    }

    fun setColorFilter(type: ColorFilterType) {
        val previous = _uiState.value.colorFilter
        _uiState.update { it.copy(colorFilter = type) }
        val needsRebind = (previous == ColorFilterType.NONE) != (type == ColorFilterType.NONE)
        if (needsRebind) {
            _uiState.update { it.copy(bindRevision = it.bindRevision + 1) }
        } else {
            interactors.setColorFilter(type)
        }
    }

    fun toggleFaceDetection() {
        _uiState.update {
            it.copy(
                faceDetectionEnabled = !it.faceDetectionEnabled,
                bindRevision = it.bindRevision + 1
            )
        }
    }

    fun onShutter(previewView: PreviewView) {
        val state = _uiState.value
        if (state.review != null) return
        if (state.motionCapturing) return
        if (state.mode == CameraMode.VIDEO || state.mode == CameraMode.SLOW_MOTION) {
            if (state.mode == CameraMode.SLOW_MOTION && !state.slowMotionSupported) return
            if (state.isRecording) stopRecording() else startRecording()
            return
        }
        if (state.mode == CameraMode.PANORAMA) {
            if (state.panoramaActive) finishPanorama() else startPanorama(previewView)
            return
        }
        if (state.countdownRemaining != null) return
        if (state.timer.seconds > 0) {
            startCountdown { takePhoto(previewView) }
        } else {
            takePhoto(previewView)
        }
    }

    fun pauseOrResume() {
        val state = _uiState.value
        if (!state.isRecording) return
        if (state.isPaused) {
            interactors.resumeRecording()
            _uiState.update { it.copy(isPaused = false) }
        } else {
            interactors.pauseRecording()
            _uiState.update { it.copy(isPaused = true) }
        }
    }

    fun toggleMute() {
        val muted = !_uiState.value.isMuted
        _uiState.update { it.copy(isMuted = muted) }
        if (_uiState.value.isRecording) interactors.muteRecording(muted)
    }

    fun consumeMessage() {
        _uiState.update { it.copy(message = null) }
    }

    fun keepCapture() {
        val review = _uiState.value.review ?: return
        publishAndFinish(review.file, review.isVideo)
        review.companions.forEach { interactors.publishMedia(it) }
        _uiState.update { it.copy(thumbnail = review.file, review = null) }
    }

    fun retakeCapture() {
        val review = _uiState.value.review ?: return
        review.file.delete()
        review.companions.forEach { it.delete() }
        _uiState.update { it.copy(review = null) }
    }

    fun setCaptureConfirmEnabled(enabled: Boolean) {
        captureConfirmEnabled = enabled
    }

    fun applyCapturePreferences(
        confirmEnabled: Boolean,
        aspect: CaptureAspect,
        quality: VideoQuality,
        videoHdrRange: VideoHdrRange,
        videoStabilization: Boolean,
        slowMotionQuality: VideoQuality,
        slowMotionRate: SlowMotionRate,
        ultraHdr: Boolean,
        rawCapture: Boolean,
        rawFullSensor: Boolean,
        flipWhileRecording: Boolean,
        lowLightBoost: Boolean
    ) {
        captureConfirmEnabled = confirmEnabled
        flipWhileRecordingEnabled = flipWhileRecording
        val state = _uiState.value
        if (state.flipWhileRecording != flipWhileRecording) {
            _uiState.update { it.copy(flipWhileRecording = flipWhileRecording) }
        }
        if (state.lowLightBoost != lowLightBoost) {
            _uiState.update { it.copy(lowLightBoost = lowLightBoost) }
            interactors.setLowLightBoost(lowLightBoost)
        }
        if (state.captureAspect == aspect &&
            state.videoQuality == quality &&
            state.videoHdrRange == videoHdrRange &&
            state.videoStabilization == videoStabilization &&
            state.slowMotionQuality == slowMotionQuality &&
            state.slowMotionRate == slowMotionRate &&
            state.ultraHdr == ultraHdr &&
            state.rawCapture == rawCapture &&
            state.rawFullSensor == rawFullSensor
        ) return
        val dropExtension = rawCapture && state.extension != CameraExtension.NONE
        if (dropExtension) manualExtension = false
        _uiState.update {
            it.copy(
                captureAspect = aspect,
                videoQuality = quality,
                videoHdrRange = videoHdrRange,
                videoStabilization = videoStabilization,
                slowMotionQuality = slowMotionQuality,
                slowMotionRate = slowMotionRate,
                ultraHdr = ultraHdr,
                rawCapture = rawCapture,
                rawFullSensor = rawFullSensor,
                lowLightBoost = lowLightBoost,
                motionPhotoEnabled = if (rawCapture) false else it.motionPhotoEnabled,
                extension = if (dropExtension) CameraExtension.NONE else it.extension,
                autoNightActive = if (dropExtension) false else it.autoNightActive,
                bindRevision = it.bindRevision + 1
            )
        }
    }

    override fun onCleared() {
        countdownJob?.cancel()
        interactors.releaseCamera()
        super.onCleared()
    }

    private fun startCountdown(onFinished: () -> Unit) {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            val seconds = _uiState.value.timer.seconds
            for (value in seconds downTo 1) {
                _uiState.update { it.copy(countdownRemaining = value) }
                delay(1_000)
            }
            _uiState.update { it.copy(countdownRemaining = null) }
            onFinished()
        }
    }

    private fun takePhoto(previewView: PreviewView) {
        val directory = outputDirectory ?: return
        val motion = !_uiState.value.rawCapture &&
            _uiState.value.stillFormat != StillFormat.RAW_JPEG &&
            (_uiState.value.motionPhotoEnabled ||
                externalCapture.kind == ExternalCaptureKind.MOTION_PHOTO)
        if (motion) _uiState.update { it.copy(motionCapturing = true) }
        interactors.capturePhoto(
            outputDirectory = directory,
            lens = _uiState.value.lens,
            colorFilter = _uiState.value.colorFilter,
            motionPhoto = motion,
            onSaved = { file ->
                completeCapture(file, video = false) {
                    copy(
                        captureFlashToken = captureFlashToken + 1,
                        motionCapturing = false
                    )
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    previewView.postDelayed({
                        previewView.foreground = ColorDrawable(android.graphics.Color.WHITE)
                        previewView.postDelayed(
                            { previewView.foreground = null },
                            ANIMATION_FAST_MILLIS
                        )
                    }, ANIMATION_SLOW_MILLIS)
                }
            },
            onError = { message ->
                _uiState.update { it.copy(message = message, motionCapturing = false) }
            }
        )
    }

    private fun startRecording() {
        val directory = outputDirectory ?: return
        pendingVideoFile = interactors.startRecording(
            outputDirectory = directory,
            muted = _uiState.value.isMuted,
            persistent = flipWhileRecordingEnabled && _uiState.value.mode == CameraMode.VIDEO,
            onEvent = { event -> handleRecordEvent(event) },
            onError = { message -> _uiState.update { it.copy(message = message) } }
        )
        if (pendingVideoFile != null) {
            _uiState.update {
                it.copy(isRecording = true, isPaused = false, recordingNanos = 0L)
            }
        }
    }

    private fun stopRecording() {
        interactors.stopRecording()
    }

    private fun handleRecordEvent(event: RecordingEvent) {
        when (event) {
            is RecordingEvent.Status -> {
                _uiState.update { it.copy(recordingNanos = event.durationNanos) }
            }

            RecordingEvent.Paused -> _uiState.update { it.copy(isPaused = true) }
            RecordingEvent.Resumed -> _uiState.update { it.copy(isPaused = false) }
            is RecordingEvent.Finalized -> {
                val file = pendingVideoFile
                pendingVideoFile = null
                if (event.success && file != null) {
                    completeCapture(file, video = true) {
                        copy(
                            isRecording = false,
                            isPaused = false,
                            recordingNanos = 0L,
                            message = null
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isRecording = false,
                            isPaused = false,
                            recordingNanos = 0L,
                            message = if (event.success) null else "Video capture failed"
                        )
                    }
                }
            }
        }
    }

    private fun applyAutoNight(scene: NightScene) {
        val state = _uiState.value
        if (manualExtension ||
            state.motionPhotoEnabled ||
            state.rawCapture ||
            state.mode == CameraMode.VIDEO ||
            state.mode == CameraMode.SLOW_MOTION ||
            state.mode == CameraMode.PANORAMA ||
            state.panoramaActive ||
            state.isRecording ||
            state.motionCapturing ||
            CameraExtension.NIGHT !in state.supportedExtensions
        ) return
        when (scene) {
            NightScene.RECOMMENDED -> if (state.extension != CameraExtension.NIGHT) {
                _uiState.update {
                    it.copy(
                        extension = CameraExtension.NIGHT,
                        autoNightActive = true,
                        bindRevision = it.bindRevision + 1
                    )
                }
            }

            NightScene.NOT_RECOMMENDED -> if (state.autoNightActive && state.extension == CameraExtension.NIGHT) {
                _uiState.update {
                    it.copy(
                        extension = CameraExtension.NONE,
                        autoNightActive = false,
                        bindRevision = it.bindRevision + 1
                    )
                }
            }

            NightScene.UNKNOWN -> Unit
        }
    }

    fun onPanoramaYaw(yaw: Float) {
        val state = _uiState.value
        if (!state.panoramaActive || panoramaCaptureBusy) return
        val previous = lastPanoramaYaw
        if (previous == null) {
            lastPanoramaYaw = yaw
            return
        }
        val delta = kotlin.math.abs(yawDelta(previous, yaw))
        if (delta >= PANORAMA_STEP_DEGREES && panoramaFrames.size < PANORAMA_MAX_FRAMES) {
            lastPanoramaYaw = yaw
            capturePanoramaFrame()
        }
    }

    private fun startPanorama(previewView: PreviewView) {
        panoramaFrames.clear()
        lastPanoramaYaw = null
        panoramaCaptureBusy = false
        _uiState.update { it.copy(panoramaActive = true, panoramaFrames = 0) }
        capturePanoramaFrame(previewView)
    }

    private fun capturePanoramaFrame(previewView: PreviewView? = null) {
        val directory = outputDirectory ?: return
        if (panoramaCaptureBusy) return
        panoramaCaptureBusy = true
        interactors.capturePhoto(
            outputDirectory = directory,
            lens = _uiState.value.lens,
            colorFilter = _uiState.value.colorFilter,
            motionPhoto = false,
            onSaved = { file ->
                panoramaFrames += file
                panoramaCaptureBusy = false
                _uiState.update {
                    it.copy(
                        panoramaFrames = panoramaFrames.size,
                        captureFlashToken = it.captureFlashToken + 1
                    )
                }
                if (panoramaFrames.size >= PANORAMA_MAX_FRAMES) finishPanorama()
            },
            onError = { message ->
                panoramaCaptureBusy = false
                _uiState.update { it.copy(message = message) }
            }
        )
        if (previewView != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            previewView.postDelayed({
                previewView.foreground = ColorDrawable(android.graphics.Color.WHITE)
                previewView.postDelayed(
                    { previewView.foreground = null },
                    ANIMATION_FAST_MILLIS
                )
            }, ANIMATION_SLOW_MILLIS)
        }
    }

    private fun finishPanorama() {
        val directory = outputDirectory
        val frames = panoramaFrames.toList()
        panoramaFrames.clear()
        lastPanoramaYaw = null
        panoramaCaptureBusy = false
        _uiState.update { it.copy(panoramaActive = false, panoramaFrames = 0) }
        if (directory == null || frames.isEmpty()) return
        viewModelScope.launch {
            try {
                val file = withContext(Dispatchers.Default) {
                    interactors.stitchPanorama(frames, directory)
                }
                frames.forEach { frame ->
                    if (frame != file) frame.delete()
                }
                completeCapture(file, video = false)
            } catch (error: Exception) {
                Logger.error(TAG, "Panorama stitch failed: ${error.message}")
                _uiState.update {
                    it.copy(message = error.message ?: "Unable to stitch panorama")
                }
            }
        }
    }

    private fun yawDelta(from: Float, to: Float): Float {
        var delta = to - from
        while (delta > 180f) delta -= 360f
        while (delta < -180f) delta += 360f
        return delta
    }

    private fun completeCapture(
        file: File,
        video: Boolean,
        extra: CameraUiState.() -> CameraUiState = { this }
    ) {
        if (captureConfirmEnabled) {
            _uiState.update { extra(it).copy(review = captureReview(file, video)) }
            return
        }
        publishAndFinish(file, video)
        companionStill(file, video).forEach { interactors.publishMedia(it) }
        _uiState.update { extra(it).copy(thumbnail = file, review = null) }
    }

    private fun publishAndFinish(file: File, video: Boolean) {
        interactors.publishMedia(file)
        if (externalCapture.kind == ExternalCaptureKind.IMAGE_CAPTURE ||
            externalCapture.kind == ExternalCaptureKind.MOTION_PHOTO ||
            (externalCapture.kind == ExternalCaptureKind.VIDEO_CAPTURE && video)
        ) {
            _externalCaptureReady.tryEmit(file)
        }
    }

    private fun captureReview(file: File, video: Boolean): CaptureReview {
        val motion = !video && MotionPhotoMuxer.isMotionPhoto(file)
        val (width, height, durationNanos) = mediaInfo(file, video)
        val format = _uiState.value.stillFormat
        val formatRes = when {
            video || motion -> null
            format == StillFormat.HEIC_ULTRA_HDR -> R.string.ultrahdr_heic
            format == StillFormat.JPEG_ULTRA_HDR -> R.string.ultrahdr
            format == StillFormat.RAW_JPEG -> R.string.raw_dng
            else -> null
        }
        return CaptureReview(
            file = file,
            isVideo = video,
            isMotionPhoto = motion,
            width = width,
            height = height,
            durationLabel = durationNanos?.let { formatRecordingTime(it) },
            formatLabelRes = formatRes,
            companions = companionStill(file, video)
        )
    }

    private fun companionStill(file: File, video: Boolean): List<File> {
        if (video) return emptyList()
        val dng = File(file.parentFile, file.nameWithoutExtension + Constants.FILE.DNG_EXTENSION)
        return if (dng.exists() && dng != file) listOf(dng) else emptyList()
    }

    private fun mediaInfo(file: File, video: Boolean): Triple<Int, Int, Long?> {
        if (video) {
            val retriever = MediaMetadataRetriever()
            return try {
                retriever.setDataSource(file.absolutePath)
                val width = retriever.extractMetadata(
                    MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH
                )?.toIntOrNull() ?: 0
                val height = retriever.extractMetadata(
                    MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT
                )?.toIntOrNull() ?: 0
                val durationMs = retriever.extractMetadata(
                    MediaMetadataRetriever.METADATA_KEY_DURATION
                )?.toLongOrNull() ?: 0L
                Triple(width, height, durationMs * 1_000_000L)
            } catch (_: Exception) {
                Triple(0, 0, null)
            } finally {
                retriever.release()
            }
        }
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, options)
        return Triple(options.outWidth, options.outHeight, null)
    }

    companion object {
        private const val TAG = "CameraViewModel"
        private const val PANORAMA_STEP_DEGREES = 14f
        private const val PANORAMA_MAX_FRAMES = 10
    }
}
