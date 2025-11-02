package com.twinmind.voicenotes

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.twinmind.voicenotes.net.Net
import com.twinmind.voicenotes.recording.RecordingService
import java.io.BufferedReader
import java.io.InputStreamReader

class MainActivity : ComponentActivity() {

    private val micPermLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 33) {
            registerForActivityResult(ActivityResultContracts.RequestPermission()) {}.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        micPermLauncher.launch(Manifest.permission.RECORD_AUDIO)

        setContent {
            MaterialTheme {
                Surface(Modifier.fillMaxSize()) {
                    AppScreen()
                }
            }
        }
    }
}

@Composable
fun AppScreen() {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    var isRecording by remember { mutableStateOf(false) }
    var transcript by remember { mutableStateOf("") }
    var suggestions by remember { mutableStateOf(listOf<String>()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("TwinMind Voice Notes", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = {
                if (!isRecording) {
                    val intent = Intent(ctx, RecordingService::class.java)
                    ContextCompat.startForegroundService(ctx, intent)
                    isRecording = true
                }
            }) { Text("Start") }

            Button(onClick = {
                if (isRecording) {
                    ctx.stopService(Intent(ctx, RecordingService::class.java))
                    isRecording = false
                    val lastWav = RecordingService.lastWavFilePath
                    if (lastWav != null) {
                        Net.ioScope.launch {
                            try {
                                val text = Net.transcribeWav(lastWav)
                                transcript = text
                                suggestions = suggestFromContext(ctx, text)
                            } catch (e: Exception) {
                                transcript = "Transcription error: " + e.message
                            }
                        }
                    }
                }
            }) { Text("Stop & Transcribe") }
        }

        Spacer(Modifier.height(16.dp))
        Text("Transcript:", fontWeight = FontWeight.SemiBold)
        Text(transcript.ifBlank { "—" })

        Spacer(Modifier.height(16.dp))
        Text("Suggestions:", fontWeight = FontWeight.SemiBold)
        LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f)) {
            items(suggestions) { s -> Text("• " + s) }
        }
    }
}

fun suggestFromContext(ctx: android.content.Context, transcript: String): List<String> {
    return try {
        val input = ctx.assets.open("context/clinician_parent.txt")
        val reader = BufferedReader(InputStreamReader(input))
        val lines = reader.readLines()
        reader.close()
        val keywords = listOf("fever","pain","duration","allergy","medication","school","sleep","diet","vaccination","travel","injury")
        val tLower = transcript.lowercase()
        val matched = lines.filter { line ->
            val l = line.lowercase()
            keywords.any { k -> tLower.contains(k) && l.contains(k) }
        }
        (matched.ifEmpty { lines.take(5) }).take(5)
    } catch (e: Exception) {
        listOf("No context available (error: ${e.message})")
    }
}
