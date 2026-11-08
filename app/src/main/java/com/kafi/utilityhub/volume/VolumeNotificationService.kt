package com.kafi.utilityhub.volume

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.media.AudioManager
import androidx.lifecycle.LifecycleService
import com.kafi.utilityhub.util.NotificationHelper

class VolumeNotificationService : LifecycleService() {
    private lateinit var audio: AudioManager

    override fun onCreate() {
        super.onCreate()
        audio = getSystemService(AudioManager::class.java)
        startForeground(NOTIFICATION_ID, buildNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        when (intent?.action) {
            ACTION_UP -> adjust(1)
            ACTION_DOWN -> adjust(-1)
            ACTION_RINGER -> toggleRinger()
            ACTION_STOP -> {
                stopSelf()
                return START_NOT_STICKY
            }
        }
        updateNotification()
        return START_STICKY
    }

    private fun adjust(delta: Int) {
        val current = audio.getStreamVolume(AudioManager.STREAM_MUSIC)
        val max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        audio.setStreamVolume(AudioManager.STREAM_MUSIC, (current + delta).coerceIn(0, max), 0)
    }

    private fun toggleRinger() {
        audio.ringerMode =
            if (audio.ringerMode == AudioManager.RINGER_MODE_NORMAL) AudioManager.RINGER_MODE_VIBRATE
            else AudioManager.RINGER_MODE_NORMAL
    }

    private fun updateNotification() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.notify(NOTIFICATION_ID, buildNotification())
    }

    private fun pending(action: String, requestCode: Int): PendingIntent =
        PendingIntent.getService(
            this,
            requestCode,
            Intent(this, VolumeNotificationService::class.java).setAction(action),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

    private fun buildNotification(): Notification {
        val current = audio.getStreamVolume(AudioManager.STREAM_MUSIC)
        val max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val mode = when (audio.ringerMode) {
            AudioManager.RINGER_MODE_SILENT -> "Silent"
            AudioManager.RINGER_MODE_VIBRATE -> "Vibrate"
            else -> "Normal"
        }
        val open = PendingIntent.getActivity(
            this,
            9,
            Intent(this, VolumeActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationHelper.builder(this, NotificationHelper.CHANNEL_VOLUME)
            .setContentTitle("Media volume $current / $max")
            .setContentText("Ringer: $mode")
            .setContentIntent(open)
            .addAction(0, "Vol -", pending(ACTION_DOWN, 1))
            .addAction(0, "Vol +", pending(ACTION_UP, 2))
            .addAction(0, "Ringer", pending(ACTION_RINGER, 3))
            .addAction(0, "Stop", pending(ACTION_STOP, 4))
            .setOngoing(true)
            .build()
    }

    companion object {
        private const val NOTIFICATION_ID = 1001
        const val ACTION_UP = "com.kafi.utilityhub.vol_up"
        const val ACTION_DOWN = "com.kafi.utilityhub.vol_down"
        const val ACTION_RINGER = "com.kafi.utilityhub.vol_ringer"
        const val ACTION_STOP = "com.kafi.utilityhub.vol_stop"
    }
}