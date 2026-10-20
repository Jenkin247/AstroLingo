// The one thing the widget needs from the app: who the main user is, as far as the widget cares.
package com.astrolingo.widget

import com.astrolingo.engine.places.City

/**
 * Implemented by the app's Application class. The widget reads it through the application context, so this module
 * never depends on the app's profile store. Only the main user of the app counts, never a saved friend.
 */
interface AstroWidgetHost {
    /** The BCP 47 tag of the language the user picked in the app ("hi"), or null to follow the phone. */
    fun widgetLanguageTag(): String?

    /** Where the main user's Today is worked out, or null before onboarding. May block: called off the main thread. */
    fun widgetPlace(): City?
}
