package com.can.oz

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

// Nota bilgisi için pozisyon tablosu
val notePositionMap = mapOf(
    "C4" to 1f, // Boşluk ya da çizgi oranı (0-1 arası)
    "D4" to 0.9f,
    "E4" to 0.8f,
    "F4" to 0.7f,
    "G4" to 0.6f,
    "A4" to 0.5f,
    "B4" to 0.4f,
    "C5" to 0.3f
)

@Composable
fun Porte(
    modifier: Modifier = Modifier,
    currentNote: String = ""
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(140.dp)
            .padding(bottom = 64.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        // 5 Porte çizgisi
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 0.dp),
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            repeat(5) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .background(Color.Black)
                )
            }
        }

        // Sol Anahtar
        Image(
            painter = painterResource(id = R.drawable.ic_treble_clef),
            contentDescription = "Sol Anahtarı",
            modifier = Modifier
                .height(140.dp)
                .aspectRatio(0.4f)
                .align(Alignment.CenterStart)
                .padding(start = 8.dp)
        )

        // Mevcut nota varsa göster
        if (currentNote.isNotEmpty()) {
            val yPositionRatio = notePositionMap[currentNote] ?: 0.5f
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .align(Alignment.TopStart)
                    .offset(
                        x = 100.dp,
                        y = (yPositionRatio * 140).dp
                    )
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(Color.Red)
                }
            }
        }
    }
}
