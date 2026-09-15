package com.krishna.personalos.core.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LauncherListModelTest {
    @Test
    fun groupsAppsAlphabeticallyAndTracksSectionIndices() {
        val model = buildLauncherListModel(
            apps = listOf(
                InstalledApp("calendar", "Calendar"),
                InstalledApp("camera", "Camera"),
                InstalledApp("browser", "Browser"),
                InstalledApp("browser", "Browser duplicate"),
            ),
            query = "",
        )

        assertEquals(setOf('B', 'C'), model.sectionIndices.keys)
        assertEquals(0, model.sectionIndices['B'])
        assertEquals(2, model.sectionIndices['C'])
        assertEquals(5, model.items.size)
    }

    @Test
    fun searchIsCaseInsensitiveAndMatchesWithinLabels() {
        val model = buildLauncherListModel(
            apps = listOf(
                InstalledApp("calendar", "Calendar"),
                InstalledApp("camera", "Camera"),
                InstalledApp("notes", "Notes"),
            ),
            query = "DAR",
        )

        val results = model.items.filterIsInstance<LauncherListItem.App>()
        assertEquals(listOf("Calendar"), results.map { it.value.label })
        assertTrue(model.sectionIndices.containsKey('C'))
    }
}
