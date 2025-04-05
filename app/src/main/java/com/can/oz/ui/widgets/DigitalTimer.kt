package com.can.oz.ui.widgets

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun DigitalTimer(timeSeconds: Int, modifier: Modifier = Modifier, segmentColor: Color = Color.Green, segmentWidth: Dp = 4.dp) {
    val minutes = timeSeconds / 60
    val seconds = timeSeconds % 60
    val digits = String.format("%02d%02d", minutes, seconds).map { it.digitToInt() }

    Row(modifier = modifier, horizontalArrangement = Arrangement.Center) {
        digits.forEachIndexed { index, digit ->
            SevenSegmentDigit(
                digit = digit,
                color = segmentColor,
                segmentWidth = segmentWidth,
                modifier = Modifier
                    .size(width = 28.dp, height = 48.dp)
                    .padding(horizontal = 2.dp)
            )

            if (index == 1) {
                Spacer(modifier = Modifier.width(4.dp))
                Colon(color = segmentColor)
                Spacer(modifier = Modifier.width(4.dp))
            }
        }
    }
}

@Composable
fun SevenSegmentDigit(digit: Int, color: Color, segmentWidth: Dp, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        drawSevenSegmentDigit(this, digit, color, segmentWidth.toPx())
    }
}

fun drawSevenSegmentDigit(scope: DrawScope, digit: Int, color: Color, stroke: Float) {
    val w = scope.size.width
    val h = scope.size.height
    val s = stroke

    val segments = listOf(
        listOf(0,1,2,4,5,6),    // 0
        listOf(2,5),           // 1
        listOf(0,2,3,4,6),     // 2
        listOf(0,2,3,5,6),     // 3
        listOf(1,2,3,5),       // 4
        listOf(0,1,3,5,6),     // 5
        listOf(0,1,3,4,5,6),   // 6
        listOf(0,2,5),         // 7
        listOf(0,1,2,3,4,5,6), // 8
        listOf(0,1,2,3,5,6)    // 9
    )

    val segCoords = listOf<() -> Pair<Offset, Offset>>(
        { Offset(s, s) to Offset(w - s, s) },                      // Top
        { Offset(s, s) to Offset(s, h / 2 - s) },                  // Top-left
        { Offset(w - s, s) to Offset(w - s, h / 2 - s) },          // Top-right
        { Offset(s, h / 2) to Offset(w - s, h / 2) },              // Middle
        { Offset(s, h / 2 + s) to Offset(s, h - s) },              // Bottom-left
        { Offset(w - s, h / 2 + s) to Offset(w - s, h - s) },      // Bottom-right
        { Offset(s, h - s) to Offset(w - s, h - s) }               // Bottom
    )

    for (i in segments[digit]) {
        val (start, end) = segCoords[i]()
        scope.drawLine(color = color, start = start, end = end, strokeWidth = stroke)
    }
}

@Composable
fun Colon(color: Color) {
    Canvas(modifier = Modifier.size(12.dp, 48.dp)) {
        val r = 2.dp.toPx()
        val cx = size.width / 2
        drawCircle(color = color, radius = r, center = Offset(cx, size.height / 3))
        drawCircle(color = color, radius = r, center = Offset(cx, 2 * size.height / 3))
    }
}
