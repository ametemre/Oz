package com.can.oz

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import java.io.ByteArrayOutputStream
import java.io.File

class Recorder(private val outputFile: File) {
    private var audioRecord: AudioRecord? = null
    private var isRecording = false

    fun startRecording() {
        val bufferSize = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize
        )

        audioRecord?.startRecording()
        isRecording = true

        Thread {
            val outputStream = ByteArrayOutputStream()
            val buffer = ByteArray(bufferSize)

            while (isRecording) {
                val read = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                if (read > 0) {
                    outputStream.write(buffer, 0, read)
                }
            }

            // Kayıt bittiğinde: WAV dosyasını oluştur
            val audioData = outputStream.toByteArray()
            WavFileWriter.writeWavFile(outputFile, audioData, SAMPLE_RATE)

            outputStream.close()
        }.start()
    }

    fun stopRecording() {
        isRecording = false
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
    }

    companion object {
        const val SAMPLE_RATE = 44100
    }
}
