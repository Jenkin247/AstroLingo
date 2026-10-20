package com.astrolingo.widget

import android.content.Context
import android.content.res.Configuration
import android.widget.FrameLayout
import android.widget.TextView
import com.astrolingo.engine.jyotish.DayColour
import com.astrolingo.engine.jyotish.Graha
import com.google.common.truth.Truth.assertThat
import java.time.LocalDate
import java.util.Locale
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class WidgetViewsTest {
    private val context: Context = RuntimeEnvironment.getApplication()
    private val shaniCalm = WidgetDay(Graha.SHANI, DayColour.DARK_BLUE, HourMood.CALM, 0)

    private fun draw(day: WidgetDay, ctx: Context = context) = FrameLayout(ctx).let { parent ->
        WidgetViews.build(ctx, day, null).apply(ctx, parent)
    }

    private fun text(root: android.view.View, id: Int) = root.findViewById<TextView>(id).text.toString()

    @Test fun `shows the planet's name and the mood of the hour`() {
        val root = draw(shaniCalm)
        assertThat(text(root, R.id.widget_planet_name)).isEqualTo("Shani")
        assertThat(text(root, R.id.widget_mood)).isEqualTo("Calm")
        assertThat(root.contentDescription.toString()).isEqualTo("Shani's day. Colour of the day: Dark blue. This hour feels calm.")
    }

    @Test fun `before the user's place is known the colour stands in for the mood`() {
        val root = draw(shaniCalm.copy(mood = null))
        assertThat(text(root, R.id.widget_mood)).isEqualTo("Dark blue")
    }

    @Test fun `follows the app's language`() {
        val hindi = context.createConfigurationContext(Configuration(context.resources.configuration).apply { setLocale(Locale.forLanguageTag("hi")) })
        val root = draw(WidgetDay(Graha.MANGAL, DayColour.RED, HourMood.ENERGISING, 0), hindi)
        assertThat(text(root, R.id.widget_planet_name)).isEqualTo("मंगल")
        assertThat(text(root, R.id.widget_mood)).isEqualTo("ऊर्जावान")
    }

    @Test fun `the offer remembers the first day and is made once`() {
        val store = WidgetOfferStore(context)
        val day = LocalDate.of(2026, 10, 20)
        assertThat(store.firstUse(day)).isEqualTo(day)
        assertThat(store.firstUse(day.plusDays(3))).isEqualTo(day)
        assertThat(store.offered()).isFalse()
        store.markOffered()
        assertThat(WidgetOfferStore(context).offered()).isTrue()
    }
}
