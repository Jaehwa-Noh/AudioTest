package starlightlab.jaehwa.audiotest

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioRecord
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import starlightlab.jaehwa.audiotest.ui.theme.AudioTestTheme
import java.io.DataInputStream
import java.io.FileInputStream
import java.io.FileOutputStream

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
    val mBufferSize = AudioTrack.getMinBufferSize(mSampleRate, mChannelCount, mAudioFormat)
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {

    }
    val scope = rememberCoroutineScope()
    var isRecord by remember { mutableStateOf(false) }
    var mAudioRecord: AudioRecord? by remember { mutableStateOf(null) }
    var mAudioTrack: AudioTrack? by remember { mutableStateOf(null) }
    val localContext = LocalContext.current

    var isPlay by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        Button(onClick = {
            isRecord = true
            if (ActivityCompat.checkSelfPermission(
                    localContext,
                    Manifest.permission.RECORD_AUDIO
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                launcher.launch(Manifest.permission.RECORD_AUDIO)
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return@Button
            }
            mAudioRecord = AudioRecord(
                mAudioSource,
                mSampleRate,
                mChannelCount,
                mAudioFormat,
                mBufferSize
            )

            mAudioRecord?.startRecording()

            scope.launch {
                withContext(Dispatchers.IO) {
                    var readData = ByteArray(mBufferSize)
                    val mFilepath =
                        Environment.getExternalStorageDirectory().absolutePath + "/Download/record.pcm"
                    var fos = FileOutputStream(mFilepath)
                    while (isRecord) {
                        val ret = mAudioRecord?.read(readData, 0, mBufferSize)

                        fos.write(readData, 0, mBufferSize)
                    }

                    mAudioRecord?.stop()
                    mAudioRecord?.release()
                    mAudioRecord = null

                    fos.close()
                }
            }
        }) {
            Text("Record")
        }

        Button(onClick = {
            isRecord = false
        }) {
            Text("Stop Record")
        }

        Button(onClick = {
            isPlay = true

            mAudioTrack = AudioTrack(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build(),
                AudioFormat.Builder()
                    .setSampleRate(mSampleRate)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                    .build(),
                mBufferSize,
                AudioTrack.MODE_STREAM,
                1,
            )

            scope.launch {
                withContext(Dispatchers.IO) {

                    val writeData = ByteArray(mBufferSize)
                    val mFilepath =
                        Environment.getExternalStorageDirectory().absolutePath + "/Download/record.pcm"
                    val fis = FileInputStream(mFilepath)
                    val dis = DataInputStream(fis)
                    mAudioTrack?.play()


                    while (isPlay) {
                        val ret = dis.read(writeData, 0, mBufferSize)
                        if (ret <= 0) {
                            isPlay = false
                            break
                        }
                        mAudioTrack?.write(writeData, 0, ret)
                    }
                    mAudioTrack?.stop()
                    mAudioTrack?.release()
                    mAudioTrack = null
                }
            }

        }) {
            Text("Play")
        }

        Button(onClick = {
            isPlay = false
        }) {
            Text("Stop Play")
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
