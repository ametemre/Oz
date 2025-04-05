package com.can.oz

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
fun RoundImageButton(onLongClick: () -> Unit, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.size(155.dp),
        shape = CircleShape,
        color = Color.Transparent,
        shadowElevation = 10.dp
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier.fillMaxSize()
        ) {
            Image(
                painter = painterResource(id = R.drawable.oz),
                contentDescription = "Round Button",
                modifier = Modifier.size(100.dp)
            )
        }
    }
}

@Composable
fun DraggableMiniFab(iconRes: Int, onClick: () -> Unit) {
    var offset by remember { mutableStateOf(Offset.Zero) }

    Box(
        Modifier
            .offset { IntOffset(offset.x.toInt(), offset.y.toInt()) }
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    offset += dragAmount
                }
            }
    ) {
        MiniFabButton(iconRes = iconRes, onClick = onClick)
    }
}

@Composable
fun MiniFabButton(iconRes: Int, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.size(65.dp),
        shape = CircleShape,
        shadowElevation = 5.dp
    ) {
        IconButton(onClick = onClick) {
            Image(
                painter = painterResource(id = iconRes),
                contentDescription = "Mini FAB",
                modifier = Modifier.size(45.dp)
            )
        }
    }
}

@Composable
fun PorteAnimation(isRunning: Boolean) {
    var position by remember { mutableStateOf(0f) }

    LaunchedEffect(isRunning) {
        while (isRunning) {
            delay(50)
            position -= 5f
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
            text = "🎵🎶🎵🎶🎵🎶",
            color = Color.White,
            modifier = Modifier.offset(x = position.dp)
        )
    }
}
