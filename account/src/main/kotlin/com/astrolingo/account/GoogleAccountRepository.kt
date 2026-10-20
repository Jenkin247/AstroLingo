// Sign in with Google through Credential Manager, then exchange Google's ID token for a Firebase account (ADR 0004).
// Asks for no Google scopes beyond the basic profile, and keeps no name or email: only the Firebase uid leaves here.
package com.astrolingo.account

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.CredentialOption
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.gms.tasks.Task
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * The Firebase project this build talks to. Every value comes from gradle.properties; all empty means sign-in is not
 * set up yet, and the app runs on this phone only.
 *
 * @property webClientId the OAuth "Web client" id Firebase creates for Google sign-in (the server client id).
 */
data class FirebaseConfig(
    val webClientId: String,
    val apiKey: String,
    val appId: String,
    val projectId: String,
) {
    val isSetUp: Boolean get() = listOf(webClientId, apiKey, appId, projectId).all { it.isNotBlank() }

    companion object {
        fun fromBuild(): FirebaseConfig = FirebaseConfig(
            webClientId = BuildConfig.FIREBASE_WEB_CLIENT_ID,
            apiKey = BuildConfig.FIREBASE_API_KEY,
            appId = BuildConfig.FIREBASE_APP_ID,
            projectId = BuildConfig.FIREBASE_PROJECT_ID,
        )
    }
}

/** The real [AccountRepository]. Touches Firebase only when [config] is set up, so test builds without it still run. */
class GoogleAccountRepository(
    context: Context,
    private val config: FirebaseConfig = FirebaseConfig.fromBuild(),
) : AccountRepository {
    private val appContext = context.applicationContext

    private val auth: FirebaseAuth? by lazy {
        if (!config.isSetUp) return@lazy null
        val app = FirebaseApp.getApps(appContext).firstOrNull { it.name == FirebaseApp.DEFAULT_APP_NAME }
            ?: FirebaseApp.initializeApp(
                appContext,
                FirebaseOptions.Builder()
                    .setApiKey(config.apiKey)
                    .setApplicationId(config.appId)
                    .setProjectId(config.projectId)
                    .build(),
            )
        FirebaseAuth.getInstance(app)
    }

    override fun current(): AccountState =
        auth?.currentUser?.uid?.let { AccountState.SignedIn(it) } ?: AccountState.SignedOut

    override suspend fun signIn(activity: Context): SignInResult {
        val auth = auth ?: return SignInResult.NotSetUp
        // Google's sheet must start from the activity itself, not a wrapper such as the app's language context.
        val host = activity.findActivity() ?: activity
        val manager = CredentialManager.create(host)
        val response = try {
            // Accounts on the phone first, as a bottom sheet. If Credential Manager finds none it can offer, fall back to
            // the "Sign in with Google" button flow, which also lets the user add an account.
            try {
                manager.getCredential(host, request(googleIdOption()))
            } catch (e: NoCredentialException) {
                manager.getCredential(host, request(GetSignInWithGoogleOption.Builder(config.webClientId).build()))
            }
        } catch (e: GetCredentialCancellationException) {
            return SignInResult.Cancelled
        } catch (e: NoCredentialException) {
            return SignInResult.NoGoogleAccount
        } catch (e: GetCredentialException) {
            return SignInResult.Failed(e.type)
        }
        val idToken = idToken(response) ?: return SignInResult.Failed("unexpected credential")
        return try {
            val result = auth.signInWithCredential(GoogleAuthProvider.getCredential(idToken, null)).await()
            val uid = result.user?.uid ?: return SignInResult.Failed("no firebase user")
            SignInResult.Success(AccountState.SignedIn(uid))
        } catch (e: CancellationException) {
            throw e
        } catch (e: FirebaseNetworkException) {
            SignInResult.Offline
        } catch (e: Exception) {
            SignInResult.Failed(e.javaClass.simpleName)
        }
    }

    override suspend fun signOut(context: Context) {
        val auth = auth ?: return
        auth.signOut()
        runCatching { CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest()) }
    }

    private fun googleIdOption(): GetGoogleIdOption = GetGoogleIdOption.Builder()
        .setServerClientId(config.webClientId)
        .setFilterByAuthorizedAccounts(false)
        .setAutoSelectEnabled(false)
        .build()

    private fun request(option: CredentialOption): GetCredentialRequest =
        GetCredentialRequest.Builder().addCredentialOption(option).build()

    /** Google's ID token from the credential, or null if Credential Manager returned something else. */
    private fun idToken(response: GetCredentialResponse): String? {
        val credential = response.credential
        if (credential !is CustomCredential || credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            return null
        }
        return runCatching { GoogleIdTokenCredential.createFrom(credential.data).idToken }.getOrNull()
    }
}

/** The activity under any [ContextWrapper]s, or null if there is none. */
private fun Context.findActivity(): Activity? {
    var context: Context? = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}

/** Waits for a Play services [Task] without blocking the main thread. */
private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { continuation ->
    addOnCompleteListener { task ->
        val error = task.exception
        when {
            error != null -> continuation.resumeWithException(error)
            task.isCanceled -> continuation.cancel()
            else -> continuation.resume(task.result)
        }
    }
}
