# MainActivity - Context-Oz-ALL Belgelendirme

Bu belge, `Oz` projesindeki `MainActivity.kt` dosyasının yapısını, görevlerini ve olay akışını "Context-Oz-ALL" standardı altında açıklar.

---

## 📌 Full Comment (Genel Açıklama)

Bu dosya, "Oz" projesinin ana aktivitesini yönetir.

İçerdiği Başlıca İşlemler:
- Mikrofon verisinden gerçek zamanlı nota tespiti (Context-Oz-Recognition)
- Gürültü analiz ve eşik kontrolü (Context-Oz-Noise)
- Kaydedilen seslerin işlenmesi ve görselleştirilmesi (Context-Oz-Noise, Context-Oz-Threading)
- Metronom modu ve gerçek zamanlı Porte animasyonu (Context-Oz-Porte)
- Çoklu panel yönetimi ve swipe ile geçiş (Context-Oz-ALL)
- Signal Generator ile test sinyali oluşturulması (Context-Oz-Noise)
- CPU/UI thread ayrımı ile performans optimizasyonu (Context-Oz-Threading)

Kullanılan Bileşenler:
- AudioRecorder
- RustBridge
- Porte
- MetronomeControlPanel
- DigitalTimer
- SignalGenerator
- NoiseTestPanel

---

## 📌 Panel Hiyerarşisi ve Yapı

```
MainActivity
└── setContent { OzTheme }
    └── Column (Ana Kolon)
        ├── Spacer
        ├── AnimatedVisibility (Panel Aç/Kapat)
        │   └── AnimatedContent (Panel Geçişi)
        │       ├── AnalyzerPanel (0)
        │       └── MetronomeControlPanel (1)
        ├── Spacer
        ├── Box (Porte ve Alt UI)
        │   ├── Porte (Nota veya Metronom)
        │   ├── Column
        │   │   ├── RecordingTimer
        │   │   ├── RedLine
        │   │   ├── RoundImageButton
        │   │   ├── MiniFabContainer (Kayıtlı dosyalar)
        │   │   └── FullScreenPopupContent
        │   └── SoundWaveVisualization
        └── FileNameDialog (Kayıt sonrası isim seçimi)
```

---

## 📌 Event Trigger Akış Haritası

```
Swipe Up/Down
    └── Panel Aç/Kapat (isPanelVisible)

Swipe Left/Right
    └── Analyzer / Metronom Panel Geçişi

RoundImageButton Tıklama
    ├── Eğer Kayıt Aktifse -> stopRecording()
    └── Eğer Kayıt Kapalıysa -> startRecording()

Mikrofon İzni Alınınca
    └── Eğer isRecording True -> startRecording()

Recorder'dan Veri Geldikçe
    ├── RustBridge ile dominant frequency analizi
    ├── currentFrequency ve currentMaxMagnitude güncellemesi
    ├── updateNote() ile notalama
    └── Dalga formu güncellemesi

MetronomeControlPanel Start/Stop
    └── isMetronomeRunning güncellenir
    └── Porte mode değişir

Format Popup Açılırsa
    └── FullScreenPopupContent görünür

Kayıt Tamamlanınca
    └── FileNameDialog açılır
    └── Dosya kaydedilir veya iptal edilir
```

---

## 📌 ToDo Listesi (Yapılacaklar)

- [x] Mikrofon erişim izni ve kayıt
- [x] Dalga formu ve FFT analizi
- [x] Gürültü eşiği kontrolü
- [x] Porte ile nota çizimi
- [x] Metronom animasyonu
- [x] Mini-Fab'lar ile kayıtlı dosya yönetimi
- [x] Swipe ile panel değişimi
- [x] Kayıt sonrası dosya isimlendirme
- [x] NoiseTestPanel entegrasyonu
- [ ] RustBridge tabanlı noise filtering
- [ ] Chord detection desteği (çoklu nota)
- [ ] Spektrogram görselleştirme
- [ ] SignalToolBox genişletmesi

---

## 📌 İlgili Context-Oz Başlıkları

- Context-Oz-ALL
- Context-Oz-Noise
- Context-Oz-Porte
- Context-Oz-Recognition
- Context-Oz-Threading

---

