// Turns a WidgetDay into the RemoteViews the launcher draws. Also used for the live preview in the in-app offer.
package com.astrolingo.widget

import android.app.PendingIntent
import android.content.Context
import android.widget.RemoteViews
import com.astrolingo.engine.jyotish.DayColour
import com.astrolingo.engine.jyotish.Graha

object WidgetViews {
    /**
     * The widget for [day].
     *
     * @param context gives the strings, so pass one in the app's language.
     * @param open what a tap does, or null for a preview that does nothing.
     */
    fun build(context: Context, day: WidgetDay, open: PendingIntent?): RemoteViews {
        val planet = context.getString(day.planet.nameRes)
        val colour = context.getString(day.colour.nameRes)
        val mood = day.mood?.let { context.getString(it.nameRes) }
        return RemoteViews(context.packageName, R.layout.widget_day).apply {
            setImageViewResource(R.id.widget_planet, day.planet.iconRes)
            setTextViewText(R.id.widget_planet_name, planet)
            setInt(R.id.widget_colour, "setColorFilter", day.colour.argb)
            // Before the user's place is known, the colour's name stands in for the mood.
            setTextViewText(R.id.widget_mood, mood ?: colour)
            setContentDescription(
                android.R.id.background,
                if (mood != null) context.getString(R.string.widget_read_out, planet, colour, mood.lowercase())
                else context.getString(R.string.widget_read_out_no_hour, planet, colour),
            )
            open?.let { setOnClickPendingIntent(android.R.id.background, it) }
        }
    }

    private val Graha.iconRes: Int
        get() = when (this) {
            Graha.SURYA -> R.drawable.widget_planet_surya
            Graha.CHANDRA -> R.drawable.widget_planet_chandra
            Graha.MANGAL -> R.drawable.widget_planet_mangal
            Graha.BUDHA -> R.drawable.widget_planet_budha
            Graha.GURU -> R.drawable.widget_planet_guru
            Graha.SHUKRA -> R.drawable.widget_planet_shukra
            Graha.SHANI, Graha.RAHU, Graha.KETU -> R.drawable.widget_planet_shani // Rahu and Ketu never rule a day
        }

    private val Graha.nameRes: Int
        get() = when (this) {
            Graha.SURYA -> R.string.widget_planet_surya
            Graha.CHANDRA -> R.string.widget_planet_chandra
            Graha.MANGAL -> R.string.widget_planet_mangal
            Graha.BUDHA -> R.string.widget_planet_budha
            Graha.GURU -> R.string.widget_planet_guru
            Graha.SHUKRA -> R.string.widget_planet_shukra
            Graha.SHANI, Graha.RAHU, Graha.KETU -> R.string.widget_planet_shani
        }

    private val HourMood.nameRes: Int
        get() = when (this) {
            HourMood.BRIGHT -> R.string.widget_mood_bright
            HourMood.CALM -> R.string.widget_mood_calm
            HourMood.ENERGISING -> R.string.widget_mood_energising
            HourMood.SHARP -> R.string.widget_mood_sharp
            HourMood.WISE -> R.string.widget_mood_wise
            HourMood.JOYFUL -> R.string.widget_mood_joyful
            HourMood.STEADY -> R.string.widget_mood_steady
        }

    private val DayColour.nameRes: Int
        get() = when (this) {
            DayColour.ORANGE -> R.string.widget_colour_orange
            DayColour.WHITE -> R.string.widget_colour_white
            DayColour.RED -> R.string.widget_colour_red
            DayColour.GREEN -> R.string.widget_colour_green
            DayColour.YELLOW -> R.string.widget_colour_yellow
            DayColour.PINK -> R.string.widget_colour_pink
            DayColour.DARK_BLUE -> R.string.widget_colour_dark_blue
        }

    /** The same swatches as the Today card. */
    private val DayColour.argb: Int
        get() = when (this) {
            DayColour.ORANGE -> 0xFFF28C28
            DayColour.WHITE -> 0xFFFFFFFF
            DayColour.RED -> 0xFFD32F2F
            DayColour.GREEN -> 0xFF2E7D32
            DayColour.YELLOW -> 0xFFFBC02D
            DayColour.PINK -> 0xFFEC407A
            DayColour.DARK_BLUE -> 0xFF1A237E
        }.toInt()
}
