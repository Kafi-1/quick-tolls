package com.kafi.utilityhub.network

import android.Manifest
import android.content.pm.PackageManager
import android.net.TrafficStats
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.materialswitch.MaterialSwitch
import com.kafi.utilityhub.R
import com.kafi.utilityhub.data.SettingsRepository
import com.kafi.utilityhub.util.Formatters
import com.kafi.utilityhub.util.PermissionHelper
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SpeedMeterActivity : AppCompatActivity() {
    private val monitor = TrafficMonitor()
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var tvDown: TextView
    private lateinit var tvUp: TextView
    private lateinit var tvTotalDown: TextView
    private lateinit var tvTotalUp: TextView

    private val tick = object : Runnable {
        override fun run() {
            val sample = monitor.sample()
            tvDown.text = Formatters.bytesPerSec(sample.downBytesPerSec)
            tvUp.text = Formatters.bytesPerSec(sample.upBytesPerSec)
            tvTotalDown.text = "Total ↓ ${Formatters.bytes(TrafficStats.getTotalRxBytes())}"
            tvTotalUp.text = "Total ↑ ${Formatters.bytes(TrafficStats.getTotalTxBytes())}"
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_speed_meter)

        tvDown = findViewById(R.id.tvSpeedDown)
        tvUp = findViewById(R.id.tvSpeedUp)
        tvTotalDown = findViewById(R.id.tvSpeedTotalDown)
        tvTotalUp = findViewById(R.id.tvSpeedTotalUp)

        val repo = SettingsRepository(this)

        findViewById<Button>(R.id.btnSpeedStart).setOnClickListener {
            requestNotificationPermission()
            SpeedMeterService.start(this)
            Toast.makeText(this, "Speed notification started", Toast.LENGTH_SHORT).show()
        }
        findViewById<Button>(R.id.btnSpeedStop).setOnClickListener {
            SpeedMeterService.stop(this)
            Toast.makeText(this, "Speed notification stopped", Toast.LENGTH_SHORT).show()
        }

        val swFloat = findViewById<MaterialSwitch>(R.id.switchSpeedFloat)
        var ready = false
        lifecycleScope.launch {
            swFloat.isChecked = repo.showFloatingSpeed.first()
            ready = true
        }
        swFloat.setOnCheckedChangeListener { _, checked ->
            if (!ready) return@setOnCheckedChangeListener
            if (checked && !PermissionHelper.canDrawOverlays(this)) {
                swFloat.isChecked = false
                startActivity(PermissionHelper.overlaySettingsIntent(this))
                Toast.makeText(this, "Allow display over other apps, then enable again", Toast.LENGTH_LONG).show()
                return@setOnCheckedChangeListener
            }
            lifecycleScope.launch { repo.setBoolean(SettingsRepository.SHOW_FLOATING_SPEED, checked) }
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 20)
        }
    }

    override fun onStart() {
        super.onStart()
        handler.post(tick)
    }

    override fun onStop() {
        super.onStop()
        handler.removeCallbacks(tick)
    }
}