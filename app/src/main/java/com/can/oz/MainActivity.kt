package com.can.oz

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.annotation.RequiresPermission
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.can.oz.audio.AudioRecorder
import com.can.oz.ui.widgets.FftProcessor
import com.can.oz.signal.SignalGenerator
import com.can.oz.ui.theme.OzTheme
import com.can.oz.ui.widgets.DigitalTimer
import com.can.oz.ui.widgets.FftProcessor.FftSpectrumView
import kotlinx.coroutines.*
import kotlin.math.PI

@OptIn(ExperimentalAnimationApi::class)
class MainActivity : ComponentActivity() {
    private var isRecording by mutableStateOf(false)
    private lateinit var audioRecorder: AudioRecorder
    private var microphonePermissionGranted by mutableStateOf(false)
    private var isRedLineVisible by mutableStateOf(false)
    private val audioVariations = listOf("Format 1", "Format 2", "Format 3")
    private var selectedFormat by mutableStateOf(audioVariations[0])

    private val visibleSamples = mutableStateListOf<Float>()
    private val fftData = mutableStateListOf<Float>()

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    private fun startRecording() {
        isRecording = true
        visibleSamples.clear()
        fftData.clear()
        audioRecorder.startRecording { buffer ->
            CoroutineScope(Dispatchers.IO).launch {
                val normalized = buffer.map { it.toFloat() / Short.MAX_VALUE }
                withContext(Dispatchers.Main) {
                    visibleSamples.addAll(normalized)
                    if (visibleSamples.size > 1024) {
                        visibleSamples.removeRange(0, visibleSamples.size - 1024)
                    }

                    if (visibleSamples.size >= 1024) {
                        val fftInput = visibleSamples.takeLast(1024).toFloatArray()
                        val fftResult = FftProcessor.computeFFT(fftInput)
                        fftData.clear()
                        fftData.addAll(fftResult.toList())
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // AudioRecorder nesnesini başlatıyoruz
        // Initialize AudioRecorder instance
        audioRecorder = AudioRecorder()

        setContent {
            // Uygulamanın temasını ayarlıyoruz
            // Set application theme
            OzTheme {

                // Mikrofon izni istemek için launcher oluşturuyoruz
                // Create launcher to request microphone permission
                val permissionLauncher = rememberLauncherForActivityResult(RequestPermission()) { isGranted ->
                    microphonePermissionGranted = isGranted
                    if (isGranted && isRecording) startRecording()
                }

                // Uygulama başlarken izni kontrol ediyoruz
                // Check permission on app start
                LaunchedEffect(Unit) {
                    if (!microphonePermissionGranted) {
                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                }

                // Popup menüyü kontrol eden state
                // State for showing popup menu
                var showPopup by remember { mutableStateOf(false) }

                // Üst panelin görünürlüğünü kontrol eden state
                // State for showing swipe-down control panel
                var isPanelVisible by remember { mutableStateOf(false) }

                // Sinyal üretici parametreleri
                // Signal generator parameters
                var amplitude by remember { mutableStateOf(1f) }
                var frequency by remember { mutableStateOf(440f) }
                var phase by remember { mutableStateOf(0f) }

                // Sinyal üretecek nesne
                // Object to generate synthetic signal
                val generator = remember { SignalGenerator() }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            // Swipe-down hareketi ile panel aç/kapat
                            // Open/close panel with swipe-down gesture
                            detectVerticalDragGestures { _, dragAmount ->
                                if (dragAmount > 15) isPanelVisible = true
                                if (dragAmount < -15) isPanelVisible = false
                            }
                        },
                    verticalArrangement = Arrangement.Top,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    // Kontrol panelini gösteriyoruz
                    // Show signal control panel
                    AnimatedVisibility(visible = isPanelVisible) {
                        SignalControlPanel(
                            amplitude = amplitude,
                            onAmplitudeChange = { amplitude = it },
                            frequency = frequency,
                            onFrequencyChange = { frequency = it },
                            phase = phase,
                            onPhaseChange = { phase = it }
                        )
                    }

                    // Mikrofon verisinden ham sinyal dalgası çizimi
                    // Draw raw microphone waveform
                    SignalWaveformView(samples = visibleSamples)

                    Spacer(modifier = Modifier.height(16.dp))

                    // FFT spektrum çizimi
                    // Draw FFT frequency spectrum
                    FftSpectrumView(frequencies = fftData)

                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {

                        // Dijital sayaç (kayıt süresi)
                        // Digital timer (recording duration)
                        RecordingTimer(isRecording)

                        // Eğer kayıt yapılıyorsa kırmızı çizgi çiz
                        // Draw red line if recording
                        if (isRedLineVisible) RedLine()

                        // Orta buton (Kayıt başlat/durdur + Uzun basınca Popup menü açılır)
                        // Main center button (Start/Stop recording + Long press opens popup menu)
                        RoundImageButton(
                            onLongClick = { showPopup = true },
                            onClick = {
                                if (microphonePermissionGranted) {
                                    if (isRecording) {
                                        isRedLineVisible = false
                                        stopRecording()
                                    } else {
                                        isRedLineVisible = true
                                        startRecording()
                                    }
                                }
                            }
                        )

                        // Popup menüyü göster
                        // Show popup menu
                        this@Column.AnimatedVisibility(visible = showPopup) {
                            FullScreenPopupContent(
                                onDismiss = { showPopup = false },
                                onFormatSelected = { selectedFormat = it }
                            )
                        }
                    }
                }
            }
        }
    }


    private fun stopRecording() {
        isRecording = false
        audioRecorder.stopRecording()
    }

    @Composable
    fun SignalWaveformView(samples: List<Float>) {
        Canvas(modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .padding(vertical = 8.dp)
        ) {
            if (samples.isEmpty()) return@Canvas
            val midY = size.height / 2
            val widthPerStep = size.width / samples.size.coerceAtLeast(1)

            samples.forEachIndexed { i, value ->
                val x = i * widthPerStep
                val y = midY + (value * midY * 0.8f)
                drawLine(
                    color = Color.Magenta,
                    start = Offset(x, midY),
                    end = Offset(x, y),
                    strokeWidth = 2f
                )
            }
        }
    }

    @Composable
    fun SignalControlPanel(
        amplitude: Float,
        onAmplitudeChange: (Float) -> Unit,
        frequency: Float,
        onFrequencyChange: (Float) -> Unit,
        phase: Float,
        onPhaseChange: (Float) -> Unit
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Amplitude: %.2f".format(amplitude), modifier = Modifier.width(100.dp))
                Slider(
                    value = amplitude,
                    onValueChange = onAmplitudeChange,
                    valueRange = 0f..1f,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Frequency: %.0f Hz".format(frequency), modifier = Modifier.width(100.dp))
                Slider(
                    value = frequency,
                    onValueChange = onFrequencyChange,
                    valueRange = 20f..2000f,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Phase: %.2f π".format(phase / PI.toFloat()), modifier = Modifier.width(100.dp))
                Slider(
                    value = phase,
                    onValueChange = onPhaseChange,
                    valueRange = 0f..(2 * PI).toFloat(),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }

    @Composable
    fun RecordingTimer(isRecording: Boolean) {
        var secondsElapsed by remember { mutableStateOf(0) }

        LaunchedEffect(isRecording) {
            if (isRecording) {
                secondsElapsed = 0
                while (isRecording) {
                    delay(1000)
                    secondsElapsed++
                }
            }
        }

        if (isRecording) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 60.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                DigitalTimer(
                    timeSeconds = secondsElapsed,
                    segmentColor = Color.Green
                )
            }
        }
    }

    @Composable
    fun FullScreenPopupContent(onDismiss: () -> Unit, onFormatSelected: (String) -> Unit) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = Color(0x99000000),
            modifier = Modifier.fillMaxSize().padding(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "Select Audio Format:", color = Color.White)
                audioVariations.forEach { format ->
                    Text(
                        text = format,
                        color = Color.White,
                        modifier = Modifier
                            .padding(8.dp)
                            .pointerInput(Unit) {
                                detectTapGestures(onTap = { onFormatSelected(format) })
                            }
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = onDismiss) {
                    Text("Close")
                }
            }
        }
    }

    @Composable
    fun RoundImageButton(onLongClick: () -> Unit, onClick: () -> Unit) {
        Surface(
            modifier = Modifier.size(155.dp),
            shape = CircleShape,
            color = Color.Transparent,
            shadowElevation = 10.dp
        ) {
            IconButton(
                onClick = onClick,
                modifier = Modifier.fillMaxSize().pointerInput(Unit) {
                    detectTapGestures(onLongPress = { onLongClick() })
                }
            ) {
                Image(
                    painter = painterResource(id = R.drawable.oz),
                    contentDescription = "Round Button",
                    modifier = Modifier.size(100.dp)
                )
            }
        }
    }

    @Composable
    fun RedLine() {
        Box(modifier = Modifier
            .fillMaxWidth()
            .height(2.dp)) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawLine(
                    color = Color.Red,
                    start = Offset(0f, size.height / 2),
                    end = Offset(x = size.width, y = size.height / 2),
                    strokeWidth = 2f
                )
            }
        }
    }

    @Preview(showBackground = true)
    @Composable
    fun PreviewRoundImageButton() {
        OzTheme {
            RoundImageButton({}, {})
        }
    }
}
