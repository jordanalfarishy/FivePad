# Platform feature audit — Android vs. macOS

Reviewed 16 September 2026. Scope: full source review of `android/app/src/main` (~30 Kotlin files, ~7300 lines, cross-checked with `./gradlew lintDebug`/`compileDebugKotlin`/`testDebugUnitTest`) and `macos/FivePad` (13 Swift files, ~1900 lines, cross-checked with an `xcodebuild` verification build). Findings are checked against `PRD.md` §7–§8 (requirement ownership and IDs) and the existing `docs/mobile-notes-feature-audit.md` / `docs/android-ui.md`.

## Cleanup performed in this pass

**Android**
- Removed 8 drawables with zero references anywhere in code or XML: `ic_arrow_forward`, `ic_calendar_month`, `ic_close`, `ic_code`, `ic_date_range`, `ic_logo` (an oversized 361dp raw Figma export), `ic_repeat`, `ic_today`.
- Removed 3 unused string keys (`format_view_markdown`, `format_view_normal`, `permission_notifications_denied`) from both `values/strings.xml` and `values-in/strings.xml`. (`format_view_markdown_short` is the one actually used and was kept.)
- Fixed [`TasksScreen.kt`](android/app/src/main/java/com/fivepad/app/ui/TasksScreen.kt): four private helpers (`taskInk`, `taskSectionLabelColor`, `taskCompletedColor`, `taskFooterColor`) hardcoded their own light/dark colors instead of using the shared `FivePadColors` theme already used everywhere else in the file. This wasn't just duplication — `taskInk(isLight=false)` returned pure white while the app's actual dark-theme ink (`DarkColors.ink`) is `#FFECECEE`, so completed/incomplete task text was rendering with two different "ink" values on the same screen. Replaced all call sites with `colors.ink` / `colors.muted` and deleted the four helpers. Also fixed the unchecked-checkbox fill/stroke, which fell back to stale pre-redesign hex literals (`#48484B`/`#6B6B6B`) in dark mode instead of the current theme's `checkboxFill`/`checkboxStroke` (`#26262A`/`#66666E`).
- Fixed [`NoteWidget.kt`](android/app/src/main/java/com/fivepad/app/widget/NoteWidget.kt) and [`note_widget.xml`](android/app/src/main/res/layout/note_widget.xml): the home-screen widget was still painting the *pre-redesign* chrome colors (`#232324`/`#F9F9F9`) and ink (`#25242C`), so since the September 15 visual redesign the widget has looked visibly different from the rest of the app. Updated to the current theme's background/ink values.
- Fixed a stale doc comment in [`colors.xml`](android/app/src/main/res/values/colors.xml) that referenced a `splash_background` value (`#F9F9F9`) two redesigns out of date with the actual `#FFFFFF`.

**macOS**
- Removed six color-token declarations in [`Colors.swift`](macos/FivePad/Theme/Colors.swift) with no callers anywhere in the target: `checkedStroke`, `checkboxFill`, `checkedFill`, `onAccent`, `dragHandle`, `separator`. These were copied over as part of the Android/macOS palette-parity comment at the top of the file but never consumed — the macOS UI doesn't yet have the checkbox/drag-handle/separator/filled-button views that would use them. (Re-add them from `docs/android-ui.md`'s equivalent tokens when those macOS views get built — see "Recommended next steps" below.)

**Repo-wide**
- Deleted 5 stray `.DS_Store` files (already gitignored; pure clutter, no data).

**Both changes verified**: `./gradlew :app:compileDebugKotlin :app:testDebugUnitTest :app:lintDebug` → build successful, all unit tests pass, lint clean. `xcodebuild -project macos/FivePad.xcodeproj -scheme FivePad -configuration Debug CODE_SIGNING_ALLOWED=NO build` → `BUILD SUCCEEDED`.

## Intentionally *not* flagged as dead code

- `android/app/src/main/res/raw/ofl_inter.txt` / `ofl_sora.txt` — unreferenced by any `R.raw` call, but these are the SIL Open Font License attribution texts for the bundled Inter/Sora fonts. Keeping them shipped alongside the font files is normal license-compliance practice even without an in-app "licenses" screen reading them. Left in place.
- `app_name`, `tab_tasks_count`, `url_privacy`, `url_terms` in `values/strings.xml` are all marked `translatable="false"` and correctly **absent** from `values-in/strings.xml` — that's standard Android practice for locale-invariant strings, not a missing translation. (An earlier automated pass flagged these as a localization gap; verified against the source and that's a false positive.)

## Feature audit — Android (FR-1, FR-2, FR-6, FR-7)

Per PRD §7 these apply to Android: FR-1 (five slots), FR-2 (tasks), FR-6 (Android-specific), FR-7 (settings/data). Legend: ✅ implemented, 🟡 partial, ❌ missing.

| Area | Status | Notes |
|---|---|---|
| FR-1 five note slots, Markdown, formatting menu, links, autoscroll-to-caret, draft-echo guard | ✅ (21/22 rows) | Full formatting toolbar, plain/Markdown toggle, combined bold+italic, list continuation, etc. all implemented and regression-tested (`MarkdownActionsTest.kt`). Only FR-1.13 (cross-slot search, P2) is missing. FR-1.4's literal "400ms debounce" wording is superseded by an immediate ordered-write queue — a deliberate improvement, documented in `docs/mobile-notes-feature-audit.md`. |
| FR-2 task list, groups, due dates, reminders, drag-reorder | ✅ (15/17 rows) | Includes drag-and-drop across groups, recurrence, local `AlarmManager` reminders requested post-save (not pre-emptively). Missing: FR-2.8's "disable auto-sink of completed tasks" *setting* (the auto-sink behavior itself is implemented, just not user-toggleable), and FR-2.12 (500-task cap / auto-archival). |
| FR-6 Android app specifics: deep links, widget, Quick Settings tile, share-sheet receive, dark-first theme, launcher icon | ✅ (6/7 rows) | Only FR-6.6 (Material You dynamic color) is missing — explicitly optional/P2. |
| FR-7 settings & data | 🟡 | Settings page, theme, language picker, daily local backups (7-day rotation), JSON export/import with preview are all implemented. But export/import/backup (FR-7.2–7.4) are explicitly **notes-only** — tasks and groups aren't included in any export/import/backup path (`NoteBackupStore.kt`'s own doc comment says so). |
| FR-3 accounts & sync | ❌ (expected) | Not started on any platform — this is M2 per the PRD release plan. `SettingsScreen.kt`'s login section is a clearly-labeled "coming soon" placeholder, not a half-built feature. |

**Known stale doc**: `PRD.md` §7's color palette (chrome `#232324`/`#F9F9F9`, `#E6210F`-derived action accent) describes the *pre-redesign* Android palette. The shipped app has used a different blue-accented system (`#3A7BFD` accent, `#0F0F10`/`#FFFFFF` chrome) since the September 15 "Match Tasks UI to reference design" commit — `docs/android-ui.md`'s "Visual system refresh" section already documents this correctly, but `PRD.md` itself was never updated to match. Worth a pass from whoever owns the PRD.

## Feature audit — macOS (FR-1, FR-2, FR-4, FR-5, FR-7)

Per PRD §7 these apply to macOS: FR-1, FR-2, FR-4 (menu bar, macOS-only), FR-5 (main window, macOS-only), FR-7. The PRD's own "Status per 15 September 2026" note is accurate and current — this section corroborates it against the actual code rather than superseding it.

| Area | Status | Notes |
|---|---|---|
| FR-1 five note slots | 🟡 core only | Five slots, per-slot label/body, 24,000/50,000-char limits, autosave-on-blur/slot-switch all implemented ([`Store.swift`](macos/FivePad/Data/Store.swift), [`NotesPane.swift`](macos/FivePad/UI/NotesPane.swift)). **Missing entirely**: Markdown rendering (FR-1.7), the plain/Markdown view toggle (FR-1.14), the formatting menu (FR-1.15/1.15a), list-continuation on Enter (FR-1.16), link insertion/opening (FR-1.17/1.18), and clear-with-undo (FR-1.11) — the `note_revisions` table and `NoteRevision` model exist in the schema (mirroring Android's Room schema for future sync parity) but no code path ever reads or writes them yet. `TextEditor` is a completely plain text field today. |
| FR-2 task list | 🟡 core only | Add/toggle/delete tasks, create groups, "no group" vs. named groups all work ([`TasksPane.swift`](macos/FivePad/UI/TasksPane.swift)). **Missing**: editing an existing task's text (FR-2.4 — no double-click handler), drag-to-reorder (FR-2.7), due dates and reminders (FR-2.10/2.11 — `Todo.dueAt` and `Recurrence` exist in the schema but nothing in `Store.swift` sets them and no UI shows them), renaming/deleting a group (FR-2.15 only supports create), auto-sink of completed tasks (FR-2.8). |
| FR-4 menu bar & quick panel | 🟡 | FR-4.1–4.6 (status item, panel layout, shared data source with main window, global shortcut w/ conflict detection, autofocus, Esc/outside-click dismiss) and FR-4.9 (quick task entry) are all implemented (`MenuBarController.swift`, `MenuBarPanel.swift`). Missing: show-at-cursor (4.7), always-on-top (4.8), drag-to-menu-bar-icon (4.10), launch-at-login (4.11), hide-dock-icon (4.12), right-click menu (4.13). |
| FR-5 main window | 🟡 | Responsive side-by-side/stacked layout, min size, light/dark theme all implemented (`ContentView.swift`). Missing: adjustable editor font size (5.2), focus mode (5.3). |
| FR-7 settings & data | 🟡 minimal | Only Appearance and quick-panel-shortcut controls exist ([`FivePadApp.swift`](macos/FivePad/FivePadApp.swift)'s `MacSettingsView`) — no language picker, no update-check/terms/privacy links, no export/import, no local backups. Android's `SettingsScreen.kt` is a reasonable reference for what's missing. |
| FR-3 accounts & sync | ❌ (expected) | Same as Android — M2, not started. |

**Bottom line**: macOS is at the milestone the PRD's own status note describes — solid local CRUD scaffolding with the menu-bar panel's core interaction loop working — but is well behind Android on FR-1/FR-2 feature depth (no Markdown, no task editing/reordering/due-dates) and on FR-7 (no settings beyond theme+shortcut, no import/export/backup). Since FR-1/FR-2/FR-7 are spec'd as shared requirements ("Semua" in §7), closing that gap — rather than starting FR-3 sync — is the more literal reading of what M1 asks for before M2 begins.

## Recommended next steps (not done in this pass)

1. **Android `ui/` package is flat.** 14 files (`HomeScreen.kt` 1393 lines, `TasksScreen.kt` ~1000, `Sheets.kt` 715, `SettingsScreen.kt` 428, plus 10 smaller files) sit directly under `ui/`, mixing notes-screen, tasks-screen, settings, and generic-sheet code, while `ui/markdown/` and `ui/theme/` already show the project's own precedent for subpackaging. Splitting into `ui/notes/`, `ui/tasks/`, `ui/settings/`, `ui/common/` would match that precedent. Not done here: it touches every file's package declaration and cross-file imports (high blast radius) for an organizational, non-functional benefit — worth doing as its own reviewable change rather than folded into a cleanup pass. Happy to do it as a follow-up if you want it.
2. **Local disk clutter, not in git**: `dist/FivePad-0.1.0.apk` + `dist/FivePad-task-flow-debug.apk` (~24MB) and `android/app/build/` + `android/.gradle/` (~167MB build caches) are sitting in the working tree. All are gitignored and regenerable from source, so deleting them is safe, but they might be intentional (e.g. `dist/` as a hand-picked distributable snapshot) — left alone pending your call.
3. **`PRD.md` §7 palette section** is stale against the shipped Android redesign (see above) — a documentation fix, not a code one.
