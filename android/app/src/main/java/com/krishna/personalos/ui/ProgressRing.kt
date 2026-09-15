package com.krishna.personalos.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import com.krishna.personalos.ui.theme.Divider
import com.krishna.personalos.ui.theme.Ice

/** Determinate completion ring. The label retains over-budget percentages. */
@Composable
internal fun ProgressRing(progress: Float, color: Color, label: String, modifier: Modifier = Modifier, valueText: String? = null, compact: Boolean = false) {
    val safe = if (progress.isFinite()) progress else 0f
    val fraction = safe.coerceIn(0f, 1f)
    val text = valueText ?: "${(safe * 100).toInt()}%"
    Row(modifier.clearAndSetSemantics {
        contentDescription = label
        stateDescription = text
        progressBarRangeInfo = ProgressBarRangeInfo(fraction, 0f..1f)
    }, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.size(if (compact) 22.dp else 64.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                val stroke = (if (compact) 3.dp else 5.dp).toPx()
                val origin = Offset(stroke / 2, stroke / 2)
                val extent = Size(size.width - stroke, size.height - stroke)
                drawArc(Divider, -90f, 360f, false, origin, extent, style = Stroke(stroke))
                if (fraction > 0f) drawArc(color, -90f, fraction * 360f, false, origin, extent, style = Stroke(stroke, cap = StrokeCap.Round))
            }
            if (!compact) Text(text, color = Ice, style = MaterialTheme.typography.labelSmall)
        }
        if (compact) Text(text, color = Ice, style = MaterialTheme.typography.bodySmall)
    }
}
