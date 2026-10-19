// "Use my location": one coarse location reading, snapped to the nearest bundled city. Only that city is kept,
// encrypted with the profile; the reading itself is never stored, logged or sent, and nothing runs in the background.
package com.astrolingo.app.ui.places

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.os.CancellationSignal
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import com.astrolingo.app.R
import com.astrolingo.app.ui.theme.Spacing
import com.astrolingo.engine.places.City
import com.astrolingo.engine.places.CityIndex

private enum class Lookup { IDLE, LOCATING, DENIED, UNAVAILABLE }

/**
 * A "Use my location" button. Asks for approximate location only: a few kilometres move sunrise and Rahu kaal
 * by seconds, so a precise fix would add nothing. If the user says no, or no reading comes, it says so and the
 * user types a city instead.
 *
 * @param index the city list, or null while it loads.
 * @param onFound called with the bundled city nearest the reading.
 */
@Composable
fun UseMyLocation(index: CityIndex?, onFound: (City) -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var lookup by remember { mutableStateOf(Lookup.IDLE) }

    fun locate() {
        lookup = Lookup.LOCATING
        readOnce(context) { location ->
            val city = location?.let { index?.nearest(it.latitude, it.longitude) }
            if (city == null) {
                lookup = Lookup.UNAVAILABLE
            } else {
                lookup = Lookup.IDLE
                onFound(city)
            }
        }
    }

    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) locate() else lookup = Lookup.DENIED
    }

    Column(modifier, verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        OutlinedButton(
            onClick = { if (hasPermission(context)) locate() else ask.launch(Manifest.permission.ACCESS_COARSE_LOCATION) },
            enabled = index != null && lookup != Lookup.LOCATING,
            modifier = Modifier.fillMaxWidth().heightIn(min = Spacing.touchTarget),
        ) {
            Text(stringResource(if (lookup == Lookup.LOCATING) R.string.location_finding else R.string.location_use))
        }
        val note = when (lookup) {
            Lookup.DENIED -> R.string.location_denied
            Lookup.UNAVAILABLE -> R.string.location_unavailable
            else -> R.string.location_note
        }
        Text(stringResource(note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun hasPermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

/**
 * One reading, as fast as the phone can give it. A reading the phone already has from the last
 * [RECENT_MINUTES] is used at once: the city is all we keep, and it will not have changed. Otherwise every
 * enabled provider is asked at the same time and the first answer wins, so a slow GPS fix never holds up a
 * network one. After [WAIT_MS] it settles for the newest reading the phone has, however old; null if none.
 */
@SuppressLint("MissingPermission") // only called once hasPermission() is true
private fun readOnce(context: Context, onResult: (Location?) -> Unit) {
    val manager = context.getSystemService(LocationManager::class.java)
    if (manager == null || !hasPermission(context)) {
        onResult(null)
        return
    }
    val enabled = manager.getProviders(true)
    val lastKnown = enabled.mapNotNull { manager.getLastKnownLocation(it) }.maxByOrNull { it.elapsedRealtimeNanos }
    if (lastKnown != null && ageMinutes(lastKnown) <= RECENT_MINUTES) {
        onResult(lastKnown)
        return
    }
    val active = enabled.filter { it != LocationManager.PASSIVE_PROVIDER }
    if (active.isEmpty()) {
        onResult(lastKnown)
        return
    }

    val main = ContextCompat.getMainExecutor(context)
    val handler = Handler(Looper.getMainLooper())
    val signals = active.map { CancellationSignal() }
    var done = false
    var pending = active.size
    // Every callback below runs on the main thread, so these plain vars are safe.
    fun finish(location: Location?) {
        if (done) return
        done = true
        handler.removeCallbacksAndMessages(null)
        signals.forEach { it.cancel() }
        onResult(location ?: lastKnown)
    }
    handler.postDelayed({ finish(null) }, WAIT_MS)
    active.forEachIndexed { i, provider ->
        LocationManagerCompat.getCurrentLocation(manager, provider, signals[i], main) { location ->
            pending--
            if (location != null || pending == 0) finish(location)
        }
    }
}

private fun ageMinutes(location: Location): Long =
    (SystemClock.elapsedRealtimeNanos() - location.elapsedRealtimeNanos) / 60_000_000_000L

/** A reading this recent is used without asking for a new one. */
private const val RECENT_MINUTES = 30L

/** How long to wait for a new reading before falling back to the last one. */
private const val WAIT_MS = 8_000L
