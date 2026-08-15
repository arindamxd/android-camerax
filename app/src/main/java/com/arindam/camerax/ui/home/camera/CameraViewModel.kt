package com.arindam.camerax.ui.home.camera

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
import com.arindam.camerax.domain.model.RecordingEvent
import com.arindam.camerax.util.ANIMATION_FAST_MILLIS
import com.arindam.camerax.util.ANIMATION_SLOW_MILLIS
import com.arindam.camerax.util.log.Logger
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    private var outputDirectory: File? = null
    private var countdownJob: Job? = null
    private var focusJob: Job? = null
    private var pendingVideoFile: File? = null

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
                            message = null
                        )
                    }
                } catch (error: Exception) {
                    Logger.error(TAG, "Bind failed: ${error.message}")
                    _uiState.update { it.copy(message = error.message ?: "Unable to start camera") }
                }
            }
        }
    }

    fun setMode(mode: CameraMode) {
        if (_uiState.value.isRecording) return
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
        if (_uiState.value.isRecording) return
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
        if (_uiState.value.isRecording) return
        _uiState.update {
            it.copy(
                extension = extension,
                mode = CameraMode.EFFECTS,
                bindRevision = it.bindRevision + 1
            )
        }
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
        interactors.capturePhoto(
            outputDirectory = directory,
            lens = _uiState.value.lens,
            colorFilter = _uiState.value.colorFilter,
            onSaved = { file ->
                _uiState.update {
                    it.copy(
                        thumbnail = file,
                        captureFlashToken = it.captureFlashToken + 1
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
            onError = { message -> _uiState.update { it.copy(message = message) } }
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
            }
        }
    }

    companion object {
        private const val TAG = "CameraViewModel"
    }
}
