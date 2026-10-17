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

    /** Key prefixes for a stored place: the birthplace keeps the original "city_" keys. */
    private const val CITY = "city"
    private const val HERE = "here"

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
            putCity(CITY, city)
            answers.currentPlace?.let { putCity(HERE, it) }
        }
    }

    /**
     * Rebuilds the answers, or returns null when there is no profile or the rows can't be read
     * (the app then asks for birth details again rather than showing a wrong chart).
     */
    fun decode(rows: Map<String, String>): OnboardingAnswers? = runCatching {
        if (rows[K_VERSION]?.toInt() != VERSION) return null
        val city = checkNotNull(rows.city(CITY))
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
            currentPlace = rows.city(HERE),
        )
    }.getOrNull()

    private fun MutableMap<String, String>.putCity(prefix: String, city: City) {
        put("${prefix}_id", city.id.toString())
        put("${prefix}_name", city.name)
        put("${prefix}_admin", city.admin)
        put("${prefix}_country", city.countryCode)
        put("${prefix}_lat", city.latitudeDegrees.toString())
        put("${prefix}_lon", city.longitudeDegrees.toString())
        put("${prefix}_zone", city.zone.id)
    }

    /** The place stored under [prefix], or null when there is none. Throws on a half-written place. */
    private fun Map<String, String>.city(prefix: String): City? {
        val id = this["${prefix}_id"] ?: return null
        return City(
            id = id.toLong(),
            name = getValue("${prefix}_name"),
            admin = getValue("${prefix}_admin"),
            countryCode = getValue("${prefix}_country"),
            latitudeDegrees = getValue("${prefix}_lat").toDouble(),
            longitudeDegrees = getValue("${prefix}_lon").toDouble(),
            zone = ZoneId.of(getValue("${prefix}_zone")),
            population = 0,
            aliases = emptyList(),
        )
    }
}
