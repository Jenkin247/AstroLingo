// Whose day the Today tab shows (LS-10): the user, or someone saved in Me. Charts are worked out on the phone, like
// the user's own; nothing here is stored or sent anywhere. No Android imports: unit tested on the JVM.
package com.astrolingo.app.today

import com.astrolingo.app.onboarding.BirthDetailsResolver
import com.astrolingo.app.profile.SavedPerson
import com.astrolingo.engine.jyotish.Kundli
import com.astrolingo.engine.jyotish.KundliCalculator
import com.astrolingo.engine.places.CityIndex
import java.io.ByteArrayInputStream

/**
 * A saved person whose day Today can show.
 *
 * @property id the saved person's id, so the choice follows them through edits.
 * @property name the nickname the user gave them.
 * @property kundli their chart; the day uses only the birth Moon and star.
 * @property moonUncertain true when their birth time is unknown and the Moon changed sign that day, so their day
 *   may be off.
 */
data class DayPerson(
    val id: String,
    val name: String,
    val kundli: Kundli,
    val moonUncertain: Boolean,
) {
    /** Never print a person's chart, so it can't reach logs or crash reports. */
    override fun toString(): String = "DayPerson(redacted)"

    companion object {
        /** The person's chart, or null if it can't be worked out. Loads the city list only for the old city-clock case. */
        fun of(person: SavedPerson): DayPerson? = runCatching {
            val index = if (person.birth.useCityClock) CityIndex.bundled() else CityIndex.load(ByteArrayInputStream(ByteArray(0)))
            val birth = BirthDetailsResolver.resolve(person.birth, index)
            DayPerson(person.id, person.name, KundliCalculator.calculate(birth.moment), birth.moonUncertain)
        }.getOrNull()
    }
}
