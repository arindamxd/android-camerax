package com.arindam.camerax.data.camera

import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.extensions.ExtensionMode
import androidx.camera.video.VideoRecordEvent
import androidx.lifecycle.LifecycleOwner
import androidx.camera.view.PreviewView
import com.arindam.camerax.domain.model.CameraExtension
import com.arindam.camerax.domain.model.CameraHost
import com.arindam.camerax.domain.model.CameraLens
import com.arindam.camerax.domain.model.FlashMode
import com.arindam.camerax.domain.model.RecordingEvent

class PreviewViewHost(
    val lifecycleOwner: LifecycleOwner,
    val previewView: PreviewView
) : CameraHost

fun CameraLens.toSelector(): CameraSelector = when (this) {
    CameraLens.BACK -> CameraSelector.DEFAULT_BACK_CAMERA
    CameraLens.FRONT -> CameraSelector.DEFAULT_FRONT_CAMERA
}

fun FlashMode.toImageCaptureMode(): Int = when (this) {
    FlashMode.OFF, FlashMode.TORCH -> ImageCapture.FLASH_MODE_OFF
    FlashMode.ON -> ImageCapture.FLASH_MODE_ON
    FlashMode.AUTO -> ImageCapture.FLASH_MODE_AUTO
}

fun CameraExtension.toExtensionMode(): Int = when (this) {
    CameraExtension.NONE -> ExtensionMode.NONE
    CameraExtension.HDR -> ExtensionMode.HDR
    CameraExtension.NIGHT -> ExtensionMode.NIGHT
    CameraExtension.PORTRAIT -> ExtensionMode.BOKEH
    CameraExtension.BEAUTY -> ExtensionMode.FACE_RETOUCH
}

fun VideoRecordEvent.toDomain(): RecordingEvent? = when (this) {
    is VideoRecordEvent.Status -> RecordingEvent.Status(recordingStats.recordedDurationNanos)
    is VideoRecordEvent.Pause -> RecordingEvent.Paused
    is VideoRecordEvent.Resume -> RecordingEvent.Resumed
    is VideoRecordEvent.Finalize -> RecordingEvent.Finalized(success = !hasError())
    else -> null
}
