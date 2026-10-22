// Stars: points a learner earns for coming back and for learning (PO, 22 Oct 2026). Later they become extra
// questions for Sitara. Code says "stars" everywhere; the name people see comes from one string resource
// (stars_name), so renaming them is a text change only.
package com.astrolingo.learn.stars

/** What earns Stars, and how many. */
enum class StarReason(val stars: Int) {
    /** The first visit of the day. */
    VISIT(2),

    /** A lesson passed for the first time. */
    LESSON(3),

    /** Every quiz question right the first time a lesson is passed, on top of [LESSON]. */
    PERFECT_QUIZ(1),

    /** The day's Review finished. */
    REVIEW(1),
}

/**
 * The learner's Stars. Immutable: every change returns a new copy. Days are epoch days on the phone's clock.
 *
 * @property total Stars held.
 * @property day with [earnedToday]: Stars earned on that day, for the daily limit.
 * @property visitDay the last day a visit earned Stars.
 * @property reviewDay the last day a finished Review earned Stars.
 */
data class Stars(
    val total: Int = 0,
    val day: Long? = null,
    val earnedToday: Int = 0,
    val visitDay: Long? = null,
    val reviewDay: Long? = null,
) {
    fun earnedOn(epochDay: Long): Int = if (day == epochDay) earnedToday else 0

    /** How many Stars [reasons] would add on [epochDay], after the daily limit. */
    fun gain(epochDay: Long, vararg reasons: StarReason): Int =
        reasons.sumOf { it.stars }.coerceAtMost(DAILY_MAX - earnedOn(epochDay)).coerceAtLeast(0)

    /** Adds Stars for [reasons] on [epochDay], up to [DAILY_MAX] a day. */
    fun earn(epochDay: Long, vararg reasons: StarReason): Stars {
        val gained = gain(epochDay, *reasons)
        return copy(total = total + gained, day = epochDay, earnedToday = earnedOn(epochDay) + gained)
    }

    /** The day's first visit earns Stars; later visits that day earn none. */
    fun visit(epochDay: Long): Stars = if (visitDay == epochDay) this else earn(epochDay, StarReason.VISIT).copy(visitDay = epochDay)

    /** A finished Review earns Stars once a day. */
    fun reviewDone(epochDay: Long): Stars =
        if (reviewDay == epochDay) this else earn(epochDay, StarReason.REVIEW).copy(reviewDay = epochDay)

    companion object {
        /** The most Stars anyone can earn in a day, so extra Sitara questions stay special. */
        const val DAILY_MAX = 9

        /** What passing a lesson for the first time earns: [StarReason.LESSON], plus [StarReason.PERFECT_QUIZ] when perfect. */
        fun forLesson(perfect: Boolean): Array<StarReason> =
            if (perfect) arrayOf(StarReason.LESSON, StarReason.PERFECT_QUIZ) else arrayOf(StarReason.LESSON)
    }
}
