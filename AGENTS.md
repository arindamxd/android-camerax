# AGENTS.md — CameraX

Guidance for AI coding agents (and humans) working in this repository. **Read this before making
changes.** It describes the rules, architecture, design system, and shared components.

Play Store camera app (`com.arindam.camerax`) built with Jetpack CameraX 1.6.1. Kotlin, Jetpack Compose, minSdk 23, compileSdk/targetSdk 37. Goal: show other apps what the CameraX library can do.

## Architecture (Clean Architecture, single module)

```
ui (presentation) → domain ← data
```

- `domain/model` — camera models with no CameraX / Compose types (`NightScene`, `ExposurePriority`, `StillFormat`, `ExposureLimits`)
- `domain/repository` — `CameraRepository`, `MediaRepository`
- `domain/usecase` — `CapturePhoto` (incl. `motionPhoto`), `StartRecording`, `BindCamera`, `SetExposure`, `ObserveNightScene`, `SetTargetRotation`, `StitchPanorama`, recording/zoom/flash/filter interactors
- `data/camera/CameraSession` — CameraX implementation of `CameraRepository`
- `data/camera/PreviewViewHost` — `CameraHost` adapter for `PreviewView`
- `data/camera/MotionPhotoMuxer` — JPEG + XMP + appended MP4 (Motion Photo 1.0)
- `data/camera/PanoramaStitcher` — horizontal sweep stitch for `CameraMode.PANORAMA`
- `data/camera/RecordingForegroundService` — `camera|microphone` FGS while recording
- `ui/` — Compose chrome + `CameraViewModel` (UI state, countdown, bind revision, night debounce)
- `di/AppContainer` — composition root; do not construct `CameraSession` from the UI

Keep use cases; do not collapse back to ViewModel → CameraSession. Do not split Gradle modules unless asked.

`CameraFragment` is a thin Compose host only (plus IMAGE_CAPTURE / MOTION_PHOTO result delivery). Do not resurrect the old View-based capture/flash/zoom code.

## CameraX rules

- Bind `Preview + ImageCapture + VideoCapture` together when possible; fall back (drop analysis, drop video, stills only) on `IllegalArgumentException`.
- Rebind only for lens, physical back cameras (ultra-wide/tele), OEM extensions, face detection, enabling/disabling the color `CameraEffect`, or night-scene auto-switch (OEM Night). Zoom, torch, flash, filter matrix, target rotation, and hybrid AE must not rebind. Extra back cameras are listed from `availableCameraInfos` + focal length and shown as 0.5x/1x/2x chips that rebind by camera id.
- OEM extension chips (HDR / Night / Portrait=`BOKEH` / Beauty=`FACE_RETOUCH`) only if `ExtensionsManager.isExtensionAvailable`. Extensions typically cannot bind with `VideoCapture`.
- Face boxes on **preview and recorded video** use `MlKitAnalyzer` + `OverlayEffect` (`PREVIEW | VIDEO_CAPTURE`), not a Compose canvas.
- Overlay Compose chrome on `PreviewView` with `ImplementationMode.COMPATIBLE` (TextureView). `PERFORMANCE` / SurfaceView steals touches so settings and footer buttons miss. Chrome uses `zIndex`, `WindowInsets.safeDrawing.union(systemGestures)`, and 48dp targets. Mode pager must tap-to-select, not only snap the centered item.
- Live filters: `ColorFilterProcessor` (`CameraEffect` subclass) for preview/video; same color matrix on still JPEGs. Skip recompress when the still is UltraHDR so gain maps survive.
- Stills: prefer reflected CameraX `*HEIC*` UltraHDR if advertised, else `OUTPUT_FORMAT_JPEG_ULTRA_HDR`, else JPEG. Media whitelist: `jpg`, `jpeg`, `heic`, `mp4`.
- After capture, `PublishMedia` copies into `DCIM/CameraX` via MediaStore so items appear in the system gallery. In-app gallery still reads the app pictures directory.
- Night auto-switch (API 36+): `CaptureResult.EXTENSION_NIGHT_MODE_INDICATOR`, debounce ~800ms, bind Night extension. Do not auto-switch if the user picked an extension, motion photo is on, or mode is video.
- Hybrid AE (API 36+): `CONTROL_AE_PRIORITY_MODE` (ISO / shutter) via `Camera2CameraControl`. Hide controls unless the device lists extra priority modes.
- Motion photos: short muted clip + still, mux via `MotionPhotoMuxer`. Enabling motion drops OEM extensions so `VideoCapture` can bind. Intents: `android.provider.action.MOTION_PHOTO_CAPTURE` / `_SECURE`. Gallery detects/plays via `MotionPhotoMuxer`.
- Do not add `COLOR_CORRECTION_MODE_CCT` unless asked.

## UI

Immersive dark preview, glass chrome, accent `#f9aa33` (`CameraAccent` / `orange_500`), Work Sans. Modes: Photo / Video / Effects / Pano. Gallery videos expose 0.5x–2x playback speed. Edge-to-edge (camera + gallery insets). Themed launcher icon includes `<monochrome>`. Large-screen rotation: `setTargetRotation` only, chrome stacks (no hardcoded 210/300 dp).

## Platform

- Predictive back: `android:enableOnBackInvokedCallback="true"`. No orientation lock.
- 16 KB: `android:extractNativeLibs="false"`.
- Share / capture results: `ClipData` + `FLAG_GRANT_READ_URI_PERMISSION`. IMAGE_CAPTURE writes `EXTRA_OUTPUT` or returns a thumbnail / FileProvider URI (FileProvider for motion photos and HEIC).
- Background recording: start `RecordingForegroundService`; stop recording on `ON_STOP`.
- `android.hardware.microphone` is optional.

## Do not

- Force-push, amend pushed commits, or commit unless asked.
- Edit the plan file in `.cursor/plans/`.
