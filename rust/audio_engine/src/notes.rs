// /home/kurmes/StudioProjects/Oz/rust/audio_engine/src/notes.rs
const A4_MIDI: u8 = 69;
const A4_FREQ: f32 = 440.0;

pub fn midi_to_freq(midi: u8) -> f32 {
    A4_FREQ * 2.0_f32.powf((midi as f32 - A4_MIDI as f32) / 12.0)
}

pub fn name_to_freq(note: &str) -> Option<f32> {
    if note.is_empty() { 
        return None;
    }
    
    let mut chars = note.chars();
    let letter = chars.next().unwrap().to_uppercase().to_string();
    let accidental = chars.next().filter(|&c| c == '#' || c == 'b');
    let octave_str: String = chars.collect();
    let octave: i32 = octave_str.parse().ok()?;
    
    let letter_index = match (letter.as_str(), accidental.unwrap_or('\0')) {
        ("C", _) => 0, 
        ("C#", _) | ("Db", _) => 1,
        ("D", _) => 2,
        ("D#", _) | ("Eb", _) => 3,
        ("E", _) => 4,
        ("F", _) => 5,
        ("F#", _) | ("Gb", _) => 6,
        ("G", _) => 7,
        ("G#", _) | ("Ab", _) => 8,
        ("A", _) => 9,
        ("A#", _) | ("Bb", _) => 10,
        ("B", _) => 11,
        _ => return None,
    };
    
    let midi = (octave + 1) * 12 + letter_index;
    Some(midi_to_freq(midi as u8))
}

// Remove the duplicate midi_note_to_freq function