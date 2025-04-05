package com.can.oz

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.can.oz.audio.AudioRecorder
import com.can.oz.signal.SignalGenerator
import com.can.oz.ui.theme.OzTheme
import com.can.oz.ui.widgets.DigitalTimer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.collections.average
import kotlin.collections.chunked
import kotlin.math.roundToInt
import kotlin.random.Random

@OptIn(ExperimentalAnimationApi::class)
class MainActivity : ComponentActivity() {
    private var isRecording by mutableStateOf(false)
    private var microphonePermissionGranted by mutableStateOf(false)
    private var isRedLineVisible by mutableStateOf(false)

    private val audioVariations = listOf("Format 1", "Format 2", "Format 3")
    private var selectedFormat by mutableStateOf(audioVariations[0])

    private lateinit var audioRecorder: AudioRecorder
    private val audioBuffer = mutableStateListOf<Short>()
    private val displayWaveform = mutableStateListOf<Float>()



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            OzTheme {
                val context = LocalContext.current
                val scope = rememberCoroutineScope()

                var recordedFiles by remember { mutableStateOf(listOf<File>()) }
                var showNameDialog by remember { mutableStateOf(false) }
                var tempRecordingFile by remember { mutableStateOf<File?>(null) }


                var currentNote by remember { mutableStateOf("C4") }
                var elapsedTime by remember { mutableStateOf(0L) }
                var timerSpeedMs by remember { mutableStateOf(100L) }


                val permissionLauncher = rememberLauncherForActivityResult(RequestPermission()) { isGranted ->
                    microphonePermissionGranted = isGranted
                }

                LaunchedEffect(Unit) {
                    if (!microphonePermissionGranted) {
                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                }
                LaunchedEffect(isRecording) {
                    while (isRecording) {
                        delay(timerSpeedMs)
                        elapsedTime += timerSpeedMs
                        currentNote = generateDummyNote()
                    }
                }
                var showPopup by remember { mutableStateOf(false) }
                // Signal UI state
                var amplitude by remember { mutableStateOf(1f) }
                var frequency by remember { mutableStateOf(440f) }
                var phase by remember { mutableStateOf(0f) }
                val generator = remember { SignalGenerator() }

                // Signal generation & waveform update
                LaunchedEffect(amplitude, frequency, phase) {
                    generator.amplitude = amplitude
                    generator.frequency = frequency
                    generator.phase = phase

                    val samples = generator.generateSamples(durationSec = 0.1f)
                    val waveform = samples.toList().chunked(samples.size / 128).map {
                        it.average().toFloat()
                    }
                    displayWaveform.clear()
                    displayWaveform.addAll(waveform)
                }
                Box(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Top,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        SignalToolBox(
                            amplitude = 1f,
                            onAmplitudeChange = {},
                            frequency = 440f,
                            onFrequencyChange = {},
                            phase = 0f,
                            onPhaseChange = {},
                            timerSpeedMs = timerSpeedMs,
                            onTimerSpeedChange = { timerSpeedMs = it }
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Şu anki nota
                        //Text(text = "Current Note: $currentNote",color = Color.White,fontSize = 24.sp)

                        Spacer(modifier = Modifier.height(8.dp))

                        // Porte Animation
                        //PorteAnimation(isRunning = isRecording, currentNote = currentNote, timerSpeedMs = timerSpeedMs)

                        Spacer(modifier = Modifier.height(8.dp))

                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Porte(
                                currentNote = currentNote,
                                modifier = Modifier.fillMaxWidth()
                                    .align(Alignment.BottomCenter))
                            RecordingTimer(isRecording)

                            if (isRedLineVisible) {
                                RedLine()
                            }

                            RoundImageButton(
                                onLongClick = { showPopup = true },
                                onClick = {
                                    if (microphonePermissionGranted) {
                                        if (isRecording) {
                                            //recorder.stopRecording()
                                            isRecording = false
                                            showNameDialog = true
                                            isRedLineVisible = false
                                            stopRecording()
                                        } else {
                                            val tempFile = File(context.cacheDir, "temp_recording.wav")
                                            audioRecorder = AudioRecorder(tempFile)
                                            tempRecordingFile = tempFile
                                            startRecording()
                                            isRecording = true
                                            isRedLineVisible = true
                                        }
                                    }
                                }
                            )
                            Spacer(modifier = Modifier.height(24.dp))

                            // MiniFAB'lar
                            recordedFiles.forEach { file ->
                                DraggableMiniFab(iconRes = R.drawable.oz) {
                                    scope.launch {
                                        Player(context).play(file)
                                    }
                                }
                            }
                            Row {
                                AnimatedVisibility(visible = showPopup) {
                                    FullScreenPopupContent(
                                        onDismiss = { showPopup = false },
                                        onFormatSelected = { selectedFormat = it }
                                    )
                                }
                            }
                            SoundWaveVisualization(isRecording)
                        }
                    }
                }
                if (showNameDialog) {
                    FileNameDialog(
                        onConfirm = { fileName ->
                            tempRecordingFile?.let { tempFile ->
                                val newFile = File(context.filesDir, "$fileName.wav")
                                tempFile.copyTo(newFile, overwrite = true)
                                tempFile.delete()

                                // EN KRİTİK SATIR:
                                recordedFiles = recordedFiles.toMutableList().apply { add(newFile) }
                            }
                            showNameDialog = false
                        },
                        onDismiss = {
                            tempRecordingFile?.delete()
                            showNameDialog = false
                        }
                    )

                }
            }
        }
    }

    private fun startRecording() {
        val outputFile = File(getExternalFilesDir(null), "recording.wav")

        if (!::audioRecorder.isInitialized) {
            audioRecorder = AudioRecorder(outputFile)
        }

        isRecording = true
        audioBuffer.clear()
        displayWaveform.clear()

        audioRecorder.startRecording { buffer ->
            CoroutineScope(Dispatchers.IO).launch {
                val sampled = processWaveform(buffer, 128)
                withContext(Dispatchers.Main) {
                    displayWaveform.clear()
                    displayWaveform.addAll(sampled)
                }
            }
        }
    }

    private fun stopRecording() {
        isRecording = false
        audioRecorder.stopRecording()

    }

    fun processWaveform(data: ShortArray, targetSize: Int): List<Float> {
        val step = (data.size / targetSize).coerceAtLeast(1)
        return data.toList()
            .chunked(step)
            .map { chunk ->
                chunk.map { it.toFloat() / Short.MAX_VALUE }.average().toFloat()
            }
    }

    @Composable
    fun SoundWaveVisualization(isRecording: Boolean) {
        if (isRecording && displayWaveform.isNotEmpty()) {
            Canvas(modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
            ) {
                val width = size.width
                val midY = size.height / 2
                val waveWidth = width / displayWaveform.size

                displayWaveform.forEachIndexed { i, value ->
                    val x = i * waveWidth
                    val y = midY + (value * midY * 0.8f)
                    drawLine(
                        color = Color.Cyan,
                        start = Offset(x, midY),
                        end = Offset(x, y),
                        strokeWidth = 2f
                    )
                }
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
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
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
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
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
    @Composable
    fun FileNameDialog(onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
        var fileName by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Enter file name") },
            text = {
                TextField(
                    value = fileName,
                    onValueChange = { fileName = it },
                    placeholder = { Text("e.g. my_recording") }
                )
            },
            confirmButton = {
                Button(onClick = { if (fileName.isNotBlank()) onConfirm(fileName) }) {
                    Text("Save")
                }
            },
            dismissButton = {
                Button(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        )
    }

    @Composable
    fun DraggableMiniFab(
        iconRes: Int,
        onClick: () -> Unit
    ) {
        var offsetX by remember { mutableStateOf(Random.nextInt(-50, 50).toFloat()) }
        var offsetY by remember { mutableStateOf(Random.nextInt(-50, 50).toFloat()) }
        var isDragging by remember { mutableStateOf(false) }

        Surface(
            modifier = Modifier
                .size(80.dp)
                .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                .graphicsLayer {
                    alpha = if (isDragging) 0.7f else 1f
                    shadowElevation = if (isDragging) 16f else 8f
                }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { isDragging = true },
                        onDragEnd = { isDragging = false },
                        onDragCancel = { isDragging = false },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            offsetX += dragAmount.x
                            offsetY += dragAmount.y
                        }
                    )
                },
            shape = CircleShape,
            shadowElevation = 8.dp
        ) {
            IconButton(
                onClick = onClick,
                modifier = Modifier.fillMaxSize()
            ) {
                Image(
                    painter = painterResource(id = iconRes),
                    contentDescription = "Mini FAB",
                    modifier = Modifier.size(48.dp)
                )
            }
        }
    }
    private fun generateDummyNote(): String {
        val notes = listOf("C4", "D4", "E4", "F4", "G4", "A4", "B4", "C5")
        return notes.random()
    }
    @Preview(showBackground = true)
    @Composable
    fun PreviewRoundImageButton() {
        OzTheme {
            RoundImageButton({}, {})
        }
    }
}