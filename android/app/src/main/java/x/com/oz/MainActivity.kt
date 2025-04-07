package x.com.oz
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlin.math.roundToInt

class MainActivity : AppCompatActivity() {

    // UI Elements
    private lateinit var frequencyInput: EditText
    private lateinit var durationInput: EditText
    private lateinit var modFreqInput: EditText
    private lateinit var modIndexInput: EditText
    private lateinit var attackInput: SeekBar
    private lateinit var decayInput: SeekBar
    private lateinit var sustainInput: SeekBar
    private lateinit var releaseInput: SeekBar
    private lateinit var playButton: Button
    private lateinit var frequencyText: TextView
    private lateinit var modTypeText: TextView
    // UI Elements
    private lateinit var frequencyGraph: FrequencyView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize UI elements
        frequencyInput = findViewById(R.id.frequency_input)
        durationInput = findViewById(R.id.duration_input)
        modFreqInput = findViewById(R.id.mod_freq_input)
        modIndexInput = findViewById(R.id.mod_index_input)
        attackInput = findViewById(R.id.attack_input)
        decayInput = findViewById(R.id.decay_input)
        sustainInput = findViewById(R.id.sustain_input)
        releaseInput = findViewById(R.id.release_input)
        playButton = findViewById(R.id.play_button)
        frequencyText = findViewById(R.id.frequency_text)
        modTypeText = findViewById(R.id.mod_type_text)
        frequencyGraph = findViewById(R.id.frequency_graph)

        // Set default values for sliders and inputs
        attackInput.max = 100
        decayInput.max = 100
        sustainInput.max = 100
        releaseInput.max = 100

        // Play button click listener
        playButton.setOnClickListener {
            onPlayButtonClicked()
        }
    }

    private fun onPlayButtonClicked() {
        // Collect parameters from UI
        val frequency = frequencyInput.text.toString().toFloatOrNull() ?: 440f // Default A4 = 440Hz
        val duration = durationInput.text.toString().toFloatOrNull() ?: 1.0f // Default 1 second
        val modFreq = modFreqInput.text.toString().toFloatOrNull() ?: 5.0f // Default modulation frequency (AM/FM)
        val modIndex = modIndexInput.text.toString().toFloatOrNull() ?: 1.0f // Default modulation index
        

        val attackTime = attackInput.progress.toFloat() / 100f // Float'a dönüştürülmeli
        val decayTime = decayInput.progress.toFloat() / 100f
        val sustainLevel = sustainInput.progress.toFloat() / 100f
        val releaseTime = releaseInput.progress.toFloat() / 100f
        


        // Show frequency text for debugging
        frequencyText.text = "Frequency: ${frequency.roundToInt()} Hz"

        // Choose modulation type (AM/FM)
        val modType = modTypeText.text.toString()
        frequencyGraph.frequency = frequency
        frequencyInput.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                val freq = s.toString().toFloatOrNull() ?: 440f
                frequencyText.text = "Frequency: ${freq.roundToInt()} Hz"
                frequencyGraph.frequency = freq
            }
        })

        // Call Rust bridge to generate the sound
        generateSound(frequency, duration, modFreq, modIndex, modType, attackTime, decayTime, sustainLevel, releaseTime)
    }

    private fun generateSound(frequency: Float, duration: Float, modFreq: Float, modIndex: Float, modType: String,
                              attackTime: Float, decayTime: Float, sustainLevel: Float, releaseTime: Float) {

        // Generate waveform (sine wave)
        val sampleRate = 44100 // Standard sample rate for audio
        val waveform = RustBridge.generateSineWave(frequency, duration, sampleRate)
        // Apply modulation (AM or FM)
// AM ve FM için modülasyon fonksiyonlarını çağırırken diziyi doğru geçirdiğinizden emin olun.
        val modulatedWave = if (modType == "AM") {
            RustBridge.applyAM(waveform, modFreq, modIndex, sampleRate)
        } else {
            RustBridge.applyFM(waveform, modFreq, modIndex, sampleRate)
        }

        // Apply ADSR envelope to the modulated wave
        val finalWave = RustBridge.applyADSR(modulatedWave, attackTime, decayTime, sustainLevel, releaseTime, sampleRate)



        // Play the final wave using Android Audio APIs
        AudioPlayer.play(finalWave)
    }
} /*
// android/app/src/main/java/x/com/oz/MainActivity.kt
package x.com.oz

import android.app.Activity
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Bundle
import android.widget.Button

class MainActivity : Activity() {
    // Ses parametreleri
    private val sampleRate: Int = 44100
    private val bufferFrames: Int = 1024  // Her seferinde üretilecek örnek sayısı (yaklaşık 23ms @44100Hz)
    private lateinit var audioTrack: AudioTrack
    private var audioThread: Thread? = null
    @Volatile private var isPlaying: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Basit bir arayüz: Bir "Çal" düğmesi, bir "Durdur" düğmesi (ayrıntılı UI için XML vs. kullanılabilir)
        val playButton = Button(this).apply { text = "Play Note" }
        val stopButton = Button(this).apply { text = "Stop" }
        // (Layout ekleme kodları varsayılıyor)

        // Rust tarafı synthesizer'ı başlat
        //RustBridge.initSynth(sampleRate)
        // Örnek olarak bir FM modülasyon uygulayalım: mod frekansı 5 Hz (vibrato gibi), sapma 10 Hz
        //ustBridge.setModulation(2, 5.0f, 10.0f)
        // (Dilersek waveform veya AM de ayarlanabilir: RustBridge.setWaveform(0) vb.)

        // AudioTrack yapılandır: PCM float format, mono kanal, düşük gecikme modu
        val minBufSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_FLOAT
        )
        audioTrack = AudioTrack.Builder()
            .setAudioAttributes(android.media.AudioAttributes.Builder()
                .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                .setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
            )
            .setAudioFormat(android.media.AudioFormat.Builder()
                .setSampleRate(sampleRate)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
                .build()
            )
            .setBufferSizeInBytes(minBufSize)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        playButton.setOnClickListener {
            if (!isPlaying) {
                isPlaying = true
                // Bir örnek nota (Orta C, MIDI 60) çal
                RustBridge.noteOn(60)
                audioTrack.play()
                // Arka planda ses işleme thread'i başlat
                audioThread = Thread {
                    val audioBuffer = FloatArray(bufferFrames)
                    while (isPlaying) {
                        // Rust'tan ses örneklerini al
                        RustBridge.renderAudio(audioBuffer, bufferFrames)
                        // AudioTrack'e yaz
                        audioTrack.write(audioBuffer, 0, bufferFrames, AudioTrack.WRITE_BLOCKING)
                    }
                }.apply { start() }
            }
        }

        stopButton.setOnClickListener {
            if (isPlaying) {
                // Nota bırak ve ses üretimini durdur
                RustBridge.noteOff()
                isPlaying = false
                // Zarfın release süresi boyunca biraz bekleyip (click/pop engellemek için)
                Thread.sleep(100)
                audioTrack.pause()
                audioTrack.flush()
            }
        }
    }

    override fun onDestroy() {
        // Uygulama kapanırken ses thread'ini durdur
        isPlaying = false
        audioThread?.join(1000)
        audioTrack.release()
        super.onDestroy()
    }
}
*/