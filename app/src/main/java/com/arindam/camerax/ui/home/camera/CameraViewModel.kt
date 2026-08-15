package com.arindam.camerax.ui.home.camera

import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.os.Build
import androidx.camera.view.PreviewView
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arindam.camerax.data.camera.PreviewViewHost
import com.arindam.camerax.di.CameraInteractors
import com.arindam.camerax.domain.model.CameraBindConfig
import com.arindam.camerax.domain.model.CameraExtension
import com.arindam.camerax.domain.model.CameraMode
import com.arindam.camerax.domain.model.ColorFilterType
import com.arindam.camerax.domain.model.ExposurePriority
import com.arindam.camerax.domain.model.NightScene
import com.arindam.camerax.domain.model.RecordingEvent
import com.arindam.camerax.util.ANIMATION_FAST_MILLIS
import com.arindam.camerax.util.ANIMATION_SLOW_MILLIS
import com.arindam.camerax.util.log.Logger
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
import java.io.File

class CameraViewModel(
    private val interactors: CameraInteractors
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

    init {
        viewModelScope.launch {
            interactors.observeNightScene().debounce(800).collect { scene ->
                _uiState.update { it.copy(nightScene = scene) }
                applyAutoNight(scene)
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
                            faceDetection = state.faceDetectionEnabled
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
                            message = null
                        )
                    }
                    interactors.setExposure(
                        _uiState.value.exposurePriority,
                        _uiState.value.iso,
                        _uiState.value.shutterNanos
                    )
                } catch (error: Exception) {
                    Logger.error(TAG, "Bind failed: ${error.message}")
                    _uiState.update { it.copy(message = error.message ?: "Unable to start camera") }
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
    }

    fun setMode(mode: CameraMode) {
        if (_uiState.value.isRecording || _uiState.value.motionCapturing || _uiState.value.lockCaptureMode) return
        countdownJob?.cancel()
        val clearExtension = mode == CameraMode.VIDEO && _uiState.value.extension != CameraExtension.NONE
        _uiState.update {
            it.copy(
                mode = mode,
                countdownRemaining = null,
                extension = if (clearExtension) CameraExtension.NONE else it.extension,
                bindRevision = if (clearExtension) it.bindRevision + 1 else it.bindRevision
            )
        }
    }

    fun toggleLens() {
        if (_uiState.value.isRecording || _uiState.value.motionCapturing) return
        _uiState.update {
            it.copy(lens = it.lens.toggle(), bindRevision = it.bindRevision + 1)
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
        if (_uiState.value.isRecording || _uiState.value.motionCapturing || _uiState.value.lockCaptureMode) return
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
        _uiState.update { it.copy(shutterNanos = value, exposurePriority = ExposurePriority.SHUTTER) }
        interactors.setExposure(ExposurePriority.SHUTTER, _uiState.value.iso, value)
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
        if (state.motionCapturing) return
        if (state.mode == CameraMode.VIDEO) {
            if (state.isRecording) stopRecording() else startRecording()
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
        val motion = _uiState.value.motionPhotoEnabled ||
            externalCapture.kind == ExternalCaptureKind.MOTION_PHOTO
        if (motion) _uiState.update { it.copy(motionCapturing = true) }
        interactors.capturePhoto(
            outputDirectory = directory,
            lens = _uiState.value.lens,
            colorFilter = _uiState.value.colorFilter,
            motionPhoto = motion,
            onSaved = { file ->
                _uiState.update {
                    it.copy(
                        thumbnail = file,
                        captureFlashToken = it.captureFlashToken + 1,
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
                if (externalCapture.kind == ExternalCaptureKind.IMAGE_CAPTURE ||
                    externalCapture.kind == ExternalCaptureKind.MOTION_PHOTO
                ) {
                    _externalCaptureReady.tryEmit(file)
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
                _uiState.update {
                    it.copy(
                        isRecording = false,
                        isPaused = false,
                        recordingNanos = 0L,
                        thumbnail = if (event.success) file ?: it.thumbnail else it.thumbnail,
                        message = if (event.success) null else "Video capture failed"
                    )
                }
                if (event.success &&
                    externalCapture.kind == ExternalCaptureKind.VIDEO_CAPTURE &&
                    file != null
                ) {
                    _externalCaptureReady.tryEmit(file)
                }
            }
        }
    }

    private fun applyAutoNight(scene: NightScene) {
        val state = _uiState.value
        if (manualExtension ||
            state.motionPhotoEnabled ||
            state.mode == CameraMode.VIDEO ||
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

    companion object {
        private const val TAG = "CameraViewModel"
    }
}
