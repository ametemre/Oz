package com.can.oz

// MainActivity.kt
// --------------------------------------------------
// PLAN:
// 1. Mikrofon kaydı başlatıldığında gelen verileri "ham" olarak normalize edip saklıyoruz.
// 2. Ham sinyali sampling yapmadan doğrudan Canvas üzerinde çiziyoruz.
// 3. Sinyal parametrelerini (amplitude, frequency, phase) sliderlar ile kontrol ediyoruz.
// 4. SignalGenerator ile sentetik sinyal üretip onu da ayrı Canvas'ta gösterebiliriz.
// 5. Swipe-down hareketi ile SignalControlPanel açılıyor/kapanıyor.
// --------------------------------------------------

import android.Manifest
import android.content.pm.PackageManager
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
import androidx.core.app.ActivityCompat
import com.can.oz.audio.AudioRecorder
import com.can.oz.signal.FftProcessor
import com.can.oz.signal.SignalGenerator
import com.can.oz.ui.theme.OzTheme
import com.can.oz.ui.widgets.DigitalTimer
import com.can.oz.ui.widgets.FftSpectrumView
import kotlinx.coroutines.*
import kotlin.math.PI

@OptIn(ExperimentalAnimationApi::class)
class MainActivity : ComponentActivity() {

    // Ses kaydı ve UI durumları
    private var isRecording by mutableStateOf(false)
    private lateinit var audioRecorder: AudioRecorder
    private var microphonePermissionGranted by mutableStateOf(false)
    private var isRedLineVisible by mutableStateOf(false)
    private val audioVariations = listOf("Format 1", "Format 2", "Format 3")
    private var selectedFormat by mutableStateOf(audioVariations[0])

    // Ham gelen veriyi saklayacağımız liste
    private val visibleSamples = mutableStateListOf<Float>()
    private val fftSpectrum = mutableStateListOf<Float>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        audioRecorder = AudioRecorder()

        setContent {
            OzTheme {

                val permissionLauncher = rememberLauncherForActivityResult(RequestPermission()) { isGranted ->
                    microphonePermissionGranted = isGranted
                    if (ActivityCompat.checkSelfPermission(
                            this,
                            Manifest.permission.RECORD_AUDIO
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        // TODO: Consider calling
                        //    ActivityCompat#requestPermissions
                        // here to request the missing permissions, and then overriding
                        //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                        //                                          int[] grantResults)
                        // to handle the case where the user grants the permission. See the documentation
                        // for ActivityCompat#requestPermissions for more details.
                    }
                    if (isGranted && isRecording) startRecording()
                }

                LaunchedEffect(Unit) {
                    if (!microphonePermissionGranted) {
                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                }

                var showPopup by remember { mutableStateOf(false) }
                var isPanelVisible by remember { mutableStateOf(false) }

                var amplitude by remember { mutableStateOf(1f) }
                var frequency by remember { mutableStateOf(440f) }
                var phase by remember { mutableStateOf(0f) }
                val generator = remember { SignalGenerator() }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectVerticalDragGestures { _, dragAmount ->
                                if (dragAmount > 15) isPanelVisible = true
                                if (dragAmount < -15) isPanelVisible = false
                            }
                        },
                    verticalArrangement = Arrangement.Top,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    // Swipe ile açılan panel
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
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        RecordingTimer(isRecording)

                        if (isRedLineVisible) RedLine()

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
                    }
                }
            }
        }
    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    private fun startRecording() {
        isRecording = true
        visibleSamples.clear()
        fftSpectrum.clear()
        audioRecorder.startRecording { buffer ->
            CoroutineScope(Dispatchers.IO).launch {
                val normalized = buffer.map { it.toFloat() / Short.MAX_VALUE }
                withContext(Dispatchers.Main) {
                    visibleSamples.addAll(normalized)
                    if (visibleSamples.size > 1024) {
                        visibleSamples.removeRange(0, visibleSamples.size - 1024)
                    }

                    // FFT Hesapla
                    if (visibleSamples.size >= 512) {
                        val fftInput = visibleSamples.takeLast(512).toFloatArray()
                        val spectrum = FftProcessor.computeFFT(fftInput)
                        fftSpectrum.clear()
                        fftSpectrum.addAll(spectrum.take(fftInput.size / 2)) // sadece pozitif frekanslar
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
                Text("Phase: %.2f π".format(phase / Math.PI.toFloat()), modifier = Modifier.width(100.dp))
                Slider(
                    value = phase,
                    onValueChange = onPhaseChange,
                    valueRange = 0f..(2 * Math.PI).toFloat(),
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
}
