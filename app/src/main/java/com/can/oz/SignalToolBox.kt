package com.can.oz

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp


import androidx.compose.foundation.layout.*
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
    var expanded by remember { mutableStateOf(false) }
    var dragOffset by remember { mutableStateOf(0f) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .pointerInput(Unit) {
                detectVerticalDragGestures { _, dragAmount ->
                    dragOffset += dragAmount
                    if (dragOffset > 100f) {
                        expanded = true
                        dragOffset = 0f
                    } else if (dragOffset < -100f) {
                        expanded = false
                        dragOffset = 0f
                    }
                }
            }
            .padding(top = 8.dp)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp),
            color = Color.Gray.copy(alpha = 0.5f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = if (expanded) "Swipe Up to Hide" else "Swipe Down to Show",
                    color = Color.White
                )
            }
        }

        AnimatedVisibility(visible = expanded) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                color = Color.DarkGray.copy(alpha = 0.7f),
                shadowElevation = 4.dp
            ) {
                SignalControlPanel(
                    amplitude = amplitude,
                    onAmplitudeChange = onAmplitudeChange,
                    frequency = frequency,
                    onFrequencyChange = onFrequencyChange,
                    phase = phase,
                    onPhaseChange = onPhaseChange,
                    timerSpeedMs = timerSpeedMs,
                    onTimerSpeedChange = onTimerSpeedChange
                )
            }
        }
    }
}

@Composable
fun SignalControlPanel(
    amplitude: Float,
    onAmplitudeChange: (Float) -> Unit,
    frequency: Float,
    onFrequencyChange: (Float) -> Unit,
    phase: Float,
    onPhaseChange: (Float) -> Unit,
    timerSpeedMs: Long,
    onTimerSpeedChange: (Long) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Amplitude: %.2f".format(amplitude), modifier = Modifier.width(100.dp))
            Slider(
                value = amplitude,
                onValueChange = onAmplitudeChange,
                valueRange = 0f..1f,
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Frequency: %.0f Hz".format(frequency), modifier = Modifier.width(100.dp))
            Slider(
                value = frequency,
                onValueChange = onFrequencyChange,
                valueRange = 20f..2000f,
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Phase: %.2f π".format(phase / Math.PI.toFloat()), modifier = Modifier.width(100.dp))
            Slider(
                value = phase,
                onValueChange = onPhaseChange,
                valueRange = 0f..(2 * Math.PI).toFloat(),
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Timer: ${timerSpeedMs}ms", modifier = Modifier.width(100.dp))
            Slider(
                value = timerSpeedMs.toFloat(),
                onValueChange = { onTimerSpeedChange(it.toLong()) },
                valueRange = 10f..1000f,
                steps = 98,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
