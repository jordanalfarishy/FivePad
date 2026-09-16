# FivePad

Five colored notes and a grouped task list. The Android app uses Kotlin, Jetpack Compose, and Room; the macOS companion lives in `macos/`.

## Development

Open `android/` in Android Studio with Android SDK 36 and JDK 17 or newer. From that directory:

```sh
./gradlew :app:testDebugUnitTest :app:assembleDebug :app:lintDebug
```

The debug APK is written to `android/app/build/outputs/apk/debug/app-debug.apk`. Device tests require a running emulator or Android device: `./gradlew :app:connectedDebugAndroidTest`.

Open `macos/FivePad.xcodeproj` in Xcode to run the native macOS 14+ app. Its first launch creates a local GRDB database in Application Support. The current Mac milestone includes:

- Five labeled, color-coded notes with 400 ms autosave.
- Live Markdown styling, source view, formatting actions, clickable links and checkboxes, version history, and clear-with-undo.
- A responsive main window with side-by-side Notes and Tasks panes.
- Local task creation/editing, due dates, recurring reminders, grouping, drag reorder, completion, deletion, and completed-task clearing with undo.
- A native, resizable menu-bar panel with Notes/Tasks tabs, instant editor focus, quick task entry, and remembered size.
- A configurable global quick-panel shortcut (`⌥Space` by default), Escape/outside-click dismissal, and `⌘1`–`⌘5` note shortcuts.
- Dark/light appearance settings plus JSON note import/export and seven-day local note backups.

From the repository root, a non-signing verification build can be run with:

```sh
xcodebuild -project macos/FivePad.xcodeproj -scheme FivePad \
  -configuration Debug -derivedDataPath /tmp/FivePadDerivedData \
  CODE_SIGNING_ALLOWED=NO build
```

Mac–Android synchronization, cross-slot search, the remaining advanced menu-bar options, task-inclusive archives, localization, app artwork, and release signing remain subsequent Mac milestones.

## Documentation

- [Product requirements](PRD.md): product scope and requirements.
- [Android UI notes](docs/android-ui.md): current formatting, sheet, and interaction behavior.
- [Mobile Notes audit](docs/mobile-notes-feature-audit.md): feature coverage, recovery rules, and historical verification.
- [Platform feature audit](docs/platform-feature-audit.md): Android vs. macOS status against the PRD, and a code-cleanup log.

Mac–Android synchronization remains pending. Local note archives contain notes and labels, not tasks or settings.
