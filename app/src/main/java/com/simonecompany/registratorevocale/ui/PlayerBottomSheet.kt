package com.simonecompany.registratorevocale.ui

import android.media.MediaPlayer
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.simonecompany.registratorevocale.model.Recording
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PlayerBottomSheet(
    recording: Recording,
    onClose: () -> Unit
) {
    var isPlaying by remember { mutableStateOf(false) }
    var position by remember { mutableFloatStateOf(0f) }
    var duration by remember { mutableFloatStateOf(1f) }

    val context = LocalContext.current
    val player = remember { MediaPlayer() }

    DisposableEffect(recording) {
        onDispose {
            runCatching { player.stop() }
            runCatching { player.release() }
        }
    }

    LaunchedEffect(recording) {
        position = 0f
        isPlaying = false
        runCatching { player.stop() }
        runCatching { player.reset() }
        runCatching {
            player.setDataSource(context, recording.uri)
            player.prepare()
            duration = player.duration.toFloat().coerceAtLeast(1f)
        }
    }

    fun togglePlay() {
        if (isPlaying) {
            runCatching { player.pause() }
        } else {
            runCatching { player.start() }
        }
        isPlaying = !isPlaying
    }

    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            runCatching { position = player.currentPosition.toFloat() }
            if (position >= duration) {
                isPlaying = false
                runCatching { player.pause() }
            }
            delay(100)
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
            progress = { (position / duration).coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(4.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            Text(formatMillis(position.toLong()), style = MaterialTheme.typography.labelSmall)
            Spacer(Modifier.weight(1f))
            Text(formatMillis(duration.toLong()), style = MaterialTheme.typography.labelSmall)
        }

        Spacer(Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { togglePlay() }) {
                Icon(
                    if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = "Play/Pause",
                    modifier = Modifier.size(40.dp)
                )
            }
            IconButton(onClick = {
                isPlaying = false
                runCatching { player.pause() }
                runCatching { player.seekTo(0) }
                position = 0f
            }) {
                Icon(Icons.Default.Stop, contentDescription = "Stop",
                    modifier = Modifier.size(40.dp))
            }
        }

        Spacer(Modifier.height(8.dp))
    }
}

private fun formatMillis(millis: Long): String {
    val totalSecs = millis / 1000
    val mins = totalSecs / 60
    val secs = totalSecs % 60
    return String.format(Locale.getDefault(), "%02d:%02d", mins, secs)
}
