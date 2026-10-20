// Who is signed in, and the outcome of trying to sign in. The rest of the app sees only these types, never Firebase's.
package com.astrolingo.account

import android.content.Context

/** Whether someone is signed in on this phone. */
sealed interface AccountState {
    data object SignedOut : AccountState

    /** @property uid the Firebase account id: the only identifier our servers will see (ADR 0004). */
    data class SignedIn(val uid: String) : AccountState
}

/** What happened when the user tapped "Continue with Google". */
sealed interface SignInResult {
    data class Success(val account: AccountState.SignedIn) : SignInResult

    /** The user closed Google's sheet. Nothing to say; they can tap again. */
    data object Cancelled : SignInResult

    /** No Google account on the phone, or none that can be used. */
    data object NoGoogleAccount : SignInResult

    /** No connection to Google or Firebase. */
    data object Offline : SignInResult

    /** This build has no Firebase project yet (see account/README.md). */
    data object NotSetUp : SignInResult

    /** Anything else; [reason] is for crash reports and never shown. */
    data class Failed(val reason: String) : SignInResult
}

/** Signs the user in and out. */
interface AccountRepository {
    /** The account remembered from earlier, read without the network. */
    fun current(): AccountState

    /** Shows Google's account sheet over [activity] and signs in with the account picked. */
    suspend fun signIn(activity: Context): SignInResult

    /** Signs out of Firebase and tells Credential Manager to forget the choice, so the sheet asks again next time. */
    suspend fun signOut(context: Context)
}

/** An [AccountRepository] that never talks to Google, for tests and previews. */
class FakeAccountRepository(
    private var state: AccountState = AccountState.SignedOut,
    /** What the next sign-in returns; success signs in as [FAKE_UID]. */
    var nextResult: SignInResult = SignInResult.Success(AccountState.SignedIn(FAKE_UID)),
) : AccountRepository {
    override fun current(): AccountState = state

    override suspend fun signIn(activity: Context): SignInResult {
        val result = nextResult
        if (result is SignInResult.Success) state = result.account
        return result
    }

    override suspend fun signOut(context: Context) {
        state = AccountState.SignedOut
    }

    companion object {
        const val FAKE_UID = "test-user"
    }
}
