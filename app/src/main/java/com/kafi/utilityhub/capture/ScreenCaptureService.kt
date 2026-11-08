package com.kafi.utilityhub.capture

import android.app.Notification
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.view.WindowManager
import androidx.lifecycle.LifecycleService
import com.kafi.utilityhub.data.SettingsRepository
import com.kafi.utilityhub.util.MediaStoreUtils
import com.kafi.utilityhub.util.NotificationHelper
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlin.concurrent.thread

class ScreenCaptureService : LifecycleService() {
    private var projection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private var capturing = false
    private var released = false

    override fun onCreate() {
        super.onCreate()
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        when (intent?.action) {
            ACTION_CAPTURE -> capture()
            ACTION_STOP -> stopSelf()
        }
        return START_NOT_STICKY
    }

    private fun capture() {
        if (capturing) return
        val data = ProjectionHolder.data
        val code = ProjectionHolder.resultCode
        if (data == null || code == 0) {
            broadcast(false, null)
            stopSelf()
            return
        }
        capturing = true
        val manager = getSystemService(MediaProjectionManager::class.java)
        val result = manager.getMediaProjection(code, data)
        if (result == null) {
            capturing = false
            broadcast(false, null)
            stopSelf()
            return
        }
        projection = result
        result.registerCallback(object : MediaProjection.Callback() {
            override fun onStop() {
                release()
            }
        })
        startCapture(result)
    }

    private fun startCapture(projection: MediaProjection) {
        val metrics = screenMetrics()
        val width = metrics.widthPixels
        val height = metrics.heightPixels
        val density = metrics.densityDpi

        val reader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
        imageReader = reader
        val display = projection.createVirtualDisplay(
            "KafiScreenshot",
            width,
            height,
            density,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            reader.surface,
            null,
            Handler(Looper.getMainLooper())
        )
        virtualDisplay = display

        reader.setOnImageAvailableListener({ r ->
            val image = try {
                r.acquireLatestImage()
            } catch (e: Exception) {
                null
            } ?: return@setOnImageAvailableListener
            try {
                val plane = image.planes[0]
                val rowPadding = plane.rowStride - plane.pixelStride * width
                val paddedWidth = width + rowPadding / plane.pixelStride
                val bitmap = Bitmap.createBitmap(paddedWidth, height, Bitmap.Config.ARGB_8888)
                bitmap.copyPixelsFromBuffer(plane.buffer)
                val cropped = Bitmap.createBitmap(bitmap, 0, 0, width, height)
                if (cropped !== bitmap) bitmap.recycle()
                r.setOnImageAvailableListener(null, null)
                save(cropped)
            } finally {
                image.close()
            }
        }, Handler(Looper.getMainLooper()))
    }

    private fun save(bitmap: Bitmap) {
        thread {
            val repo = SettingsRepository(this)
            val format = runBlocking { repo.screenshotFormat.first() }
            val quality = runBlocking { repo.screenshotQuality.first() }
            val uri = MediaStoreUtils.saveImage(this, bitmap, format, quality)
            bitmap.recycle()
            Handler(Looper.getMainLooper()).post {
                broadcast(uri != null, uri?.toString())
                release()
                stopSelf()
            }
        }
    }

    private fun release() {
        if (released) return
        released = true
        try {
            virtualDisplay?.release()
        } catch (_: Exception) {
        }
        try {
            imageReader?.close()
        } catch (_: Exception) {
        }
        try {
            projection?.stop()
        } catch (_: Exception) {
        }
        virtualDisplay = null
        imageReader = null
        projection = null
        capturing = false
    }

    private fun broadcast(success: Boolean, uri: String?) {
        val intent = Intent(ACTION_RESULT)
            .setPackage(packageName)
            .putExtra(EXTRA_SUCCESS, success)
            .putExtra(EXTRA_URI, uri)
        sendBroadcast(intent)
    }

    private fun screenMetrics(): DisplayMetrics {
        val wm = getSystemService(WindowManager::class.java)
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        wm.defaultDisplay.getRealMetrics(metrics)
        return metrics
    }

    private fun buildNotification(): Notification =
        NotificationHelper.builder(this, NotificationHelper.CHANNEL_CAPTURE)
            .setContentTitle("Capturing screen")
            .setContentText("Saving screenshot…")
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setOngoing(true)
            .build()

    override fun onDestroy() {
        release()
        super.onDestroy()
    }

    companion object {
        private const val NOTIFICATION_ID = 2001
        const val ACTION_CAPTURE = "com.kafi.utilityhub.capture.SCREENSHOT"
        const val ACTION_STOP = "com.kafi.utilityhub.capture.STOP"
        const val ACTION_RESULT = "com.kafi.utilityhub.capture.RESULT"
        const val EXTRA_SUCCESS = "success"
        const val EXTRA_URI = "uri"

        fun start(context: Context) {
            val intent = Intent(context, ScreenCaptureService::class.java)
                .setAction(ACTION_CAPTURE)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }
}