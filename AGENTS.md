# AGENTS.md — CameraX

Guidance for AI coding agents (and humans) working in this repository. **Read this before making
changes.** It describes the rules, architecture, design system, and shared components.

Play Store camera app (`com.arindam.camerax`) built with Jetpack CameraX 1.6.1. Kotlin, Jetpack Compose, minSdk 23, compileSdk/targetSdk 37. Goal: show other apps what the CameraX library can do.

## Architecture (Clean Architecture, single module)

```
ui (presentation) → domain ← data
```

Do not construct `CameraSession` from the UI. Keep use cases; do not collapse to ViewModel → CameraSession. Do not split Gradle modules unless asked.

Disk and MediaStore work (`list` / `delete` / `publish` / `stitch` / motion mux / still effects) must stay off the main thread: repositories use `AppDispatchers.io`, ViewModels `withContext`. Do not call `listFiles`, `BitmapFactory.decodeFile`, or `MediaMetadataRetriever` from Compose or click handlers.

### File map (where to change what)

| Goal | Start here |
|---|---|
| Add a pager mode (Photo/Video/…) | `domain/model/CameraModeCatalog.kt` + `CameraMode.labelRes` in `ui/home/camera/CameraModels.kt`. New CameraX session only in `CameraSession` if bind flags are not enough. |
| Bind / capture / zoom / flash / AE | `CameraViewModel` → `domain/usecase` → `CameraRepository` → `data/camera/CameraSession.kt` |
| Live-feed chrome (header, shutter, zoom) | `ui/home/camera/CameraChrome.kt` |
| Mode pager physics | `ui/home/camera/CameraPager.kt` |
| Settings row | `settingsSections()` in `ui/settings/SettingsCatalog.kt`; capabilities in `SettingsViewModel` |
| Theme (Light/Dark/System) | `util/theme/NightMode.kt` + `SettingsActivity` |
| Motion Photo mux / gallery play | `data/camera/MotionPhotoMuxer.kt` |
| Panorama stitch | `data/camera/PanoramaStitcher.kt` |
| Publish to DCIM | `data/media/MediaStorePublisher.kt` via `PublishMedia` |
| Composition root | `di/AppContainer.kt` (`CameraInteractors`) |

`CameraFragment` is a thin Compose host only (plus IMAGE_CAPTURE / MOTION_PHOTO result delivery). Do not resurrect the old View-based capture/flash/zoom code. Public types have KDoc describing which layer they belong to.

## CameraX rules

- Bind `Preview + ImageCapture + VideoCapture` together when possible; fall back (drop video, stills only) on `IllegalArgumentException`. Slow-motion (`CameraMode.SLOW_MOTION`) uses `HighSpeedVideoSessionConfig` (`Preview + VideoCapture` only, no audio, no ImageCapture / extensions / color `CameraEffect`). Hide Slo-mo from the mode pager if `Recorder.getHighSpeedVideoCapabilities` is null / has no SDR qualities; probe that at ViewModel start so the chip is not delayed until bind. Settings still show slo-mo quality and capture fps; disable those rows when the device lists none.
- Rebind only for lens, physical back cameras (ultra-wide/tele), OEM extensions, enabling/disabling the color `CameraEffect`, night-scene auto-switch (OEM Night), Settings photo aspect / video quality / video stabilization / Ultra HDR / RAW / DNG / full-sensor RAW / slo-mo quality and fps / 60 fps (`GroupableFeature.FPS_60`), or entering/leaving slow-motion or Dual. Zoom, torch, flash, effect matrix, target rotation, hybrid AE, and exposure compensation must not rebind. Extra back cameras are listed from `availableCameraInfos` + focal length and shown as 0.5x/1x/2x chips that rebind by camera id. Flip during a normal video recording is allowed when Settings **Flip while recording** is on (default off): start with `PendingRecording.asPersistentRecording()`, keep the same `VideoCapture`/`Recorder`, `unbindAll()`, and bind the new lens — do not stop the clip. Skip persistent recording for slow-motion, Dual, and motion photos.
- Dual mode binds concurrent front+back `Preview` via `availableConcurrentCameraInfos` (hide Dual if empty). Prefer concurrent stills; fall back to preview-only if `ImageCapture` cannot bind. Do not add ML Kit / face overlays. `ImageAnalysis` is used only in Effects mode for live ColorMatrix effects (same approach as the CameraX effects sample).
- OEM extension chips (HDR / Night / Portrait=`BOKEH` / Beauty=`FACE_RETOUCH`) only if `ExtensionsManager.isExtensionAvailable`. Extensions typically cannot bind with `VideoCapture`.
- Overlay Compose chrome on `PreviewView` with `ImplementationMode.COMPATIBLE` (TextureView). `PERFORMANCE` / SurfaceView steals touches so settings and footer buttons miss. Chrome uses `zIndex`, `WindowInsets.safeDrawing.union(systemGestures)`, and 48dp targets. Mode pager must tap-to-select, not only snap the centered item. Live-feed header icons and status chips are **mode-based** (Photo: flash/timer/grid/motion/exposure + Ultra HDR/RAW; Video: flash/grid/exposure + Stabilized/video HDR/60 fps/LLB, never Ultra HDR; Slo-mo: flash + fps chip; Effects: flash/timer/grid/exposure + None/Grayscale/Invert/Sepia/Cool/Warm/Vivid chips; Pano: grid + pan hint, no zoom; Dual: flash/grid + Front+back chip, PiP at bottom-end, hide zoom/flip/timer/motion/exposure). Full Settings stay behind the gear. Effects mode draws the analyzed frame in Compose (no `PreviewView`); `ColorMatrixColorFilter` requires a software ARGB bitmap (`ImageProxy.toBitmap()` is often HARDWARE and silently skips the matrix).
- Live Effects mode uses `ImageAnalysis` (`STRATEGY_KEEP_ONLY_LATEST`, ~1280×720) and the CameraX effects-sample `ColorMatrix` set: None / Grayscale / Invert / Sepia / Cool / Warm / Vivid. Display the processed frame over `PreviewView`. Switching chips only updates the analyzer (no rebind). Same matrix on still JPEGs. Skip recompress when the still is UltraHDR so gain maps survive. Do not bind `CameraEffect` / `Media3Effect` for these chips.
- Video 60 fps: query `CameraInfo.isSessionConfigSupported` on a `SessionConfig` with `GroupableFeature.FPS_60`, then bind that required feature group. Settings toggle (default off). Skip slow-motion and Dual. Show a **60 fps** chip when that group is actually bound.
- Stills: prefer reflected CameraX `*HEIC*` UltraHDR if advertised, else `OUTPUT_FORMAT_JPEG_ULTRA_HDR`, else JPEG. Settings toggle (default on) when `ImageCapture` capabilities list an Ultra HDR format. RAW: Settings **RAW / DNG** (default off) when capabilities list `OUTPUT_FORMAT_RAW_JPEG`; bind that format and `takePicture` DNG + JPEG together. Settings **Full sensor RAW** (default off) when the camera lists an ultra-high-resolution / maximum-resolution RAW map; bind with `SENSOR_PIXEL_MODE_MAXIMUM_RESOLUTION` and highest still size. Skip Ultra HDR, OEM extensions, motion photos, color `CameraEffect`, and night auto-switch while RAW is on. Media whitelist: `jpg`, `jpeg`, `heic`, `dng`, `mp4`.
- After capture, `PublishMedia` copies into `DCIM/CameraX` via MediaStore so items appear in the system gallery. In-app gallery still reads the app pictures directory. Capture confirmation (Retake / Done) is off by default and can be turned on in Settings. Photo aspect defaults to Full; video quality defaults to FHD.
- Night auto-switch (API 36+): `CaptureResult.EXTENSION_NIGHT_MODE_INDICATOR`, debounce ~800ms, bind Night extension. Do not auto-switch if the user picked an extension, motion photo is on, RAW / DNG is on, or mode is video / slow-motion.
- Hybrid AE (API 36+): `CONTROL_AE_PRIORITY_MODE` (ISO / shutter) via `Camera2CameraControl`. Hide controls unless the device lists extra priority modes. Photo, Video, and Effects share one **Exposure** header button (not slo-mo / panorama / Dual). The same panel has exposure compensation (`CameraControl.setExposureCompensationIndex`) with no rebind; keep it while recording.
- Motion photos: short muted clip + still, mux via `MotionPhotoMuxer`. Enabling motion drops OEM extensions so `VideoCapture` can bind. Intents: `android.provider.action.MOTION_PHOTO_CAPTURE` / `_SECURE`. Gallery detects/plays via `MotionPhotoMuxer`.
- Video HDR: `Recorder.getVideoCapabilities` `getSupportedDynamicRanges`, then `VideoCapture.Builder.setDynamicRange`. Settings chips for SDR / HLG 10-bit / HDR10 / HDR10+ that the device lists (disabled if only SDR). Rebind on change. Skip slow-motion high-speed sessions. Show an HLG/HDR10 chip when that range is actually bound.
- Video stabilization: `Preview.Builder.setPreviewStabilizationEnabled` (viewfinder) and `VideoCapture.Builder.setVideoStabilizationEnabled` (recording) after `Preview.getPreviewCapabilities` / `Recorder.getVideoCapabilities` `isStabilizationSupported`. Settings toggle (default on). Skip on slow-motion high-speed sessions. Show **Stabilized** only when preview stabilization is actually on.
- Low light boost: `CameraInfo.isLowLightBoostSupported` then `CameraControl.enableLowLightBoostAsync` (no rebind). Observe `CameraInfo.lowLightBoostState` for the HUD. Settings toggle (default on); disable the row when unsupported. Skip on slow-motion and torch.
- Do not add `COLOR_CORRECTION_MODE_CCT` unless asked.

## UI

Immersive dark preview, glass chrome, accent `#f9aa33` (`CameraAccent` / `orange_500`). Type: Space Grotesk (UI) and Space Mono (HUD / chips / metadata). Modes: Photo / Video / Slo-mo / Effects / Pano / Dual. If the device cannot do high-speed, hide Slo-mo from the pager and disable slo-mo Settings. Hide Dual if concurrent cameras are unavailable. Gallery videos expose 0.5x–2x playback speed. Edge-to-edge (camera + gallery insets). Settings is Compose (`SettingsScreen`); add rows in `settingsSections()` in `SettingsCatalog.kt`. Themed launcher icon includes `<monochrome>`. Large-screen rotation: `setTargetRotation` only, chrome stacks (no hardcoded 210/300 dp).

## Platform

- Predictive back: `android:enableOnBackInvokedCallback="true"`. No orientation lock.
- 16 KB: `packaging.jniLibs.useLegacyPackaging = false` (do not set `android:extractNativeLibs` in the manifest).
- Share / capture results: `ClipData` + `FLAG_GRANT_READ_URI_PERMISSION`. IMAGE_CAPTURE writes `EXTRA_OUTPUT` or returns a thumbnail / FileProvider URI (FileProvider for motion photos and HEIC).
- Background recording: start `RecordingForegroundService`; stop recording on `ON_STOP`.
- `android.hardware.microphone` is optional.
- Release: R8 minify + resource shrink, native `SYMBOL_TABLE` for Play Console, Crashlytics mapping when `app/google-services.json` is present. Backup is off (`allowBackup=false` + data-extraction rules). Debug builds enable StrictMode (log only). See [RELEASE.md](RELEASE.md) for the Play Store checklist.

## Do not

- Force-push, amend pushed commits, or commit unless asked.
- Edit the plan file in `.cursor/plans/`.
