// The app's side of the notification engine: reads the profile on the phone and words the note in the app's language.
package com.astrolingo.app.notes

import android.content.Context
import android.content.res.Configuration
import com.astrolingo.app.onboarding.BirthDetailsResolver
import com.astrolingo.app.onboarding.OnboardingAnswers
import com.astrolingo.app.profile.EncryptedProfileStore
import com.astrolingo.app.profile.ProfileRepository
import com.astrolingo.app.settings.LanguagePreference
import com.astrolingo.app.today.TodayCard
import com.astrolingo.engine.jyotish.KundliCalculator
import com.astrolingo.engine.places.CityIndex
import com.astrolingo.notify.NoteKind
import com.astrolingo.notify.NotePlan
import com.astrolingo.notify.NoteText
import java.io.ByteArrayInputStream
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import java.util.Locale

/**
 * Works out a note for the user's own day or week, as the Today card would show it.
 *
 * @param profiles where the profile is read; null opens the encrypted store.
 */
class AppNotes(private val profiles: ProfileRepository? = null) {
    /** The note of [kind], or null before onboarding or when the profile can't be read. */
    fun note(context: Context, kind: NoteKind): NoteText? {
        val answers = (profiles ?: EncryptedProfileStore(context)).load() ?: return null
        val locale = LanguagePreference(context).load()?.let { Locale.forLanguageTag(it.tag) } ?: Locale.getDefault()
        val config = Configuration(context.resources.configuration).apply { setLocale(locale) }
        val res = context.createConfigurationContext(config).resources
        val kundli = kundliOf(answers)
        val place = answers.todayPlace
        val today = LocalDate.now(place.zone)
        return when (kind) {
            NoteKind.DAILY -> NoteWords.daily(res, TodayCard.build(kundli, today, place), answers.showDeity)
            NoteKind.WEEKLY -> {
                val monday = today.with(TemporalAdjusters.previousOrSame(NotePlan.WEEK_START))
                NoteWords.weekly(res, (0L..6L).map { TodayCard.build(kundli, monday.plusDays(it), place) }, locale)
            }
        }
    }

    /** The user's chart, as the app works it out. Loads the city list only for the rare old Bombay/Calcutta clock. */
    private fun kundliOf(answers: OnboardingAnswers) = run {
        val index = if (answers.birth.useCityClock) CityIndex.bundled() else CityIndex.load(ByteArrayInputStream(ByteArray(0)))
        KundliCalculator.calculate(BirthDetailsResolver.resolve(answers.birth, index).moment)
    }
}
