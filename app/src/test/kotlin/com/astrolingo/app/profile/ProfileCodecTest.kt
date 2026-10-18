package com.astrolingo.app.profile

import com.astrolingo.app.onboarding.BirthDetails
import com.astrolingo.app.onboarding.DailyGoal
import com.astrolingo.app.onboarding.DayPart
import com.astrolingo.app.onboarding.Level
import com.astrolingo.app.onboarding.OnboardingAnswers
import com.astrolingo.app.onboarding.Purpose
import com.astrolingo.engine.places.City
import com.google.common.truth.Truth.assertThat
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import org.junit.Test

// Fictional people only.
class ProfileCodecTest {
    private val pune = City(1259229, "Pune", "Maharashtra", "IN", 18.51957, 73.85535, ZoneId.of("Asia/Kolkata"), 0, emptyList())

    private val asha = OnboardingAnswers(
        purpose = Purpose.LEARN,
        goal = DailyGoal.TEN,
        level = Level.NEW,
        birth = BirthDetails(LocalDate.of(1990, 8, 15), LocalTime.of(6, 30), pune),
    )

    @Test fun roundTripKeepsEveryAnswer() {
        assertThat(ProfileCodec.decode(ProfileCodec.encode(asha))).isEqualTo(asha)
    }

    @Test fun unknownTimeAndNoGoalRoundTrip() {
        val ravi = asha.copy(
            purpose = Purpose.GUIDANCE,
            goal = null,
            showDeity = false,
            birth = asha.birth.copy(time = null, useCityClock = true, dayPart = DayPart.EVENING),
        )
        val rows = ProfileCodec.encode(ravi)
        assertThat(rows).doesNotContainKey("birth_time")
        assertThat(ProfileCodec.decode(rows)).isEqualTo(ravi)
    }

    @Test fun currentPlaceRoundTripsAndDefaultsToBirthplace() {
        val london = City(2643743, "London", "England", "GB", 51.50853, -0.12574, ZoneId.of("Europe/London"), 0, emptyList())
        assertThat(asha.todayPlace).isEqualTo(pune)
        val moved = asha.copy(currentPlace = london)
        assertThat(ProfileCodec.decode(ProfileCodec.encode(moved))).isEqualTo(moved)
        assertThat(moved.todayPlace).isEqualTo(london)
    }

    @Test fun emptyTableMeansNoProfile() {
        assertThat(ProfileCodec.decode(emptyMap())).isNull()
    }

    @Test fun unreadableRowsMeanNoProfile() {
        val broken = ProfileCodec.encode(asha) + ("city_zone" to "Mars/Olympus")
        assertThat(ProfileCodec.decode(broken)).isNull()
    }

    @Test fun futureVersionIsNotGuessed() {
        val newer = ProfileCodec.encode(asha) + ("version" to "99")
        assertThat(ProfileCodec.decode(newer)).isNull()
    }

    @Test fun birthDetailsNeverPrintThemselves() {
        assertThat(asha.birth.toString()).doesNotContain("1990")
        assertThat(asha.birth.toString()).doesNotContain("Pune")
        assertThat(asha.toString()).doesNotContain("Pune")
    }

    @Test fun savedPersonRoundTripsWithAndWithoutATime() {
        val meera = SavedPerson("p1", "Meera", BirthDetails(LocalDate.of(1996, 1, 23), null, pune), createdAt = 42L)
        val ravi = meera.copy(id = "p2", name = "Ravi", birth = meera.birth.copy(time = LocalTime.of(7, 5)))
        assertThat(ProfileCodec.decodePerson("p1", ProfileCodec.encodePerson(meera))).isEqualTo(meera)
        assertThat(ProfileCodec.decodePerson("p2", ProfileCodec.encodePerson(ravi))).isEqualTo(ravi)
        assertThat(ProfileCodec.encodePerson(meera)).doesNotContainKey("birth_time")
    }

    @Test fun unreadablePersonIsLeftOut() {
        val meera = SavedPerson("p1", "Meera", BirthDetails(LocalDate.of(1996, 1, 23), null, pune), createdAt = 42L)
        assertThat(ProfileCodec.decodePerson("p1", ProfileCodec.encodePerson(meera) - "name")).isNull()
        assertThat(ProfileCodec.decodePerson("p1", emptyMap())).isNull()
    }

    @Test fun personNamesAreTidiedAndKeptShort() {
        assertThat(SavedPerson.cleanName("  Asha   Rani ")).isEqualTo("Asha Rani")
        assertThat(SavedPerson.cleanName("x".repeat(40))).hasLength(SavedPerson.MAX_NAME_LENGTH)
    }

    @Test fun personDetailsAreNeverPrinted() {
        val meera = SavedPerson("p1", "Meera", BirthDetails(LocalDate.of(1996, 1, 23), null, pune), createdAt = 42L)
        assertThat(meera.toString()).doesNotContain("1996")
        assertThat(meera.toString()).doesNotContain("Meera")
    }
}
