// The account choices kept on this phone: whether sign-in was offered yet, and the updates the user agreed to.
package com.astrolingo.account

import android.content.Context

/** What the app remembers about sign-in between launches. Holds no name, email or birth data. */
interface AccountChoices {
    /** True once the user has signed in or tapped "Maybe later", so the sign-in screen is not shown again. */
    fun signInOffered(): Boolean

    fun markSignInOffered()

    fun consent(): UpdatesConsent

    fun saveConsent(consent: UpdatesConsent)

    /** Forgets everything, for "Delete my details". */
    fun clear()
}

/** [AccountChoices] in the app's private SharedPreferences. */
class AccountPreferences(context: Context) : AccountChoices {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    override fun signInOffered(): Boolean = prefs.getBoolean(KEY_OFFERED, false)

    override fun markSignInOffered() {
        prefs.edit().putBoolean(KEY_OFFERED, true).apply()
    }

    override fun consent(): UpdatesConsent = UpdatesConsent(
        lessons = prefs.getBoolean(KEY_LESSONS, false),
        insights = prefs.getBoolean(KEY_INSIGHTS, false),
        offers = prefs.getBoolean(KEY_OFFERS, false),
        newsletter = prefs.getBoolean(KEY_NEWSLETTER, false),
        decidedAt = prefs.getLong(KEY_DECIDED_AT, NEVER).takeIf { it != NEVER },
    )

    override fun saveConsent(consent: UpdatesConsent) {
        val editor = prefs.edit()
            .putBoolean(KEY_LESSONS, consent.lessons)
            .putBoolean(KEY_INSIGHTS, consent.insights)
            .putBoolean(KEY_OFFERS, consent.offers)
            .putBoolean(KEY_NEWSLETTER, consent.newsletter)
        val decidedAt = consent.decidedAt
        if (decidedAt == null) editor.remove(KEY_DECIDED_AT) else editor.putLong(KEY_DECIDED_AT, decidedAt)
        editor.apply()
    }

    override fun clear() {
        prefs.edit().clear().apply()
    }

    private companion object {
        const val PREFS = "astrolingo_account"
        const val KEY_OFFERED = "sign_in_offered"
        const val KEY_LESSONS = "updates_lessons"
        const val KEY_INSIGHTS = "updates_insights"
        const val KEY_OFFERS = "updates_offers"
        const val KEY_NEWSLETTER = "updates_newsletter"
        const val KEY_DECIDED_AT = "updates_decided_at"
        const val NEVER = -1L
    }
}

/** [AccountChoices] in memory, for tests and previews. */
class InMemoryAccountChoices(
    private var offered: Boolean = false,
    private var consent: UpdatesConsent = UpdatesConsent.NONE,
) : AccountChoices {
    override fun signInOffered(): Boolean = offered

    override fun markSignInOffered() {
        offered = true
    }

    override fun consent(): UpdatesConsent = consent

    override fun saveConsent(consent: UpdatesConsent) {
        this.consent = consent
    }

    override fun clear() {
        offered = false
        consent = UpdatesConsent.NONE
    }
}
