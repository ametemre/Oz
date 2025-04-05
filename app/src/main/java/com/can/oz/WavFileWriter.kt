package com.can.oz

import java.io.File
import java.io.FileOutputStream

object WavFileWriter {
    fun writeWavFile(file: File, audioData: ByteArray, sampleRate: Int) {
        val totalDataLen = audioData.size + 36
        val byteRate = 16 * sampleRate / 8

        val header = byteArrayOf(
            'R'.code.toByte(), 'I'.code.toByte(), 'F'.code.toByte(), 'F'.code.toByte(),
            (totalDataLen and 0xff).toByte(),
            (totalDataLen shr 8 and 0xff).toByte(),
            (totalDataLen shr 16 and 0xff).toByte(),
            (totalDataLen shr 24 and 0xff).toByte(),
            'W'.code.toByte(), 'A'.code.toByte(), 'V'.code.toByte(), 'E'.code.toByte(),
            'f'.code.toByte(), 'm'.code.toByte(), 't'.code.toByte(), ' '.code.toByte(),
            16, 0, 0, 0,   // Subchunk1Size for PCM
            1, 0,          // AudioFormat (1 = PCM)
            1, 0,          // NumChannels (1 = mono)
            (sampleRate and 0xff).toByte(),
            (sampleRate shr 8 and 0xff).toByte(),
            (sampleRate shr 16 and 0xff).toByte(),
            (sampleRate shr 24 and 0xff).toByte(),
            (byteRate and 0xff).toByte(),
            (byteRate shr 8 and 0xff).toByte(),
            (byteRate shr 16 and 0xff).toByte(),
            (byteRate shr 24 and 0xff).toByte(),
            (2).toByte(), 0,    // BlockAlign
            (16).toByte(), 0,   // BitsPerSample
            'd'.code.toByte(), 'a'.code.toByte(), 't'.code.toByte(), 'a'.code.toByte(),
            (audioData.size and 0xff).toByte(),
            (audioData.size shr 8 and 0xff).toByte(),
            (audioData.size shr 16 and 0xff).toByte(),
            (audioData.size shr 24 and 0xff).toByte()
        )

        val fos = FileOutputStream(file)
        fos.write(header)
        fos.write(audioData)
        fos.close()
    }
}
