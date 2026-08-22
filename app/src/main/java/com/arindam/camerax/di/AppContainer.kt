package com.arindam.camerax.di

import android.content.Context
import com.arindam.camerax.data.camera.CameraDeviceFeaturesRepository
import com.arindam.camerax.data.camera.CameraSession
import com.arindam.camerax.data.media.FileMediaRepository
import com.arindam.camerax.data.settings.PreferenceSettingsRepository
import com.arindam.camerax.domain.repository.CameraRepository
import com.arindam.camerax.domain.repository.DeviceFeaturesRepository
import com.arindam.camerax.domain.repository.MediaRepository
import com.arindam.camerax.domain.repository.SettingsRepository
import com.arindam.camerax.domain.usecase.BindCamera
import com.arindam.camerax.domain.usecase.CapturePhoto
import com.arindam.camerax.domain.usecase.DeleteMedia
import com.arindam.camerax.domain.usecase.GetLatestMedia
import com.arindam.camerax.domain.usecase.ListMedia
import com.arindam.camerax.domain.usecase.LoadCaptureSettings
import com.arindam.camerax.domain.usecase.MuteRecording
import com.arindam.camerax.domain.usecase.ObserveEffectFrame
import com.arindam.camerax.domain.usecase.ObserveLowLightBoost
import com.arindam.camerax.domain.usecase.ObserveNightScene
import com.arindam.camerax.domain.usecase.ObserveRecording
import com.arindam.camerax.domain.usecase.PauseRecording
import com.arindam.camerax.domain.usecase.PicturesDirectory
import com.arindam.camerax.domain.usecase.ProbeDeviceFeatures
import com.arindam.camerax.domain.usecase.PublishMedia
import com.arindam.camerax.domain.usecase.ReleaseCamera
import com.arindam.camerax.domain.usecase.ResumeRecording
import com.arindam.camerax.domain.usecase.SetEffect
import com.arindam.camerax.domain.usecase.SetExposure
import com.arindam.camerax.domain.usecase.SetExposureCompensation
import com.arindam.camerax.domain.usecase.SetFlash
import com.arindam.camerax.domain.usecase.SetLowLightBoost
import com.arindam.camerax.domain.usecase.SetTargetRotation
import com.arindam.camerax.domain.usecase.SetZoom
import com.arindam.camerax.domain.usecase.StartRecording
import com.arindam.camerax.domain.usecase.StitchPanorama
import com.arindam.camerax.domain.usecase.StopRecording
import com.arindam.camerax.domain.usecase.TapToFocus
import com.arindam.camerax.domain.usecase.UnbindCamera

/**
 * Composition root. Owns [CameraSession] / [FileMediaRepository] and the [CameraInteractors]
 * the UI may call. Do not construct CameraX types from Fragments or Compose.
 */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    val dispatchers = AppDispatchers()
    val cameraRepository: CameraRepository = CameraSession(appContext)
    val mediaRepository: MediaRepository = FileMediaRepository(appContext)
    val settingsRepository: SettingsRepository = PreferenceSettingsRepository(appContext)
    val deviceFeaturesRepository: DeviceFeaturesRepository =
        CameraDeviceFeaturesRepository(appContext)
    val cameraInteractors = cameraInteractors(
        cameraRepository,
        mediaRepository,
        settingsRepository,
        deviceFeaturesRepository
    )
}

/**
 * Use cases the camera UI may call. Built once in [AppContainer].
 */
data class CameraInteractors(
    val bindCamera: BindCamera,
    val capturePhoto: CapturePhoto,
    val startRecording: StartRecording,
    val pauseRecording: PauseRecording,
    val resumeRecording: ResumeRecording,
    val stopRecording: StopRecording,
    val muteRecording: MuteRecording,
    val setFlash: SetFlash,
    val setLowLightBoost: SetLowLightBoost,
    val setZoom: SetZoom,
    val tapToFocus: TapToFocus,
    val setEffect: SetEffect,
    val setTargetRotation: SetTargetRotation,
    val setExposure: SetExposure,
    val setExposureCompensation: SetExposureCompensation,
    val observeNightScene: ObserveNightScene,
    val observeLowLightBoost: ObserveLowLightBoost,
    val observeRecording: ObserveRecording,
    val observeEffectFrame: ObserveEffectFrame,
    val unbindCamera: UnbindCamera,
    val releaseCamera: ReleaseCamera,
    val getLatestMedia: GetLatestMedia,
    val listMedia: ListMedia,
    val deleteMedia: DeleteMedia,
    val stitchPanorama: StitchPanorama,
    val publishMedia: PublishMedia,
    val picturesDirectory: PicturesDirectory,
    val loadCaptureSettings: LoadCaptureSettings,
    val probeDeviceFeatures: ProbeDeviceFeatures
)

fun cameraInteractors(
    cameraRepository: CameraRepository,
    mediaRepository: MediaRepository,
    settingsRepository: SettingsRepository,
    deviceFeaturesRepository: DeviceFeaturesRepository
) = CameraInteractors(
    bindCamera = BindCamera(cameraRepository),
    capturePhoto = CapturePhoto(cameraRepository),
    startRecording = StartRecording(cameraRepository),
    pauseRecording = PauseRecording(cameraRepository),
    resumeRecording = ResumeRecording(cameraRepository),
    stopRecording = StopRecording(cameraRepository),
    muteRecording = MuteRecording(cameraRepository),
    setFlash = SetFlash(cameraRepository),
    setLowLightBoost = SetLowLightBoost(cameraRepository),
    setZoom = SetZoom(cameraRepository),
    tapToFocus = TapToFocus(cameraRepository),
    setEffect = SetEffect(cameraRepository),
    setTargetRotation = SetTargetRotation(cameraRepository),
    setExposure = SetExposure(cameraRepository),
    setExposureCompensation = SetExposureCompensation(cameraRepository),
    observeNightScene = ObserveNightScene(cameraRepository),
    observeLowLightBoost = ObserveLowLightBoost(cameraRepository),
    observeRecording = ObserveRecording(cameraRepository),
    observeEffectFrame = ObserveEffectFrame(cameraRepository),
    unbindCamera = UnbindCamera(cameraRepository),
    releaseCamera = ReleaseCamera(cameraRepository),
    getLatestMedia = GetLatestMedia(mediaRepository),
    listMedia = ListMedia(mediaRepository),
    deleteMedia = DeleteMedia(mediaRepository),
    stitchPanorama = StitchPanorama(mediaRepository),
    publishMedia = PublishMedia(mediaRepository),
    picturesDirectory = PicturesDirectory(mediaRepository),
    loadCaptureSettings = LoadCaptureSettings(settingsRepository),
    probeDeviceFeatures = ProbeDeviceFeatures(deviceFeaturesRepository)
)
