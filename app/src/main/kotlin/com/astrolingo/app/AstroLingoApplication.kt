// The app's Application class. Its only job so far is to tell the home-screen widget about the main user.
package com.astrolingo.app

import android.app.Application
import com.astrolingo.app.profile.EncryptedProfileStore
import com.astrolingo.app.settings.LanguagePreference
import com.astrolingo.engine.places.City
import com.astrolingo.widget.AstroWidgetHost

class AstroLingoApplication : Application(), AstroWidgetHost {
    override fun widgetLanguageTag(): String? = LanguagePreference(this).load()?.tag

    /** The main user's Today place only; saved friends never reach the widget. */
    override fun widgetPlace(): City? = runCatching { EncryptedProfileStore(this).load()?.todayPlace }.getOrNull()
}
