package com.arindam.camerax.di

import android.content.Context
import com.arindam.camerax.data.camera.CameraSession
import com.arindam.camerax.data.media.FileMediaRepository
import com.arindam.camerax.domain.repository.CameraRepository
import com.arindam.camerax.domain.repository.MediaRepository
import com.arindam.camerax.domain.usecase.BindCamera
import com.arindam.camerax.domain.usecase.CapturePhoto
import com.arindam.camerax.domain.usecase.GetLatestMedia
import com.arindam.camerax.domain.usecase.MuteRecording
import com.arindam.camerax.domain.usecase.PauseRecording
import com.arindam.camerax.domain.usecase.ReleaseCamera
import com.arindam.camerax.domain.usecase.ResumeRecording
import com.arindam.camerax.domain.usecase.SetColorFilter
import com.arindam.camerax.domain.usecase.SetFlash
import com.arindam.camerax.domain.usecase.SetZoom
import com.arindam.camerax.domain.usecase.StartRecording
import com.arindam.camerax.domain.usecase.StopRecording
import com.arindam.camerax.domain.usecase.TapToFocus

class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    val cameraRepository: CameraRepository = CameraSession(appContext)
    val mediaRepository: MediaRepository = FileMediaRepository()
    val cameraInteractors = CameraInteractors(
        bindCamera = BindCamera(cameraRepository),
        capturePhoto = CapturePhoto(cameraRepository),
        startRecording = StartRecording(cameraRepository),
        pauseRecording = PauseRecording(cameraRepository),
        resumeRecording = ResumeRecording(cameraRepository),
        stopRecording = StopRecording(cameraRepository),
        muteRecording = MuteRecording(cameraRepository),
        setFlash = SetFlash(cameraRepository),
        setZoom = SetZoom(cameraRepository),
        tapToFocus = TapToFocus(cameraRepository),
        setColorFilter = SetColorFilter(cameraRepository),
        releaseCamera = ReleaseCamera(cameraRepository),
        getLatestMedia = GetLatestMedia(mediaRepository)
    )
}

data class CameraInteractors(
    val bindCamera: BindCamera,
    val capturePhoto: CapturePhoto,
    val startRecording: StartRecording,
    val pauseRecording: PauseRecording,
    val resumeRecording: ResumeRecording,
    val stopRecording: StopRecording,
    val muteRecording: MuteRecording,
    val setFlash: SetFlash,
    val setZoom: SetZoom,
    val tapToFocus: TapToFocus,
    val setColorFilter: SetColorFilter,
    val releaseCamera: ReleaseCamera,
    val getLatestMedia: GetLatestMedia
)
