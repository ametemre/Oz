package x.com.oz

import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack

object AudioPlayer {
    fun play(samples: FloatArray) {
        // Örnek: AudioTrack kullanarak ses çalmak
        val sampleRate = 44100
        val buffer = ShortArray(samples.size)

        // Float verisini Short'a dönüştür
        for (i in samples.indices) {
            buffer[i] = (samples[i] * Short.MAX_VALUE).toInt().toShort()
        }

        // AudioTrack nesnesini oluştur
        val audioTrack = AudioTrack(
            AudioManager.STREAM_MUSIC,
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            buffer.size * 2,
            AudioTrack.MODE_STREAM
        )

        // Ses çalmaya başla
        audioTrack.play()
        audioTrack.write(buffer, 0, buffer.size)
    }
}
