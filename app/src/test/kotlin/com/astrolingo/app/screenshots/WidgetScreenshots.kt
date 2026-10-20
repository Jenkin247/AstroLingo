package com.astrolingo.app.screenshots

import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.astrolingo.app.settings.AppLanguage
import com.astrolingo.app.settings.InLanguage
import com.astrolingo.app.ui.theme.AstroLingoTheme
import com.astrolingo.engine.jyotish.DayColour
import com.astrolingo.engine.jyotish.Graha
import com.astrolingo.widget.HourMood
import com.astrolingo.widget.WidgetDay
import com.astrolingo.widget.WidgetOfferDialog
import com.astrolingo.widget.WidgetPreview
import com.github.takahirom.roborazzi.captureRoboImage
import java.util.Locale
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** The home-screen widget for each planet of the week, in light, dark and Hindi, and the in-app offer. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class WidgetScreenshots {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @Before
    fun noAnimations() {
        Settings.Global.putFloat(compose.activity.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 0f)
    }

    /** Sunday to Saturday, each with a different hour's mood so every word shows once. */
    private val week = listOf(Graha.SURYA, Graha.CHANDRA, Graha.MANGAL, Graha.BUDHA, Graha.GURU, Graha.SHUKRA, Graha.SHANI)
        .mapIndexed { i, lord -> WidgetDay(lord, DayColour.of(lord), HourMood.entries[(i + 1) % HourMood.entries.size], 0) }

    @Composable
    private fun Week(wallpaper: Color) {
        Column(
            Modifier.background(wallpaper).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            week.chunked(4).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    row.forEach { WidgetPreview(it, Modifier.size(width = 88.dp, height = 96.dp)) }
                }
            }
        }
    }

    private fun shot(name: String, content: @Composable () -> Unit) {
        compose.setContent(content)
        compose.waitForIdle()
        compose.onRoot().captureRoboImage("build/outputs/roborazzi/$name.png")
    }

    @Test
    fun light() = shot("50_widget_week") { Week(Color(0xFFBFD8E8)) }

    @Test
    @Config(qualifiers = "+night")
    fun dark() = shot("51_widget_week_dark") { Week(Color(0xFF0E1A2B)) }

    @Test
    fun hindi() {
        val before = Locale.getDefault()
        try {
            shot("52_widget_week_hindi") { InLanguage(AppLanguage.HINDI) { Week(Color(0xFFBFD8E8)) } }
        } finally {
            Locale.setDefault(before)
        }
    }

    @Test
    fun offer() {
        compose.setContent { AstroLingoTheme { WidgetOfferDialog(week[6], onDone = {}) } }
        compose.waitForIdle()
        // The dialog is its own window: capture the last root, not the activity's.
        compose.onAllNodes(isRoot()).onLast().captureRoboImage("build/outputs/roborazzi/53_widget_offer.png")
    }
}
