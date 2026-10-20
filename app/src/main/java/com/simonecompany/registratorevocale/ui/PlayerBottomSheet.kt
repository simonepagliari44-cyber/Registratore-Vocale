package com.simonecompany.registratorevocale.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.simonecompany.registratorevocale.model.Recording
import com.simonecompany.registratorevocale.service.PlaybackController
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PlayerBottomSheet(
    recording: Recording,
    onClose: () -> Unit,
    onRename: (String) -> Unit
) {
    val context = LocalContext.current

    val current by PlaybackController.current.collectAsState()
    val ctrlPlaying by PlaybackController.isPlaying.collectAsState()
    val ctrlPosition by PlaybackController.positionMs.collectAsState()
    val ctrlDuration by PlaybackController.durationMs.collectAsState()

    val active = current?.uri == recording.uri
    val isPlaying = active && ctrlPlaying
    val position = if (active) ctrlPosition else 0L
    val duration = (if (active && ctrlDuration > 0) ctrlDuration else recording.durationMillis)
        .coerceAtLeast(1L)

    var showRename by remember { mutableStateOf(false) }
    var newName by remember(recording) {
        mutableStateOf(recording.name.substringBeforeLast('.'))
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {}

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val date = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        .format(Date(recording.createdAt))

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Riproduzione", style = MaterialTheme.typography.titleLarge)

        Spacer(Modifier.height(16.dp))

        Text(
            text = recording.name,
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(Modifier.height(4.dp))

        Text(
            text = date,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(20.dp))

        LinearProgressIndicator(
            progress = { (position.toFloat() / duration).coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .pointerInput(duration) {
                    detectTapGestures { offset ->
                        if (active) {
                            val fraction = (offset.x / size.width).coerceIn(0f, 1f)
                            PlaybackController.seekTo((fraction * duration).toLong())
                        }
                    }
                }
        )

        Spacer(Modifier.height(4.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            Text(formatMillis(position), style = MaterialTheme.typography.labelSmall)
            Spacer(Modifier.weight(1f))
            Text(formatMillis(duration), style = MaterialTheme.typography.labelSmall)
        }

        Spacer(Modifier.height(8.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(onClick = {
                if (isPlaying) {
                    PlaybackController.pause(context)
                } else {
                    PlaybackController.play(context, recording)
                }
            }) {
                Icon(
                    if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = "Play/Pause",
                    modifier = Modifier.size(40.dp)
                )
            }
            IconButton(onClick = {
                if (active) PlaybackController.stop(context)
            }) {
                Icon(Icons.Default.Stop, contentDescription = "Stop",
                    modifier = Modifier.size(40.dp))
            }
            IconButton(onClick = { shareRecording(context, recording) }) {
                Icon(Icons.Default.Share, contentDescription = "Condividi",
                    modifier = Modifier.size(32.dp))
            }
            IconButton(onClick = { showRename = true }) {
                Icon(Icons.Default.Edit, contentDescription = "Rinomina",
                    modifier = Modifier.size(32.dp))
            }
        }

        Spacer(Modifier.height(8.dp))
    }

    if (showRename) {
        AlertDialog(
            onDismissRequest = { showRename = false },
            title = { Text("Rinomina registrazione") },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    singleLine = true,
                    label = { Text("Nome") }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val trimmed = newName.trim()
                        showRename = false
                        if (trimmed.isNotEmpty()) onRename(trimmed)
                    }
                ) {
                    Text("Salva")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRename = false }) {
                    Text("Annulla")
                }
            }
        )
    }
}

private fun shareRecording(context: android.content.Context, recording: Recording) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "audio/*"
        putExtra(Intent.EXTRA_STREAM, recording.uri)
        putExtra(Intent.EXTRA_SUBJECT, recording.name)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    runCatching {
        context.startActivity(Intent.createChooser(intent, "Condividi registrazione"))
    }
}

private fun formatMillis(millis: Long): String {
    val totalSecs = millis / 1000
    val mins = totalSecs / 60
    val secs = totalSecs % 60
    return String.format(Locale.getDefault(), "%02d:%02d", mins, secs)
}
