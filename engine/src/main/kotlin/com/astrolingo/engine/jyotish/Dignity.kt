// How comfortable a graha is in a sign (dignity), and the element of each sign. Used by Learn's compare sheet
// (LS-9) once the learner has finished the Dignity chapter. Sign level only: moolatrikona and exact degrees of
// exaltation are left out, so a graha in its moolatrikona sign shows as "own sign" (Astro Guru, 18 Oct 2026).
package com.astrolingo.engine.jyotish

/** The four elements (tattva) of the signs. Source: BPHS ch. 4. */
enum class Element { FIRE, EARTH, AIR, WATER }

/** Fire, earth, air and water repeat from Mesha: Mesha fire, Vrishabha earth, Mithuna air, Karka water, and so on. */
val Rashi.element: Element get() = Element.entries[ordinal % 4]

/** A graha's dignity in a sign, strongest first. */
enum class Dignity {
    EXALTED,
    OWN_SIGN,
    FRIEND,
    NEUTRAL,
    ENEMY,
    DEBILITATED,
    ;

    companion object {
        /** Exaltation signs (BPHS ch. 3). Debilitation is the sign opposite. */
        private val EXALTATION = mapOf(
            Graha.SURYA to Rashi.MESHA,
            Graha.CHANDRA to Rashi.VRISHABHA,
            Graha.MANGAL to Rashi.MAKARA,
            Graha.BUDHA to Rashi.KANYA,
            Graha.GURU to Rashi.KARKA,
            Graha.SHUKRA to Rashi.MEENA,
            Graha.SHANI to Rashi.TULA,
        )

        /** Natural friends and enemies (naisargika maitri, BPHS ch. 3); every other graha is neutral. */
        private val FRIENDS = mapOf(
            Graha.SURYA to setOf(Graha.CHANDRA, Graha.MANGAL, Graha.GURU),
            Graha.CHANDRA to setOf(Graha.SURYA, Graha.BUDHA),
            Graha.MANGAL to setOf(Graha.SURYA, Graha.CHANDRA, Graha.GURU),
            Graha.BUDHA to setOf(Graha.SURYA, Graha.SHUKRA),
            Graha.GURU to setOf(Graha.SURYA, Graha.CHANDRA, Graha.MANGAL),
            Graha.SHUKRA to setOf(Graha.BUDHA, Graha.SHANI),
            Graha.SHANI to setOf(Graha.BUDHA, Graha.SHUKRA),
        )
        private val ENEMIES = mapOf(
            Graha.SURYA to setOf(Graha.SHUKRA, Graha.SHANI),
            Graha.CHANDRA to emptySet(),
            Graha.MANGAL to setOf(Graha.BUDHA),
            Graha.BUDHA to setOf(Graha.CHANDRA),
            Graha.GURU to setOf(Graha.BUDHA, Graha.SHUKRA),
            Graha.SHUKRA to setOf(Graha.SURYA, Graha.CHANDRA),
            Graha.SHANI to setOf(Graha.SURYA, Graha.CHANDRA, Graha.MANGAL),
        )

        /**
         * The dignity of [graha] in [rashi], or null for Rahu and Ketu, whose dignities the classics don't agree on.
         * Exaltation wins over own sign, as for Budha in Kanya.
         */
        fun of(graha: Graha, rashi: Rashi): Dignity? {
            val exalted = EXALTATION[graha] ?: return null
            val lord = rashi.lord
            return when {
                rashi == exalted -> EXALTED
                rashi.ordinal == (exalted.ordinal + 6) % 12 -> DEBILITATED
                lord == graha -> OWN_SIGN
                lord in FRIENDS.getValue(graha) -> FRIEND
                lord in ENEMIES.getValue(graha) -> ENEMY
                else -> NEUTRAL
            }
        }
    }
}
