
// rust/audio_engine/adsr.rs
#[derive(Debug, Copy, Clone, PartialEq)]
enum EnvState { Idle, Attack, Decay, Sustain, Release }

pub struct ADSR {
    attack_time: f32,   // saniye cinsinden Attack süresi
    decay_time: f32,    // saniye cinsinden Decay süresi
    sustain_level: f32, // 0.0-1.0 aralığında Sustain genlik seviyesi
    release_time: f32,  // saniye cinsinden Release süresi
    sample_rate: f32,   // örnekleme hızı (Hz)
    state: EnvState,
    level: f32,         // mevcut zarf genlik değeri [0.0-1.0]
    attack_inc: f32,    // Attack evresindeki her örnek başına artış
    decay_dec: f32,     // Decay evresindeki her örnek başına azalış
    release_dec: f32,   // Release evresindeki her örnek başına azalış
}
impl ADSR {
    pub fn new(sample_rate: f32, attack: f32, decay: f32, sustain: f32, release: f32) -> Self {
        let mut env = ADSR {
            attack_time: attack,
            decay_time: decay,
            sustain_level: sustain,
            release_time: release,
            sample_rate,
            state: EnvState::Idle,
            level: 0.0,
            attack_inc: 0.0,
            decay_dec: 0.0,
            release_dec: 0.0,
        };
        // Önceden hesaplanan artış/azalış oranları:
        env.attack_inc = if attack > 0.0 { 1.0 / (attack * sample_rate) } else { 1.0 };
        env.decay_dec = if decay > 0.0 { (1.0 - sustain) / (decay * sample_rate) } else { 1.0 - sustain };
        // Release azalması, anlık seviyeye göre her tetiklenişte hesaplanacak.
        env.release_dec = 0.0;
        env
    }

    pub fn note_on(&mut self) {
        // Nota başlat: seviye sıfırlanır ve Attack evresi başlar
        self.state = EnvState::Attack;
        self.level = 0.0;
        // Attack evresi için artış hızı zaten attack_inc olarak saklı
    }

    pub fn note_off(&mut self) {
        // Nota kes: Release evresine geç, mevcut seviyeden 0'a inmeye hazırlan
        if self.state != EnvState::Idle && self.state != EnvState::Release {
            self.state = EnvState::Release;
            // Mevcut seviyeden 0'a release_time süresince azalacak
            self.release_dec = if self.release_time > 0.0 {
                self.level / (self.release_time * self.sample_rate)
            } else {
                self.level  // release_time çok küçükse, direkt bitir
            };
        }
    }

    pub fn next_amplitude(&mut self) -> f32 {
        match self.state {
            EnvState::Idle => {
                // Pasif durumda genlik 0
                0.0
            }
            EnvState::Attack => {
                self.level += self.attack_inc;
                if self.level >= 1.0 {
                    // Zirveye ulaştı, Decay evresine geç
                    self.level = 1.0;
                    self.state = EnvState::Decay;
                }
                self.level
            }
            EnvState::Decay => {
                self.level -= self.decay_dec;
                if self.level <= self.sustain_level {
                    // Sustain seviyesine ulaştı, Sustain evresine geç
                    self.level = self.sustain_level;
                    self.state = EnvState::Sustain;
                }
                self.level
            }
            EnvState::Sustain => {
                // Nota basılı tutulduğu sürece sabit sustain seviyesi
                self.sustain_level
            }
            EnvState::Release => {
                self.level -= self.release_dec;
                if self.level <= 0.0 {
                    // Ses tamamıyla azaldı, zarf kapandı
                    self.level = 0.0;
                    self.state = EnvState::Idle;
                }
                self.level
            }
        }
    }

    pub fn is_idle(&self) -> bool {
        self.state == EnvState::Idle
    }
}
