package starlightlab.jaehwa.audiotest

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.MediaRecorder
import android.os.Bundle
import android.os.Environment
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.launch
import starlightlab.jaehwa.audiotest.play.PlayController
import starlightlab.jaehwa.audiotest.record.RecordController
import starlightlab.jaehwa.audiotest.ui.theme.AudioTestTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AudioTestTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(
                        name = "Android",
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    val mAudioSource = MediaRecorder.AudioSource.MIC
    val mSampleRate = 44100
    val mChannelCount = AudioFormat.CHANNEL_IN_STEREO
    val mAudioFormat = AudioFormat.ENCODING_PCM_16BIT
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {

    }
    val scope = rememberCoroutineScope()
    var isRecording by remember { mutableStateOf(false) }
    val localContext = LocalContext.current

    var isPlaying by remember { mutableStateOf(false) }
    val fileName by remember {
        mutableStateOf(
            Environment.getExternalStorageDirectory().absolutePath + "/Download/record_${System.currentTimeMillis()}.pcm"
        )
    }
    val playController = remember {
        PlayController(
            usage = AudioAttributes.USAGE_MEDIA,
            contentType = AudioAttributes.CONTENT_TYPE_MUSIC,
            sampleRate = mSampleRate,
            encoding = AudioFormat.ENCODING_PCM_16BIT,
            channelMask = AudioFormat.CHANNEL_OUT_STEREO,
            mode = AudioTrack.MODE_STREAM,
            sessionId = 0,
            fileName = fileName,
            onPlayFinish = {
                if (isPlaying) isPlaying = false
            }
        )
    }

    val audioRecordController = remember {
        RecordController(
            mAudioSource,
            mSampleRate,
            mChannelCount,
            mAudioFormat,
            AudioFormat.CHANNEL_IN_STEREO,
            fileName,
        )
    }

    Column(modifier = modifier) {
        if (isRecording) {
            Button(onClick = {
                audioRecordController.stopRecord()
                isRecording = false
            }) {
                Text("Stop Record")
            }
        } else {
            Button(onClick = {
                isRecording = true
                if (ActivityCompat.checkSelfPermission(
                        localContext,
                        Manifest.permission.RECORD_AUDIO
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    launcher.launch(Manifest.permission.RECORD_AUDIO)
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return@Button
                }
                scope.launch {
                    audioRecordController.startRecord()
                }

            }) {
                Text("Record")
            }
        }

        if (isPlaying) {
            Button(onClick = {
                isPlaying = false
            }) {
                Text("Stop Play")
            }
        } else {
            Button(onClick = {
                isPlaying = true
                scope.launch {
                    playController.play()
                }

            }) {
                Text("Play")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    AudioTestTheme {
        Greeting("Android")
    }
}
