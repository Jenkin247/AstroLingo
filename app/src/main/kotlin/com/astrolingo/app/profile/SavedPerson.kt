// Other people whose charts the learner keeps on the phone to learn from (LS-8). Birth data like the user's own:
// encrypted on the phone, never synced, never sent to Sitara (ADR 0002). No Android imports: unit tested on the JVM.
package com.astrolingo.app.profile

import com.astrolingo.app.onboarding.BirthDetails

/**
 * Someone the learner saved, with their consent, to compare charts while learning.
 *
 * @property id stable random id, so edits replace the same person.
 * @property name the nickname the learner gave, such as "Asha".
 * @property createdAt when they were added, in epoch milliseconds; the list keeps that order.
 */
data class SavedPerson(
    val id: String,
    val name: String,
    val birth: BirthDetails,
    val createdAt: Long,
) {
    /** Never print a person's details, so they can't reach logs or crash reports. */
    override fun toString(): String = "SavedPerson(redacted)"

    companion object {
        /** People besides the user: four charts in all fit on one compare sheet (PO, 18 Oct 2026). */
        const val MAX_OTHERS = 3

        /** Longest nickname; it has to fit beside a chip on the compare sheet. */
        const val MAX_NAME_LENGTH = 24

        /** The name as it will be saved: trimmed, inner spaces collapsed, cut to [MAX_NAME_LENGTH]. */
        fun cleanName(raw: String): String = raw.trim().replace(Regex("\\s+"), " ").take(MAX_NAME_LENGTH)
    }
}
