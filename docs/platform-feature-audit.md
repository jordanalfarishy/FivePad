# Platform feature audit — Android vs. macOS

Reviewed 17 September 2026. Scope: full source review of `android/app/src/main` and the current `macos/FivePad` implementation (33 Swift files, ~4,900 lines), cross-checked with Android verification and clean macOS Debug/Release builds. Findings are checked against `PRD.md` §7–§8 and the existing `docs/mobile-notes-feature-audit.md` / `docs/android-ui.md`.

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

Per PRD §7 these apply to macOS: FR-1, FR-2, FR-4 (menu bar, macOS-only), FR-5 (main window, macOS-only), and FR-7. The six macOS commits after the original 16 September audit materially changed this result, so the table below supersedes the older "core only" assessment.

| Area | Status | Notes |
|---|---|---|
| FR-1 five note slots | ✅ P0 / 🟡 P1 | Five database-constrained slots, label/body limits, warning threshold, 400 ms autosave plus lifecycle flush, Markdown styling/source mode, all formatting actions, list continuation, clickable links and checkboxes, slot actions, history, and clear-with-undo are implemented. Remaining: drag text onto a slot (P2), cross-slot search (P2), and exact marker-hiding parity with FR-1.14 (the Mac intentionally dims markers so AppKit selection offsets remain stable). |
| FR-2 task list | ✅ P0 / 🟡 P1 | Add/edit/toggle/delete, due dates, recurrence, local reminders, groups with rename/delete/reorder, cross-group task drag, auto-sink, clear-completed and undo are implemented. Remaining: the setting to disable auto-sink (P1), a richer insertion indicator/edge autoscroll during drag, and the 500-active-task archival cap (P2). |
| FR-4 menu bar & quick panel | ✅ P0 / 🟡 P1 | Status item, shared store, resizable remembered panel, global shortcut with conflict reporting, autofocus, Escape/outside-click save-and-dismiss, quick task entry, and the right-click menu are implemented. Remaining: show-at-cursor, detachable always-on-top behavior, text drop onto the status item, launch at login, and optional Dock-icon hiding. |
| FR-5 main window | ✅ P0 / 🟡 P1 | Responsive side-by-side/stacked layout, minimum size, and light/dark themes are implemented. Adjustable editor font size, focus mode, and follow-system appearance remain. |
| FR-7 settings & data | 🟡 | In-app and standard macOS Settings share one implementation with theme, language-system-settings link, shortcut choice, update/terms/privacy links, login placeholder, JSON note import/export, preview, and seven rotating local backups. Archives are still notes-only; the PRD also requires task Markdown/JSON export. Mac strings are not yet externalized/localized. |
| FR-3 accounts & sync | ❌ (expected) | Same as Android — M2, not started. |

### macOS correctness fixes from the 17 September re-audit

- Made label editing editor-owned, preventing asynchronous GRDB observations from replaying older labels over fast typing.
- Flushed pending drafts before termination, backgrounding, clear, restore, import, and export; exports now read the committed database snapshot instead of stale observed state.
- Re-schedule the first task reminder after notification authorization succeeds.
- Made clear-completed one database transaction.
- Added the SQLite `slot BETWEEN 1 AND 5` constraint through a preserving migration.
- Fixed bundled variable-font discovery after Xcode flattened the resource directory.
- Preserved native Undo for Markdown formatting and enlarged slot hit targets.

**Bottom line**: the local Mac product is now substantially feature-complete through M1 and much of M3/M4, and its highest-risk local persistence paths have been hardened. It is not release-complete: synchronization (the product's core cross-platform differentiator), localization, task-inclusive portability, advanced menu-bar options, app artwork/signing, and formal automated macOS test/CI coverage remain.

## Recommended next steps (not done in this pass)

1. **Android `ui/` package is flat.** 14 files (`HomeScreen.kt` 1393 lines, `TasksScreen.kt` ~1000, `Sheets.kt` 715, `SettingsScreen.kt` 428, plus 10 smaller files) sit directly under `ui/`, mixing notes-screen, tasks-screen, settings, and generic-sheet code, while `ui/markdown/` and `ui/theme/` already show the project's own precedent for subpackaging. Splitting into `ui/notes/`, `ui/tasks/`, `ui/settings/`, `ui/common/` would match that precedent. Not done here: it touches every file's package declaration and cross-file imports (high blast radius) for an organizational, non-functional benefit — worth doing as its own reviewable change rather than folded into a cleanup pass. Happy to do it as a follow-up if you want it.
2. **Local disk clutter, not in git**: `dist/FivePad-0.1.0.apk` + `dist/FivePad-task-flow-debug.apk` (~24MB) and `android/app/build/` + `android/.gradle/` (~167MB build caches) are sitting in the working tree. All are gitignored and regenerable from source, so deleting them is safe, but they might be intentional (e.g. `dist/` as a hand-picked distributable snapshot) — left alone pending your call.
3. **`PRD.md` §7 palette section** is stale against the shipped Android redesign (see above) — a documentation fix, not a code one.
