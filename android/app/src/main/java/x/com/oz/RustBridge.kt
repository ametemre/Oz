package x.com.oz

object RustBridge {
    init {
        System.loadLibrary("ozsynth") // Looks for libozsynth.so
    }
    // JNI ile eşlenen native fonksiyon tanımları:
    external fun initSynth(sampleRate: Int)
    external fun setWaveform(waveId: Int)
    external fun setModulation(mode: Int, modFreq: Float, modIndex: Float)
    external fun noteOn(midiNote: Int)
    external fun noteOff()
    external fun renderAudio(buffer: FloatArray, length: Int)
    // Native function declarations (implemented in Rust library)
    external fun generateSineWave(freq: Float, duration: Float, sampleRate: Int): FloatArray
    external fun applyAM(
        carrier: FloatArray,
        modulator: Float,
        modulationIndex: Float,
        sampleRate: Int
    ): FloatArray
    external fun applyFM(baseFrequency: FloatArray, modSignal: Float, frequencyDeviation: Float, sampleRate: Int): FloatArray
    external fun applyADSR(
        samples: FloatArray, sampleRate: Float,
        attack: Float, decay: Float, sustain: Float, release: Int
    ): FloatArray
    external fun noteToFrequency(midiNote: Int): Float
}
