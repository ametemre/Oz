package com.can.oz

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.core.content.ContextCompat
import com.can.oz.ui.theme.OzTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.ln

@OptIn(ExperimentalAnimationApi::class)
class MainActivity : ComponentActivity() {
    private var isRecording by mutableStateOf(false)
    private var audioVariations =
        listOf("Format 1", "Format 2", "Format 3") // Add desired formats here
    private var selectedFormat by mutableStateOf(audioVariations[0])
    private var audioBuffer = mutableListOf<Short>()

    // State to manage permission
    private var microphonePermissionGranted by mutableStateOf(false)
    private var isRedLineVisible by mutableStateOf(false) // New state for red line visibility
    private var currentDetectedNote by mutableStateOf("")
    private val noteBuffer = mutableListOf<String>()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            OzTheme {
                // Permission request launcher
                val permissionLauncher =
                    rememberLauncherForActivityResult(RequestPermission()) { isGranted ->
                        microphonePermissionGranted = isGranted
                        if (isGranted && isRecording) {
                            startRecording() // Start recording if permission is granted after the request
                        }
                    }

                // Request permission if not granted
                LaunchedEffect(Unit) {
                    if (!microphonePermissionGranted) {
                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                }

                var showPopup by remember { mutableStateOf(false) }

                Box(modifier = Modifier.fillMaxSize()) {

                    // 1. ALT KATMAN: Porte + RedLine
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                    ) {
                        if (isRedLineVisible) {
                            RedLine()
                        }
                    }

                    // 2. ÜST KATMAN: Butonlar ve diğer UI
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isRedLineVisible) {
                            RedLine()
                        }
                        if (currentDetectedNote.isNotEmpty()) {
                            Text(
                                text = "Tahmin edilen nota: $currentDetectedNote",
                                style = MaterialTheme.typography.headlineMedium,
                                color = Color.Black,
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .padding(top = 32.dp)
                            )
                        }

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

                        AnimatedVisibility(visible = showPopup) {
                            FullScreenPopupContent(
                                onDismiss = { showPopup = false },
                                onFormatSelected = { selectedFormat = it })
                        }

                        SoundWaveVisualization(isRecording)

                        // EN SON KATMAN - PORTE
                        PorteView(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomCenter) // Ekranın tam altına hizala
                                .padding(bottom = 32.dp) // İstersen yere biraz boşluk bırak (isteğe bağlı)
                        )

                    }

                }


            }
        }
    }

    private fun startRecording() {
        if (microphonePermissionGranted) {
            isRecording = true
            // Start a coroutine for audio recording
            CoroutineScope(Dispatchers.IO).launch {
                recordAudio()
            }
        }
    }

    private fun stopRecording() {
        isRecording = false
    }

    private suspend fun recordAudio() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val bufferSize = AudioRecord.getMinBufferSize(
            SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT
        )
        val audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize
        )

        audioRecord.startRecording()

        val audioData = ShortArray(bufferSize)
        while (isRecording) {
            val readResult = audioRecord.read(audioData, 0, bufferSize)
            if (readResult > 0) {
                val frequency = calculateFrequency(audioData, SAMPLE_RATE)
                val note = frequencyToNoteName(frequency)
                if (note.isNotEmpty()) {
                    currentDetectedNote = note
                    noteBuffer.add(note) // Geçmişe ekle (istersen)
                }
            }
        }

        audioRecord.stop()
        audioRecord.release()
    }


    @Composable
    fun SoundWaveVisualization(isRecording: Boolean) {
        if (isRecording) {
            Canvas(modifier = Modifier.fillMaxWidth().height(200.dp)) {
                val width = size.width
                val midY = size.height / 2
                val waveWidth = width / audioBuffer.size

                // Draw the waveform
                for (i in audioBuffer.indices) {
                    val x = i * waveWidth
                    val y =
                        midY + (audioBuffer[i] / Short.MAX_VALUE.toFloat()) * midY * 0.8f // Scale the wave height
                    if (i == 0) {
                        ///moveTo(x, y)
                    } else {
                        //lineTo(x, y)
                    }
                }
            }
        }
    }

    @Composable
    fun AnimatedRedLine(isVisible: Boolean) {
        // Define animation state
        var width by remember { mutableStateOf(0f) }
        var yOffset by remember { mutableStateOf(0f) }

        // Update width and yOffset when the line becomes visible
        LaunchedEffect(isVisible) {
            if (isVisible) {
                // Animate width to full width
                width = 200f // Set this to your desired maximum width
                // Animate the yOffset to create a parabolic effect
                yOffset = 30f // Change this based on the parabolic effect you want
            } else {
                // Reset width and yOffset
                width = 0f
                yOffset = 0f
            }
        }


        // Draw the animated line
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
        ) {
            drawLine(
                color = Color.Red,
                start = Offset(x = (size.width - width) / 2, y = (size.height / 2) - yOffset),
                end = Offset(x = (size.width + width) / 2, y = (size.height / 2) - yOffset),
                strokeWidth = 2f
            )
        }
    }

    @Composable
    fun FullScreenPopupContent(onDismiss: () -> Unit, onFormatSelected: (String) -> Unit) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = Color(0x99000000), // 59% transparent black
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
                    painter = painterResource(id = R.drawable.oz), // Change this to your image
                    contentDescription = "Round Button",
                    modifier = Modifier.size(100.dp)
                )
            }
        }
    }

    @Composable
    fun RedLine() {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawLine(
                    color = Color.Red,
                    start = Offset(0f, size.height / 2),
                    end = Offset(x = size.width, y = size.height / 2),
                    strokeWidth = 2f // Line thickness
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

    companion object {
        const val SAMPLE_RATE = 44100 // Sample rate for audio
    }

    @Composable
    fun PorteView(modifier: Modifier = Modifier) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(140.dp) // toplam porte alanı
                .padding(bottom = 32.dp),
            contentAlignment = Alignment.CenterStart // Sol Anahtarı sola sabitle
        ) {
            // 5 porte çizgisi
            Column(
                modifier = Modifier.fillMaxSize()
                    .fillMaxWidth()
                    .align(Alignment.Center), // Çizgileri ortala
                verticalArrangement = Arrangement.SpaceEvenly
            ) {
                repeat(5) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                            .background(Color.Black)
                    )
                }
            }

            // Sol Anahtarı (Porte üstünde)
            Image(
                painter = painterResource(id = R.drawable.ic_treble_clef),
                contentDescription = "Sol Anahtarı",
                modifier = Modifier
                    .height(140.dp) // Porte'ye uyacak boyut
                    .aspectRatio(0.4f) // Dar yap, dikey uzunluğu koru
                    .padding(start = 8.dp)
            )

            // Notalar
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 64.dp), // Sol Anahtardan sonra başlasın
                verticalArrangement = Arrangement.SpaceEvenly
            ) {
                val notes = listOf(
                    "Si", "La", "Sol", "Fa", "Mi", "Re", "Do"
                )
                notes.forEachIndexed { index, note ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Canvas(
                            modifier = Modifier
                                .size(14.dp)
                        ) {
                            drawCircle(Color.Black)
                        }
                    }
                }
            }
        }
    }
    data class Nota(
        val isim: String,
        val yPositionRatio: Float // Ekranın yüksekliğine oranlı konum (%)
    )
    fun calculateFrequency(buffer: ShortArray, sampleRate: Int): Float {
        // basit autocorrelation yöntemi
        val size = buffer.size
        var maxCorrelation = 0
        var bestLag = 0

        for (lag in 10..(size / 2)) {
            var correlation = 0
            for (i in 0 until size - lag) {
                correlation += buffer[i] * buffer[i + lag]
            }
            if (correlation > maxCorrelation) {
                maxCorrelation = correlation
                bestLag = lag
            }
        }

        return if (bestLag != 0) {
            sampleRate.toFloat() / bestLag
        } else {
            0f
        }
    }

    fun log2(x: Double): Double {
        return ln(x) / ln(2.0)
    }

    fun frequencyToNoteName(frequency: Float): String {
        if (frequency <= 0f) return ""

        val noteNames = listOf(
            "C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"
        )

        val n = (12 * log2(frequency / 440.0) + 69).toInt()
        val noteIndex = (n % 12 + 12) % 12
        val octave = n / 12 - 1

        return "${noteNames[noteIndex]}$octave"
    }


}
