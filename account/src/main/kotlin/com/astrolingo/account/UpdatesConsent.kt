// What the user agreed to hear from us after signing in. Every choice is off until they turn it on (GDPR, DPDP).
package com.astrolingo.account

/**
 * The user's choice of updates from AstroLingo.
 *
 * @property lessons a notification when new lessons arrive.
 * @property insights notifications about notable sky events, such as an eclipse or a festival.
 * @property offers notifications about offers.
 * @property newsletter an occasional email newsletter to the user's Google address.
 * @property decidedAt when the user last saved this choice (epoch milliseconds), or null if never asked. Kept as the
 * record of consent.
 */
data class UpdatesConsent(
    val lessons: Boolean = false,
    val insights: Boolean = false,
    val offers: Boolean = false,
    val newsletter: Boolean = false,
    val decidedAt: Long? = null,
) {
    /** True if any update is turned on. */
    val any: Boolean get() = lessons || insights || offers || newsletter

    /** The same choices, stamped as decided at [now]. */
    fun decided(now: Long): UpdatesConsent = copy(decidedAt = now)

    companion object {
        /** Nothing agreed: the state before the user is asked, after they say "Not now", and after they sign out. */
        val NONE = UpdatesConsent()
    }
}
