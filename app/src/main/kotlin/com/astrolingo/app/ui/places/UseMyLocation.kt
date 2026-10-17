// "Use my location": one coarse location reading, snapped to the nearest bundled city. Only that city is kept,
// encrypted with the profile; the reading itself is never stored, logged or sent, and nothing runs in the background.
package com.astrolingo.app.ui.places

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
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

/** One reading from the best enabled provider, falling back to the last known one; null if there is none. */
@SuppressLint("MissingPermission") // only called once hasPermission() is true
private fun readOnce(context: Context, onResult: (Location?) -> Unit) {
    val manager = context.getSystemService(LocationManager::class.java)
    val enabled = manager?.getProviders(true).orEmpty()
    val preferred = buildList {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) add(LocationManager.FUSED_PROVIDER)
        add(LocationManager.NETWORK_PROVIDER)
        add(LocationManager.GPS_PROVIDER)
        add(LocationManager.PASSIVE_PROVIDER)
    }
    val provider = preferred.firstOrNull { it in enabled }
    if (manager == null || provider == null || !hasPermission(context)) {
        onResult(null)
        return
    }
    LocationManagerCompat.getCurrentLocation(manager, provider, CancellationSignal(), ContextCompat.getMainExecutor(context)) { location ->
        onResult(location ?: enabled.firstNotNullOfOrNull { manager.getLastKnownLocation(it) })
    }
}
