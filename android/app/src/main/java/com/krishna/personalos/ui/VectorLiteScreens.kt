@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.krishna.personalos.ui

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Circle
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.graphics.drawable.toBitmap
import com.krishna.personalos.core.database.Priority
import com.krishna.personalos.core.database.ProjectStatus
import com.krishna.personalos.core.database.TaskEntity
import com.krishna.personalos.core.database.TaskStatus
import com.krishna.personalos.core.launcher.InstalledApp
import com.krishna.personalos.core.launcher.LauncherListItem
import com.krishna.personalos.core.launcher.buildLauncherListModel
import com.krishna.personalos.core.model.WorkspaceSnapshot
import com.krishna.personalos.core.repository.CaptureKind
import com.krishna.personalos.ui.theme.Amber
import com.krishna.personalos.ui.theme.Cyan
import com.krishna.personalos.ui.theme.Divider
import com.krishna.personalos.ui.theme.Glass
import com.krishna.personalos.ui.theme.Ice
import com.krishna.personalos.ui.theme.DeepNavy
import com.krishna.personalos.ui.theme.SurfaceNavy
import com.krishna.personalos.ui.theme.Ink
import com.krishna.personalos.ui.theme.Mint
import com.krishna.personalos.ui.theme.Muted
import com.krishna.personalos.ui.theme.RaisedNavy
import com.krishna.personalos.ui.theme.Violet
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle as JavaTextStyle
import java.util.Locale

@Composable
internal fun LiteHomeScreen(
    workspace: WorkspaceSnapshot,
    onToggleTask: (String) -> Unit,
    onOpenPlan: () -> Unit,
    onOpenSettings: () -> Unit,
    insets: PaddingValues,
) {
    val today = remember { LocalDate.now().toEpochDay() }
    val date = remember { LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.getDefault())) }
    val greeting = remember {
        when (LocalTime.now().hour) {
            in 5..11 -> "Good morning"
            in 12..16 -> "Good afternoon"
            else -> "Good evening"
        }
    }
    val tasks = workspace.todayTasks(today)
    val focus = tasks.firstOrNull { it.status != TaskStatus.DONE }
    val done = tasks.count { it.status == TaskStatus.DONE }

    Column(
        Modifier
            .fillMaxSize()
            .padding(insets)
            .statusBarsPadding()
            .padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 12.dp),
    ) {
        VectorHeader(date, greeting) {
            FrostedIconButton(Icons.Rounded.Settings, "Open settings", onOpenSettings)
        }
        Spacer(Modifier.height(8.dp))
        VectorOrb(Modifier.fillMaxWidth().height(284.dp))
        Spacer(Modifier.height(4.dp))
        TodayCard(
            focus = focus,
            completed = done,
            total = tasks.size,
            onComplete = { focus?.let { onToggleTask(it.id) } },
            onOpenPlan = onOpenPlan,
        )
    }
}

@Composable
private fun TodayCard(
    focus: TaskEntity?,
    completed: Int,
    total: Int,
    onComplete: () -> Unit,
    onOpenPlan: () -> Unit,
) {
    AmbientCard(
        modifier = Modifier.fillMaxWidth(),
        colors = listOf(Color(0xFF202735), Color(0xFF151B24)),
    ) {
        Column(Modifier.padding(21.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Today", color = Ice, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                Surface(shape = CircleShape, color = Color.White.copy(alpha = .07f)) {
                    Text(
                        text = if (total == 0) "Clear" else "$completed / $total",
                        color = if (total > 0 && completed == total) Mint else Muted,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
                    )
                }
            }
            Spacer(Modifier.height(15.dp))
            ProgressRing(if (total == 0) 0f else completed.toFloat() / total,
                if (total > 0 && completed == total) Mint else Cyan, "Today's task progress")
            Spacer(Modifier.height(18.dp))
            if (focus == null) {
                Text("Your day is open.", color = Ice, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text("Nothing else is asking for your attention.", color = Muted, style = MaterialTheme.typography.bodyMedium)
            } else {
                Text("Up next", color = Cyan, style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(6.dp))
                Text(focus.title, color = Ice, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(4.dp))
                Text("${focus.priority.name.lowercase().replaceFirstChar { it.uppercase() }} priority", color = Muted, style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (focus != null) {
                    CompactAction("Complete", Icons.Rounded.Check, Modifier.weight(1f), prominent = true, onClick = onComplete)
                }
                CompactAction("Open plan", Icons.AutoMirrored.Rounded.ArrowForward, Modifier.weight(1f), onClick = onOpenPlan)
            }
        }
    }
}

private enum class PlanView(val label: String) { TODAY("Today"), INBOX("Inbox"), LATER("Later") }

@Composable
internal fun LitePlanScreen(
    workspace: WorkspaceSnapshot,
    onToggleTask: (String) -> Unit,
    onScheduleToday: (String) -> Unit,
    insets: PaddingValues,
) {
    val today = remember { LocalDate.now().toEpochDay() }
    val todayTasks = workspace.todayTasks(today)
    val inbox = workspace.tasks.filter { it.status == TaskStatus.INBOX }
    val later = workspace.tasks.filter { it.status == TaskStatus.UPCOMING && it.dueEpochDay != today }
    var selectedName by rememberSaveable { mutableStateOf(PlanView.TODAY.name) }
    var showCompleted by rememberSaveable { mutableStateOf(false) }
    val selected = PlanView.valueOf(selectedName)
    val completed = todayTasks.filter { it.status == TaskStatus.DONE }
    val visibleTasks = when (selected) {
        PlanView.TODAY -> todayTasks.filter { it.status != TaskStatus.DONE } + if (showCompleted) completed else emptyList()
        PlanView.INBOX -> inbox
        PlanView.LATER -> later
    }

    Column(Modifier.fillMaxSize().padding(insets).padding(top = 14.dp)) {
        VectorHeader("Make room for what matters", "Plan", Modifier.padding(horizontal = 20.dp))
        Spacer(Modifier.height(16.dp))
        WeekStrip(Modifier.padding(horizontal = 14.dp))
        Spacer(Modifier.height(17.dp))
        PlanOverview(todayTasks, Modifier.padding(horizontal = 20.dp))
        Spacer(Modifier.height(17.dp))
        SegmentedSelector(
            labels = PlanView.entries.map { it.label },
            selectedIndex = selected.ordinal,
            onSelect = { selectedName = PlanView.entries[it].name },
            modifier = Modifier.padding(horizontal = 20.dp),
        )
        LazyColumn(
            Modifier.fillMaxWidth().weight(1f),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (visibleTasks.isEmpty()) {
                item { EmptyState(when (selected) {
                    PlanView.TODAY -> "Today is clear. Add something only if it matters."
                    PlanView.INBOX -> "Your inbox is empty."
                    PlanView.LATER -> "Nothing is waiting later."
                }) }
            } else {
                items(visibleTasks, key = { it.id }) { task ->
                    PlanTaskCard(
                        task = task,
                        onClick = { if (selected == PlanView.TODAY) onToggleTask(task.id) else onScheduleToday(task.id) },
                    )
                }
            }
            if (selected == PlanView.TODAY && completed.isNotEmpty()) {
                item {
                    CompletedDisclosure(count = completed.size, expanded = showCompleted) { showCompleted = !showCompleted }
                }
            }
        }
    }
}

@Composable
private fun WeekStrip(modifier: Modifier = Modifier) {
    val today = remember { LocalDate.now() }
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        (-3L..3L).forEach { offset ->
            val day = today.plusDays(offset)
            val current = offset == 0L
            Column(
                Modifier
                    .width(44.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(if (current) Cyan.copy(alpha = .10f) else Color.Transparent)
                    .padding(vertical = 9.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    day.dayOfWeek.getDisplayName(JavaTextStyle.SHORT, Locale.getDefault()).take(1),
                    color = if (current) Cyan else Muted,
                    style = MaterialTheme.typography.labelMedium,
                )
                Spacer(Modifier.height(5.dp))
                Text(day.dayOfMonth.toString(), color = if (current) Ice else Muted, style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
private fun PlanOverview(tasks: List<TaskEntity>, modifier: Modifier = Modifier) {
    val open = tasks.count { it.status != TaskStatus.DONE }
    val done = tasks.count { it.status == TaskStatus.DONE }
    AmbientCard(modifier.fillMaxWidth(), colors = listOf(Color(0xFF202438), Color(0xFF191E29))) {
        Row(Modifier.padding(horizontal = 20.dp, vertical = 17.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(if (open == 0) "A quiet day" else "$open ${if (open == 1) "thing" else "things"} left", color = Ice, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text("$done completed", color = Muted, style = MaterialTheme.typography.bodyMedium)
            }
            Box(Modifier.size(46.dp).background(Violet.copy(alpha = .15f), CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Bolt, null, tint = Violet, modifier = Modifier.size(21.dp))
            }
        }
    }
}

@Composable
private fun PlanTaskCard(task: TaskEntity, onClick: () -> Unit) {
    val done = task.status == TaskStatus.DONE
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onClick),
        color = Color.Transparent,
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(42.dp).background(priorityColor(task.priority).copy(alpha = .12f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(if (done) Icons.Rounded.CheckCircle else Icons.Rounded.Circle, null, tint = if (done) Mint else priorityColor(task.priority), modifier = Modifier.size(21.dp))
            }
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    task.title,
                    color = if (done) Muted else Ice,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textDecoration = if (done) TextDecoration.LineThrough else null,
                )
                Spacer(Modifier.height(4.dp))
                Text("${task.priority.name.lowercase().replaceFirstChar { it.uppercase() }} priority", color = Muted, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun CompletedDisclosure(count: Int, expanded: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(52.dp).clickable(role = Role.Button, onClick = onClick).padding(horizontal = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.DoneAll, null, tint = Mint, modifier = Modifier.size(19.dp))
        Spacer(Modifier.width(10.dp))
        Text("$count completed", color = Muted, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Icon(if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, null, tint = Muted, modifier = Modifier.size(20.dp))
    }
}

@Composable
internal fun LiteAppsScreen(
    apps: List<InstalledApp>,
    favoritePackages: List<String>,
    onRefresh: (Boolean) -> Unit,
    onLaunchApp: (String) -> Unit,
    onOpenAppInfo: (String) -> Unit,
    onToggleFavorite: (String) -> Unit,
    insets: PaddingValues,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val model = remember(apps, query) { buildLauncherListModel(apps, query) }
    val listState = remember { LazyListState() }
    val scope = rememberCoroutineScope()
    val currentLetter by remember(model, listState) {
        derivedStateOf {
            model.items
                .take(listState.firstVisibleItemIndex + 1)
                .filterIsInstance<LauncherListItem.Header>()
                .lastOrNull()?.letter
        }
    }
    val favorites = remember(apps, favoritePackages) { apps.filter { it.packageName in favoritePackages }.take(8) }

    Column(Modifier.fillMaxSize().padding(insets).padding(top = 14.dp)) {
        VectorHeader("${apps.size} installed", "Apps", Modifier.padding(horizontal = 20.dp)) {
            FrostedIconButton(Icons.Rounded.Refresh, "Refresh apps") { onRefresh(true) }
        }
        Spacer(Modifier.height(16.dp))
        SearchField(query, apps.size, onQueryChange = { query = it }, Modifier.padding(horizontal = 20.dp))
        if (favorites.isNotEmpty() && query.isBlank()) {
            Spacer(Modifier.height(15.dp))
            FavoriteDock(favorites, onLaunchApp, Modifier.padding(horizontal = 20.dp))
        }
        Spacer(Modifier.height(8.dp))
        Box(Modifier.fillMaxWidth().weight(1f)) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 20.dp, end = 55.dp, top = 2.dp, bottom = 24.dp),
            ) {
                if (model.items.isEmpty()) item { EmptyState("No apps match “$query”.") }
                itemsIndexed(
                    items = model.items,
                    key = { index, item -> when (item) {
                        is LauncherListItem.Header -> "header-${item.letter}-$index"
                        is LauncherListItem.App -> item.value.packageName
                    } },
                ) { _, item ->
                    when (item) {
                        is LauncherListItem.Header -> AppLetter(item.letter)
                        is LauncherListItem.App -> AppIndexRow(
                            app = item.value,
                            favorite = item.value.packageName in favoritePackages,
                            onLaunch = { onLaunchApp(item.value.packageName) },
                            onInfo = { onOpenAppInfo(item.value.packageName) },
                            onFavorite = { onToggleFavorite(item.value.packageName) },
                        )
                    }
                }
            }
            AlphabetRail(
                letters = model.sectionIndices.keys.toList(),
                selected = currentLetter,
                modifier = Modifier.align(Alignment.CenterEnd).padding(end = 9.dp),
                onSelect = { letter -> model.sectionIndices[letter]?.let { index -> scope.launch { listState.scrollToItem(index) } } },
            )
        }
    }
}

@Composable
private fun SearchField(query: String, count: Int, onQueryChange: (String) -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth().height(56.dp),
        shape = RoundedCornerShape(22.dp),
        color = SurfaceNavy,
        border = BorderStroke(1.dp, Color.White.copy(alpha = .07f)),
    ) {
        Row(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Search, null, tint = Cyan, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = TextStyle(color = Ice, fontFamily = MaterialTheme.typography.bodyLarge.fontFamily, fontSize = 16.sp),
                modifier = Modifier.weight(1f),
                decorationBox = { inner -> if (query.isBlank()) Text("Search $count apps", color = Muted, style = MaterialTheme.typography.bodyMedium) else inner() },
            )
        }
    }
}

@Composable
private fun FavoriteDock(apps: List<InstalledApp>, onLaunch: (String) -> Unit, modifier: Modifier = Modifier) {
    LazyRow(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(11.dp)) {
        items(apps, key = { it.packageName }) { app ->
            Column(
                Modifier.width(58.dp).clickable(role = Role.Button) { onLaunch(app.packageName) },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                AppIcon(app, 48)
                Spacer(Modifier.height(5.dp))
                Text(app.label, color = Muted, style = MaterialTheme.typography.labelMedium, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun AppLetter(letter: Char) {
    Text(
        letter.toString(),
        color = Cyan,
        style = MaterialTheme.typography.labelLarge,
        modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 4.dp, start = 2.dp),
    )
}

@Composable
private fun AppIndexRow(app: InstalledApp, favorite: Boolean, onLaunch: () -> Unit, onInfo: () -> Unit, onFavorite: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(68.dp).combinedClickable(role = Role.Button, onClick = onLaunch, onLongClick = onInfo),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIcon(app, 46)
        Spacer(Modifier.width(14.dp))
        Text(app.label, color = Ice, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        Icon(
            if (favorite) Icons.Rounded.Star else Icons.Rounded.StarBorder,
            contentDescription = if (favorite) "Unfavorite ${app.label}" else "Favorite ${app.label}",
            tint = if (favorite) Amber else Muted.copy(alpha = .28f),
            modifier = Modifier.size(48.dp).clickable(role = Role.Button, onClick = onFavorite).padding(14.dp),
        )
    }
}

@Composable
private fun AlphabetRail(letters: List<Char>, selected: Char?, modifier: Modifier = Modifier, onSelect: (Char) -> Unit) {
    if (letters.isEmpty()) return
    var railHeightPx by remember { mutableIntStateOf(1) }
    Surface(
        modifier = modifier
            .width(44.dp)
            .fillMaxHeight()
            .onSizeChanged { railHeightPx = it.height.coerceAtLeast(1) }
            .pointerInput(letters, railHeightPx) {
                var previousLetter: Char? = null
                fun selectAt(y: Float) {
                    val fraction = (y / railHeightPx).coerceIn(0f, 0.9999f)
                    val letter = letters[(fraction * letters.size).toInt()]
                    if (letter != previousLetter) {
                        previousLetter = letter
                        onSelect(letter)
                    }
                }
                detectDragGestures(
                    onDragStart = { selectAt(it.y) },
                    onDragEnd = { previousLetter = null },
                    onDragCancel = { previousLetter = null },
                ) { change, _ ->
                    change.consume()
                    selectAt(change.position.y)
                }
            },
        shape = RoundedCornerShape(22.dp),
        color = Color(0xB219202A),
        border = BorderStroke(1.dp, Divider.copy(alpha = .5f)),
    ) {
        Column(
            Modifier.fillMaxHeight().padding(vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly,
        ) {
            letters.forEach { letter ->
                Box(
                    Modifier
                        .width(42.dp)
                        .weight(1f)
                        .clip(CircleShape)
                        .background(if (letter == selected) Cyan.copy(alpha = .16f) else Color.Transparent)
                        .clickable(role = Role.Button) { onSelect(letter) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(letter.toString(), color = if (letter == selected) Cyan else Muted, style = MaterialTheme.typography.labelMedium, fontSize = 9.sp)
                }
            }
        }
    }
}

@Composable
private fun AppIcon(app: InstalledApp, size: Int) {
    val bitmap = remember(app.packageName, app.icon) { app.icon?.toBitmap(size * 2, size * 2)?.asImageBitmap() }
    if (bitmap != null) {
        Image(bitmap, contentDescription = null, modifier = Modifier.size(size.dp).clip(RoundedCornerShape((size * .28f).dp)))
    } else {
        Box(Modifier.size(size.dp).background(Cyan.copy(alpha = .12f), RoundedCornerShape((size * .28f).dp)), contentAlignment = Alignment.Center) {
            Text(app.label.take(1).uppercase(), color = Cyan, style = MaterialTheme.typography.titleMedium)
        }
    }
}

private enum class LibraryView(val label: String) { NOTES("Notes"), PROJECTS("Projects"), GOALS("Goals") }

@Composable
internal fun LiteLibraryScreen(workspace: WorkspaceSnapshot, insets: PaddingValues) {
    var selectedName by rememberSaveable { mutableStateOf(LibraryView.NOTES.name) }
    val selected = LibraryView.valueOf(selectedName)
    Column(Modifier.fillMaxSize().padding(insets).padding(top = 14.dp)) {
        VectorHeader("Everything worth keeping", "Library", Modifier.padding(horizontal = 20.dp))
        Spacer(Modifier.height(18.dp))
        SegmentedSelector(
            labels = LibraryView.entries.map { it.label },
            selectedIndex = selected.ordinal,
            onSelect = { selectedName = LibraryView.entries[it].name },
            modifier = Modifier.padding(horizontal = 20.dp),
        )
        Spacer(Modifier.height(4.dp))
        when (selected) {
            LibraryView.NOTES -> NotesCollection(workspace, Modifier.weight(1f))
            LibraryView.PROJECTS -> ProjectCollection(workspace, Modifier.weight(1f))
            LibraryView.GOALS -> GoalCollection(workspace, Modifier.weight(1f))
        }
    }
}

@Composable
private fun NotesCollection(workspace: WorkspaceSnapshot, modifier: Modifier) {
    LazyColumn(modifier, contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (workspace.notes.isEmpty()) item { EmptyState("Notes will collect here when you capture them.") }
        itemsIndexed(workspace.notes, key = { _, note -> note.id }) { index, note ->
            val accent = if (index % 2 == 0) Violet else Cyan
            AmbientCard(Modifier.fillMaxWidth(), listOf(accent.copy(alpha = .14f), Color(0xFF171C25))) {
                Column(Modifier.padding(20.dp)) {
                    Box(Modifier.size(38.dp).background(accent.copy(alpha = .14f), CircleShape), contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.Description, null, tint = accent, modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.height(17.dp))
                    Text(note.title, color = Ice, style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(7.dp))
                    Text(note.body, color = Muted, style = MaterialTheme.typography.bodyMedium, maxLines = 4, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
private fun ProjectCollection(workspace: WorkspaceSnapshot, modifier: Modifier) {
    LazyColumn(modifier, contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (workspace.projects.isEmpty()) item { EmptyState("Projects will live here without crowding your day.") }
        itemsIndexed(workspace.projects, key = { _, project -> project.id }) { index, project ->
            val accent = listOf(Cyan, Violet, Mint)[index % 3]
            AmbientCard(Modifier.fillMaxWidth(), listOf(accent.copy(alpha = .13f), Color(0xFF171C25))) {
                Column(Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(42.dp).background(accent.copy(alpha = .14f), CircleShape), contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.Folder, null, tint = accent, modifier = Modifier.size(20.dp))
                        }
                        Spacer(Modifier.width(13.dp))
                        Column(Modifier.weight(1f)) {
                            Text(project.area, color = Muted, style = MaterialTheme.typography.labelMedium)
                            Text(project.title, color = Ice, style = MaterialTheme.typography.titleMedium, maxLines = 2)
                        }
                    }
                    Spacer(Modifier.height(17.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(7.dp).background(if (project.status == ProjectStatus.ACTIVE) Mint else Muted, CircleShape))
                        Spacer(Modifier.width(8.dp))
                        Text(project.status.name.lowercase().replaceFirstChar { it.uppercase() }, color = Muted, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun GoalCollection(workspace: WorkspaceSnapshot, modifier: Modifier) {
    LazyColumn(modifier, contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (workspace.goals.isEmpty()) item { EmptyState("Goals will appear here when you are ready to define them.") }
        itemsIndexed(workspace.goals, key = { _, goal -> goal.id }) { index, goal ->
            val accent = if (index % 2 == 0) Violet else Mint
            AmbientCard(Modifier.fillMaxWidth(), listOf(accent.copy(alpha = .13f), Color(0xFF171C25))) {
                Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    GoalRing(goal.progress, accent)
                    Spacer(Modifier.width(17.dp))
                    Column(Modifier.weight(1f)) {
                        Text(goal.horizon, color = accent, style = MaterialTheme.typography.labelMedium)
                        Spacer(Modifier.height(5.dp))
                        Text(goal.title, color = Ice, style = MaterialTheme.typography.titleMedium, maxLines = 3, overflow = TextOverflow.Ellipsis)
                    }
                    Icon(Icons.Rounded.Lightbulb, null, tint = accent.copy(alpha = .55f), modifier = Modifier.size(22.dp))
                }
            }
        }
    }
}

@Composable
private fun GoalRing(progress: Int, color: Color) {
    ProgressRing(progress.coerceIn(0, 100) / 100f, color, "Goal progress")
}

@Composable
internal fun LiteSettingsScreen(
    workspace: WorkspaceSnapshot,
    appCount: Int,
    onRefreshApps: () -> Unit,
    onBack: () -> Unit,
    insets: PaddingValues,
) {
    val context = LocalContext.current
    var isDefault by remember { mutableStateOf(isVectorHome(context)) }
    val homeLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { isDefault = isVectorHome(context) }
    val records = workspace.tasks.size + workspace.projects.size + workspace.goals.size + workspace.notes.size
    Column(Modifier.fillMaxSize().padding(insets).padding(horizontal = 20.dp, vertical = 14.dp)) {
        VectorHeader("Shape your personal space", "Settings") {
            FrostedIconButton(Icons.AutoMirrored.Rounded.ArrowBack, "Back", onBack)
        }
        Spacer(Modifier.height(24.dp))
        AmbientCard(Modifier.fillMaxWidth(), listOf(Violet.copy(alpha = .16f), Color(0xFF171C25))) {
            Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(52.dp).background(Brush.linearGradient(listOf(Cyan, Cyan)), CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Home, null, tint = Ink, modifier = Modifier.size(24.dp))
                }
                Spacer(Modifier.width(15.dp))
                Column {
                    Text("VECTOR", color = Ice, style = MaterialTheme.typography.titleLarge)
                    Text(if (isDefault) "Active as your Home app" else "Ready to become your Home app", color = Muted, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        SoftPanel(Modifier.fillMaxWidth()) {
            Column {
                SettingsRow(Icons.Rounded.Home, "Default home", if (isDefault) "Active" else "Choose VECTOR") {
                    if (!isDefault) homeLauncher.launch(homeRoleIntent(context))
                }
                SettingsDivider()
                SettingsRow(Icons.Rounded.Apps, "App library", "$appCount installed", onRefreshApps)
                SettingsDivider()
                SettingsRow(Icons.Rounded.Storage, "Local workspace", "$records records on this device", null)
                SettingsDivider()
                SettingsRow(Icons.Rounded.Bolt, "Power", "Static ambience · no continuous scanner", null)
            }
        }
        Spacer(Modifier.weight(1f))
        Text("Version 0.1 · Local first", color = Muted.copy(alpha = .65f), style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(bottom = 6.dp))
    }
}

@Composable
private fun SettingsRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, detail: String, onClick: (() -> Unit)?) {
    val interaction = if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier
    Row(interaction.fillMaxWidth().padding(horizontal = 18.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = Cyan, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = Ice, style = MaterialTheme.typography.titleMedium)
            Text(detail, color = Muted, style = MaterialTheme.typography.labelMedium)
        }
        if (onClick != null) Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, tint = Muted, modifier = Modifier.size(18.dp))
    }
}

@Composable
internal fun LiteCaptureDialog(onDismiss: () -> Unit, onCapture: (CaptureKind, String) -> Unit) {
    var kindName by rememberSaveable { mutableStateOf(CaptureKind.TASK.name) }
    var title by rememberSaveable { mutableStateOf("") }
    val kind = CaptureKind.valueOf(kindName)
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Box(Modifier.fillMaxSize().background(Ink.copy(alpha = .72f)).imePadding().padding(12.dp), contentAlignment = Alignment.BottomCenter) {
            Surface(
                modifier = Modifier.fillMaxWidth().testTag("capture-sheet"),
                shape = RoundedCornerShape(16.dp),
                color = DeepNavy,
                border = BorderStroke(1.dp, Divider),
                shadowElevation = 18.dp,
            ) {
                Column(Modifier.padding(22.dp)) {
                    Text("Capture", color = Ice, style = MaterialTheme.typography.headlineMedium)
                    Spacer(Modifier.height(5.dp))
                    Text("Put it somewhere safe, then return to your day.", color = Muted, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(20.dp))
                    SegmentedSelector(
                        labels = CaptureKind.entries.filter { it != CaptureKind.HABIT }.map { it.label },
                        selectedIndex = CaptureKind.entries.filter { it != CaptureKind.HABIT }.indexOf(kind),
                        onSelect = { kindName = CaptureKind.entries.filter { it != CaptureKind.HABIT }[it].name },
                    )
                    Spacer(Modifier.height(18.dp))
                    Surface(shape = RoundedCornerShape(22.dp), color = Divider.copy(alpha = .5f)) {
                        BasicTextField(
                            value = title,
                            onValueChange = { title = it },
                            textStyle = TextStyle(color = Ice, fontFamily = MaterialTheme.typography.bodyLarge.fontFamily, fontSize = 17.sp, lineHeight = 24.sp),
                            modifier = Modifier.fillMaxWidth().height(112.dp).padding(17.dp),
                            decorationBox = { inner -> Box { if (title.isBlank()) Text("What do you want to remember?", color = Muted, style = MaterialTheme.typography.bodyLarge); inner() } },
                        )
                    }
                    Spacer(Modifier.height(14.dp))
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Brush.linearGradient(if (title.isBlank()) listOf(RaisedNavy, RaisedNavy) else listOf(Cyan, Cyan)))
                            .clickable(enabled = title.isNotBlank(), role = Role.Button) { onCapture(kind, title.trim()) },
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Rounded.Add, null, tint = if (title.isBlank()) Muted else Ink, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Save ${kind.label.lowercase()}", color = if (title.isBlank()) Muted else Ink, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}

@Composable
private fun SegmentedSelector(labels: List<String>, selectedIndex: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth().height(48.dp),
        shape = RoundedCornerShape(20.dp),
        color = SurfaceNavy,
        border = BorderStroke(1.dp, Divider.copy(alpha = .5f)),
    ) {
        Row(Modifier.fillMaxSize().padding(4.dp)) {
            labels.forEachIndexed { index, label ->
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (selectedIndex == index) Cyan.copy(alpha = .12f) else Color.Transparent)
                        .clickable(role = Role.Tab) { onSelect(index) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(label, color = if (selectedIndex == index) Ice else Muted, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
private fun FrostedIconButton(icon: androidx.compose.ui.graphics.vector.ImageVector, description: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.size(48.dp).clickable(role = Role.Button, onClick = onClick),
        shape = CircleShape,
        color = SurfaceNavy,
        border = BorderStroke(1.dp, SurfaceNavy),
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = description, tint = Ice.copy(alpha = .9f), modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun CompactAction(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    prominent: Boolean = false,
    onClick: () -> Unit,
) {
    val background = if (prominent) Brush.linearGradient(listOf(Cyan, Cyan)) else Brush.linearGradient(listOf(SurfaceNavy, SurfaceNavy))
    Row(
        modifier.height(46.dp).clip(RoundedCornerShape(17.dp)).background(background).clickable(role = Role.Button, onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = if (prominent) Ink else Ice, modifier = Modifier.size(17.dp))
        Spacer(Modifier.width(7.dp))
        Text(label, color = if (prominent) Ink else Ice, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun AmbientCard(
    modifier: Modifier = Modifier,
    colors: List<Color>,
    content: @Composable () -> Unit,
) {
    @Suppress("UNUSED_VARIABLE") val retainedPalette = colors
    Box(modifier) {
        content()
    }
}

@Composable
private fun EmptyState(text: String) {
    Column(Modifier.fillMaxWidth().padding(vertical = 36.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(44.dp).background(SurfaceNavy, CircleShape), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.CheckCircle, null, tint = Mint.copy(alpha = .8f), modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.height(12.dp))
        Text(text, color = Muted, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun SettingsDivider() {
    Box(Modifier.fillMaxWidth().padding(horizontal = 18.dp).height(1.dp).background(Divider.copy(alpha = .62f)))
}

private fun priorityColor(priority: Priority): Color = when (priority) {
    Priority.HIGH -> Amber
    Priority.MEDIUM -> Cyan
    Priority.LOW -> Mint
}

private fun homeRoleIntent(context: Context): Intent {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val manager = context.getSystemService(RoleManager::class.java)
        if (manager?.isRoleAvailable(RoleManager.ROLE_HOME) == true) return manager.createRequestRoleIntent(RoleManager.ROLE_HOME)
    }
    return Intent(Settings.ACTION_HOME_SETTINGS)
}

@Suppress("DEPRECATION")
private fun isVectorHome(context: Context): Boolean {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val manager = context.getSystemService(RoleManager::class.java)
        if (manager?.isRoleAvailable(RoleManager.ROLE_HOME) == true) return manager.isRoleHeld(RoleManager.ROLE_HOME)
    }
    val homeIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
    return context.packageManager.resolveActivity(homeIntent, 0)?.activityInfo?.packageName == context.packageName
}
