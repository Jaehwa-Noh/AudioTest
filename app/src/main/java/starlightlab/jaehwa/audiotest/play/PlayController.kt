package starlightlab.jaehwa.audiotest.play

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.DataInputStream
import java.io.File

class PlayController(
    private val usage: Int,
    private val contentType: Int,
    private val sampleRate: Int,
    private val encoding: Int,
    private val channelMask: Int,
    private val mode: Int,
    private val sessionId: Int,
    private val fileName: String,
    private val onPlayFinish: () -> Unit,
) {
    private var audioTack: AudioTrack? = null
    private var bufferSize = 0
    private var isPlaying = false

    private fun initAudioTrack() {
        bufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            channelMask,
            encoding,
        )

        audioTack = AudioTrack(
            AudioAttributes.Builder()
                .setUsage(usage)
                .setContentType(contentType)
                .build(),
            AudioFormat.Builder()
                .setSampleRate(sampleRate)
                .setEncoding(encoding)
                .setChannelMask(channelMask)
                .build(),
            bufferSize,
            mode,
            sessionId,
        )
    }

    private fun releaseAudioTrack() {
        audioTack?.apply {
            stop()
            release()
        }

        audioTack = null
        stopPlaying()
    }

    fun stopPlaying() {
        isPlaying = false
    }

    suspend fun play() {
        withContext(Dispatchers.Default) {
            releaseAudioTrack()
            isPlaying = true
            initAudioTrack()
            audioTack?.play()

            withContext(Dispatchers.IO) {
                val writeData = ByteArray(bufferSize)
                val file = File(fileName)

                file.inputStream().use { currentFileInputStream ->
                    DataInputStream(currentFileInputStream).use { currentDataInputStream ->
                        while (isPlaying) {
                            val readBytes = currentFileInputStream.read(writeData, 0, bufferSize)
                            if (readBytes <= 0) {
                                isPlaying = false
                                break
                            }
                            audioTack?.write(writeData, 0, readBytes)
                        }
                    }
                }

                releaseAudioTrack()

            }
        }
        onPlayFinish()
    }

}
