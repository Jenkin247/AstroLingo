// Search the offline city list: used for the birthplace in onboarding and for where the user is now on Today.
package com.astrolingo.app.ui.places

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.astrolingo.app.R
import com.astrolingo.app.ui.theme.Spacing
import com.astrolingo.engine.places.City
import com.astrolingo.engine.places.CityIndex

/**
 * A search field with matching places below it. Picking one fills the field with its full label.
 *
 * @param index the city list, or null while it loads (the field is disabled until then).
 * @param selected the place picked so far, if any.
 */
@Composable
fun CitySearch(index: CityIndex?, selected: City?, onSelect: (City) -> Unit, modifier: Modifier = Modifier) {
    var query by rememberSaveable { mutableStateOf(selected?.label ?: "") }
    // A place picked from outside the field (Use my location) shows up in it too.
    LaunchedEffect(selected) { selected?.let { query = it.label } }
    val results = remember(query, index) { if (index == null || query == selected?.label) emptyList() else index.search(query) }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text(stringResource(R.string.onboarding_place_hint)) },
            supportingText = { Text(stringResource(R.string.onboarding_place_village)) },
            singleLine = true,
            enabled = index != null,
            modifier = Modifier.fillMaxWidth(),
        )
        if (index == null) Text(stringResource(R.string.onboarding_place_loading))
        if (query.length >= 2 && results.isEmpty() && query != selected?.label) {
            Text(stringResource(R.string.onboarding_place_none), style = MaterialTheme.typography.bodyMedium)
        }
        LazyColumn(Modifier.weight(1f, fill = false)) {
            items(results, key = { it.id }) { city ->
                Text(
                    city.label,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(city); query = city.label }
                        .heightIn(min = Spacing.touchTarget)
                        .padding(vertical = Spacing.sm),
                )
            }
        }
    }
}
