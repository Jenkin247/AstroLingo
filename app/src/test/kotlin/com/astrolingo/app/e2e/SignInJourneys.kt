package com.astrolingo.app.e2e

import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.ComposeTimeoutException
import androidx.compose.ui.test.SemanticsNodeInteractionCollection
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.astrolingo.account.AccountGate
import com.astrolingo.account.AccountState
import com.astrolingo.account.FakeAccountRepository
import com.astrolingo.account.InMemoryAccountChoices
import com.astrolingo.account.SignInResult
import com.astrolingo.account.UpdatesConsent
import com.astrolingo.app.onboarding.BirthDetails
import com.astrolingo.app.onboarding.DailyGoal
import com.astrolingo.app.onboarding.Level
import com.astrolingo.app.onboarding.OnboardingAnswers
import com.astrolingo.app.onboarding.Purpose
import com.astrolingo.app.profile.InMemoryProfileRepository
import com.astrolingo.app.profile.ProfileRepository
import com.astrolingo.app.settings.AppLanguage
import com.astrolingo.app.settings.InLanguage
import com.astrolingo.app.ui.AstroLingoApp
import com.astrolingo.app.ui.theme.AstroLingoTheme
import com.astrolingo.engine.places.CityIndex
import com.github.takahirom.roborazzi.captureRoboImage
import com.google.common.truth.Truth.assertThat
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Sign-in journeys (ADR 0012) with a fake Google account: the offer after onboarding, "Maybe later", the updates
 * question, and the account block in Me. Fictional user only.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class SignInJourneys {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @get:Rule
    val testName = TestName()

    private val index = CityIndex.bundled()
    private val repository = FakeAccountRepository()
    private val choices = InMemoryAccountChoices()
    private val gate = AccountGate(repository, choices, now = { DECIDED_AT })

    @Before
    fun noAnimations() {
        Settings.Global.putFloat(compose.activity.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 0f)
    }

    private fun answers() = OnboardingAnswers(
        purpose = Purpose.LEARN,
        goal = DailyGoal.TEN,
        level = Level.NEW,
        birth = BirthDetails(LocalDate.of(1992, 3, 9), LocalTime.of(5, 50), index.search("Pune").first()),
    )

    /** The app as MainActivity shows it, with the fake account. */
    private fun showApp(profiles: ProfileRepository = InMemoryProfileRepository(answers())) {
        compose.setContent {
            AstroLingoTheme {
                InLanguage(AppLanguage.ENGLISH) {
                    AstroLingoApp(profiles, account = gate)
                }
            }
        }
    }

    private fun failureShot() {
        val name = "e2e_failure_" + testName.methodName.replace(Regex("[^A-Za-z0-9]+"), "_").take(40)
        runCatching { compose.onAllNodes(isRoot()).onLast().captureRoboImage("build/outputs/roborazzi/$name.png") }
    }

    private fun shows(text: String) = compose.onAllNodesWithText(text, substring = true).fetchSemanticsNodes().isNotEmpty()

    private fun waitForText(text: String) {
        try {
            compose.waitUntil(WAIT_MILLIS) { shows(text) }
        } catch (e: ComposeTimeoutException) {
            failureShot()
            throw AssertionError("Timed out waiting for: $text", e)
        }
    }

    private fun tap(text: String) {
        waitForText(text)
        click(compose.onAllNodesWithText(text), text)
    }

    /** Clicks the first of [nodes] that is on screen, scrolling to it first. */
    private fun click(nodes: SemanticsNodeInteractionCollection, what: String) {
        val count = nodes.fetchSemanticsNodes().size
        for (i in 0 until count) {
            val node = nodes[i]
            runCatching { node.performScrollTo() }
            if (runCatching { node.isDisplayed() }.getOrDefault(false)) {
                node.performClick()
                compose.waitForIdle()
                return
            }
        }
        failureShot()
        throw AssertionError("Nothing on screen to tap: $what ($count found off screen)")
    }

    @Test
    fun `maybe later skips sign-in and Me offers it again`() {
        showApp()
        waitForText("Sign in with Google")
        compose.onAllNodes(isRoot()).onLast().captureRoboImage("build/outputs/roborazzi/30_sign_in.png")
        tap("Maybe later")
        waitForText("Learn")
        assertThat(choices.signInOffered()).isTrue()
        assertThat(repository.current()).isEqualTo(AccountState.SignedOut)
        tap("Me")
        waitForText("Not signed in")
        assertThat(shows("Continue with Google")).isTrue()
    }

    @Test
    fun `signing in asks about updates with everything off, and Me keeps the choice`() {
        showApp()
        tap("Continue with Google")
        waitForText("Would you like to hear from us?")
        compose.onAllNodes(isRoot()).onLast().captureRoboImage("build/outputs/roborazzi/31_updates_choice.png")
        // Nothing is agreed until the user turns it on.
        assertThat(choices.consent()).isEqualTo(UpdatesConsent.NONE)
        tap("New lessons")
        tap("Save my choice")
        waitForText("Learn")
        assertThat(choices.consent()).isEqualTo(UpdatesConsent(lessons = true, decidedAt = DECIDED_AT))
        assertThat(repository.current()).isEqualTo(AccountState.SignedIn(FakeAccountRepository.FAKE_UID))

        tap("Me")
        waitForText("Signed in with Google")
        compose.onAllNodes(isRoot()).onLast().captureRoboImage("build/outputs/roborazzi/32_me_signed_in.png")
        tap("Sky insights")
        assertThat(choices.consent().insights).isTrue()
        tap("Sign out")
        waitForText("Not signed in")
        // Signing out withdraws the updates agreed for that account.
        assertThat(choices.consent().any).isFalse()
    }

    @Test
    fun `not now records a choice with every update off`() {
        showApp()
        tap("Continue with Google")
        tap("Not now")
        waitForText("Learn")
        assertThat(choices.consent()).isEqualTo(UpdatesConsent(decidedAt = DECIDED_AT))
    }

    @Test
    fun `with no Google account the offer says so and stays`() {
        repository.nextResult = SignInResult.NoGoogleAccount
        showApp()
        tap("Continue with Google")
        waitForText("no Google account on this phone")
        assertThat(shows("Maybe later")).isTrue()
        assertThat(choices.signInOffered()).isFalse()
    }

    @Test
    fun `the offer is not shown again once answered`() {
        choices.markSignInOffered()
        showApp()
        waitForText("Learn")
        assertThat(shows("Sign in with Google")).isFalse()
    }

    @Test
    fun `deleting my details signs out and offers sign-in after onboarding again`() {
        choices.markSignInOffered()
        repository.nextResult = SignInResult.Success(AccountState.SignedIn(FakeAccountRepository.FAKE_UID))
        showApp()
        tap("Me")
        tap("Continue with Google")
        waitForText("Signed in with Google")
        tap("Delete my birth details")
        waitForText("Delete your birth details?")
        tap("Delete")
        waitForText("What brings you here?")
        assertThat(repository.current()).isEqualTo(AccountState.SignedOut)
        assertThat(choices.signInOffered()).isFalse()
    }

    private companion object {
        const val WAIT_MILLIS = 20_000L
        const val DECIDED_AT = 1_760_000_000_000L
    }
}
