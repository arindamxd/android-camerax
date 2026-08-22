## Contributing

1. Fork it.
2. Checkout the development branch: `git checkout development`
3. Create your feature branch: `git checkout -b my-new-feature`
4. Follow [AGENTS.md](AGENTS.md): single-module Clean Architecture (`ui → domain ← data`), no Hilt, no copying `camera-samples/` or `architecture-samples/`.
5. Keep glass chrome controls at **`ChromeControlSize` (44dp)** — back, Play motion photo, Retake/Done (`ChromeActionPill`), gallery share/delete. Reuse `CameraGlassButton` / `ChromeActionPill`; do not hardcode a different button height. Full-screen headers (Gallery / Confirm / Permissions / Settings): `safeDrawing` only (no `systemGestures` on top) + **20dp** horizontal and **8dp** top padding to the back button — see [AGENTS.md](AGENTS.md#chrome-screen-insets--header-padding).
6. Keep disk / MediaStore / bitmap / motion-mux work off the main thread (`AppDispatchers.io` in repositories; `withContext` in ViewModels). Do not call `listFiles`, `BitmapFactory.decodeFile`, or `MediaMetadataRetriever` from Compose or click handlers.
7. Add or update JVM tests under `app/src/test` for ViewModels, use cases, domain policy, and data helpers you change.
8. Run before opening a PR:
   ```sh
   ./gradlew testDebugUnitTest
   ./gradlew lintDebug
   ```
9. Commit your changes: `git commit -m 'Add some feature'`
10. Push to the branch: `git push origin my-new-feature`
11. Submit a pull request against the `development` branch

For Play Store releases, see [RELEASE.md](RELEASE.md).
