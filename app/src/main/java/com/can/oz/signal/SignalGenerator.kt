package com.can.oz.signal

import kotlin.math.PI
import kotlin.math.sin

class SignalGenerator(
    var amplitude: Float = 1f,
    var frequency: Float = 440f,
    var phase: Float = 0f
) {

    fun generateSamples(
        sampleRate: Int = 44100,
        durationSec: Float = 1f
    ): FloatArray {
        val totalSamples = (sampleRate * durationSec).toInt()
        val samples = FloatArray(totalSamples)

        for (i in 0 until totalSamples) {
            val t = i.toFloat() / sampleRate
            samples[i] = amplitude * sin(2 * PI.toFloat() * frequency * t + phase)
        }

        return samples
    }
}
