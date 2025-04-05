package com.example.ozproject

data class Nota(
    val isim: String, // "Do", "Re", "Mi" vs.
    val yPositionRatio: Float // 0.0f ile 1.0f arasında, porte yüksekliğine oranlı
)
