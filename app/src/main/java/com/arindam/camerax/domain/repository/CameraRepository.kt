package com.arindam.camerax.domain.repository

import com.arindam.camerax.domain.model.CameraBindConfig
import com.arindam.camerax.domain.model.CameraBindResult
import com.arindam.camerax.domain.model.CameraHost
import com.arindam.camerax.domain.model.CameraLens
import com.arindam.camerax.domain.model.ColorFilterType
import com.arindam.camerax.domain.model.FlashMode
import com.arindam.camerax.domain.model.RecordingEvent
import com.arindam.camerax.domain.model.ZoomInfo
import java.io.File

interface CameraRepository {
    suspend fun bind(host: CameraHost, config: CameraBindConfig): CameraBindResult
    fun capturePhoto(
        outputDirectory: File,
        lens: CameraLens,
        colorFilter: ColorFilterType,
        onSaved: (File) -> Unit,
        onError: (String) -> Unit
    )
    fun startRecording(
        outputDirectory: File,
        muted: Boolean,
        onEvent: (RecordingEvent) -> Unit,
        onError: (String) -> Unit
    ): File?
    fun pauseRecording()
    fun resumeRecording()
    fun stopRecording()
    fun muteRecording(muted: Boolean)
    fun setFlash(mode: FlashMode)
    fun setZoomRatio(ratio: Float): ZoomInfo?
    fun tapToFocus(x: Float, y: Float)
    fun setColorFilter(type: ColorFilterType)
    fun release()
}

interface MediaRepository {
    fun latest(directory: File): File?
}
