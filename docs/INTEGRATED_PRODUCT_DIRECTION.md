# Current product direction

The Android app is a standalone personal template workspace. This supersedes the earlier launcher dashboard and installed-app discovery direction.

Home lists all available templates and opens them directly. Apps and the separate Habits section are removed. Plan and Library remain secondary surfaces. Home-role integration returns to the template list.

The local build imports the user's three populated private Notion templates as editable native pages, preserving their covers, content, records, and useful view structure. All interaction and persistence work locally. See [Template workspace](FLEXIBLE_TRACKERS.md) for verified scope and import limitations.

Use open rows, typography, spacing, and fine separators. Keep editing controls focused and navigation labeled. Avoid repeated decorative cards and require no Notion account at runtime.

Room remains the persistence boundary. Compose consumes AppViewModel state and repository actions. Private bundled content is excluded from source control. The web prototype remains secondary; Android is the product target.
