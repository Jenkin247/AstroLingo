// Counts how long a card is on screen while the app is in front, for the daily goal (LS-4).
package com.astrolingo.app.learn

import android.os.SystemClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.astrolingo.learn.progress.StudyTime

/**
 * Times the card identified by [key]. When the card changes, the app goes to the background or the screen closes,
 * [onCredit] gets the seconds it was on screen, capped by [StudyTime.credit]. Zero seconds are not reported.
 */
@Composable
internal fun StudyTimer(key: Any, onCredit: (Int) -> Unit) {
    val credit by rememberUpdatedState(onCredit)
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(key, lifecycle) {
        var startedAt: Long? = null
        fun stop() {
            startedAt?.let { start ->
                val seconds = StudyTime.credit(SystemClock.elapsedRealtime() - start)
                if (seconds > 0) credit(seconds)
            }
            startedAt = null
        }
        // Adding the observer replays ON_RESUME when the app is already in front, which starts the clock.
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> startedAt = SystemClock.elapsedRealtime()
                Lifecycle.Event.ON_PAUSE -> stop()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            stop()
        }
    }
}
