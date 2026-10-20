// The home-screen widget's receiver: draws the widget and sets one alarm for the next change.
package com.astrolingo.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import java.time.ZoneId
import java.util.Locale
import kotlin.concurrent.thread

/**
 * The small "planet of the day" widget. It redraws when it is added, when its alarm fires (the next planetary hour
 * or midnight), when the phone's clock, time zone or language changes, and when the app goes to the background.
 * The alarm is inexact and does not wake the phone: a sleeping phone catches up when the screen next turns on.
 */
class AstroDayWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        redrawInBackground(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_REFRESH, Intent.ACTION_TIME_CHANGED, Intent.ACTION_TIMEZONE_CHANGED, Intent.ACTION_LOCALE_CHANGED ->
                redrawInBackground(context)
        }
    }

    override fun onDisabled(context: Context) {
        context.getSystemService(AlarmManager::class.java)?.cancel(refreshIntent(context))
    }

    /** Reading the user's place opens the encrypted database, so it runs off the main thread. */
    private fun redrawInBackground(context: Context) {
        val pending = goAsync()
        thread(name = "astro-widget") {
            try {
                redraw(context.applicationContext)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        private const val ACTION_REFRESH = "com.astrolingo.widget.action.REFRESH"

        /** True when the user has the widget on a home screen. */
        fun isOnHomeScreen(context: Context): Boolean = widgetIds(context).isNotEmpty()

        /** Redraws the widget, if there is one, for example after the user changed their place or language. */
        fun refresh(context: Context) {
            if (isOnHomeScreen(context)) context.sendBroadcast(Intent(context, AstroDayWidget::class.java).setAction(ACTION_REFRESH))
        }

        /** The widget's content for the main user right now. May block. */
        fun today(context: Context): WidgetDay {
            val place = (context.applicationContext as? AstroWidgetHost)?.let { runCatching { it.widgetPlace() }.getOrNull() }
            return WidgetDay.at(System.currentTimeMillis(), place, ZoneId.systemDefault())
        }

        /** [context] with resources in the language the user picked in the app. */
        fun inAppLanguage(context: Context): Context {
            val tag = (context.applicationContext as? AstroWidgetHost)?.widgetLanguageTag() ?: return context
            val config = Configuration(context.resources.configuration).apply { setLocale(Locale.forLanguageTag(tag)) }
            return context.createConfigurationContext(config)
        }

        private fun redraw(context: Context) {
            val ids = widgetIds(context)
            if (ids.isEmpty()) return
            val day = today(context)
            AppWidgetManager.getInstance(context).updateAppWidget(ids, WidgetViews.build(inAppLanguage(context), day, openApp(context)))
            context.getSystemService(AlarmManager::class.java)?.set(AlarmManager.RTC, day.refreshAtUtcMillis, refreshIntent(context))
        }

        private fun widgetIds(context: Context): IntArray =
            AppWidgetManager.getInstance(context)?.getAppWidgetIds(ComponentName(context, AstroDayWidget::class.java)) ?: IntArray(0)

        private fun refreshIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            Intent(context, AstroDayWidget::class.java).setAction(ACTION_REFRESH),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        /** A tap opens the app; the widget module does not know its activity, so it asks for the launcher's intent. */
        private fun openApp(context: Context): PendingIntent? =
            context.packageManager.getLaunchIntentForPackage(context.packageName)?.let {
                PendingIntent.getActivity(context, 0, it, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
            }
    }
}
