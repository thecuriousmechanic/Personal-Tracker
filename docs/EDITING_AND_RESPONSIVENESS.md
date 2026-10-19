# Editing and responsiveness fixes - 10 September 2026

Rows in Plan and Library now open full editors. Task completion has its own checkbox; tapping a task title no longer unexpectedly completes or schedules it. Plan includes All, Today, Inbox, Upcoming, and Done views with search. Notes, projects, and goals have searchable lists, visible edit icons, and contextual New actions.

Editors support task title/status/priority/date/project, note title/body, project title/area/status/date, and goal title/timeframe/progress. Save and Delete stay in a fixed footer above the keyboard. Unsaved edits require a discard decision. Destructive deletion requires a confirmation. Project deletion clears task links while retaining the tasks; entity relations and activity references are cleaned in the same transaction. Task tombstones prevent deleting all tasks from triggering demo reseeding.

Home template rows expose Edit and Delete menus. Imported pages also expose Rename, Duplicate, and Delete in their top toolbar. Custom trackers expose Delete beside Customize. Imported-template deletion retains an empty tombstone in Room so the bundled importer does not resurrect the page. The original bundled source and Notion pages are unchanged. Room version 4 adds the deleted flag via an additive 3-to-4 migration.

Large template JSON is parsed and cached off the UI thread before being presented. Covers are cached and downsampled; calendar dates are parsed once and indexed. Relation pickers virtualize their rows. Checklist edits update the visible document immediately and queue persisted changes in order; they are not discarded while another checkbox save is pending. Queued saves continue when the user navigates away, and errors are surfaced when the page remains visible.

The optimized build variant disables debugging, enables R8 and resource shrinking, and uses the existing development signing key so it can update the user's current installation without uninstalling or clearing data. It is a local development distribution, not a production signing setup. Build with `./gradlew assembleOptimized`; APK: `android/app/build/outputs/apk/optimized/app-optimized.apk`.

Before changes, the installed debug app reported 253 janky frames out of 4,858 (5.21%) and a 99th-percentile frame time of 81 ms. This cumulative sample is diagnostic evidence, not a controlled benchmark. Do not compare it directly to a short test run. Optimization follows the Android team's [Compose performance guidance](https://developer.android.com/develop/ui/compose/performance).
