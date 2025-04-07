// rust/audio_engine/synth.rs
use std::f32::consts::PI;
use crate::adsr::ADSR;

use crate::notes::midi_to_freq;

#[derive(Copy, Clone)]
pub enum Waveform {
    Sine,
    Square,
    Saw,
    Triangle,
}

pub struct Oscillator {
    waveform: Waveform,
    freq: f32,       // Frekans (Hz)
    phase: f32,      // [0, 2π) faz akümülatörü
    sample_rate: f32,
    phase_increment: f32, // 2π * freq / sample_rate
}

impl Oscillator {
    pub fn new(waveform: Waveform, freq: f32, sample_rate: f32) -> Self {
        Oscillator {
            waveform,
            freq,
            phase: 0.0,
            sample_rate,
            phase_increment: 2.0 * PI * freq / sample_rate,
        }
    }

    #[inline]
    pub fn set_freq(&mut self, freq: f32) {
        self.freq = freq;
        self.phase_increment = 2.0 * PI * freq / self.sample_rate;
    }

    #[inline]
    pub fn next_sample(&mut self) -> f32 {
        // Gelecek örnek için sinyal üret ve fazı ilerlet
        let out = match self.waveform {
            Waveform::Sine => self.phase.sin(),
            Waveform::Square => if self.phase.sin() >= 0.0 { 1.0 } else { -1.0 },
            Waveform::Saw => 2.0 * (self.phase / (2.0 * PI)) - 1.0,      // -1 ile 1 arası testere
            Waveform::Triangle => {
                // Üçgen dalga: -1 ile 1 arası lineer inip çıkış
                if self.phase < PI {
                    // 0->PI arası -1'den +1'e
                    -1.0 + (2.0 * (self.phase / PI))
                } else {
                    // PI->2PI arası +1'den -1'e
                    1.0 - (2.0 * ((self.phase - PI) / PI))
                }
            }
        };
        // Fazı ilerlet ve 2π'yı aştıysa sar (wrap) işlemi
        self.phase += self.phase_increment;
        if self.phase >= 2.0 * PI {
            self.phase -= 2.0 * PI;
        }
        out
    }
}

#[derive(Copy, Clone, PartialEq)]
pub enum Modulation {
    None,
    AM,
    FM,
}

pub struct Synthesizer {
    sample_rate: f32,
    carrier: Oscillator,
    modulator: Oscillator,
    adsr: ADSR,
    modulation: Modulation,
    mod_index: f32,       // AM için genlik derinliği veya FM için frekans sapma miktarı
    base_freq: f32,       // Taşıyıcı osilatörün temel frekansı (nota frekansı)
}

impl Synthesizer {
    pub fn new(sample_rate: f32) -> Self {
        // Varsayılan: Sine dalgası, modülasyon yok, ADSR parametreleri makul bir şekilde ayarlanıyor
        Synthesizer {
            sample_rate,
            carrier: Oscillator::new(Waveform::Sine, 440.0, sample_rate),    // başlangıç frekansı A4=440Hz
            modulator: Oscillator::new(Waveform::Sine, 0.0, sample_rate),    // modülatör başlangıçta kapalı (0 Hz)
            adsr: ADSR::new(sample_rate, 0.01, 0.1, 0.8, 0.3),  // hızlı atak, kısa decay, %80 sustain, orta release
            modulation: Modulation::None,
            mod_index: 0.0,
            base_freq: 440.0,
        }
    }

    pub fn set_waveform(&mut self, wave: Waveform) {
        self.carrier.waveform = wave;
        // modülatör dalga biçimini de değiştirebiliriz (gerekirse):
        self.modulator.waveform = wave;
    }

    pub fn set_modulation(&mut self, mode: Modulation, mod_freq: f32, mod_index: f32) {
        self.modulation = mode;
        self.mod_index = mod_index;
        if mode != Modulation::None {
            // Modülasyon etkinse modülatör frekansını ve faz artışını ayarla
            self.modulator.set_freq(mod_freq);
            // Not: AM için mod_index genlik modülasyon derinliğini (0-1 arası), 
            // FM için mod_index frekans sapma miktarını (Hz cinsinden) temsil eder.
        } else {
            // Modülasyon kapalıysa modülatörü sıfırla
            self.modulator.set_freq(0.0);
            self.modulator.phase = 0.0;
        }
    }

    pub fn note_on(&mut self, midi_note: u8) {
        self.base_freq = midi_to_freq(midi_note);
        // Yeni nota basıldı: frekansı hesapla ve taşıyıcı osilatörü ayarla
        // self.base_freq = midi_note_to_freq(midi_note);                     --------------------------dikkat!!! DİKKAT
        self.carrier.set_freq(self.base_freq);
        self.carrier.phase = 0.0;
        // Modülatörü de sıfırla (faz reset) - özellikle FM için başlangıç fazını sabitlemek faydalı olabilir
        self.modulator.phase = 0.0;
        // Zarfı tetikle (Attack evresine gir)
        self.adsr.note_on();
    }

    pub fn note_off(&mut self) {
        // Nota bırakıldı: zarfı salınıma (Release) geçir
        self.adsr.note_off();
    }

    pub fn generate_samples(&mut self, out_buffer: &mut [f32]) {
        // İstenen boyutta ses örneği üret
        for sample in out_buffer.iter_mut() {
            let carrier_sample;
            let mod_sample = if self.modulation != Modulation::None {
                // Modülasyon aktifse modülatör osilatörden örnek al
                self.modulator.next_sample()
            } else {
                0.0
            };
            match self.modulation {
                Modulation::None => {
                    // Modülasyon yok, taşıyıcıdan doğrudan örnek al
                    carrier_sample = self.carrier.next_sample();
                }
                Modulation::AM => {
                    // Amplitüd Modülasyonu: taşıyıcı genliğini modülatör ile çarp
                    // Burada modülatör -1..1 aralığında; genlik modülasyonu için 0..1 aralığına taşıyoruz
                    let amplitude_mod = 0.5 * (mod_sample + 1.0) * self.mod_index;
                    carrier_sample = self.carrier.next_sample() * amplitude_mod;
                }
                Modulation::FM => {
                    // Frekans Modülasyonu: taşıyıcının frekansını modülatör çıktısına göre ayarla
                    // mod_index, frekans sapma genişliğini temsil eder (Hz). mod_sample -1..1 aralığında.
                    let current_freq = self.base_freq + mod_sample * self.mod_index;
                    // Taşıyıcı fazını anlık frekansa göre ilerlet
                    self.carrier.phase += 2.0 * PI * current_freq / self.sample_rate;
                    if self.carrier.phase >= 2.0 * PI {
                        self.carrier.phase -= 2.0 * PI;
                    }
                    // Taşıyıcı dalga biçimine göre çıkış al (faz konumuna göre)
                    carrier_sample = match self.carrier.waveform {
                        Waveform::Sine => self.carrier.phase.sin(),
                        Waveform::Square => if self.carrier.phase.sin() >= 0.0 { 1.0 } else { -1.0 },
                        Waveform::Saw => 2.0 * (self.carrier.phase / (2.0 * PI)) - 1.0,
                        Waveform::Triangle => {
                            if self.carrier.phase < PI {
                                -1.0 + (2.0 * (self.carrier.phase / PI))
                            } else {
                                1.0 - (2.0 * ((self.carrier.phase - PI) / PI))
                            }
                        }
                    };
                }
            }
            // ADSR zarfını uygula (amplitüdü zarf değeriyle ölçekle)
            let env_amp = self.adsr.next_amplitude();
            *sample = carrier_sample * env_amp;
        }
        // Eğer zarf tamamen bittiyse (ses sönümlendiyse), carrier osilatör fazını sabit tutmaya gerek yok
        // (Not: İleri optimizasyon olarak zarf idle ise tüm örnekler 0 yapılabilir.)
    }
}
