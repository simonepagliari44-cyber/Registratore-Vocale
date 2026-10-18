package com.simonecompany.registratorevocale.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.simonecompany.registratorevocale.data.AudioRecorder
import com.simonecompany.registratorevocale.model.Recording
import kotlinx.coroutines.delay
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordScreen(
    recorder: AudioRecorder,
    onRecordingStarted: () -> Unit,
    onRecordingSaved: (Recording) -> Unit
) {
    var isRecording by remember { mutableStateOf(recorder.isRecording) }
    var elapsed by remember { mutableFloatStateOf(0f) }
    var wavePhase by remember { mutableFloatStateOf(0f) }

    val infiniteTransition = rememberInfiniteTransition(label = "wave")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    LaunchedEffect(isRecording) {
        while (isRecording) {
            elapsed = ((System.currentTimeMillis() - recorder.startTime) / 1000f)
            wavePhase = phase
            delay(100)
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
                active = isRecording,
                phase = wavePhase,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
            )

            Spacer(Modifier.height(40.dp))

            RecordButton(
                isRecording = isRecording,
                onClick = {
                    if (isRecording) {
                        recorder.stop()?.let { onRecordingSaved(it) }
                        isRecording = false
                    } else {
                        recorder.start()
                        elapsed = 0f
                        isRecording = true
                        onRecordingStarted()
                    }
                }
            )
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
private fun RecordButton(isRecording: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(120.dp)
            .clip(CircleShape)
            .background(if (isRecording) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.primary)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.PlayArrow,
            contentDescription = if (isRecording) "Stop" else "Registra",
            modifier = Modifier.size(56.dp),
            tint = Color.White
        )
    }
}

@Composable
private fun WaveformBar(active: Boolean, phase: Float, modifier: Modifier = Modifier) {
    val barColor = if (active) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)

    Canvas(modifier = modifier) {
        val barCount = 24
        val barWidth = size.width / barCount
        for (i in 0 until barCount) {
            val normalized = (i / barCount.toFloat())
            val base = 0.08f + 0.12f * kotlin.math.abs(kotlin.math.sin(normalized * 3.14f * 2f + phase * 6.28f))
            val activeHeight = base * size.height
            val staticHeight = 0.12f * size.height

            val h = if (active) activeHeight else staticHeight
            val left = i * barWidth
            drawRect(
                color = barColor,
                topLeft = androidx.compose.ui.geometry.Offset(
                    left + barWidth * 0.25f,
                    (size.height - h) / 2
                ),
                size = androidx.compose.ui.geometry.Size(barWidth * 0.5f, h)
            )
        }
    }
}
