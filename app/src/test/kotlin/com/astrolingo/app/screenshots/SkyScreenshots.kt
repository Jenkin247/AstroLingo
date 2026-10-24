package com.astrolingo.app.screenshots

import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.astrolingo.app.today.SkyHeader
import com.astrolingo.app.ui.theme.AstroLingoTheme
import com.astrolingo.app.ui.theme.SkyPhase
import com.astrolingo.app.ui.theme.moonLit
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** The Today sky in each part of the day, so the colours and the Moon can be checked without waiting for the clock. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h400dp-xxhdpi")
class SkyScreenshots {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @Before
    fun noAnimations() {
        // The stars twinkle forever otherwise, which a test clock never waits out.
        Settings.Global.putFloat(compose.activity.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 0f)
    }

    private fun sky(name: String, phase: SkyPhase, tithi: Int, greeting: String) {
        compose.setContent {
            AstroLingoTheme {
                Surface {
                    SkyHeader(phase, moonLit(tithi), waxing = tithi <= 15, rememberScrollState(), overlap = 24.dp) {
                        Column {
                            Text(greeting, style = MaterialTheme.typography.headlineMedium)
                            Text("Saturday, 24 October 2026", style = MaterialTheme.typography.bodyLarge)
                        }
                        Text("Patience pays off", style = MaterialTheme.typography.headlineLarge)
                        Text("Times for London, England, GB", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
        compose.waitForIdle()
        compose.onRoot().captureRoboImage("build/outputs/roborazzi/$name.png")
    }

    @Test
    fun dawn() = sky("70_sky_dawn", SkyPhase.DAWN, 13, "Good morning!")

    @Test
    fun day() = sky("71_sky_day", SkyPhase.DAY, 13, "Good afternoon!")

    @Test
    fun dusk() = sky("72_sky_dusk", SkyPhase.DUSK, 13, "Good evening!")

    // Two days before full Moon, then a waning crescent, lit on the other side.
    @Test
    fun nightWaxing() = sky("73_sky_night", SkyPhase.NIGHT, 13, "Good evening!")

    @Test
    fun nightWaning() = sky("74_sky_night_waning", SkyPhase.NIGHT, 26, "Namaste!")
}
