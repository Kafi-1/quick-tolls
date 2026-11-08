package com.kafi.utilityhub.capture

import android.app.Notification
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.WindowManager
import android.widget.ImageView
import android.widget.Toast
import androidx.lifecycle.LifecycleService
import com.kafi.utilityhub.R
import com.kafi.utilityhub.util.NotificationHelper

class OverlayService : LifecycleService() {
    private lateinit var windowManager: WindowManager
    private var overlayView: ImageView? = null
    private lateinit var params: WindowManager.LayoutParams

    private var downX = 0f
    private var downY = 0f
    private var startX = 0
    private var startY = 0
    private var dragged = false

    override fun onCreate() {
        super.onCreate()
        startForeground(NOTIFICATION_ID, buildNotification())
        windowManager = getSystemService(WindowManager::class.java)
        showOverlay()
    }

    private fun showOverlay() {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }
        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 24
            y = 300
        }

        val view = LayoutInflater.from(this)
            .inflate(R.layout.overlay_capture_button, null) as ImageView
        view.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    downX = event.rawX
                    downY = event.rawY
                    startX = params.x
                    startY = params.y
                    dragged = false
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - downX).toInt()
                    val dy = (event.rawY - downY).toInt()
                    if (Math.abs(dx) > 8 || Math.abs(dy) > 8) dragged = true
                    params.x = startX + dx
                    params.y = startY + dy
                    windowManager.updateViewLayout(view, params)
                }
                MotionEvent.ACTION_UP -> {
                    if (!dragged) {
                        v.performClick()
                    }
                }
            }
            true
        }
        view.setOnClickListener {
            startActivity(
                Intent(this, CaptureActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            )
        }
        view.setOnLongClickListener {
            Toast.makeText(this, "Floating button removed", Toast.LENGTH_SHORT).show()
            stopSelf()
            true
        }
        windowManager.addView(view, params)
        overlayView = view
    }

    private fun buildNotification(): Notification =
        NotificationHelper.builder(this, NotificationHelper.CHANNEL_CAPTURE)
            .setContentTitle("Floating capture button")
            .setContentText("Tap the bubble to open capture, long-press to hide")
            .setSmallIcon(R.drawable.ic_notification)
            .setOngoing(true)
            .build()

    override fun onDestroy() {
        overlayView?.let {
            try {
                windowManager.removeView(it)
            } catch (_: Exception) {
            }
        }
        overlayView = null
        super.onDestroy()
    }

    companion object {
        private const val NOTIFICATION_ID = 2003
    }
}