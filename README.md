# FivePad

Five colored notes and a grouped task list. The Android app uses Kotlin, Jetpack Compose, and Room; the macOS companion lives in `macos/`.

## Development

Open `android/` in Android Studio with Android SDK 36 and JDK 17 or newer. From that directory:

```sh
./gradlew :app:testDebugUnitTest :app:assembleDebug :app:lintDebug
```

The debug APK is written to `android/app/build/outputs/apk/debug/app-debug.apk`. Device tests require a running emulator or Android device: `./gradlew :app:connectedDebugAndroidTest`.

## Documentation

- [Product requirements](PRD.md): product scope and requirements.
- [Android UI notes](docs/android-ui.md): current formatting, sheet, and interaction behavior.
- [Mobile Notes audit](docs/mobile-notes-feature-audit.md): feature coverage, recovery rules, and historical verification.

Mac–Android synchronization remains pending. Local note archives contain notes and labels, not tasks or settings.
