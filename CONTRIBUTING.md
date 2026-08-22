## Contributing

1. Fork it.
2. Checkout the development branch: `git checkout development`
3. Create your feature branch: `git checkout -b my-new-feature`
4. Follow [AGENTS.md](AGENTS.md): single-module Clean Architecture (`ui → domain ← data`), no Hilt, no copying `camera-samples/` or `architecture-samples/`.
5. Keep disk / MediaStore / bitmap / motion-mux work off the main thread (`AppDispatchers.io` in repositories; `withContext` in ViewModels). Do not call `listFiles`, `BitmapFactory.decodeFile`, or `MediaMetadataRetriever` from Compose or click handlers.
6. Add or update JVM tests under `app/src/test` for ViewModels, use cases, domain policy, and data helpers you change.
7. Run before opening a PR:
   ```sh
   ./gradlew testDebugUnitTest
   ./gradlew lintDebug
   ```
8. Commit your changes: `git commit -m 'Add some feature'`
9. Push to the branch: `git push origin my-new-feature`
10. Submit a pull request against the `development` branch

For Play Store releases, see [RELEASE.md](RELEASE.md).
