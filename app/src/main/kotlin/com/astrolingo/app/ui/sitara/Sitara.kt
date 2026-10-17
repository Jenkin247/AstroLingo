// Sitara, AstroLingo's guide: first look (sprint 1). A static illustration until the Lottie loops arrive (ADR 0007).
package com.astrolingo.app.ui.sitara

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.astrolingo.app.R
import com.astrolingo.app.ui.theme.AstroLingoTheme
import com.astrolingo.app.ui.theme.Spacing

/**
 * Sitara's portrait.
 *
 * @param size rendered size; the art is designed at 120dp and stays crisp at any size.
 */
@Composable
fun SitaraAvatar(modifier: Modifier = Modifier, size: Dp = 120.dp) {
    Image(
        painter = painterResource(R.drawable.sitara),
        contentDescription = stringResource(R.string.sitara_description),
        modifier = modifier.size(size),
    )
}

/**
 * Sitara saying one line, as on Lesson complete and the "Why?" sheet.
 *
 * @param text what Sitara says: warm, short, one idea per sentence (see the voice guide).
 */
@Composable
fun SitaraSays(text: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SitaraAvatar(size = 56.dp)
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = AstroLingoTheme.tabColors.learn,
            contentColor = AstroLingoTheme.tabColors.onPastel,
        ) {
            Text(text, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(Spacing.md))
        }
    }
}

@Preview(name = "Sitara light", showBackground = true)
@Preview(name = "Sitara dark", showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SitaraPreview() {
    AstroLingoTheme {
        Surface {
            Column(Modifier.padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                SitaraAvatar()
                SitaraSays("Good question! The 10th house is about your work and how the world sees you.")
            }
        }
    }
}
