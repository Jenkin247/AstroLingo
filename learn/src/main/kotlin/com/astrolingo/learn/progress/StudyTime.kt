// Minutes spent learning each day, for the daily goal ring (LS-4) and, later, the streak.
package com.astrolingo.learn.progress

/** How time on screen turns into learning time. */
object StudyTime {
    /** A card left on screen longer than this counts only this long, so an idle phone doesn't fill the goal. */
    const val MAX_SECONDS_PER_CARD = 120

    /** Days kept, counting today: enough for a month of streak later, small enough to store as text. */
    const val DAYS_KEPT = 30

    /** Seconds credited for one card that was on screen for [millis]. */
    fun credit(millis: Long): Int = (millis / 1000).coerceIn(0, MAX_SECONDS_PER_CARD.toLong()).toInt()
}

/**
 * Today's learning against the learner's goal.
 *
 * @property goalMinutes the daily goal chosen in onboarding.
 * @property seconds learning time today.
 */
data class GoalProgress(val goalMinutes: Int, val seconds: Int) {
    /** Whole minutes learned, as shown under the ring. */
    val minutes: Int get() = seconds / 60

    /** 0 to 1, for the ring. */
    val fraction: Float get() = (seconds / (goalMinutes * 60f)).coerceIn(0f, 1f)

    val met: Boolean get() = seconds >= goalMinutes * 60
}
