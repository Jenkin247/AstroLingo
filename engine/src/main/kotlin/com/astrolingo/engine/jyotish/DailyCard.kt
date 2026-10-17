// The rules behind the daily card. They pick facts only; the Astro Guru's message library turns them into words.
package com.astrolingo.engine.jyotish

/** How a transit or tara leans. */
enum class Leaning { FAVOURABLE, MIXED, CHALLENGING }

/**
 * Colour of the day, from the weekday lord.
 *
 * @property graha the lord it belongs to.
 */
enum class DayColour(val graha: Graha) {
    ORANGE(Graha.SURYA),
    WHITE(Graha.CHANDRA),
    RED(Graha.MANGAL),
    GREEN(Graha.BUDHA),
    YELLOW(Graha.GURU),
    PINK(Graha.SHUKRA),
    DARK_BLUE(Graha.SHANI),
    ;

    companion object {
        /** The colour for a weekday lord. */
        fun of(lord: Graha): DayColour = entries.first { it.graha == lord }
    }
}

/**
 * Deity of the day, from the weekday lord. The user can turn this off; the app then never shows it.
 *
 * @property graha the lord it belongs to.
 */
enum class Deity(val graha: Graha) {
    SURYA(Graha.SURYA),
    SHIVA(Graha.CHANDRA),
    HANUMAN(Graha.MANGAL),
    GANESHA(Graha.BUDHA),
    VISHNU(Graha.GURU),
    LAKSHMI(Graha.SHUKRA),
    SHANI(Graha.SHANI),
    ;

    companion object {
        /** The deity for a weekday lord. */
        fun of(lord: Graha): Deity = entries.first { it.graha == lord }
    }
}

/**
 * The nine taras: today's nakshatra counted from the birth nakshatra, in cycles of nine.
 *
 * @property leaning Janma is mixed; Sampat, Kshema, Sadhana, Mitra and Parama Mitra are favourable; the rest challenging.
 */
enum class Tara(val leaning: Leaning) {
    JANMA(Leaning.MIXED),
    SAMPAT(Leaning.FAVOURABLE),
    VIPAT(Leaning.CHALLENGING),
    KSHEMA(Leaning.FAVOURABLE),
    PRATYAK(Leaning.CHALLENGING),
    SADHANA(Leaning.FAVOURABLE),
    NAIDHANA(Leaning.CHALLENGING),
    MITRA(Leaning.FAVOURABLE),
    PARAMA_MITRA(Leaning.FAVOURABLE),
    ;

    companion object {
        /** Counts inclusively from the birth nakshatra (which is 1) to today's, then folds into nine. */
        fun of(birth: Nakshatra, today: Nakshatra): Tara {
            val count = (today.ordinal - birth.ordinal + Nakshatra.entries.size) % Nakshatra.entries.size
            return entries[count % entries.size]
        }
    }
}

/** The card's headline, written gently: there is no "bad day", only a day to go easy. */
enum class DayStrength { STRONG, STEADY, GENTLE }

/**
 * Everything the daily card shows, before wording.
 *
 * @property panchang the day's panchang at the user's current place.
 * @property colour colour of the day.
 * @property deity deity of the day; the app hides it when the user turned deities off.
 * @property gocharHouse house of today's Moon counted from the birth Moon sign, 1 to 12.
 * @property gocharLeaning how that house leans.
 * @property tara today's tara from the birth nakshatra.
 * @property strength the headline.
 */
data class DailyCardFacts(
    val panchang: DayPanchang,
    val colour: DayColour,
    val deity: Deity,
    val gocharHouse: Int,
    val gocharLeaning: Leaning,
    val tara: Tara,
    val strength: DayStrength,
)

/** Applies the Astro Guru's daily card rules (docs/spec/daily-card-rules.md). Pure function. */
object DailyCardRules {
    private val favourableHouses = setOf(1, 3, 6, 7, 10, 11)
    private val mixedHouses = setOf(2, 5, 9)

    /**
     * Picks the facts for one day.
     *
     * @param birthMoonSign the user's Chandra rashi.
     * @param birthNakshatra the user's janma nakshatra.
     * @param today the panchang for the day at the user's current place.
     */
    fun facts(birthMoonSign: Rashi, birthNakshatra: Nakshatra, today: DayPanchang): DailyCardFacts {
        val house = KundliCalculator.wholeSignHouse(today.moonRashi, birthMoonSign)
        val houseLeaning = gocharLeaning(house)
        val tara = Tara.of(birthNakshatra, today.nakshatra.nakshatra)
        val lord = today.vara.lord
        return DailyCardFacts(
            panchang = today,
            colour = DayColour.of(lord),
            deity = Deity.of(lord),
            gocharHouse = house,
            gocharLeaning = houseLeaning,
            tara = tara,
            strength = strength(houseLeaning, tara.leaning),
        )
    }

    /** Chandra gochar: houses 1, 3, 6, 7, 10 and 11 from the birth Moon favour; 2, 5 and 9 are mixed; 4, 8 and 12 ask for care. */
    fun gocharLeaning(house: Int): Leaning = when (house) {
        in favourableHouses -> Leaning.FAVOURABLE
        in mixedHouses -> Leaning.MIXED
        else -> Leaning.CHALLENGING
    }

    /** Gochar and tara each score +1, 0 or -1. Both favourable is strong; a negative total is gentle; the rest steady. */
    fun strength(gochar: Leaning, tara: Leaning): DayStrength = when (score(gochar) + score(tara)) {
        2 -> DayStrength.STRONG
        in 0..1 -> DayStrength.STEADY
        else -> DayStrength.GENTLE
    }

    private fun score(leaning: Leaning): Int = when (leaning) {
        Leaning.FAVOURABLE -> 1
        Leaning.MIXED -> 0
        Leaning.CHALLENGING -> -1
    }
}
