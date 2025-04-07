use jni::{
    JNIEnv,
    objects::{JClass, JFloatArray, ReleaseMode},
    sys::{jint, jfloat, jfloatArray, jboolean},
};
use lazy_static::lazy_static;
use std::sync::Mutex;

mod adsr;
mod notes;
mod synth;

use synth::{Synthesizer, Modulation, Waveform};

lazy_static! {
    static ref SYNTH: Mutex<Synthesizer> = Mutex::new(Synthesizer::new(44100.0));
}

#[no_mangle]
pub extern "system" fn Java_x_com_oz_RustBridge_initSynth(
    _env: JNIEnv,
    _class: JClass,
    sample_rate: jint,
) -> jboolean {
    match SYNTH.lock() {
        Ok(mut synth) => {
            *synth = Synthesizer::new(sample_rate as f32);
            1
        }
        Err(_) => 0
    }
}

#[no_mangle]
pub extern "C" fn Java_x_com_oz_RustBridge_setWaveform(
    _env: JNIEnv,
    _class: JClass,
    wave_id: jint,
) {
    let mut synth = SYNTH.lock().unwrap();
    let waveform = match wave_id {
        1 => Waveform::Square,
        2 => Waveform::Saw,
        3 => Waveform::Triangle,
        _ => Waveform::Sine,
    };
    synth.set_waveform(waveform);
}

#[no_mangle]
pub extern "C" fn Java_x_com_oz_RustBridge_setModulation(
    _env: JNIEnv,
    _class: JClass,
    mode: jint,
    mod_freq: jfloat,
    mod_index: jfloat,
) {
    let mut synth = SYNTH.lock().unwrap();
    let modulation = match mode {
        1 => Modulation::AM,
        2 => Modulation::FM,
        _ => Modulation::None,
    };
    synth.set_modulation(modulation, mod_freq, mod_index);
}

#[no_mangle]
pub extern "C" fn Java_x_com_oz_RustBridge_noteOn(
    _env: JNIEnv,
    _class: JClass,
    midi_note: jint,
) {
    let mut synth = SYNTH.lock().unwrap();
    synth.note_on(midi_note as u8);
}

#[no_mangle]
pub extern "C" fn Java_x_com_oz_RustBridge_noteOff(
    _env: JNIEnv,
    _class: JClass,
) {
    let mut synth = SYNTH.lock().unwrap();
    synth.note_off();
}

#[no_mangle]
pub extern "system" fn Java_x_com_oz_RustBridge_renderAudio(
    mut env: JNIEnv,
    _class: JClass,
    buffer: jfloatArray,
    length: jint,
) {
    let mut synth = SYNTH.lock().unwrap();
    let len = length as usize;
    unsafe {
        let arr = JFloatArray::from_raw(buffer);
        let elements = env.get_array_elements(&arr, ReleaseMode::NoCopyBack)
            .expect("Failed to get array elements");
        let slice = std::slice::from_raw_parts_mut(elements.as_ptr() as *mut f32, len);
        synth.generate_samples(slice);
    }
}

#[no_mangle]
pub extern "system" fn Java_x_com_oz_RustBridge_generateSineWave(
    env: JNIEnv,
    _class: JClass,
    freq: jfloat,
    duration: jfloat,
    sample_rate: jint,
) -> jfloatArray {
    let total_samples = (duration * sample_rate as f32) as usize;
    let mut samples = Vec::with_capacity(total_samples);
    let two_pi = std::f32::consts::PI * 2.0;

    for i in 0..total_samples {
        let t = i as f32 / sample_rate as f32;
        samples.push((two_pi * freq * t).sin());
    }

    let output = env.new_float_array(samples.len() as i32).unwrap();
    env.set_float_array_region(&output, 0, &samples).unwrap();
    output.into_raw()
}

#[no_mangle]
pub extern "system" fn Java_x_com_oz_RustBridge_applyAM(
    mut env: JNIEnv,
    _class: JClass,
    carrier: jfloatArray,
    modulator: jfloat,
    modulation_index: jfloat,
    sample_rate: jint,
) -> jfloatArray {
    unsafe {
        let carrier_array = JFloatArray::from_raw(carrier);
        let carrier_data = env.get_array_elements(&carrier_array, ReleaseMode::NoCopyBack).unwrap();
        let len = env.get_array_length(&carrier_array).unwrap() as usize;
        let carrier_slice = std::slice::from_raw_parts(carrier_data.as_ptr() as *const f32, len);

        let mut modulated = Vec::with_capacity(len);
        let two_pi = std::f32::consts::PI * 2.0;

        for (i, &sample) in carrier_slice.iter().enumerate() {
            let t = i as f32 / sample_rate as f32;
            let mod_signal = (two_pi * t * modulator).sin();
            let am = 0.5 * (1.0 + mod_signal) * modulation_index;
            modulated.push(sample * am);
        }

        let out_array = env.new_float_array(len as i32).unwrap();
        env.set_float_array_region(&out_array, 0, &modulated).unwrap();
        out_array.into_raw()
    }
}

#[no_mangle]
pub extern "system" fn Java_x_com_oz_RustBridge_applyFM(
    mut env: JNIEnv,
    _class: JClass,
    base_frequency: jfloatArray,
    mod_signal: jfloat,
    frequency_deviation: jfloat,
    sample_rate: jint,
) -> jfloatArray {
    unsafe {
        let base_array = JFloatArray::from_raw(base_frequency);
        let base_data = env.get_array_elements(&base_array, ReleaseMode::NoCopyBack).unwrap();
        let len = env.get_array_length(&base_array).unwrap() as usize;
        let base_slice = std::slice::from_raw_parts(base_data.as_ptr() as *const f32, len);

        let mut modulated = Vec::with_capacity(len);
        let two_pi = std::f32::consts::PI * 2.0;

        for (i, &sample) in base_slice.iter().enumerate() {
            let t = i as f32 / sample_rate as f32;
            let freq_offset = mod_signal * (two_pi * t).sin() * frequency_deviation;
            modulated.push((two_pi * (sample + freq_offset) * t).sin());
        }

        let out_array = env.new_float_array(len as i32).unwrap();
        env.set_float_array_region(&out_array, 0, &modulated).unwrap();
        out_array.into_raw()
    }
}

#[no_mangle]
pub extern "system" fn Java_x_com_oz_RustBridge_applyADSR(
    mut env: JNIEnv,
    _class: JClass,
    samples: jfloatArray,
    attack: jfloat,
    decay: jfloat,
    sustain: jfloat,
    release: jint,
    sample_rate: jfloat,
) -> jfloatArray {
    unsafe {
        let samples_array = JFloatArray::from_raw(samples);
        let samples_data = env.get_array_elements(&samples_array, ReleaseMode::NoCopyBack).unwrap();
        let len = env.get_array_length(&samples_array).unwrap() as usize;
        let samples_slice = std::slice::from_raw_parts(samples_data.as_ptr() as *const f32, len);

        let mut output = Vec::with_capacity(len);
        let attack_samples = (attack * sample_rate) as usize;
        let decay_samples = (decay * sample_rate) as usize;
        let release_samples = (release as f32 * sample_rate) as usize;

        for (i, &sample) in samples_slice.iter().enumerate() {
            let amp = if i < attack_samples {
                i as f32 / attack_samples as f32
            } else if i < attack_samples + decay_samples {
                1.0 - ((i - attack_samples) as f32 / decay_samples as f32) * (1.0 - sustain)
            } else if i < len - release_samples {
                sustain
            } else {
                let release_pos = i - (len - release_samples);
                sustain * (1.0 - release_pos as f32 / release_samples as f32)
            };
            output.push(sample * amp);
        }

        let out_array = env.new_float_array(len as i32).unwrap();
        env.set_float_array_region(&out_array, 0, &output).unwrap();
        out_array.into_raw()
    }
}

#[no_mangle]
pub extern "C" fn Java_x_com_oz_RustBridge_noteToFrequency(
    _env: JNIEnv,
    _class: JClass,
    midi_note: jint,
) -> jfloat {
    const A4: f32 = 440.0;
    const A4_MIDI: i32 = 69;
    A4 * 2.0_f32.powf((midi_note as f32 - A4_MIDI as f32) / 12.0)
}
