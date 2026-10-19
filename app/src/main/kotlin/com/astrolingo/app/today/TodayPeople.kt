// Today for a saved person (LS-10): chips to choose whose day Today shows, and the banner that says it isn't yours.
package com.astrolingo.app.today

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.astrolingo.app.R
import com.astrolingo.app.ui.components.InfoCard
import com.astrolingo.app.ui.theme.AstroLingoTheme
import com.astrolingo.app.ui.theme.Spacing

/**
 * "You · Asha · Ravi": whose day Today shows. With nobody saved, a link to add someone instead ([onAddPerson]; null
 * hides it, as when Me is full).
 */
@Composable
internal fun PersonChips(
    people: List<DayPerson>,
    viewing: DayPerson?,
    onView: (DayPerson?) -> Unit,
    onAddPerson: (() -> Unit)?,
) {
    if (people.isEmpty()) {
        if (onAddPerson != null) {
            TextButton(onClick = onAddPerson, modifier = Modifier.heightIn(min = Spacing.touchTarget)) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(Spacing.xs))
                Text(stringResource(R.string.today_see_friend))
            }
        }
        return
    }
    Row(
        Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        FilterChip(
            selected = viewing == null,
            onClick = { onView(null) },
            label = { Text(stringResource(R.string.people_you)) },
            leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null, modifier = Modifier.size(18.dp)) },
        )
        people.forEach { person ->
            FilterChip(
                selected = viewing?.id == person.id,
                onClick = { onView(person) },
                label = { Text(person.name) },
            )
        }
    }
}

/** Says plainly whose day this is, what is theirs in it and what is the same for everyone. */
@Composable
internal fun PersonBanner(person: DayPerson, onBack: () -> Unit) {
    InfoCard(container = AstroLingoTheme.tabColors.me) {
        val ink = AstroLingoTheme.tabColors.onPastel
        Text(
            stringResource(R.string.today_person_banner, person.name),
            style = MaterialTheme.typography.bodyMedium,
            color = ink,
        )
        if (person.moonUncertain) {
            Text(
                stringResource(R.string.today_person_moon_uncertain, person.name),
                style = MaterialTheme.typography.bodyMedium,
                color = ink,
            )
        }
        TextButton(onClick = onBack, modifier = Modifier.heightIn(min = Spacing.touchTarget)) {
            Text(stringResource(R.string.today_person_back), color = ink)
        }
    }
}

/** "Asha's day" in place of the greeting, so the screen is never mistaken for the user's own. */
@Composable
internal fun PersonTitle(person: DayPerson) {
    Text(
        stringResource(R.string.today_person_day, person.name),
        style = MaterialTheme.typography.headlineMedium,
        modifier = Modifier.semantics { heading() },
    )
}
