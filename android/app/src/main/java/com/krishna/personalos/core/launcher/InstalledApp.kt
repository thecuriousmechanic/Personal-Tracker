package com.krishna.personalos.core.launcher

import android.graphics.drawable.Drawable

data class InstalledApp(
    val packageName: String,
    val label: String,
    val icon: Drawable? = null,
) {
    val firstLetter: Char
        get() = label.trim().firstOrNull()?.uppercaseChar()?.takeIf { it in 'A'..'Z' } ?: '#'
}

sealed interface LauncherListItem {
    data class Header(val letter: Char) : LauncherListItem
    data class App(val value: InstalledApp) : LauncherListItem
}

data class LauncherListModel(
    val items: List<LauncherListItem>,
    val sectionIndices: Map<Char, Int>,
)

fun buildLauncherListModel(apps: List<InstalledApp>, query: String): LauncherListModel {
    val cleanQuery = query.trim()
    val filtered = apps
        .asSequence()
        .filter { cleanQuery.isBlank() || it.label.contains(cleanQuery, ignoreCase = true) }
        .distinctBy(InstalledApp::packageName)
        .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER, InstalledApp::label))
        .toList()

    val items = mutableListOf<LauncherListItem>()
    val indices = linkedMapOf<Char, Int>()
    filtered.groupBy(InstalledApp::firstLetter).forEach { (letter, sectionApps) ->
        indices[letter] = items.size
        items += LauncherListItem.Header(letter)
        items += sectionApps.map(LauncherListItem::App)
    }
    return LauncherListModel(items, indices)
}
