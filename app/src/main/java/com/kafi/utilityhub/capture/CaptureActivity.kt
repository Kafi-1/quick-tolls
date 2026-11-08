package com.kafi.utilityhub.capture

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.kafi.utilityhub.R
import com.kafi.utilityhub.data.SettingsRepository
import com.kafi.utilityhub.util.PermissionHelper
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class CaptureActivity : AppCompatActivity() {
    private lateinit var btnScreenshot: Button
    private lateinit var btnRecord: Button
    private lateinit var tvStatus: TextView
    private var recording = false
    private var pendingAction: String? = null

    private val consentLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK && result.data != null) {
                ProjectionHolder.store(result.resultCode, result.data)
                val action = pendingAction
                pendingAction = null
                when (action) {
                    ACTION_SCREENSHOT -> ScreenCaptureService.start(this)
                    ACTION_RECORD -> {
                        recording = true
                        btnRecord.text = "Stop recording"
                        ScreenRecordService.start(this)
                    }
                }
            } else {
                pendingAction = null
                Toast.makeText(this, "Screen capture permission denied", Toast.LENGTH_SHORT).show()
            }
        }

    private val micPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (!granted) {
                Toast.makeText(this, "Microphone denied, recording without audio", Toast.LENGTH_SHORT).show()
            }
            requestProjection(ACTION_RECORD)
        }

    private val resultReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val success = intent?.getBooleanExtra(ScreenCaptureService.EXTRA_SUCCESS, false) == true
            when (intent?.action) {
                ScreenCaptureService.ACTION_RESULT -> {
                    tvStatus.text = if (success) "Screenshot saved to Pictures/Quick Tool" else "Screenshot failed"
                    if (success) {
                        Toast.makeText(this@CaptureActivity, "Screenshot saved", Toast.LENGTH_SHORT).show()
                    }
                }
                ScreenRecordService.ACTION_RESULT -> {
                    recording = false
                    btnRecord.text = "Start recording"
                    val uri = intent.getStringExtra(ScreenCaptureService.EXTRA_URI)
                    tvStatus.text = if (success) "Recording saved to Movies/Quick Tool" else "Recording failed"
                    if (success && uri != null) openVideo(Uri.parse(uri))
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_capture)

        btnScreenshot = findViewById(R.id.btnScreenshot)
        btnRecord = findViewById(R.id.btnRecord)
        tvStatus = findViewById(R.id.tvCaptureStatus)

        btnScreenshot.setOnClickListener {
            tvStatus.text = "Requesting screen capture permission…"
            requestProjection(ACTION_SCREENSHOT)
        }
        btnRecord.setOnClickListener {
            if (recording) {
                ScreenRecordService.stop(this)
                recording = false
                btnRecord.text = "Start recording"
            } else {
                lifecycleScope.launch {
                    val mic = SettingsRepository(this@CaptureActivity).recordMic.first()
                    if (mic && checkSelfPermission(Manifest.permission.RECORD_AUDIO) !=
                        PackageManager.PERMISSION_GRANTED
                    ) {
                        micPermission.launch(Manifest.permission.RECORD_AUDIO)
                    } else {
                        requestProjection(ACTION_RECORD)
                    }
                }
            }
        }
        findViewById<Button>(R.id.btnCaptureEditor).setOnClickListener {
            startActivity(Intent(this, EditorActivity::class.java))
        }
        findViewById<Button>(R.id.btnCaptureStitch).setOnClickListener {
            startActivity(Intent(this, StitchActivity::class.java))
        }
        findViewById<Button>(R.id.btnCaptureFloat).setOnClickListener {
            if (!PermissionHelper.canDrawOverlays(this)) {
                startActivity(PermissionHelper.overlaySettingsIntent(this))
                Toast.makeText(this, "Allow display over other apps, then tap again", Toast.LENGTH_LONG).show()
            } else {
                ContextCompat.startForegroundService(
                    this,
                    Intent(this, OverlayService::class.java)
                )
                Toast.makeText(this, "Floating button enabled", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun requestProjection(action: String) {
        pendingAction = action
        val manager = getSystemService(MediaProjectionManager::class.java)
        consentLauncher.launch(manager.createScreenCaptureIntent())
    }

    private fun openVideo(uri: Uri) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(uri, "video/mp4"))
        } catch (_: Exception) {
        }
    }

    override fun onStart() {
        super.onStart()
        val filter = IntentFilter().apply {
            addAction(ScreenCaptureService.ACTION_RESULT)
            addAction(ScreenRecordService.ACTION_RESULT)
        }
        ContextCompat.registerReceiver(this, resultReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
    }

    override fun onStop() {
        super.onStop()
        try {
            unregisterReceiver(resultReceiver)
        } catch (_: IllegalArgumentException) {
        }
    }

    companion object {
        private const val ACTION_SCREENSHOT = "screenshot"
        private const val ACTION_RECORD = "record"
    }
}