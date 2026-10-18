// The daily goal ring on Learn home: today's learning minutes against the goal chosen in onboarding (LS-4).
// It never scolds: a goal not met yet gets a nudge, a goal met gets Sitara.
package com.astrolingo.app.learn

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.astrolingo.app.R
import com.astrolingo.app.ui.sitara.SitaraAvatar
import com.astrolingo.app.ui.theme.Spacing
import com.astrolingo.learn.progress.GoalProgress

@Composable
internal fun GoalCard(goal: GoalProgress, modifier: Modifier = Modifier) {
    val animated by animateFloatAsState(goal.fraction, tween(GOAL_RING_MILLIS), label = "goal ring")
    val track = MaterialTheme.colorScheme.outlineVariant
    val fill = MaterialTheme.colorScheme.primary
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {},
    ) {
        Row(
            Modifier.padding(Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            Box(Modifier.size(GOAL_RING_SIZE), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) {
                    val stroke = 7.dp.toPx()
                    drawCircle(track, radius = (size.minDimension - stroke) / 2, style = Stroke(stroke))
                    drawArc(
                        fill,
                        startAngle = -90f,
                        sweepAngle = 360f * animated,
                        useCenter = false,
                        topLeft = Offset(stroke / 2, stroke / 2),
                        size = Size(size.width - stroke, size.height - stroke),
                        style = Stroke(stroke, cap = StrokeCap.Round),
                    )
                }
                if (goal.met) {
                    SitaraAvatar(size = 40.dp)
                } else {
                    Text("${goal.minutes}", style = MaterialTheme.typography.titleLarge)
                }
            }
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.learn_goal_title), style = MaterialTheme.typography.labelLarge)
                Text(
                    if (goal.met) {
                        stringResource(R.string.learn_goal_met)
                    } else {
                        stringResource(R.string.learn_goal_progress, goal.minutes, goal.goalMinutes)
                    },
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    stringResource(if (goal.met) R.string.learn_goal_met_body else R.string.learn_goal_nudge),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private val GOAL_RING_SIZE = 64.dp
private const val GOAL_RING_MILLIS = 600
