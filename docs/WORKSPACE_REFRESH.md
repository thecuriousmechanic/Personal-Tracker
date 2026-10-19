# Workspace refresh — 12 October 2026

Android remains the product target. Home now has a compact Today section, direct task editing, a searchable page list, and pinned pages. Home, Plan, Library, and Settings use labeled bottom navigation. Appearance and currency controls live in Settings. Light and dark appearances retain the paper/green palette, with readable text, rounded controls, restrained page icons, and no placeholder cover banner on pages without a cover.

The **New page** flow offers blank, daily routine, budget/spending, project board, reading list, and weekly review starters, plus local Notion export import. Existing custom trackers remain editable and appear alongside native pages. Routine and finance starters are created through the same native page editor, with no private starter records.

Room version 5 adds `workspace_trash`, `workspace_preferences`, and task repeat/reminder columns through an additive automatic 4-to-5 migration. Existing database names and package IDs are preserved. Appearance preferences migrate from DataStore to Room so backups include them. Existing source and web changes present before this request are retained.

Deletion captures data and removes it from active views within one transaction. Trash can restore pages, sections, native records, custom tracker entries, tasks, notes, projects, and goals. Restoring a project reconnects tasks that still lack a project without replacing edits made since deletion. Restoring a native record recalculates reciprocal relations. A missing parent must be restored before its individual entries. Trash keeps deleted content until the user erases it; imported-page tombstones still prevent startup from resurrecting deleted pages. App-level deletion notices offer Undo.

Backups use the `personal-tracker-workspace` JSON format, version 1, database version 5. All supported Room tables, including complete activity history, deleted-task tombstones, pinned pages, appearance, currency, and Trash, are captured consistently. Available bundled cover images are embedded in the backup and stored locally on restoration so the backup does not require the same private APK assets. No file is uploaded. Restore validates table/column allowlists, required values, duplicate keys, enum values, numeric bounds, and tracker/project/habit links before committing a full replacement. Malformed files leave the current workspace untouched. Restored workspaces disable the legacy bundle/demo cleanup so startup cannot replace their content. Limits are 32 MB and 100,000 records. Backups are plain JSON containing personal data; keep them in a trusted location. Backup versions other than the explicitly supported version are rejected rather than guessed.

Tasks support once/daily/weekly repetition. Completion and skip generate one deterministic successor in the same transaction. Repeated completion cannot duplicate the next occurrence. Skipping moves the current occurrence to Trash. Pausing stops successor generation and reminders; changing to Once removes the pause flag. Overdue repeats start from today instead of generating a backlog of missed tasks.

Task reminders use local due-date/time values, notification channels, and optional Android 13 notification permission. Pending alarms are replaced or canceled when tasks change. Completion, deletion, and pause suppress notifications. The receiver rechecks persisted task state before posting and rejects stale alarm timestamps. Reboot, app updates, and clock/timezone changes reschedule valid reminders. Notifications open the intended task. Delivery uses inexact alarms and may be delayed by Android battery restrictions; no exact-alarm permission is required. Implementation follows [Android alarm guidance](https://developer.android.com/develop/background-work/services/alarms) and [notification permission guidance](https://developer.android.com/develop/ui/compose/notifications/notification-permission).

Excel monthly/yearly summaries exclude archived records by default. The export dialog can explicitly include archived records in those totals. Complete record sheets retain archive flags. The default totals therefore use the same active-record policy as the app.

Validation commands:

```sh
cd android
./gradlew testDebugUnitTest lintDebug assembleDebug
./gradlew -PqaBuild=true connectedDebugAndroidTest
./gradlew assembleOptimized
```

Automated device tests use only the QA package. Screenshots are generated in the ignored Android build output. The distributable APK remains `com.krishna.personalos` and uses the existing local debug signing key. Installed-app migration and launch checks preserve the main installation’s data; no uninstall or app-data clearing is performed.

Verification on 12 October 2026: all 28 JVM tests and all 53 device tests passed on the OnePlus LE2121 running Android 14. Android lint completed with zero errors, and the optimized 0.6.0 APK built successfully. Device checks cover complete backup/restore, malformed-file rejection, Trash recovery, recurring tasks, database migration, imports, record browsers, page editors, 1.3× font scaling, notification delivery, and opening the intended task from a notification. Home, new-page, budget-page, task-editor, Settings, Trash, and dark-appearance screenshots were visually inspected.

The legacy import checks now verify the exact bundled page IDs and only check Obsidian pages when those optional assets are present. The notification test exercises the actual Android permission dialog on devices that restrict shell grants, and owns its activity lifecycle so cleanup does not hide delivery results. Routine save messages no longer overlay page editing controls; the existing save indicator remains visible and actionable deletion/error notices retain their snackbar.

Before installing, the existing 0.5.0 APK and the main app’s databases/files were backed up locally. The backup passed SQLite integrity validation (schema 4, nine pages and nine native records); existing records do not overlap the legacy bundle-cleanup IDs. After reconnection, the actual installed data migrated from schema 4 to 5, passed integrity validation, and matched every original row in every existing table. The optimized 0.6.0 update was then installed with the same package/signing certificate using a data-preserving replacement. Package metadata confirms version code 7 with debugging disabled. The installed Home screen opens, displays existing pages, and reports no startup import error; launch logs contain no fatal exception or migration error. No uninstall or app-data clearing was performed.
