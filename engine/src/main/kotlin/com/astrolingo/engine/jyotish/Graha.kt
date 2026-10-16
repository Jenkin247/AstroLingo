// The nine grahas used in Vedic astrology.
package com.astrolingo.engine.jyotish

/**
 * Graha (planet) in Vedic astrology. Rahu and Ketu are the lunar nodes.
 *
 * @property englishName the Western name, used as a gloss in lessons.
 */
enum class Graha(val englishName: String) {
    SURYA("Sun"),
    CHANDRA("Moon"),
    MANGAL("Mars"),
    BUDHA("Mercury"),
    GURU("Jupiter"),
    SHUKRA("Venus"),
    SHANI("Saturn"),
    RAHU("North node"),
    KETU("South node"),
}
