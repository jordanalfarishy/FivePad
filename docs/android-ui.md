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

## Spacing refinement

Task sections have 16 dp outer gutters, 14 sp group titles, and task rows with 16 dp vertical padding. Header controls remain 48 dp. The existing 2 dp item gaps and 7 dp section gaps are retained; section gaps now show the page background instead of a colored separator strip.

Notes use 16/28 sp body typography in both source and formatted views. Markdown paragraphs explicitly retain their full line height, including blank lines, quotes, and legacy code blocks. Heading leading is 36 sp for H1, 32 sp for H2, and 30 sp for smaller headings.

## Input contrast

Shared form fields use opaque theme colors for their fill, hints, labels, and boundaries. Dark fields use a lighter charcoal surface (#3A3A40); light fields use a darker cool-gray surface (#C4C4CC). The same fill is retained across focus, disabled, and error states. Focus uses a solid outline and cursor, while unfocused boundaries remain visible. Fill-to-sheet contrast is 1.55:1 in dark mode and 1.44:1 in light mode; the boundary is additionally identified by an outline exceeding 3:1 against both adjacent surfaces. These colors apply to task and group forms and note link inputs. Material input surfaces and outlines also use the app palette instead of fallback colors.
