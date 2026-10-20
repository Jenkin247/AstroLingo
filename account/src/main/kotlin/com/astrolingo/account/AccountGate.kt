// Everything the app needs for sign-in, handed down as one value. Null in the app means sign-in is not offered at all.
package com.astrolingo.account

import androidx.annotation.StringRes

/**
 * Sign-in and the choices around it.
 *
 * @property now the clock that stamps the updates choice, so tests can fix it.
 */
class AccountGate(
    val repository: AccountRepository,
    val choices: AccountChoices,
    val now: () -> Long = System::currentTimeMillis,
)

/** The line to show after a sign-in attempt, or null when there is nothing to say (success, or the user closed the sheet). */
@StringRes
fun SignInResult.message(): Int? = when (this) {
    is SignInResult.Success, SignInResult.Cancelled -> null
    SignInResult.NoGoogleAccount -> R.string.account_error_no_google
    SignInResult.Offline -> R.string.account_error_offline
    SignInResult.NotSetUp -> R.string.account_error_not_set_up
    is SignInResult.Failed -> R.string.account_error_failed
}
