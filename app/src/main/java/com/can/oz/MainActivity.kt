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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.can.oz.ui.theme.OzTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalAnimationApi::class)
class MainActivity : ComponentActivity() {
    private var isRecording by mutableStateOf(false)
    private var audioVariations = listOf("Format 1", "Format 2", "Format 3")
    private var selectedFormat by mutableStateOf(audioVariations[0])
    private var audioBuffer = mutableListOf<Short>()

    private var microphonePermissionGranted by mutableStateOf(false)
    private var isRedLineVisible by mutableStateOf(false)

    // --- EKLEDİKLERİMİZ ---
    private var elapsedTime by mutableStateOf(0L)
    private var currentNote by mutableStateOf("C4")
    private var recordedFiles by mutableStateOf(listOf<File>())
    private var showNameDialog by mutableStateOf(false)
    private var tempRecordingFile by mutableStateOf<File?>(null)

    private lateinit var recorder: Recorder
    private lateinit var player: Player

    private val retroTextStyle = TextStyle(
        color = Color(0xFFCCCCCC),
        fontSize = 48.sp,
        fontFamily = FontFamily.Monospace
    )

    private val retroNoteTextStyle = TextStyle(
        color = Color(0xFF88FF88),
        fontSize = 36.sp,
        fontFamily = FontFamily.Monospace
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            OzTheme {
                val permissionLauncher = rememberLauncherForActivityResult(RequestPermission()) { isGranted ->
                    microphonePermissionGranted = isGranted
                    if (isGranted && isRecording) {
                        startRecording()
                    }
                }

                LaunchedEffect(Unit) {
                    if (!microphonePermissionGranted) {
                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                }

                LaunchedEffect(isRecording) {
                    while (isRecording) {
                        delay(100)
                        elapsedTime += 100
                        currentNote = generateDummyNote()
                    }
                }

                var showPopup by remember { mutableStateOf(false) }

                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    if (isRedLineVisible) {
                        RedLine()
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = formatElapsedTime(elapsedTime),
                            style = retroTextStyle
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "Note: $currentNote",
                            style = retroNoteTextStyle
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        PorteAnimation(isRunning = isRecording, currentNote = currentNote)

                        Spacer(modifier = Modifier.height(8.dp))

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

                    AnimatedVisibility(visible = showPopup) {
                        FullScreenPopupContent(onDismiss = { showPopup = false }, onFormatSelected = { selectedFormat = it })
                    }

                    SoundWaveVisualization(isRecording)

                    recordedFiles.forEach { file ->
                        DraggableMiniFab(iconRes = R.drawable.oz) {
                            CoroutineScope(Dispatchers.Main).launch {
                                player = Player(this@MainActivity)
                                player.play(file)
                            }
                        }
                    }

                    if (showNameDialog) {
                        FileNameDialog(
                            onConfirm = { fileName ->
                                tempRecordingFile?.let { tempFile ->
                                    val newFile = File(filesDir, "$fileName.wav")
                                    tempFile.copyTo(newFile, overwrite = true)
                                    recordedFiles = recordedFiles + newFile
                                    tempFile.delete()
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
    }

    private fun startRecording() {
        if (microphonePermissionGranted) {
            isRecording = true
            elapsedTime = 0L
            val tempFile = File(cacheDir, "temp_recording.wav")
            recorder = Recorder(tempFile)
            tempRecordingFile = tempFile
            CoroutineScope(Dispatchers.IO).launch {
                recordAudio()
            }
        }
    }

    private fun stopRecording() {
        isRecording = false
        showNameDialog = true
    }

    private suspend fun recordAudio() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
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
            audioRecord.read(audioData, 0, bufferSize)
        }

        audioRecord.stop()
        audioRecord.release()
    }

    private fun formatElapsedTime(ms: Long): String {
        val seconds = (ms / 1000) % 60
        val minutes = (ms / 1000) / 60
        val millis = (ms % 1000) / 100
        return "%02d:%02d.%d".format(minutes, seconds, millis)
    }

    private fun generateDummyNote(): String {
        val notes = listOf("C4", "D4", "E4", "F4", "G4", "A4", "B4", "C5")
        return notes.random()
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

    @Composable
    fun FileNameDialog(onConfirm: (String) -> Unit, onDismiss: () -> Unit) { /* standart dosya adı dialog */ }

    @Composable
    fun DraggableMiniFab(iconRes: Int, onClick: () -> Unit) { /* draggable fab yapısı */ }

    companion object {
        const val SAMPLE_RATE = 44100
    }
}
