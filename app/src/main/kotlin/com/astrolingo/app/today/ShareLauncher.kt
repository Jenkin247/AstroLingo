// Starts the share sheet for Share the day (LS-11). Tests swap it through LocalShareLauncher to read the intent
// without starting another activity, which left the next test's screen stuck under Robolectric.
package com.astrolingo.app.today

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.staticCompositionLocalOf

/** Opens a ready-made share chooser [Intent]. */
fun interface ShareLauncher {
    fun launch(context: Context, chooser: Intent)
}

/** The real share sheet. */
val LocalShareLauncher = staticCompositionLocalOf { ShareLauncher { context, chooser -> context.startActivity(chooser) } }
