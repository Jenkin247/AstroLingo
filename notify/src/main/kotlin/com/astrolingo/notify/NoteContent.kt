// What a note says. The engine only shows it: the app works the words out on the phone and hands them over here.
package com.astrolingo.notify

import android.content.Context

/** The seven weekday planets, each with its own calm picture ([PlanetArt]). */
enum class Planet { SUN, MOON, MARS, MERCURY, JUPITER, VENUS, SATURN }

/**
 * One note, already in the app's language.
 *
 * @property title the line everyone sees first, such as "A strong day: Begin what matters".
 * @property text the second line, such as "Colour: Yellow · Deity: Vishnu".
 * @property planet whose picture goes with it.
 */
data class NoteText(val title: String, val text: String, val planet: Planet) {
    override fun toString(): String = "NoteText($planet)" // the words come from the user's chart
}

/**
 * Supplies the words for each kind of note. The app's Application implements it, so the engine finds it from any
 * context. Calls run on a background thread and may read the encrypted profile. Return null when there is nothing to
 * say, such as before onboarding; then no note goes out.
 */
interface NoteContent {
    fun note(context: Context, kind: NoteKind): NoteText?
}
