package com.can.oz

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun RecordingTimer(isRecording: Boolean, timerSpeedMs: Long) {
    var elapsedMs by remember { mutableStateOf(0L) }

    LaunchedEffect(isRecording) {
        if (isRecording) {
            elapsedMs = 0L
            while (isRecording) {
                delay(timerSpeedMs)
                elapsedMs += timerSpeedMs
            }
        }
    }

    if (isRecording) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 60.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            Text(
                text = formatElapsedTime(elapsedMs),
                color = Color(0xFFCCCCCC),
                fontSize = 36.sp
            )
        }
    }
}

fun formatElapsedTime(ms: Long): String {
    val seconds = (ms / 1000) % 60
    val minutes = (ms / 1000) / 60
    val millis = (ms % 1000) / 100
    return "%02d:%02d.%d".format(minutes, seconds, millis)
}
