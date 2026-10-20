package com.simonecompany.registratorevocale.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.simonecompany.registratorevocale.data.AudioQuality
import com.simonecompany.registratorevocale.data.AudioRecorder
import com.simonecompany.registratorevocale.data.SettingsStore
import com.simonecompany.registratorevocale.model.Recording
import kotlinx.coroutines.delay
import java.util.Locale

private const val WAVE_BARS = 48

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordScreen(
    recorder: AudioRecorder,
    onRecordingStarted: () -> Unit,
    onRecordingSaved: (Recording) -> Unit
) {
    var isRecording by remember { mutableStateOf(recorder.isRecording) }
    var isPaused by remember { mutableStateOf(recorder.isPaused) }
    var elapsed by remember { mutableFloatStateOf(0f) }
    val levels = remember { mutableStateListOf<Float>() }

    val context = LocalContext.current
    var quality by remember { mutableStateOf(SettingsStore.getQuality(context)) }

    LaunchedEffect(isRecording, isPaused) {
        while (isRecording && !isPaused) {
            elapsed = recorder.elapsedMillis() / 1000f
            val amplitude = recorder.currentAmplitude() / 32767f
            val level = kotlin.math.sqrt(amplitude.coerceIn(0f, 1f))
            levels.add(level)
            if (levels.size > WAVE_BARS) levels.removeAt(0)
            delay(60)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Registratore Vocale") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = formatTime(elapsed),
                style = MaterialTheme.typography.displaySmall
            )

            Spacer(Modifier.height(16.dp))

            WaveformBar(
                levels = levels,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(96.dp)
            )

            Spacer(Modifier.height(12.dp))

            Text(
                text = when {
                    isPaused -> "In pausa"
                    isRecording -> "Registrazione in corso"
                    else -> "Tocca per iniziare a registrare"
                },
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(36.dp))

            if (!isRecording) {
                Text(
                    text = "Qualità",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(8.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AudioQuality.entries.forEach { option ->
                        FilterChip(
                            selected = quality == option,
                            onClick = {
                                quality = option
                                SettingsStore.setQuality(context, option)
                            },
                            label = { Text(option.label) }
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))

                RecordButton(
                    onClick = {
                        if (recorder.start(quality)) {
                            elapsed = 0f
                            isPaused = false
                            levels.clear()
                            isRecording = true
                            onRecordingStarted()
                        }
                    }
                )
            } else {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(32.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ControlButton(
                        icon = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                        contentDescription = if (isPaused) "Riprendi" else "Pausa",
                        container = MaterialTheme.colorScheme.primary,
                        onClick = {
                            if (isPaused) {
                                if (recorder.resume()) isPaused = false
                            } else {
                                if (recorder.pause()) {
                                    elapsed = recorder.elapsedMillis() / 1000f
                                    isPaused = true
                                }
                            }
                        }
                    )
                    ControlButton(
                        icon = Icons.Default.Stop,
                        contentDescription = "Stop",
                        container = MaterialTheme.colorScheme.error,
                        onClick = {
                            recorder.stop()?.let { onRecordingSaved(it) }
                            isRecording = false
                            isPaused = false
                            elapsed = 0f
                            levels.clear()
                        }
                    )
                }
            }
        }
    }
}

private fun formatTime(seconds: Float): String {
    val s = seconds.toInt()
    val mins = s / 60
    val secs = s % 60
    return String.format(Locale.getDefault(), "%02d:%02d", mins, secs)
}

@Composable
private fun RecordButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(120.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.PlayArrow,
            contentDescription = "Registra",
            modifier = Modifier.size(56.dp),
            tint = Color.White
        )
    }
}

@Composable
private fun ControlButton(
    icon: ImageVector,
    contentDescription: String,
    container: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(96.dp)
            .clip(CircleShape)
            .background(container)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(44.dp),
            tint = Color.White
        )
    }
}

@Composable
private fun WaveformBar(levels: List<Float>, modifier: Modifier = Modifier) {
    val activeColor = MaterialTheme.colorScheme.primary
    val idleColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
    val data = remember(levels.toList()) { levels.toList() }

    Canvas(modifier = modifier) {
        val barWidth = size.width / WAVE_BARS
        val stroke = (barWidth * 0.55f).coerceAtLeast(2f)
        val centerY = size.height / 2f
        val minHalf = stroke / 2f
        val maxHalf = size.height / 2f

        for (i in 0 until WAVE_BARS) {
            val index = i - (WAVE_BARS - data.size)
            val raw = if (index >= 0 && index < data.size) data[index] else 0f
            val half = (raw.coerceIn(0f, 1f) * (maxHalf - minHalf) + minHalf)
            val x = i * barWidth + barWidth / 2f
            drawLine(
                color = if (raw > 0f) activeColor else idleColor,
                start = Offset(x, centerY - half),
                end = Offset(x, centerY + half),
                strokeWidth = stroke,
                cap = StrokeCap.Round
            )
        }
    }
}
