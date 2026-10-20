package com.astrolingo.account

import org.robolectric.RuntimeEnvironment
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SignInTest {
    private val context = RuntimeEnvironment.getApplication()

    @Test
    fun `a build without a Firebase project stays signed out and says sign-in is not set up`() = runBlocking {
        val repository = GoogleAccountRepository(context, FirebaseConfig("", "", "", ""))
        assertThat(repository.current()).isEqualTo(AccountState.SignedOut)
        assertThat(repository.signIn(context)).isEqualTo(SignInResult.NotSetUp)
        repository.signOut(context) // must not throw without Firebase
    }

    @Test
    fun `the config counts as set up only when every value is there`() {
        assertThat(FirebaseConfig("web", "key", "app", "project").isSetUp).isTrue()
        assertThat(FirebaseConfig("web", "key", "", "project").isSetUp).isFalse()
    }

    @Test
    fun `only real problems get a message`() {
        assertThat(SignInResult.Success(AccountState.SignedIn("u")).message()).isNull()
        assertThat(SignInResult.Cancelled.message()).isNull()
        assertThat(SignInResult.NoGoogleAccount.message()).isEqualTo(R.string.account_error_no_google)
        assertThat(SignInResult.Offline.message()).isEqualTo(R.string.account_error_offline)
        assertThat(SignInResult.NotSetUp.message()).isEqualTo(R.string.account_error_not_set_up)
        assertThat(SignInResult.Failed("x").message()).isEqualTo(R.string.account_error_failed)
    }
}
