package com.can.oz

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun PorteAnimation(isRunning: Boolean, currentNote: String) {
    var position by remember { mutableStateOf(0f) }
    var displayText by remember { mutableStateOf("🎵 🎶 🎵 🎶 🎵 ") }

    LaunchedEffect(isRunning) {
        while (isRunning) {
            delay(100)
            position -= 5f
            displayText = if (currentNote.isNotBlank()) {
                "🎵 🎶 🎵 🎶 🎵 "
            } else {
                " . . . . . "
            }
        }
    }

    Box(
        Modifier
            .fillMaxWidth()
            .height(50.dp)
            .background(Color.Black),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = displayText,
            color = Color.White,
            fontSize = 28.sp,
            modifier = Modifier.offset(x = position.dp)
        )
    }
}
