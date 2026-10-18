// Where the user's profile lives. The app talks to this interface; the encrypted store implements it.
package com.astrolingo.app.profile

import com.astrolingo.app.onboarding.OnboardingAnswers

/** Saves, loads and deletes the one profile on this phone. Calls may block: run them off the main thread. */
interface ProfileRepository {
    /** The saved answers, or null when there are none (first launch, after delete, or unreadable). */
    fun load(): OnboardingAnswers?

    fun save(answers: OnboardingAnswers)

    /** People saved to learn from (LS-8), oldest first. Unreadable ones are left out. */
    fun loadPeople(): List<SavedPerson>

    /** Adds the person, or replaces the one with the same id. */
    fun savePerson(person: SavedPerson)

    fun deletePerson(id: String)

    /** Removes the profile, saved people and the database key, as "Delete my details" promises. */
    fun deleteAll()
}

/** Keeps the profile in memory only. For previews and tests. */
class InMemoryProfileRepository(
    private var answers: OnboardingAnswers? = null,
    people: List<SavedPerson> = emptyList(),
) : ProfileRepository {
    private val people = people.associateBy { it.id }.toMutableMap()

    override fun load(): OnboardingAnswers? = answers

    override fun save(answers: OnboardingAnswers) {
        this.answers = answers
    }

    override fun loadPeople(): List<SavedPerson> = people.values.sortedBy { it.createdAt }

    override fun savePerson(person: SavedPerson) {
        people[person.id] = person
    }

    override fun deletePerson(id: String) {
        people.remove(id)
    }

    override fun deleteAll() {
        answers = null
        people.clear()
    }
}
