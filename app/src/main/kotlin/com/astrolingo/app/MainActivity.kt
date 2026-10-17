// Single activity hosting the whole Compose UI.
package com.astrolingo.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.astrolingo.app.profile.EncryptedProfileStore
import com.astrolingo.app.ui.AstroLingoApp
import com.astrolingo.app.ui.theme.AstroLingoTheme

/** Entry point of the app. Everything visible is drawn by [AstroLingoApp]. */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val profiles = EncryptedProfileStore(this)
        setContent {
            AstroLingoTheme {
                AstroLingoApp(profiles)
            }
        }
    }
}
