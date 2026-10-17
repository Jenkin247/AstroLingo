// Turns onboarding answers into key/value rows for the encrypted profile table, and back. No Android imports: unit tested on the JVM.
package com.astrolingo.app.profile

import com.astrolingo.app.onboarding.BirthDetails
import com.astrolingo.app.onboarding.DailyGoal
import com.astrolingo.app.onboarding.DayPart
import com.astrolingo.app.onboarding.Level
import com.astrolingo.app.onboarding.OnboardingAnswers
import com.astrolingo.app.onboarding.Purpose
import com.astrolingo.engine.places.City
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * Key/value layout of the profile table. A version key lets later releases migrate old rows.
 * The birthplace is stored in full so the app can open without loading the 37,000-city list.
 */
object ProfileCodec {
    const val VERSION = 1

    private const val K_VERSION = "version"
    private const val K_PURPOSE = "purpose"
    private const val K_GOAL = "goal"
    private const val K_LEVEL = "level"
    private const val K_DATE = "birth_date"
    private const val K_TIME = "birth_time"
    private const val K_CITY_CLOCK = "use_city_clock"
    private const val K_DAY_PART = "day_part"
    private const val K_SHOW_DEITY = "show_deity"
    private const val K_CITY_ID = "city_id"
    private const val K_CITY_NAME = "city_name"
    private const val K_CITY_ADMIN = "city_admin"
    private const val K_CITY_COUNTRY = "city_country"
    private const val K_CITY_LAT = "city_lat"
    private const val K_CITY_LON = "city_lon"
    private const val K_CITY_ZONE = "city_zone"

    /** Rows to write. A null value means "not set" (unknown time, no goal) and is stored as an absent key. */
    fun encode(answers: OnboardingAnswers): Map<String, String> {
        val birth = answers.birth
        val city = birth.city
        return buildMap {
            put(K_VERSION, VERSION.toString())
            put(K_PURPOSE, answers.purpose.name)
            answers.goal?.let { put(K_GOAL, it.name) }
            put(K_LEVEL, answers.level.name)
            put(K_SHOW_DEITY, answers.showDeity.toString())
            put(K_DATE, birth.date.toString())
            birth.time?.let { put(K_TIME, it.toString()) }
            put(K_CITY_CLOCK, birth.useCityClock.toString())
            birth.dayPart?.let { put(K_DAY_PART, it.name) }
            put(K_CITY_ID, city.id.toString())
            put(K_CITY_NAME, city.name)
            put(K_CITY_ADMIN, city.admin)
            put(K_CITY_COUNTRY, city.countryCode)
            put(K_CITY_LAT, city.latitudeDegrees.toString())
            put(K_CITY_LON, city.longitudeDegrees.toString())
            put(K_CITY_ZONE, city.zone.id)
        }
    }

    /**
     * Rebuilds the answers, or returns null when there is no profile or the rows can't be read
     * (the app then asks for birth details again rather than showing a wrong chart).
     */
    fun decode(rows: Map<String, String>): OnboardingAnswers? = runCatching {
        if (rows[K_VERSION]?.toInt() != VERSION) return null
        val city = City(
            id = rows.getValue(K_CITY_ID).toLong(),
            name = rows.getValue(K_CITY_NAME),
            admin = rows.getValue(K_CITY_ADMIN),
            countryCode = rows.getValue(K_CITY_COUNTRY),
            latitudeDegrees = rows.getValue(K_CITY_LAT).toDouble(),
            longitudeDegrees = rows.getValue(K_CITY_LON).toDouble(),
            zone = ZoneId.of(rows.getValue(K_CITY_ZONE)),
            population = 0,
            aliases = emptyList(),
        )
        OnboardingAnswers(
            purpose = Purpose.valueOf(rows.getValue(K_PURPOSE)),
            goal = rows[K_GOAL]?.let(DailyGoal::valueOf),
            level = Level.valueOf(rows.getValue(K_LEVEL)),
            birth = BirthDetails(
                date = LocalDate.parse(rows.getValue(K_DATE)),
                time = rows[K_TIME]?.let(LocalTime::parse),
                city = city,
                useCityClock = rows.getValue(K_CITY_CLOCK).toBooleanStrict(),
                dayPart = rows[K_DAY_PART]?.let(DayPart::valueOf),
            ),
            showDeity = rows[K_SHOW_DEITY]?.toBooleanStrict() ?: true,
        )
    }.getOrNull()
}
