package com.kafi.utilityhub.volume

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.kafi.utilityhub.R
import com.kafi.utilityhub.util.PermissionHelper

class VolumeActivity : AppCompatActivity() {
    private lateinit var audio: AudioManager
    private lateinit var seekMedia: SeekBar
    private lateinit var seekAlarm: SeekBar
    private lateinit var seekNotification: SeekBar
    private lateinit var seekRing: SeekBar
    private lateinit var tvMedia: TextView
    private lateinit var tvAlarm: TextView
    private lateinit var tvNotification: TextView
    private lateinit var tvRing: TextView
    private lateinit var tvMode: TextView

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) = updateUi()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_volume)
        audio = getSystemService(AudioManager::class.java)

        seekMedia = findViewById(R.id.seekMedia)
        seekAlarm = findViewById(R.id.seekAlarm)
        seekNotification = findViewById(R.id.seekNotification)
        seekRing = findViewById(R.id.seekRing)
        tvMedia = findViewById(R.id.tvMedia)
        tvAlarm = findViewById(R.id.tvAlarm)
        tvNotification = findViewById(R.id.tvNotification)
        tvRing = findViewById(R.id.tvRing)
        tvMode = findViewById(R.id.tvMode)

        bindSlider(seekMedia, tvMedia, AudioManager.STREAM_MUSIC)
        bindSlider(seekAlarm, tvAlarm, AudioManager.STREAM_ALARM)
        bindSlider(seekNotification, tvNotification, AudioManager.STREAM_NOTIFICATION)
        bindSlider(seekRing, tvRing, AudioManager.STREAM_RING)

        findViewById<Button>(R.id.btnNormal).setOnClickListener { setRinger(AudioManager.RINGER_MODE_NORMAL) }
        findViewById<Button>(R.id.btnVibrate).setOnClickListener { setRinger(AudioManager.RINGER_MODE_VIBRATE) }
        findViewById<Button>(R.id.btnSilent).setOnClickListener {
            if (needsPolicy()) startActivity(PermissionHelper.notificationPolicySettingsIntent(this))
            else setRinger(AudioManager.RINGER_MODE_SILENT)
        }
        findViewById<Button>(R.id.btnPolicy).setOnClickListener {
            startActivity(PermissionHelper.notificationPolicySettingsIntent(this))
        }
        findViewById<Button>(R.id.btnStartService).setOnClickListener {
            ContextCompat.startForegroundService(this, Intent(this, VolumeNotificationService::class.java))
        }
        findViewById<Button>(R.id.btnStopService).setOnClickListener {
            stopService(Intent(this, VolumeNotificationService::class.java))
        }

        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 10)
        }

        updateUi()
    }

    override fun onStart() {
        super.onStart()
        val filter = IntentFilter().apply {
            // AudioManager.VOLUME_CHANGED_ACTION is hidden from the public SDK
            addAction("android.media.VOLUME_CHANGED_ACTION")
            addAction(AudioManager.RINGER_MODE_CHANGED_ACTION)
        }
        ContextCompat.registerReceiver(this, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
    }

    override fun onStop() {
        super.onStop()
        try {
            unregisterReceiver(receiver)
        } catch (_: IllegalArgumentException) {
        }
    }

    private fun bindSlider(seek: SeekBar, label: TextView, stream: Int) {
        seek.max = audio.getStreamMaxVolume(stream)
        seek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                label.text = "$progress / ${seek.max}"
                if (fromUser) {
                    try {
                        audio.setStreamVolume(stream, progress, 0)
                    } catch (_: SecurityException) {
                    }
                }
            }

            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })
    }

    private fun needsPolicy(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !PermissionHelper.hasNotificationPolicyAccess(this)

    private fun setRinger(mode: Int) {
        try {
            audio.ringerMode = mode
        } catch (_: SecurityException) {
            startActivity(PermissionHelper.notificationPolicySettingsIntent(this))
        }
        updateUi()
    }

    private fun updateUi() {
        updateSlider(seekMedia, tvMedia, AudioManager.STREAM_MUSIC)
        updateSlider(seekAlarm, tvAlarm, AudioManager.STREAM_ALARM)
        updateSlider(seekNotification, tvNotification, AudioManager.STREAM_NOTIFICATION)
        updateSlider(seekRing, tvRing, AudioManager.STREAM_RING)

        val mode = when (audio.ringerMode) {
            AudioManager.RINGER_MODE_SILENT -> "Silent"
            AudioManager.RINGER_MODE_VIBRATE -> "Vibrate"
            else -> "Normal"
        }
        tvMode.text = "Ringer mode: $mode"
    }

    private fun updateSlider(seek: SeekBar, label: TextView, stream: Int) {
        val max = audio.getStreamMaxVolume(stream)
        val current = audio.getStreamVolume(stream)
        seek.max = max
        seek.progress = current
        label.text = "$current / $max"
    }
}