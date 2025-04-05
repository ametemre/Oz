package com.can.oz.ui.widgets

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.math.log10

@Composable
fun FftSpectrumView(magnitudes: List<Float>) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
    ) {
        if (magnitudes.isEmpty()) return@Canvas

        val barWidth = size.width / magnitudes.size
        val maxMagnitude = magnitudes.maxOrNull()?.takeIf { it > 0f } ?: 1f

        magnitudes.forEachIndexed { index, value ->
            val normalizedDb = 20 * log10((value / maxMagnitude).coerceAtLeast(1e-5f))
            val dbScaled = (normalizedDb + 100) / 100 // normalize dB to 0..1
            val barHeight = size.height * dbScaled

            drawLine(
                color = Color.Cyan,
                start = Offset(x = index * barWidth, y = size.height),
                end = Offset(x = index * barWidth, y = size.height - barHeight),
                strokeWidth = barWidth * 0.8f
            )
        }
    }
}
