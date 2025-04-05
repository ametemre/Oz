package com.example.ozproject

import kotlin.math.log2
import kotlin.math.abs

object RealTimeNoteDetector {

    // 1. Frekansı hesapla (autocorrelation yöntemi)
    fun calculateFrequency(buffer: ShortArray, sampleRate: Int): Float {
        val size = buffer.size
        var maxCorrelation = 0
        var bestLag = 0

        for (lag in 10..(size / 2)) {
            var correlation = 0
            for (i in 0 until size - lag) {
                correlation += buffer[i] * buffer[i + lag]
            }
            if (correlation > maxCorrelation) {
                maxCorrelation = correlation
                bestLag = lag
            }
        }

        return if (bestLag != 0) {
            sampleRate.toFloat() / bestLag
        } else {
            0f
        }
    }

    // 2. Frekansı nota ismine çevir
    fun frequencyToNoteName(frequency: Float): String {
        if (frequency <= 0f) return ""

        val noteNames = listOf(
            "C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"
        )

        val n = (12 * log2(frequency / 440.0) + 69).toInt()
        val noteIndex = (n % 12 + 12) % 12
        val octave = n / 12 - 1

        return "${noteNames[noteIndex]}$octave"
    }
}
