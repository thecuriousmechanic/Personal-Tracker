# Personal-Tracker

An Android-first personal workspace, with a companion Next.js web prototype in the same repository.

## Android

- Editable templates, nested sections, checklists, tables, boards and calendar views.
- Drag handles for sections, checklist lines and board cards; multi-select, archive and delete for records.
- Circular progress indicators, editable currency and downloadable Excel summaries.
- Local Notion export import: **Home → New template → Import from Notion**.
- Local Room storage, no account or runtime Notion credentials required.
- Refreshed Home with today’s tasks, pinned pages, full-content search, and one **New page** flow.
- Blank, daily routine, budget, project board, reading, and weekly review starters.
- Daily/weekly tasks with pause, skip, and optional notifications.
- Recoverable Trash, Undo for deletion, and full workspace backup/restore in **Settings**.

Export a Notion page using **Export → Markdown & CSV**, then select the ZIP (or a standalone `.md` / `.csv`) in the app. Preview the import, choose whether to include database records, and confirm. Every import creates a separate template without replacing existing pages.

This is an offline conversion, not live Notion sync or full Notion compatibility. Markdown headings/checklists/text become editable sections; CSV columns and values become editable text fields. Images, layout, relations, buttons and formula logic are not recreated. See [import details](docs/NOTION_IMPORT.md).

### Build

Requirements: JDK 17, Android SDK platform 36, and an SDK path configured through `ANDROID_HOME` or ignored `android/local.properties`.

```sh
cd android
./gradlew testDebugUnitTest lintDebug assembleDebug
# Isolated test package on a connected Android device:
./gradlew -PqaBuild=true connectedDebugAndroidTest
# Optimized local APK:
./gradlew assembleOptimized
```

APK: `android/app/build/outputs/apk/optimized/app-optimized.apk`.
The optimized local build uses the developer's debug signing key. Configure your own release signing before distributing production builds. No signing keys are committed.

The displayed name is **Personal-Tracker**. The existing package ID `com.krishna.personalos` and database names are intentionally retained so upgrades preserve installed data. QA uses `com.krishna.personalos.qa`.

## Web app

The companion prototype is in `src/`, using Next.js, React and Dexie/IndexedDB. It includes planning, capture, knowledge, health and analytics screens. It does not yet have Android's native template-import/editor feature set, and there is no Android/web synchronization. The prototype initializes demonstration data in a new browser profile.

```sh
npm ci
npm run dev
# Lint, tests and production build:
npm run check
```

## Repository boundaries

Source, tests, Room migration schemas, Gradle wrapper and dependency lockfiles are included. Build outputs, caches, screenshots, local SDK settings, credentials, private Notion exports and personal Obsidian notes are excluded. A clean Android checkout starts without private bundled templates; create a template or import your own export.

Legacy private-template device tests skip when their optional local fixtures are absent. Public importer, persistence and editor tests run without those fixtures. Optional legacy fixtures can be supplied locally with `-PprivateAssetsDir=/absolute/path/to/assets`; never publish an APK containing someone else's private data.

## Data safety

Android data stays in the app's local storage. Imported file contents are not uploaded. **Settings → Back up workspace** saves a versioned JSON backup of pages, records, tasks, notes, projects, goals, history, pins, appearance, available page covers, and Trash. **Restore a backup** validates and previews the file before replacing the local workspace in one transaction. Keep a current backup before uninstalling or clearing app data. Backups are limited to 32 MB and 100,000 records and must use the supported backup/database version. Excel worksheets remain reports rather than restorable backups.

Deleted pages, sections, records, tasks, notes, projects, and goals stay in **Settings → Trash** until explicitly erased. Restore a parent page before restoring its individual records. Daily/weekly tasks create one next occurrence when completed or skipped; overdue repeats start from today. Pausing stops the next occurrence and its reminder. Android may delay notifications to save battery; notifications require permission on Android 13 and later. Excel summary totals exclude archived records by default, with an explicit option to include them.

Web data stays in that browser profile. Review exports before sharing them. See [the refresh details](docs/WORKSPACE_REFRESH.md).
