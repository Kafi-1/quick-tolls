package com.kafi.utilityhub.network

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.Gravity
import android.view.WindowManager
import android.widget.TextView
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.kafi.utilityhub.data.SettingsRepository
import com.kafi.utilityhub.util.Formatters
import com.kafi.utilityhub.util.NotificationHelper
import com.kafi.utilityhub.util.PermissionHelper
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class SpeedMeterService : LifecycleService() {
    private val monitor = TrafficMonitor()
    private val handler = Handler(Looper.getMainLooper())
    private var overlay: TextView? = null
    private var windowManager: WindowManager? = null
    private var floatingEnabled = false

    private val tick = object : Runnable {
        override fun run() {
            val sample = monitor.sample()
            updateNotification(sample)
            updateOverlay(sample)
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate() {
        super.onCreate()
        startForeground(NOTIFICATION_ID, buildNotification(TrafficMonitor.Sample(0, 0)))
        handler.post(tick)
        lifecycleScope.launch {
            SettingsRepository(this@SpeedMeterService).showFloatingSpeed.collectLatest { enabled ->
                floatingEnabled = enabled
                applyFloatingState()
            }
        }
    }

    private fun applyFloatingState() {
        if (floatingEnabled && PermissionHelper.canDrawOverlays(this)) showOverlay() else hideOverlay()
    }

    private fun updateNotification(sample: TrafficMonitor.Sample) {
        val nm = getSystemService(android.app.NotificationManager::class.java)
        nm.notify(NOTIFICATION_ID, buildNotification(sample))
    }

    private fun updateOverlay(sample: TrafficMonitor.Sample) {
        val view = overlay ?: return
        view.text = "↓ ${Formatters.bytesPerSec(sample.downBytesPerSec)}  ↑ ${Formatters.bytesPerSec(sample.upBytesPerSec)}"
    }

    private fun showOverlay() {
        if (overlay != null) return
        val wm = getSystemService(WindowManager::class.java)
        windowManager = wm
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = 16
            y = 120
        }
        val view = TextView(this).apply {
            setTextColor(Color.WHITE)
            setBackgroundColor(0x99000000.toInt())
            setPadding(24, 12, 24, 12)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            text = "↓ 0 B/s  ↑ 0 B/s"
        }
        try {
            wm.addView(view, params)
            overlay = view
        } catch (_: Exception) {
        }
    }

    private fun hideOverlay() {
        val view = overlay ?: return
        try {
            windowManager?.removeView(view)
        } catch (_: Exception) {
        }
        overlay = null
    }

    private fun buildNotification(sample: TrafficMonitor.Sample): Notification {
        val stop = PendingIntent.getService(
            this,
            1,
            Intent(this, SpeedMeterService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationHelper.builder(this, NotificationHelper.CHANNEL_SPEED)
            .setContentTitle("↓ ${Formatters.bytesPerSec(sample.downBytesPerSec)}")
            .setContentText("↑ ${Formatters.bytesPerSec(sample.upBytesPerSec)}")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .addAction(0, "Stop", stop)
            .setOngoing(true)
            .build()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        if (intent?.action == ACTION_STOP) stopSelf()
        return START_STICKY
    }

    override fun onDestroy() {
        handler.removeCallbacks(tick)
        hideOverlay()
        super.onDestroy()
    }

    companion object {
        private const val NOTIFICATION_ID = 3001
        const val ACTION_STOP = "com.kafi.utilityhub.speed_stop"

        fun start(context: Context) {
            val intent = Intent(context, SpeedMeterService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, SpeedMeterService::class.java))
        }
    }
}