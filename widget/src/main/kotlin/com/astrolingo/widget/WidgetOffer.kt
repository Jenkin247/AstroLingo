// The in-app offer to add the widget: shown once, after a couple of days of use, with a live preview.
package com.astrolingo.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.widget.FrameLayout
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** When to offer the widget. No Android imports: unit tested on the JVM. */
object WidgetOfferRule {
    /** Days after the first use before the offer, so it comes once the user knows the app. */
    const val DAYS_BEFORE_OFFER = 2L

    fun shouldOffer(firstUse: LocalDate, today: LocalDate, alreadyOffered: Boolean, onHomeScreen: Boolean): Boolean =
        !alreadyOffered && !onHomeScreen && !today.isBefore(firstUse.plusDays(DAYS_BEFORE_OFFER))
}

/** Remembers the first day of use and whether the offer was shown. On this phone only. */
class WidgetOfferStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** The first day the user had a profile, saved as [today] the first time this is asked. */
    fun firstUse(today: LocalDate): LocalDate {
        prefs.getLong(KEY_FIRST_USE, Long.MIN_VALUE).takeIf { it != Long.MIN_VALUE }?.let { return LocalDate.ofEpochDay(it) }
        prefs.edit().putLong(KEY_FIRST_USE, today.toEpochDay()).apply()
        return today
    }

    fun offered(): Boolean = prefs.getBoolean(KEY_OFFERED, false)

    fun markOffered() {
        prefs.edit().putBoolean(KEY_OFFERED, true).apply()
    }

    private companion object {
        const val PREFS = "astrolingo_widget"
        const val KEY_FIRST_USE = "first_use_day"
        const val KEY_OFFERED = "offered"
    }
}

/**
 * Shows the widget offer once, when [WidgetOfferRule] says so. Put it in the app once the main user has a profile.
 * The offer counts as made as soon as it shows, so a user who closes the app is not asked again.
 */
@Composable
fun WidgetOffer(today: LocalDate = LocalDate.now()) {
    val context = LocalContext.current
    var day by remember { mutableStateOf<WidgetDay?>(null) }
    LaunchedEffect(today) {
        day = withContext(Dispatchers.IO) {
            val store = WidgetOfferStore(context)
            val show = WidgetOfferRule.shouldOffer(store.firstUse(today), today, store.offered(), AstroDayWidget.isOnHomeScreen(context))
            if (show) {
                store.markOffered()
                AstroDayWidget.today(context)
            } else {
                null
            }
        }
    }
    day?.let { WidgetOfferDialog(it, onDone = { day = null }) }
}

/** The offer itself: a preview of the real widget for today, and a button that asks the launcher to add it. */
@Composable
fun WidgetOfferDialog(day: WidgetDay, onDone: () -> Unit) {
    val context = LocalContext.current
    val manager = remember { AppWidgetManager.getInstance(context) }
    // Some launchers cannot add a widget for an app; then the dialog says how to add it by hand.
    val canPin = remember { manager?.isRequestPinAppWidgetSupported == true }
    AlertDialog(
        onDismissRequest = onDone,
        title = { Text(stringResource(R.string.widget_offer_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    WidgetPreview(day, Modifier.size(width = 88.dp, height = 96.dp))
                }
                Text(stringResource(R.string.widget_offer_body), style = MaterialTheme.typography.bodyMedium)
                if (!canPin) Text(stringResource(R.string.widget_offer_manual), style = MaterialTheme.typography.bodyMedium)
            }
        },
        confirmButton = {
            if (canPin) {
                TextButton(onClick = {
                    manager.requestPinAppWidget(ComponentName(context, AstroDayWidget::class.java), null, null)
                    onDone()
                }) { Text(stringResource(R.string.widget_offer_add)) }
            } else {
                TextButton(onClick = onDone) { Text(stringResource(R.string.widget_offer_got_it)) }
            }
        },
        dismissButton = if (canPin) {
            { TextButton(onClick = onDone) { Text(stringResource(R.string.widget_offer_not_now)) } }
        } else {
            null
        },
    )
}

/** The real widget layout, drawn inside the app, in the app's language. */
@Composable
fun WidgetPreview(day: WidgetDay, modifier: Modifier = Modifier) {
    AndroidView(
        factory = { FrameLayout(it) },
        update = { frame ->
            frame.removeAllViews()
            frame.addView(WidgetViews.build(frame.context, day, null).apply(frame.context, frame))
        },
        modifier = modifier,
    )
}
