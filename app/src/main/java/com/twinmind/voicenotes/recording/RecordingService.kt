package com.twinmind.voicenotes.recording

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.twinmind.voicenotes.R
import com.twinmind.voicenotes.util.WavUtil
import kotlinx.coroutines.*
import java.io.File
import java.io.FileOutputStream

class RecordingService : Service() {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    companion object {
        private const val CHANNEL_ID = "tm_recording"
        @Volatile var lastWavFilePath: String? = null
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
        val notif: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Recording in progress")
            .setContentText("TwinMind Voice Notes")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .build()
        startForeground(1001, notif)
        scope.launch { recordPcmThenWav() }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(CHANNEL_ID, "Recording", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
        }
    }

    private suspend fun recordPcmThenWav() {
        val sampleRate = 16000
        val channelConfig = AudioFormat.CHANNEL_IN_MONO
        val audioFormat = AudioFormat.ENCODING_PCM_16BIT
        val minBuf = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
        val bufferSize = minBuf * 2

        val recorder = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            channelConfig,
            audioFormat,
            bufferSize
        )

        val sessionDir = File(cacheDir, "session_${System.currentTimeMillis()}")
        sessionDir.mkdirs()
        val pcmFile = File(sessionDir, "full_capture.pcm")

        try {
            recorder.startRecording()
            val buf = ByteArray(bufferSize)
            FileOutputStream(pcmFile).use { out ->
                while (isActive) {
                    val read = recorder.read(buf, 0, buf.size)
                    if (read > 0) out.write(buf, 0, read)
                    delay(10)
                }
            }
        } catch (_: Exception) {
        } finally {
            try { recorder.stop() } catch (_: Exception) {}
            recorder.release()
        }

        val wavFile = File(sessionDir, "full_capture.wav")
        WavUtil.pcmToWav(pcmFile, wavFile, sampleRate, 1)
        lastWavFilePath = wavFile.absolutePath
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }
}
