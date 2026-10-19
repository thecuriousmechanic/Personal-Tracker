package com.krishna.personalos.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.krishna.personalos.ui.theme.Cyan
import com.krishna.personalos.ui.theme.Glass
import com.krishna.personalos.ui.theme.Ice
import com.krishna.personalos.ui.theme.Ink
import com.krishna.personalos.ui.theme.Muted
import com.krishna.personalos.ui.theme.SurfaceNavy
import com.krishna.personalos.ui.theme.Violet
import com.krishna.personalos.ui.theme.Paper
import com.krishna.personalos.ui.theme.SageWash
import com.krishna.personalos.ui.theme.Divider

internal enum class LiteDestination(val label: String, val icon: ImageVector) {
    HOME("Home", Icons.Rounded.Home),
    TRACKERS("Trackers", Icons.Rounded.Checklist),
    PLAN("Plan", Icons.Rounded.Checklist),
    LIBRARY("Library", Icons.AutoMirrored.Rounded.MenuBook),
    CAPTURE("Add", Icons.Rounded.Add),
    SETTINGS("Settings", Icons.Rounded.Settings),
}

@Composable
internal fun LiteBackground(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize().background(Ink)) {
        content()
    }
}

@Composable
internal fun VectorHeader(
    eyebrow: String,
    title: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(
                text = eyebrow,
                color = Muted,
                style = MaterialTheme.typography.labelMedium,
            )
            Spacer(Modifier.height(5.dp))
            Text(
                text = title,
                color = Ice,
                style = MaterialTheme.typography.headlineMedium,
                fontFamily = FontFamily.Serif,
                fontSize = 34.sp,
            )
        }
        action?.invoke()
    }
}

@Composable
internal fun SoftPanel(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val interaction = if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier
    Surface(
        modifier = modifier.then(interaction),
        color = Color.Transparent,
        content = content,
    )
}

@Composable
internal fun VectorOrb(modifier: Modifier = Modifier) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(286.dp)) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Cyan.copy(alpha = .18f), Violet.copy(alpha = .07f), Color.Transparent),
                    center = center,
                    radius = size.minDimension * .5f,
                ),
                radius = size.minDimension * .5f,
            )
        }
        Icon(Icons.Rounded.Checklist, "Personal-Tracker", tint = Cyan, modifier = Modifier.size(96.dp))
    }
}

@Composable
internal fun LiteBottomNavigation(selected: LiteDestination, onSelect: (LiteDestination) -> Unit) {
    val haptics = LocalHapticFeedback.current
    val destinations = listOf(LiteDestination.HOME, LiteDestination.PLAN, LiteDestination.LIBRARY, LiteDestination.SETTINGS)
    Column(Modifier.fillMaxWidth().background(Paper).navigationBarsPadding()) {
        HorizontalDivider(color = Divider)
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(
                modifier = Modifier.weight(1f).height(64.dp),
                color = Paper,
            ) {
                Row(Modifier.fillMaxSize().padding(horizontal = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                    destinations.forEach { item ->
                        val active = selected == item
                        Box(
                            Modifier
                                .weight(1f)
                                .height(52.dp)
                                .semantics { this.selected = active }
                                .clickable(role = Role.Tab) {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onSelect(item)
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    item.icon,
                                    contentDescription = null,
                                    tint = if (active) Cyan else Muted,
                                    modifier = Modifier.background(if (active) SageWash else Color.Transparent, RoundedCornerShape(10.dp)).padding(horizontal = 12.dp, vertical = 5.dp).size(19.dp),
                                )
                                Spacer(Modifier.height(3.dp))
                                Text(item.label, color = if (active) Cyan else Muted, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.width(8.dp))
            Box(
                Modifier
                    .size(48.dp)
                    .background(Cyan, RoundedCornerShape(12.dp))
                    .clickable(role = Role.Button) {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onSelect(LiteDestination.CAPTURE)
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "Add", tint = Ink, modifier = Modifier.size(24.dp))
            }
        }
    }
}
