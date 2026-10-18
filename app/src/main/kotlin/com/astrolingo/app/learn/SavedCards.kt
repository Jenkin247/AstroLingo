// Bookmarks: the button on every teaching card, and the Saved list on the Learn tab (PO request, 17 Oct 2026).
package com.astrolingo.app.learn

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.astrolingo.app.R
import com.astrolingo.app.ui.sitara.SitaraSays
import com.astrolingo.app.ui.theme.AstroLingoTheme
import com.astrolingo.app.ui.theme.Spacing
import com.astrolingo.learn.content.CardLocation
import com.astrolingo.learn.content.Course
import com.astrolingo.learn.content.Language
import com.astrolingo.learn.content.LearnCard
import com.astrolingo.learn.content.YourChartCard
import com.astrolingo.learn.progress.LearnProgress

/** The bookmark on a card. It pops when tapped (a plain change with animations off). */
@Composable
internal fun BookmarkButton(saved: Boolean, onToggle: () -> Unit) {
    val reduceMotion = rememberReduceMotion()
    val scale = remember { Animatable(1f) }
    // No pop on first show, only when the learner taps.
    val first = remember { booleanArrayOf(true) }
    LaunchedEffect(saved) {
        if (first[0]) {
            first[0] = false
        } else if (!reduceMotion) {
            scale.snapTo(0.6f)
            scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium))
        }
    }
    IconButton(onClick = onToggle) {
        Icon(
            painterResource(if (saved) R.drawable.ic_bookmark else R.drawable.ic_bookmark_border),
            contentDescription = stringResource(if (saved) R.string.learn_unsave_card else R.string.learn_save_card),
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
            },
        )
    }
}

/** The "Saved" button in the Learn header. */
@Composable
internal fun SavedButton(onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = Modifier.heightIn(min = Spacing.touchTarget)) {
        Icon(painterResource(R.drawable.ic_bookmark), contentDescription = null)
        Text(stringResource(R.string.learn_saved), modifier = Modifier.padding(start = Spacing.xs))
    }
}

/**
 * Saved cards, newest first. Tapping one opens its lesson at that card; the bookmark removes it.
 * Cards no longer in the course (after a content change) are left out.
 */
@Composable
internal fun SavedCards(
    course: Course,
    progress: LearnProgress,
    language: Language,
    facts: ChartFacts,
    onOpen: (CardLocation) -> Unit,
    onToggle: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val saved = progress.bookmarks.mapNotNull { course.locate(it.cardId) }
    Column(modifier.fillMaxSize()) {
        Row(Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.sm), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.learn_back)) }
            Text(
                stringResource(R.string.learn_saved_title),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.semantics { heading() },
            )
        }
        if (saved.isEmpty()) {
            SitaraSays(stringResource(R.string.learn_saved_empty), Modifier.padding(Spacing.lg))
            return@Column
        }
        LazyColumn(
            contentPadding = PaddingValues(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            items(saved, key = { it.card.id }) { location ->
                SavedRow(location, language, facts, onOpen = { onOpen(location) }, onRemove = { onToggle(location.card.id) })
            }
        }
    }
}

@Composable
private fun SavedRow(location: CardLocation, language: Language, facts: ChartFacts, onOpen: () -> Unit, onRemove: () -> Unit) {
    val (title, body) = when (val card = location.card) {
        is LearnCard -> card.title.get(language) to card.body.get(language)
        is YourChartCard -> card.title.get(language) to card.text(language, facts.valueOf(card.fact))
        else -> return
    }
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = AstroLingoTheme.tabColors.learn.copy(alpha = 0.3f),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onOpen),
    ) {
        Row(Modifier.padding(start = Spacing.md, top = Spacing.sm, bottom = Spacing.sm), verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    "${location.chapter.title.get(language)} · ${location.lesson.title.get(language)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(body, style = MaterialTheme.typography.bodyMedium, maxLines = 3, overflow = TextOverflow.Ellipsis)
            }
            BookmarkButton(saved = true, onToggle = onRemove)
        }
    }
}
