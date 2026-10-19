// Whose kundli the Kundli tab shows: the user's by default, or a saved person's (PO, 19 Oct 2026).
package com.astrolingo.app.people

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.astrolingo.app.R
import com.astrolingo.app.learn.ChartPerson
import com.astrolingo.app.profile.SavedPerson
import com.astrolingo.app.ui.theme.Spacing

/**
 * A row of chips, one per chart, with the user's first. "Add a person" shows while there are places left.
 *
 * @param selectedId the id of the chart on screen.
 */
@Composable
fun KundliPicker(
    people: List<ChartPerson>,
    selectedId: String,
    onSelect: (ChartPerson) -> Unit,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Text(
            stringResource(R.string.kundli_pick),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            people.forEachIndexed { index, person ->
                FilterChip(
                    selected = person.id == selectedId,
                    onClick = { onSelect(person) },
                    label = { Text(person.name) },
                    leadingIcon = { Box(Modifier.size(12.dp).background(personColor(index), CircleShape)) },
                )
            }
            if (people.count { !it.isYou } < SavedPerson.MAX_OTHERS) {
                AssistChip(
                    onClick = onAdd,
                    label = { Text(stringResource(R.string.people_add)) },
                    leadingIcon = { Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp)) },
                )
            }
        }
    }
}
