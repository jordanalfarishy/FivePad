# Mobile Notes feature audit

Reviewed 14 September 2026. Reference: **FiveNotes Mobile by Apptorium**. Target: the existing FivePad Android app. This is a functional comparison against public product documentation and a source/test audit of FivePad; it is not a hands-on certification of the FiveNotes iOS app.

## Reference features and implementation status

The official [FiveNotes Mobile page](https://www.apptorium.com/fivenotes/mobile) documents five notes, Mac sync through iCloud, themes, automatic backups, Markdown, Apple Shortcuts, and Apple Watch support. The developer's [App Store listing and version history](https://apps.apple.com/us/app/fivenotes-mobile/id6472349827) additionally document widgets, to-dos, remembering the last note, long-press copy/paste/clear, note sharing, selected-text sharing, backup deletion, and alternate icons.

| Feature | FivePad before this task | Result |
|---|---|---|
| Exactly five colored notes | Implemented | Kept; no new slots, folders, or tags |
| Swipe and tap to switch notes | Implemented | Kept |
| Remember the last note | Only transient pager state | Saved preference, restored on a fresh launch |
| Automatic saving | A shared delayed save could cancel a different slot's write | Ordered, application-owned writes queued immediately; body and label drafts are retried on exit |
| Markdown formatting | Headers, emphasis, lists, checkboxes, quotes, code, links | Kept; regression tests added and quote toggle fixed |
| Raw Markdown view | Survived screen rotation | Also persists across app launches |
| Inline to-dos | Implemented | Kept separately from the Tasks tab |
| Long-press slot actions | Copy and clear | Added append from clipboard, share, Markdown export, history |
| Share selected text | No explicit action | Select text, then long-press its slot and choose **Share selected text** |
| Clear with recovery | Confirmation and five-second undo | Latest queued typing is preserved; history remains available after the undo window |
| Recover earlier text | Revisions existed only for clear, without a browser | History browser, full text preview, restore, delete history |
| Automatic backups | Android system-backup flag only | Local JSON snapshot after edits; latest snapshot for each of seven active days |
| Delete backups | Missing | Confirmed deletion from backup settings |
| Home-screen widget | Missing | Resizable Notes widget, five slot selectors, per-widget selection, tap to open and write |
| Quick automation access | Slot deep links | Added Quick Settings tile opening the last slot |
| Themes and colors | Light/dark and fixed slot accents | Preserved the approved PRD palette; arbitrary theme files/colors remain outside this implementation |
| Alternate app icons | Android adaptive/monochrome resources | Existing resources kept; no in-app icon picker |
| Mac synchronization | No backend or account implementation | Still pending; no backend details were supplied |
| Watch app | Missing | Apple Watch is outside the Android target; no Wear OS app added |
| Apple Shortcuts read/write actions | Not an Android capability | Deep links and Quick Settings provide launch access; programmable read/append APIs remain pending |

Additional FivePad Notes features completed here: receive plain text through Android's share sheet with an explicit destination slot, export an individual slot as Markdown, and export/import all five notes as a portable JSON archive with a preview before replacement.

## Where to find the new features

- **Long-press a colored dot:** copy, append clipboard text, share the whole note, export Markdown, browse history, or clear. When text is selected, a selected-text sharing action is also available.
- **Settings → Notes backups → Export, import & backups:** export five notes to JSON, import a JSON file, inspect daily backups, restore, or delete daily backups.
- **Android launcher → Widgets → FivePad Notes:** add the resizable widget. Its five numbered color buttons select the preview independently for each widget. Tap its title or body to open that slot with editor focus.
- **Quick Settings → Edit tiles → Quick note:** add the tile for the last-used slot.
- **Another app → Share → FivePad:** choose which slot receives the text. Existing content is preserved and separated by a blank line.

## Recovery and data boundaries

History keeps up to ten text versions per slot for thirty days. Normal editing checkpoints the previous nonempty body at most once every five minutes. Emptying the editor or removing at least 128 characters also checkpoints immediately. Clear, restore, and import capture displaced text in the same Room transaction as replacement. Restoring a version saves the displaced current body, so that text can also be recovered. History stores note bodies, not earlier labels.

Daily backups contain all five labels and their raw Markdown bodies. They refresh after a one-second pause in changes while the application process runs, including on a fresh launch. They retain seven **active days**, not seven scheduled midnight executions. Local backups are in app-private storage; exported JSON files provide an independent copy. The archive format is `fivepad-notes`, version 1. It deliberately excludes tasks, groups, reminders, preferences, and history. It is not a FiveNotes/iCloud import format.

Import validation requires exactly slots 1–5, unique integer slot identifiers, string labels and bodies, labels no longer than 24 characters, bodies no longer than 50,000 characters, and a file no larger than 2 MB. Invalid files are rejected before any slot changes. The preview shows all five destination slots and their incoming labels/text. Import replaces all five notes and labels. Displaced bodies remain in history.

Clipboard/share imports append rather than replace. Text that would exceed a slot's limit is rejected without partial insertion. Sharing and Markdown export use raw note text. The compact widget preview also displays raw note text rather than rich Markdown.

Immediate writes remove the old 400 ms debounce loss window and preserve ordering across slots. They run in application scope so navigation or disposal of a ViewModel does not cancel them. A process kill or storage failure can still interrupt an uncommitted write; no absolute crash-durability claim is made.

## Remaining product work

1. **Mac–Android synchronization:** choose/configure the shared backend and implement accounts, authentication, conflict handling, and matching Mac integration. FiveNotes' iCloud service is not a backend for this Android app.
2. **Optional customization:** decide whether to expand the approved light/dark themes, fixed colors, and Android monochrome icon into a user-editable theme/icon system.
3. **Platform expansions:** Wear OS and richer automation read/append APIs require separate product work.
4. **Other PRD scope:** cross-slot search, task widgets, and full-app export/import including tasks are not completed by this Notes-focused change.

The Tasks tab's existing grouping, reminders, recurrence, and ordering are outside this comparison. Concurrent task/date-picker changes in the shared workspace were preserved.

## Verification

- Debug app and instrumentation APKs build successfully.
- All **19 JVM unit tests** pass: 7 Notes save/Markdown tests and 12 existing task/date tests.
- All **4 Android instrumentation tests** pass on the Pixel 10 emulator (Android 17), executed through `adb shell am instrument -w com.fivepad.app.test/androidx.test.runner.AndroidJUnitRunner`. They use temporary databases/files and cover clear/restore, displaced-text recovery, bounded history, complete/atomic import validation, Markdown/Unicode round trips, seven-day backup retention/deletion, and immediate lifecycle flush after undo/import.
- Android lint completes with **0 errors and 59 warnings**. Remaining warnings include existing dependencies/resources, compatibility fallbacks, and localization suggestions; this is not a warning-free report.
- On-screen emulator checks confirm the slot action sheet, empty history state, Settings entry, and backup action sheet render and open correctly. Existing emulator notes were not edited during these checks.
- Full physical-device, widget-host, Quick Settings tile, and exhaustive accessibility/large-font testing remain release QA work. The checks above do not certify every feature on every supported Android version.

Build: `android/app/build/outputs/apk/debug/app-debug.apk`.

Reproduce build/unit/lint checks from `android/` using `./gradlew :app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest :app:lintDebug`. Run instrumented checks with `./gradlew :app:connectedDebugAndroidTest` on a responsive emulator or test device.
