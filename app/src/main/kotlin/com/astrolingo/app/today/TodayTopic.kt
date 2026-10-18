// Which lesson explains each part of the daily card, for its "Learn why" links (LS-6). Checked by the Astro Guru.
package com.astrolingo.app.today

/** A part of the daily card and the lesson behind it. */
enum class TodayTopic(val lessonId: String) {
    /** The strength and theme of the day come from tara bala and the Moon's transit. */
    STRENGTH("gochar.tara"),

    /** The colour and deity follow the weekday's lord. */
    VARA("panchang.vara"),
    RAHU_KAAL("panchang.rahu-kaal"),
    TITHI("panchang.tithi"),
    NAKSHATRA("nakshatras.mansions"),
}
