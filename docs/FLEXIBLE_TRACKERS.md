# Template workspace

Home is a searchable list of available templates. Tapping a row opens the full native page. Apps and the separate Habits section are removed. Existing Plan and Library remain accessible. Routine tracking inside an imported template remains part of that template.

## Private import, 9 September 2026

The connected Notion tool was used to read these populated private pages, database schemas, views, and all paginated records:

- [Week Plan](https://app.notion.com/p/254c8b8deafe802da08ecd2b8f328d65): original cover, weekly matrix with six routines, daily baseline, and seven training-day checklists.
- [MY TRACKER](https://app.notion.com/p/b9dc8b8deafe83d08b2101df22c0543f): 735 daily records, 12 monthly records, and nine status records; daily quest, weekly table, monthly calendar, and monthly overview.
- [Budget & Finance Tracker](https://app.notion.com/p/d50c8b8deafe83aead6a819fee18d2c9): eight budget categories, 25 expenses, eight incomes, and 24 month records; editable relations and locally calculated summaries.

Total: 821 source database records plus six weekly matrix rows. A fourth private page was empty and was skipped. No source pages were changed.

Private JSON and original covers reside in `android/app/src/main/assets/private-notion/` and are ignored by Git. Local builds bundle these files; they must not be treated as a public sample dataset. When the bundle is missing, the app still supports custom templates and reports no fabricated import. Source URLs are provenance only; pages do not open Notion or require a connection.

The connector exposed unsupported Notion button blocks and reported truncated page text around those blocks. All database record pages were fetched to completion. Browser access reached sign-in, so visual parity with the signed-in Notion app was not verified. The implementation adapts the available page structure and original covers for native mobile use. Unknown Notion buttons were not executed or claimed to be replicated; finance quick-add is implemented locally. Promotional embeds were omitted.

## Interaction

- Open a template directly from Home; search, archive, restore, rename, or duplicate it.
- Toggle routine matrix cells and checklists. Scroll matrix fields sideways while names remain visible.
- Navigate daily quests and weekly/monthly periods. Open existing calendar records and edit notes, dates, and checks.
- Add or edit finance records, select linked categories and months, and see totals recalculate. Income records are grouped by type.
- Open searchable complete record lists, including archived records. Archive and restore without deleting data.
- Rename/hide/restore fields, adjust choices while retaining used values, and add text, number, date, or checkbox fields. Relation pickers retain original linked IDs and update reciprocal links when saved.
- Edit section text, hide/restore sections, move them up, and add text or checklist sections. Nested checklist text is editable.
- Create custom trackers from local starters or a blank template, using typed fields, list/table views, entries, search, and independent copies.

Typography, spacing, covers, and fine separators provide structure. The app avoids repeated dashboard cards. Long record collections use bounded lazy lists; the page itself scrolls vertically.

## Persistence and calculations

AppViewModel delegates to TemplateRepository and TrackerRepository. Room version 3 adds `template_pages` through an automatic migration from version 2; the version 1-to-2 tracker migration remains intact. Imported IDs are stable and inserts ignore existing pages, so restarts cannot replace edits. Copies own separate document JSON and can be modified independently.

Numbers and dates are validated before a record write. Reciprocal relations and record changes commit in one transaction. Budget totals use expense dates and linked categories, with decimal arithmetic. Daily completion, linked monthly averages, income/expense totals, and net values are calculated locally. Original USD formatting is retained. Month classification links are editable; users must select the intended linked month when adding records for relation-based historical summaries.

This is a native adaptation of these three templates, not an arbitrary Notion formula or automation engine. Unsupported formula outputs show a dash. It does not provide recurring automation, drag-and-drop boards, an arbitrary block editor, live sync, or a general-purpose Notion import picker. Rebuilding with updated bundled data does not overwrite already imported pages.

## Verification

Use JDK 17 and run `./gradlew testDebugUnitTest lintDebug assembleDebug`. Device tests use `./gradlew -PqaBuild=true connectedDebugAndroidTest`; run a normal `assembleDebug` afterwards to restore the distributable package ID.

Tests cover import counts and idempotence, editable copies, bidirectional relations and budget totals, validation rollback, database reopen persistence, migration preserving existing tasks/notes, custom tracker editing, Home navigation, and imported template interaction. The private-import test is skipped if the intentionally untracked asset bundle is absent.

Device screenshots are written beneath `android/app/build/outputs/connected_android_test_additional_output/`, inside the tested device's `tracker-qa/` directory.

Verified on 9 September 2026: all seven JVM tests and all 11 Android device tests passed on a OnePlus LE2121 running Android 14. Lint and the normal debug build passed. Home and imported page screenshots were inspected; the routine checkbox, daily percentage, and expense summary interactions were exercised. The final APK is com.krishna.personalos. Device verification used the separate QA package and did not replace the main installed app.

## Paper appearance refresh

The app now uses a warm paper palette with forest-green actions and muted terracotta/lilac accents. Home has a sage header, serif title, cover thumbnails, and small page icons. Templates use a lighter document canvas, cover-overlapping icons, restrained heading sizes, soft quotation callouts, and fine database row/column rules. Calendar cells highlight today; navigation marks the selected destination. Plan, Library, capture, and full-screen editors inherit the same light palette, with dark system-bar icons.

The visual references are Notion's [page styling guidance](https://www.notion.com/help/customize-and-style-your-content), particularly covers, page icons, typography, and callouts. The source records and page order are unchanged. This refresh uses a deliberate light appearance rather than the previous fixed dark theme.

Appearance verification: seven JVM tests and 11 device tests passed after the refresh. Screenshots of Home, each imported template, Plan, Library, the expense editor, and capture were inspected. The normal APK was rebuilt; lint completed with no errors and five warnings.
