package com.can.oz.signal

import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object FftProcessor {

    fun computeFFT(real: FloatArray): FloatArray {
        val n = real.size
        if (n == 0 || (n and (n - 1)) != 0) {
            throw IllegalArgumentException("FFT input size must be a power of 2")
        }

        val realPart = real.copyOf()
        val imagPart = FloatArray(n)

        var step = 1
        while (step < n) {
            val jump = step shl 1
            val deltaAngle = (-2.0 * Math.PI / jump).toFloat()
            val sinHalf = sin(deltaAngle / 2)
            val multiplier = -2 * sinHalf * sinHalf
            val phaseShift = sin(deltaAngle)

            for (group in 0 until step) {
                var wr = 1f
                var wi = 0f
                for (pair in group until n step jump) {
                    val match = pair + step
                    val tempr = wr * realPart[match] - wi * imagPart[match]
                    val tempi = wr * imagPart[match] + wi * realPart[match]

                    realPart[match] = realPart[pair] - tempr
                    imagPart[match] = imagPart[pair] - tempi

                    realPart[pair] += tempr
                    imagPart[pair] += tempi
                }
                val wtemp = wr
                wr += wr * multiplier - wi * phaseShift
                wi += wi * multiplier + wtemp * phaseShift
            }
            step = jump
        }

        // Magnitude hesapla
        val magnitudes = FloatArray(n / 2)
        for (i in magnitudes.indices) {
            magnitudes[i] = sqrt(realPart[i] * realPart[i] + imagPart[i] * imagPart[i])
        }
        return magnitudes
    }
}
