// The app process. It hands the notification engine the words for each note (NoteContent).
package com.astrolingo.app

import android.app.Application
import android.content.Context
import com.astrolingo.app.notes.AppNotes
import com.astrolingo.notify.NoteContent
import com.astrolingo.notify.NoteKind
import com.astrolingo.notify.NoteText

class AstroLingoApplication : Application(), NoteContent {
    override fun note(context: Context, kind: NoteKind): NoteText? = AppNotes().note(context, kind)
}
