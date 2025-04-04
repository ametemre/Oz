package com.can.oz

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.can.oz.ui.theme.OzTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

class MainActivity : ComponentActivity() {

    private lateinit var recorder: Recorder
    private lateinit var player: Player

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            OzTheme {
                val context = LocalContext.current
                val scope = rememberCoroutineScope()

                var isRecording by remember { mutableStateOf(false) }
                var recordedFiles by remember { mutableStateOf(listOf<File>()) }
                var showNameDialog by remember { mutableStateOf(false) }
                var tempRecordingFile by remember { mutableStateOf<File?>(null) }
                var elapsedTime by remember { mutableStateOf(0L) } // milliseconds
                var currentNote by remember { mutableStateOf("C4") }

                val permissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { isGranted ->
                    if (!isGranted) return@rememberLauncherForActivityResult
                }

                LaunchedEffect(Unit) {
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                        != PackageManager.PERMISSION_GRANTED) {
                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                }

                // Sayaç güncelleme ve dummy nota değişimi
                LaunchedEffect(isRecording) {
                    while (isRecording) {
                        delay(100)
                        elapsedTime += 100
                        currentNote = generateDummyNote()
                    }
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = formatElapsedTime(elapsedTime), style = MaterialTheme.typography.headlineMedium)

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(text = "Note: $currentNote", style = MaterialTheme.typography.titleMedium)

                        Spacer(modifier = Modifier.height(8.dp))

                        PorteAnimation(isRunning = isRecording)

                        Spacer(modifier = Modifier.height(8.dp))

                        RoundImageButton(
                            onLongClick = {},
                            onClick = {
                                if (isRecording) {
                                    recorder.stopRecording()
                                    isRecording = false
                                    showNameDialog = true
                                } else {
                                    val tempFile = File(context.cacheDir, "temp_recording.wav")
                                    recorder = Recorder(tempFile)
                                    recorder.startRecording()
                                    tempRecordingFile = tempFile
                                    isRecording = true
                                    elapsedTime = 0L
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        recordedFiles.forEach { file ->
                            DraggableMiniFab(iconRes = R.drawable.oz) {
                                scope.launch {
                                    player = Player(context)
                                    player.play(file)
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }

                    if (showNameDialog) {
                        FileNameDialog(
                            onConfirm = { fileName ->
                                tempRecordingFile?.let { tempFile ->
                                    val newFile = File(context.filesDir, "$fileName.wav")
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
    fun FileNameDialog(onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
        var fileName by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Dosya Adı Girin") },
            text = {
                TextField(
                    value = fileName,
                    onValueChange = { fileName = it },
                    placeholder = { Text("örn: kayit1") }
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (fileName.isNotBlank()) onConfirm(fileName)
                }) {
                    Text("Kaydet")
                }
            },
            dismissButton = {
                Button(onClick = onDismiss) {
                    Text("İptal")
                }
            }
        )
    }

}
