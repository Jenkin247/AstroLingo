package com.astrolingo.app.spike

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.astrolingo.app.ui.theme.AstroLingoTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Debug-only screen for the encryption and restore spike. Steps are in docs/spikes/encryption-restore.md. */
class EncryptionSpikeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { AstroLingoTheme { SpikeScreen() } }
    }
}

@Composable
private fun SpikeScreen() {
    val context = LocalContext.current
    val spike = remember { EncryptionSpike(context.applicationContext) }
    val scope = rememberCoroutineScope()
    var results by remember { mutableStateOf<List<SpikeCheck>>(emptyList()) }
    var running by remember { mutableStateOf(false) }

    fun run(action: suspend EncryptionSpike.() -> List<SpikeCheck>) {
        running = true
        scope.launch {
            results = withContext(Dispatchers.IO) { spike.action() }
            running = false
        }
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Encryption check", style = MaterialTheme.typography.headlineSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { run { runAll() } }, enabled = !running) { Text("1. Run") }
                Button(onClick = { run { restoreCheck() } }, enabled = !running) { Text("2. Restore") }
                OutlinedButton(onClick = { run { clearAll() } }, enabled = !running) { Text("Clear") }
            }
            if (running) Text("Running…")
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(results) { check ->
                    Text(
                        text = (if (check.passed) "✓ " else "✗ ") + check.name + "\n" + check.detail,
                        color = if (check.passed) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}
