package com.astrolingo.app.today

import com.astrolingo.app.onboarding.BirthDetails
import com.astrolingo.app.profile.SavedPerson
import com.astrolingo.engine.places.City
import com.google.common.truth.Truth.assertThat
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import org.junit.Test

// Fictional people only.
class DayPersonTest {
    private val pune = City(1259229, "Pune", "Maharashtra", "IN", 18.51957, 73.85535, ZoneId.of("Asia/Kolkata"), 0, emptyList())
    private val asha = SavedPerson("a", "Asha", BirthDetails(LocalDate.of(1990, 8, 15), LocalTime.of(6, 30), pune), 1)
    private val ravi = SavedPerson("r", "Ravi", BirthDetails(LocalDate.of(1988, 5, 2), null, pune), 2)

    @Test fun `a saved person gets their own chart`() {
        val person = checkNotNull(DayPerson.of(asha))
        assertThat(person.id).isEqualTo("a")
        assertThat(person.name).isEqualTo("Asha")
        assertThat(person.kundli.moonSign).isNotNull()
    }

    @Test fun `two people born apart can have different days`() {
        val a = checkNotNull(DayPerson.of(asha))
        val r = checkNotNull(DayPerson.of(ravi))
        val day = LocalDate.of(2026, 10, 22)
        val cards = listOf(a, r).map { TodayCard.build(it.kundli, day, pune) }
        // Same weekday colour for everyone, as the banner says.
        assertThat(cards[0].colour).isEqualTo(cards[1].colour)
        // The Moon's house is counted from each person's own birth Moon, so different birth Moons give different houses.
        val houses = cards.map { checkNotNull(it.facts).gocharHouse }
        if (a.kundli.moonSign != r.kundli.moonSign) assertThat(houses[0]).isNotEqualTo(houses[1]) else assertThat(houses[0]).isEqualTo(houses[1])
    }

    @Test fun `a person without a birth time still gets a day`() {
        assertThat(DayPerson.of(ravi)).isNotNull()
    }

    @Test fun `printing a person shows nothing about them`() {
        assertThat(DayPerson.of(asha).toString()).isEqualTo("DayPerson(redacted)")
    }
}
