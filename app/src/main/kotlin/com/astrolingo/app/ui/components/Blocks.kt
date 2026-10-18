// Shared building blocks for screens: a titled card and a label/value row. Learn and the other tabs reuse them,
// so every screen groups its information the same way.
package com.astrolingo.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.astrolingo.app.ui.theme.Spacing

/**
 * A rounded block that groups one kind of information, with an optional small heading.
 *
 * @param title short heading such as "Timings"; read by TalkBack as a heading.
 * @param icon decorative icon shown before the title.
 * @param container background; the plain surface by default, a tab pastel for the hero card.
 */
@Composable
fun InfoCard(
    modifier: Modifier = Modifier,
    title: String? = null,
    icon: ImageVector? = null,
    container: Color = MaterialTheme.colorScheme.surface,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = container,
        tonalElevation = 1.dp,
        shadowElevation = 1.dp,
    ) {
        Column(Modifier.padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            if (title != null) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    if (icon != null) {
                        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    }
                    Text(
                        title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.semantics { heading() },
                    )
                }
            }
            content()
        }
    }
}

/** One fact in a card: a muted label on the left, the value on the right, and an optional note under the value. */
@Composable
fun FactRow(label: String, value: String, note: String? = null) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Column(Modifier.weight(2f)) {
            Text(value, style = MaterialTheme.typography.bodyLarge)
            if (note != null) {
                Text(note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
