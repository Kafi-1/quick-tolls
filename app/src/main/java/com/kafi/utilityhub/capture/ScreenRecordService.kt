package com.kafi.utilityhub.capture

import android.Manifest
import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.net.Uri
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.util.DisplayMetrics
import android.view.WindowManager
import androidx.lifecycle.LifecycleService
import com.kafi.utilityhub.data.SettingsRepository
import com.kafi.utilityhub.util.MediaStoreUtils
import com.kafi.utilityhub.util.NotificationHelper
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.io.File

class ScreenRecordService : LifecycleService() {
    private var projection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null
    private var recording = false
    private var micEnabled = false
    private var finishing = false

    override fun onCreate() {
        super.onCreate()
        val notification = buildNotification()
        micEnabled = runBlocking { SettingsRepository(this@ScreenRecordService).recordMic.first() } &&
            checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            var type = ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            if (micEnabled) type = type or ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            startForeground(NOTIFICATION_ID, notification, type)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        when (intent?.action) {
            ACTION_START -> start()
            ACTION_STOP -> stopRecording()
        }
        return START_NOT_STICKY
    }

    private fun start() {
        if (recording) return
        val data = ProjectionHolder.data
        val code = ProjectionHolder.resultCode
        if (data == null || code == 0) {
            broadcast(false)
            stopSelf()
            return
        }
        val manager = getSystemService(MediaProjectionManager::class.java)
        val result = manager.getMediaProjection(code, data)
        if (result == null) {
            broadcast(false)
            stopSelf()
            return
        }
        projection = result
        result.registerCallback(object : MediaProjection.Callback() {
            override fun onStop() = stopRecording()
        })
        try {
            setupRecorder(result)
            recording = true
        } catch (e: Exception) {
            broadcast(false)
            cleanup()
            stopSelf()
        }
    }

    private fun setupRecorder(projection: MediaProjection) {
        val repo = SettingsRepository(this)
        val resolution = runBlocking { repo.recordResolution.first() }
        val fps = runBlocking { repo.recordFps.first() }
        val bitrate = runBlocking { repo.recordBitrateKbps.first() }
        val mic = micEnabled

        val metrics = screenMetrics()
        val targetHeight = when (resolution) {
            "480p" -> 480
            "1080p" -> 1080
            else -> 720
        }
        val scale = targetHeight.toFloat() / metrics.heightPixels
        var width = (metrics.widthPixels * scale).toInt() and 0xFFFFFFFE
        var height = targetHeight and 0xFFFFFFFE
        if (width < 2) width = 2
        if (height < 2) height = 2

        val file = MediaStoreUtils.newVideoFile()
        outputFile = file

        val rec = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(this)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }
        rec.setVideoSource(MediaRecorder.VideoSource.SURFACE)
        if (mic) rec.setAudioSource(MediaRecorder.AudioSource.MIC)
        rec.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        rec.setVideoEncoder(MediaRecorder.VideoEncoder.H264)
        if (mic) rec.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        rec.setVideoSize(width, height)
        rec.setVideoFrameRate(fps)
        rec.setVideoEncodingBitRate(bitrate * 1000)
        rec.setOutputFile(file.absolutePath)
        rec.prepare()
        recorder = rec

        val display = projection.createVirtualDisplay(
            "KafiRecord",
            width,
            height,
            metrics.densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            rec.surface,
            null,
            null
        )
        virtualDisplay = display
        rec.start()
    }

    private fun stopRecording() {
        if (finishing) return
        finishing = true
        val wasRecording = recording
        recording = false
        try {
            if (wasRecording) recorder?.stop()
        } catch (_: Exception) {
        }
        cleanup()
        val file = outputFile
        if (wasRecording && file != null && file.exists() && file.length() > 0) {
            val uri = MediaStoreUtils.publishVideo(this, file)
            broadcast(uri != null, uri)
        } else {
            file?.delete()
            broadcast(false)
        }
        stopSelf()
    }

    private fun cleanup() {
        try {
            recorder?.reset()
            recorder?.release()
        } catch (_: Exception) {
        }
        try {
            virtualDisplay?.release()
        } catch (_: Exception) {
        }
        try {
            projection?.stop()
        } catch (_: Exception) {
        }
        recorder = null
        virtualDisplay = null
        projection = null
    }

    private fun broadcast(success: Boolean, uri: Uri? = null) {
        sendBroadcast(
            Intent(ACTION_RESULT)
                .setPackage(packageName)
                .putExtra(ScreenCaptureService.EXTRA_SUCCESS, success)
                .putExtra(ScreenCaptureService.EXTRA_URI, uri?.toString())
        )
    }

    private fun screenMetrics(): DisplayMetrics {
        val wm = getSystemService(WindowManager::class.java)
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        wm.defaultDisplay.getRealMetrics(metrics)
        return metrics
    }

    private fun buildNotification(): Notification {
        val stop = PendingIntent.getService(
            this,
            1,
            Intent(this, ScreenRecordService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationHelper.builder(this, NotificationHelper.CHANNEL_RECORD)
            .setContentTitle("Recording screen")
            .setContentText("Tap Stop to finish")
            .setSmallIcon(android.R.drawable.presence_video_online)
            .addAction(0, "Stop", stop)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        if (recording) stopRecording() else cleanup()
        super.onDestroy()
    }

    companion object {
        private const val NOTIFICATION_ID = 2002
        const val ACTION_START = "com.kafi.utilityhub.capture.RECORD_START"
        const val ACTION_STOP = "com.kafi.utilityhub.capture.RECORD_STOP"
        const val ACTION_RESULT = "com.kafi.utilityhub.capture.RECORD_RESULT"

        fun start(context: Context) {
            val intent = Intent(context, ScreenRecordService::class.java)
                .setAction(ACTION_START)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.startService(
                Intent(context, ScreenRecordService::class.java).setAction(ACTION_STOP)
            )
        }
    }
}