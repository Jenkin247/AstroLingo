// Where the user's profile lives. The app talks to this interface; the encrypted store implements it.
package com.astrolingo.app.profile

import com.astrolingo.app.onboarding.OnboardingAnswers

/** Saves, loads and deletes the one profile on this phone. Calls may block: run them off the main thread. */
interface ProfileRepository {
    /** The saved answers, or null when there are none (first launch, after delete, or unreadable). */
    fun load(): OnboardingAnswers?

    fun save(answers: OnboardingAnswers)

    /** Removes the profile and the database key, as "Delete my details" promises. */
    fun deleteAll()
}

/** Keeps the profile in memory only. For previews and tests. */
class InMemoryProfileRepository(private var answers: OnboardingAnswers? = null) : ProfileRepository {
    override fun load(): OnboardingAnswers? = answers

    override fun save(answers: OnboardingAnswers) {
        this.answers = answers
    }

    override fun deleteAll() {
        answers = null
    }
}
