# Android UI notes

Updated 14 September 2026. Applies to the Android Notes, Tasks, and Settings interfaces.

## Notes formatting

Header, Sub header, and Quote retain a collapsed cursor when applied without a selection, so subsequent typing preserves existing text. Selected text retains its direction and attachment to content. A selection ending at the start of the next line does not format that next line. Heading levels replace one another, and tapping the same format again removes it.

Enter continues a quote; Enter on an empty quote exits it. The Code Block action has been removed. Inline Code remains, and existing fenced Markdown is still rendered so saved notes remain readable.

## Sheets and forms

All modal sheets open fully expanded on the theme surface with 24 dp top corners. Content can scroll on short screens. Text forms use shared 12 dp rounded fields, neutral surfaces, restrained focus rings, and filled confirmation buttons. Name and link drafts survive rotation. Reminder selection stays within the task sheet, with Save and Cancel above the scrolling calendar.

The reference is [Todoist Quick Add](https://www.todoist.com/help/articles/use-task-quick-add-in-todoist-va4Lhpzz): a focused composer with the task name first and optional scheduling alongside it. FivePad retains its own colors and navigation.

## Task groups and feedback

The add-task control sits in each group header before its menu, including an add control for ungrouped tasks. Each header action has a separate 48 dp touch target.

Sheet actions, formatting tiles, task completion, and shared navigation actions use a subtle spring press response and light haptic feedback. Animations follow Compose's system duration scale.

## Verification

Run the commands in the [README](../README.md). Regression tests cover formatting round trips, collapsed cursors, reversed selections, selection boundaries, quote continuation, and empty-quote exit. Historical device checks in the feature audit predate this refresh and do not verify these changes.

Refresh verification: all 23 JVM tests pass; debug APK builds; lint reports 0 errors and 59 warnings. Pixel 10 emulator checks confirm group-header action placement and the add-task sheet with keyboard visible. No tasks or notes were changed during these visual checks. Exhaustive large-font, landscape, light-theme, and physical-device QA remains outstanding.
