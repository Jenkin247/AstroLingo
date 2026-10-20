// The words in the daily and weekly notes, from the same rules and message library as the Today card. Worked out on
// the phone; nothing about the user goes anywhere.
package com.astrolingo.app.notes

import android.content.res.Resources
import com.astrolingo.app.R
import com.astrolingo.app.today.TodayCard
import com.astrolingo.app.today.focus
import com.astrolingo.app.today.label
import com.astrolingo.engine.jyotish.DayStrength
import com.astrolingo.engine.jyotish.Vara
import com.astrolingo.notify.NoteText
import com.astrolingo.notify.Planet
import java.time.format.TextStyle
import java.util.Locale

object NoteWords {
    /**
     * The morning note: the day in one line, then its colour and, unless the user turned deities off, its deity.
     * "A strong day: Begin what matters" / "Colour: Yellow · Deity: Vishnu".
     */
    fun daily(res: Resources, card: TodayCard, showDeity: Boolean): NoteText {
        val facts = card.facts
        val title = if (facts != null) {
            res.getString(R.string.notes_daily_title, res.getString(facts.strength.label), res.getStringArray(facts.strength.focus)[facts.gocharHouse - 1])
        } else {
            // No sunrise at the user's place today (far north or south): the weekday still gives the day its planet.
            res.getStringArray(R.array.daily_vara_title)[card.vara.ordinal]
        }
        val colour = res.getString(R.string.notes_label_value, res.getString(R.string.today_label_colour), res.getString(card.colour.label))
        val text = if (showDeity) {
            val deity = res.getString(R.string.notes_label_value, res.getString(R.string.today_label_deity), res.getStringArray(R.array.notes_deity_names)[card.deity.ordinal])
            res.getString(R.string.notes_daily_text, colour, deity)
        } else {
            colour
        }
        return NoteText(title, text, card.vara.planet)
    }

    /**
     * The Monday note for someone who has been away: the week's best day by the card's rules, and a reason to look.
     *
     * @param week the seven cards from Monday to Sunday.
     */
    fun weekly(res: Resources, week: List<TodayCard>, locale: Locale): NoteText {
        // The strongest day of the week, the earliest if several tie. Strength runs STRONG, STEADY, GENTLE.
        val best = week.filter { it.facts != null }.minByOrNull { it.facts!!.strength.ordinal }
        val day = best?.date?.dayOfWeek?.getDisplayName(TextStyle.FULL, locale)
        val text = when (best?.facts?.strength) {
            DayStrength.STRONG -> res.getString(R.string.notes_week_strong, day)
            DayStrength.STEADY -> res.getString(R.string.notes_week_steady, day)
            DayStrength.GENTLE -> res.getString(R.string.notes_week_gentle)
            null -> res.getString(R.string.notes_week_none)
        }
        val planet = (best ?: week.first()).vara.planet
        return NoteText(res.getString(R.string.notes_week_title), text, planet)
    }
}

/** The weekday's planet, for the note's picture. */
internal val Vara.planet: Planet
    get() = when (this) {
        Vara.RAVIVARA -> Planet.SUN
        Vara.SOMAVARA -> Planet.MOON
        Vara.MANGALAVARA -> Planet.MARS
        Vara.BUDHAVARA -> Planet.MERCURY
        Vara.GURUVARA -> Planet.JUPITER
        Vara.SHUKRAVARA -> Planet.VENUS
        Vara.SHANIVARA -> Planet.SATURN
    }
