// Share the day (LS-11): the text a user sends to a friend through Android's share sheet. Only the day's outcome
// goes in it, never birth date, time, place, Moon sign, birth star or deity. No Android imports: unit tested on the JVM.
package com.astrolingo.app.today

/**
 * One shared day, already in the app's language.
 *
 * @property title "Asha's day" or "My day".
 * @property date the day, written out.
 * @property strength the strength label, or null on a day with no sunrise.
 * @property headline the day's focus line, or null with [strength].
 * @property tips the day's two tips.
 * @property colour "Colour of the day: White".
 * @property rahu "Rahu kaal in Pune: 7:30 – 9:00 AM", or null on a day with no sunrise.
 * @property footer "Shared from AstroLingo".
 */
data class DayShare(
    val title: String,
    val date: String,
    val strength: String?,
    val headline: String?,
    val tips: List<String>,
    val colour: String,
    val rahu: String?,
    val footer: String,
) {
    /** Short lines a friend can read in a chat at a glance. */
    fun text(): String = buildList {
        add("$title · $date")
        if (strength != null) add(if (headline != null) "$strength: $headline" else strength)
        tips.forEach { add("✓ $it") }
        add(colour)
        rahu?.let(::add)
        add("")
        add(footer)
    }.joinToString("\n")

    override fun toString(): String = "DayShare(redacted)" // it names a person
}
