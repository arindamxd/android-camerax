# Play Store release checklist

Use this before uploading a new version to Google Play.

## Version bump

Edit `versionMajor`, `versionMinor`, or `versionPatch` in the root [`build.gradle.kts`](build.gradle.kts).  
`versionCode` is derived automatically (`major * 10000 + minor * 100 + patch`).

## Local signing

Add to `local.properties` (never commit):

```properties
storeFile=/path/to/keystore.jks
storePassword=...
keyAlias=...
keyPassword=...
```

Release builds pick up signing only on the `release` build type.

## Firebase / Crashlytics (optional)

Place `app/google-services.json` locally or in CI secrets. Without it the app still builds; Crashlytics mapping upload is skipped.

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
- [ ] `./gradlew assembleRelease` succeeds
- [ ] Smoke test on a physical device: photo, video, gallery, settings, external capture intents
- [ ] Privacy policy URL reachable ([in-app Settings](app/src/main/res/values/strings.xml))
- [ ] App Links: `assetlinks.json` hosted for `arindamxd.github.io` if using verified links

## Play Console notes

- **Target API:** compile/targetSdk 37 (see root `build.gradle.kts`)
- **16 KB page size:** `packaging.jniLibs.useLegacyPackaging = false`
- **Backup:** disabled (`allowBackup=false` + data-extraction rules)
- **Microphone:** optional hardware feature

## Architecture constraints (do not regress)

- Disk / MediaStore / bitmap / motion mux work stays off the main thread
- Keep `ui → domain ← data`; do not construct `CameraSession` from UI
- See [AGENTS.md](AGENTS.md) for CameraX bind/rebind rules
