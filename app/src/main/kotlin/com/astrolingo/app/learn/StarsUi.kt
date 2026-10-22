// Stars on screen: the count on Learn home and the Stars a lesson earned. The name comes from R.string.stars_name only,
// so renaming Stars never touches code.
package com.astrolingo.app.learn

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import com.astrolingo.app.R
import com.astrolingo.app.ui.theme.Spacing
import com.astrolingo.learn.stars.Stars

/** "12 Stars", using the current name of Stars. */
@Composable
internal fun starsText(count: Int): String = stringResource(R.string.stars_count, count, stringResource(R.string.stars_name))

/** The learner's Stars on Learn home: the total, what today earned, and how to earn more. */
@Composable
internal fun StarsCard(stars: Stars, epochDay: Long, modifier: Modifier = Modifier) {
    val name = stringResource(R.string.stars_name)
    val today = stars.earnedOn(epochDay)
    val total = starsText(stars.total)
    val todayText = stringResource(R.string.stars_today, today)
    val hint = if (today >= Stars.DAILY_MAX) stringResource(R.string.stars_limit, name) else stringResource(R.string.stars_how, name)
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier
            .fillMaxWidth()
            .clearAndSetSemantics { contentDescription = "$total. $todayText. $hint" },
    ) {
        Row(
            Modifier.padding(Spacing.md),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(STAR_GLYPH, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
            Column(Modifier.weight(1f)) {
                Text(total, style = MaterialTheme.typography.titleMedium)
                if (today > 0) Text(todayText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(hint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/** "+4 Stars" on the lesson done card. */
@Composable
internal fun StarsEarned(count: Int, color: Color, modifier: Modifier = Modifier) {
    Text(
        "$STAR_GLYPH " + stringResource(R.string.stars_earned, count, stringResource(R.string.stars_name)),
        style = MaterialTheme.typography.titleMedium,
        color = color,
        textAlign = TextAlign.Center,
        modifier = modifier.fillMaxWidth(),
    )
}

// The plain star symbol with the text variation selector, so it takes the theme colour rather than an emoji.
private const val STAR_GLYPH = "★︎"
