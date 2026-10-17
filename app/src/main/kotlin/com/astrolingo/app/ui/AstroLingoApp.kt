// App shell: the four tabs (Today, Learn, Kundli, Me) with placeholder screens.
// Each feature replaces its placeholder in the sprint named on screen.
package com.astrolingo.app.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.astrolingo.app.R
import com.astrolingo.app.ui.theme.AstroLingoTheme

/** The four top-level destinations, in bottom-bar order. */
enum class Tab(@StringRes val label: Int, @StringRes val placeholder: Int, val icon: ImageVector) {
    TODAY(R.string.tab_today, R.string.placeholder_today, Icons.Filled.Home),
    LEARN(R.string.tab_learn, R.string.placeholder_learn, Icons.AutoMirrored.Filled.List),
    KUNDLI(R.string.tab_kundli, R.string.placeholder_kundli, Icons.Filled.Star),
    ME(R.string.tab_me, R.string.placeholder_me, Icons.Filled.Person),
}

/** Root composable: bottom navigation plus the selected tab's screen. */
@Composable
fun AstroLingoApp() {
    var selected by rememberSaveable { mutableStateOf(Tab.TODAY) }
    Scaffold(
        bottomBar = {
            NavigationBar {
                Tab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = tab == selected,
                        onClick = { selected = tab },
                        icon = { Icon(tab.icon, contentDescription = null) },
                        label = { Text(stringResource(tab.label)) },
                    )
                }
            }
        },
    ) { padding ->
        PlaceholderScreen(tab = selected, modifier = Modifier.padding(padding))
    }
}

/** Temporary screen for a tab, tinted with that tab's pastel colour. */
@Composable
private fun PlaceholderScreen(tab: Tab, modifier: Modifier = Modifier) {
    val colors = AstroLingoTheme.tabColors
    val tint: Color = when (tab) {
        Tab.TODAY -> colors.today
        Tab.LEARN -> colors.learn
        Tab.KUNDLI -> colors.kundli
        Tab.ME -> colors.me
    }
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth()
                .background(tint, RoundedCornerShape(20.dp))
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(tab.label),
                style = MaterialTheme.typography.headlineMedium,
                color = colors.onPastel,
            )
            Text(
                text = stringResource(R.string.tagline),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onPastel,
            )
            Text(
                text = stringResource(tab.placeholder),
                style = MaterialTheme.typography.bodyLarge,
                color = colors.onPastel,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AstroLingoAppPreview() {
    AstroLingoTheme { AstroLingoApp() }
}
