package com.can.oz

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun SignalToolBox(
    amplitude: Float,
    onAmplitudeChange: (Float) -> Unit,
    frequency: Float,
    onFrequencyChange: (Float) -> Unit,
    phase: Float,
    onPhaseChange: (Float) -> Unit,
    timerSpeedMs: Long,
    onTimerSpeedChange: (Long) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(8.dp),
        color = Color.DarkGray.copy(alpha = 0.7f)
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(text = "Timer Speed: ${timerSpeedMs}ms")
            Slider(
                value = timerSpeedMs.toFloat(),
                onValueChange = { onTimerSpeedChange(it.toLong()) },
                valueRange = 10f..1000f,
                steps = 98
            )
        }
    }
}
