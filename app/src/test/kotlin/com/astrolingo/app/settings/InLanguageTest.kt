// The language wrapper must keep the activity reachable, or anything that asks for a permission closes the app.
package com.astrolingo.app.settings

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class InLanguageTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `the activity stays reachable for permission requests inside the wrapper`() {
        val owners = mutableMapOf<AppLanguage, Any?>()
        val tags = mutableMapOf<AppLanguage, String>()
        compose.setContent {
            AppLanguage.entries.forEach { language ->
                InLanguage(language) {
                    owners[language] = LocalActivityResultRegistryOwner.current
                    tags[language] = LocalConfiguration.current.locales[0].language
                }
            }
        }
        compose.waitForIdle()
        AppLanguage.entries.forEach { language ->
            assertThat(owners[language]).isSameInstanceAs(compose.activity)
            assertThat(tags[language]).isEqualTo(language.tag)
        }
    }
}
