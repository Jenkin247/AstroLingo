package com.astrolingo.app.profile

import com.astrolingo.app.onboarding.BirthDetails
import com.astrolingo.app.onboarding.DailyGoal
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
            birth = asha.birth.copy(time = null, useCityClock = true),
        )
        val rows = ProfileCodec.encode(ravi)
        assertThat(rows).doesNotContainKey("birth_time")
        assertThat(ProfileCodec.decode(rows)).isEqualTo(ravi)
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
}
