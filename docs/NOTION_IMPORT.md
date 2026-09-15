# Notion export import (Android)

1. In Notion, open a page or database and choose `••• → Export → Markdown & CSV`. Include subpages to include nested content.
2. Download the ZIP to the Android device.
3. In Personal-Tracker, choose `Home → New template → Import from Notion → Choose export file`.
4. Review section/database/record counts and any warnings. Rename the template if desired.
5. Leave `Include database records` off for empty database structures, or enable it to copy exported rows.
6. Tap `Import template`. Existing templates are not overwritten.

Official export instructions: https://www.notion.com/help/export-your-content

## Conversion and limits

- ZIP exports and individual UTF-8 Markdown/CSV files are accepted.
- Markdown files become collapsible sections. Headings, quotes and checkbox lines have native block types; other Markdown stays readable, editable text.
- CSV databases preserve column names, exact cell text, quoted commas/newlines and escaped quotes. The first column becomes the record title. Duplicate column labels have distinct internal IDs.
- CSV cannot reliably describe Notion property types. Formulas and relation labels remain text snapshots, not active calculations or links. No scripts, HTML or formulas execute.
- Images, unsupported files and original multi-column layouts are not imported. Nested files are grouped by filename, not reconstructed as an exact Notion page hierarchy. Warnings appear before confirmation.
- Excluding database records removes CSV rows, not Markdown page text. Export only the pages you intend to copy.
- Maximum 10 MB total decompressed content, 500 archive entries, 100 columns per table, 5,000 data rows per CSV, and 20,000 total records or Markdown blocks. Unsafe paths, malformed CSV and over-limit exports are rejected before saving.
- No files are extracted to disk and no external URLs are fetched. The app requests access only to the selected document.
- Imports are transactional and get a new template ID. Cancelling before confirmation does not save a page.

This does not connect to a Notion account, accept a Notion page URL or synchronize changes. Export again to make a new snapshot.
