package starlightlab.jaehwa.audiotest.record

import android.Manifest
import android.media.AudioFormat
import android.media.AudioRecord
import androidx.annotation.RequiresPermission
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class RecordController(
    private val audioSource: Int,
    private val mSampleRate: Int,
    private val mChannelCount: Int,
    private val mAudioFormat: Int,
    private val channelMask: Int,
    private val fileName: String,
) {
    private var audioRecord: AudioRecord? = null
    private var bufferSize = 0
    private var isRecording = false

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    private fun initAudioRecord() {
        val audioFormat = AudioFormat.Builder()
            .setSampleRate(mSampleRate)
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setChannelMask(channelMask)
            .build()

        bufferSize = AudioRecord.getMinBufferSize(
            mSampleRate,
            mChannelCount,
            mAudioFormat,
        )

        audioRecord = AudioRecord.Builder()
            .setAudioSource(audioSource)
            .setAudioFormat(audioFormat)
            .setBufferSizeInBytes(bufferSize)
            .build()
    }

    private fun releaseAudioRecord() {
        audioRecord?.apply {
            stop()
            release()
        }
        audioRecord = null
        stopRecord()
    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    suspend fun startRecord() {
        withContext(Dispatchers.Default) {
            releaseAudioRecord()
            isRecording = true
            initAudioRecord()
            audioRecord?.startRecording()
            withContext(Dispatchers.IO) {
                val readData = ByteArray(bufferSize)
                val file = File(fileName)

//                val fos = FileOutputStream(mFilepath)
                file.outputStream().use { currentFileOutputStream ->
                    while (isRecording) {
                        val ret = audioRecord?.read(readData, 0, bufferSize)

                        if (ret == null) break

                        currentFileOutputStream.write(readData, 0, bufferSize)
                    }
//                }
                    releaseAudioRecord()
                }
            }
        }
    }

    fun stopRecord() {
        isRecording = false
    }

}
