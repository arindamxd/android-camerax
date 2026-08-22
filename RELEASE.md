# Play Store release checklist

Use this before uploading a new version to Google Play.

## Version bump

Edit `versionMajor`, `versionMinor`, or `versionPatch` in the root [`build.gradle.kts`](build.gradle.kts) for a new `versionName`.  
For another Play upload of the **same** `X.Y.Z`, bump `versionCodeOffset` instead (keeps `versionName`, raises `versionCode`).  
`versionCode` = `major * 10000 + minor * 100 + patch + versionCodeOffset`.

Current shipping target: **1.7.0** (`versionCode` **10701**, `versionCodeOffset` **1**).

## Local signing

Add to `local.properties` (never commit):

```properties
storeFile=/path/to/keystore.jks
storePassword=...
keyAlias=...
keyPassword=...
```

Release builds pick up signing only on the `release` build type. `storeFile` may be relative to the `:app` module (for example `../keystore.jks`).

## Firebase / Crashlytics (optional)

Place `app/google-services.json` locally or in CI secrets. Without it the app still builds; Crashlytics mapping upload is skipped.

Analytics collection is **disabled** in the manifest (`firebase_analytics_collection_enabled=false`). Crashlytics still receives release `Logger.error` breadcrumbs / non-fatals when `google-services.json` is present — declare that in Play **Data safety** and the privacy policy.

## Build artifacts

```sh
./gradlew bundleRelease
./gradlew printNativeDebugSymbols   # after bundleRelease
```

| Artifact | Path |
| --- | --- |
| App Bundle (AAB) | `app/build/outputs/bundle/release/app-release.aab` |
| R8 mapping | `app/build/outputs/mapping/release/mapping.txt` |
| Native debug symbols | `app/build/outputs/native-debug-symbols/release/native-debug-symbols.zip` |

Upload all three to Play Console for deobfuscation and native crash symbolication.

## Pre-upload verification

- [ ] `./gradlew testDebugUnitTest` passes
- [ ] `./gradlew lintDebug` — review warnings; fix regressions
- [ ] `./gradlew assembleRelease` / `./gradlew bundleRelease` succeeds with release signing
- [ ] Smoke test on a physical device: photo, video, gallery, settings, external capture intents
- [ ] Privacy policy URL reachable ([in-app Settings](app/src/main/res/values/strings.xml))
- [ ] App Links: `https://arindamxd.github.io/.well-known/assetlinks.json` includes `com.arindam.camerax` + the **Play App signing** certificate SHA-256 (Play Console → App integrity; not the upload key when Play App Signing is on)
- [ ] Play Console **Data safety** matches shipping SDKs (Crashlytics records non-fatals in release via `Logger.error`; Analytics collection is off in the manifest)
- [ ] Privacy policy text matches Data safety (last updated date + crash / diagnostics disclosure if Crashlytics ships)

## Play Console notes

- **Version:** `versionName` / `versionCode` from root [`build.gradle.kts`](build.gradle.kts) (currently **1.7.0** / **10700**)
- **Target API:** compile/targetSdk 37 (see root `build.gradle.kts`)
- **16 KB page size:** `packaging.jniLibs.useLegacyPackaging = false`
- **Native symbols:** `ndk.debugSymbolLevel = "SYMBOL_TABLE"` (upload the zip from `printNativeDebugSymbols`)
- **Backup:** disabled (`allowBackup=false` + data-extraction rules)
- **Microphone:** optional hardware feature
- **Themed icon:** adaptive launcher includes `<monochrome>`

## Architecture / UI constraints (do not regress)

- Disk / MediaStore / bitmap / motion mux work stays off the main thread (`AppDispatchers.io`)
- Keep `ui → domain ← data`; do not construct `CameraSession` from UI
- Glass chrome height stays at **`ChromeControlSize` (44dp)**; overlay headers use Settings spacing (`safeDrawing` + 20dp / 8dp) — see [AGENTS.md](AGENTS.md)
- See [AGENTS.md](AGENTS.md) for CameraX bind/rebind rules

## Last local verification (2026-08-22)

Automated checks already green on this machine for **1.7.0 / 10700**:

- [x] `./gradlew testDebugUnitTest` — 102 tests, 0 failures
- [x] `./gradlew lintDebug` — completed (warnings only; no fatal lint)
- [x] `./gradlew assembleRelease` — signed release APK produced
- [x] Privacy policy URL returns HTTP 200
- [x] `assetlinks.json` includes `com.arindam.camerax`
- [x] Docs name types that exist in code (`AppDispatchers`, `ChromeControlSize`, `CameraInteractors`, `CameraSession`)

Still required before Play upload (manual):

- [ ] Device smoke test
- [ ] Confirm `assetlinks.json` SHA-256 matches Play **App signing** key
- [ ] Play Data safety + privacy policy disclose Crashlytics if it ships
- [ ] `./gradlew bundleRelease` and upload AAB + R8 mapping + native symbols
