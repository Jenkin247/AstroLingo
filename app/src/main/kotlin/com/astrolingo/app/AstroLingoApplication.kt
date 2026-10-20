// The app process. It hands the notification engine the words for each note (NoteContent) and tells the home-screen
// widget about the main user (AstroWidgetHost).
package com.astrolingo.app

import android.app.Application
import android.content.Context
import com.astrolingo.app.notes.AppNotes
import com.astrolingo.app.profile.EncryptedProfileStore
import com.astrolingo.app.settings.LanguagePreference
import com.astrolingo.engine.places.City
import com.astrolingo.notify.NoteContent
import com.astrolingo.notify.NoteKind
import com.astrolingo.notify.NoteText
import com.astrolingo.widget.AstroWidgetHost

class AstroLingoApplication : Application(), NoteContent, AstroWidgetHost {
    override fun note(context: Context, kind: NoteKind): NoteText? = AppNotes().note(context, kind)

    override fun widgetLanguageTag(): String? = LanguagePreference(this).load()?.tag

    /** The main user's Today place only; saved friends never reach the widget. */
    override fun widgetPlace(): City? = runCatching { EncryptedProfileStore(this).load()?.todayPlace }.getOrNull()
}
