package com.astrolingo.account

import org.robolectric.RuntimeEnvironment
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AccountChoicesTest {
    private val prefs = AccountPreferences(RuntimeEnvironment.getApplication())

    @Test
    fun `nothing is agreed and sign-in not yet offered on a fresh install`() {
        assertThat(prefs.signInOffered()).isFalse()
        assertThat(prefs.consent()).isEqualTo(UpdatesConsent.NONE)
        assertThat(UpdatesConsent.NONE.any).isFalse()
    }

    @Test
    fun `the updates choice and its date survive a restart`() {
        val chosen = UpdatesConsent(lessons = true, newsletter = true).decided(1_760_000_000_000L)
        prefs.saveConsent(chosen)
        prefs.markSignInOffered()

        val again = AccountPreferences(RuntimeEnvironment.getApplication())
        assertThat(again.consent()).isEqualTo(chosen)
        assertThat(again.signInOffered()).isTrue()
    }

    @Test
    fun `clearing forgets the offer and the choice`() {
        prefs.saveConsent(UpdatesConsent(offers = true).decided(1L))
        prefs.markSignInOffered()
        prefs.clear()
        assertThat(prefs.signInOffered()).isFalse()
        assertThat(prefs.consent()).isEqualTo(UpdatesConsent.NONE)
    }

    @Test
    fun `in-memory choices behave like the stored ones`() {
        val memory = InMemoryAccountChoices()
        memory.saveConsent(UpdatesConsent(insights = true))
        memory.markSignInOffered()
        assertThat(memory.consent().insights).isTrue()
        memory.clear()
        assertThat(memory.signInOffered()).isFalse()
        assertThat(memory.consent()).isEqualTo(UpdatesConsent.NONE)
    }
}
