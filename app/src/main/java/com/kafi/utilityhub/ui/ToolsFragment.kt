package com.kafi.utilityhub.ui

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.View
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.materialswitch.MaterialSwitch
import com.kafi.utilityhub.R
import com.kafi.utilityhub.capture.CaptureActivity
import com.kafi.utilityhub.capture.OverlayService
import com.kafi.utilityhub.data.SettingsRepository
import com.kafi.utilityhub.network.SpeedMeterActivity
import com.kafi.utilityhub.network.SpeedMeterService
import com.kafi.utilityhub.qr.QrScannerActivity
import com.kafi.utilityhub.util.PermissionHelper
import com.kafi.utilityhub.volume.VolumeActivity
import kotlinx.coroutines.launch

class ToolsFragment : Fragment(R.layout.fragment_tools) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val repo = SettingsRepository(requireContext())

        view.findViewById<View>(R.id.btnVolume).setOnClickListener {
            startActivity(Intent(requireContext(), VolumeActivity::class.java))
        }
        view.findViewById<View>(R.id.btnCapture).setOnClickListener {
            startActivity(Intent(requireContext(), CaptureActivity::class.java))
        }
        view.findViewById<View>(R.id.btnSpeed).setOnClickListener {
            startActivity(Intent(requireContext(), SpeedMeterActivity::class.java))
        }
        view.findViewById<View>(R.id.btnQr).setOnClickListener {
            startActivity(Intent(requireContext(), QrScannerActivity::class.java))
        }

        val speedSwitch = view.findViewById<MaterialSwitch>(R.id.switchSpeedNotification)
        val floatSwitch = view.findViewById<MaterialSwitch>(R.id.switchFloatingCapture)

        lifecycleScope.launch { repo.showSpeedNotification.collect { speedSwitch.isChecked = it } }
        lifecycleScope.launch { repo.floatingCapture.collect { floatSwitch.isChecked = it } }

        speedSwitch.setOnCheckedChangeListener { _, checked ->
            lifecycleScope.launch { repo.setBoolean(SettingsRepository.SHOW_SPEED_NOTIFICATION, checked) }
            val intent = Intent(requireContext(), SpeedMeterService::class.java)
            if (checked) ContextCompat.startForegroundService(requireContext(), intent)
            else requireContext().stopService(intent)
        }

        floatSwitch.setOnCheckedChangeListener { _, checked ->
            lifecycleScope.launch { repo.setBoolean(SettingsRepository.FLOATING_CAPTURE, checked) }
            val ctx = requireContext()
            val intent = Intent(ctx, OverlayService::class.java)
            if (checked) {
                if (Settings.canDrawOverlays(ctx)) ContextCompat.startForegroundService(ctx, intent)
                else startActivity(PermissionHelper.overlaySettingsIntent(ctx))
            } else {
                ctx.stopService(intent)
            }
        }
    }
}