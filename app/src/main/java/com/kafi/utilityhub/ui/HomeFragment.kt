package com.kafi.utilityhub.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.kafi.utilityhub.R
import com.kafi.utilityhub.capture.CaptureActivity
import com.kafi.utilityhub.network.SpeedMeterActivity
import com.kafi.utilityhub.qr.QrScannerActivity
import com.kafi.utilityhub.volume.VolumeActivity

class HomeFragment : Fragment(R.layout.fragment_home) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.findViewById<View>(R.id.cardVolume).setOnClickListener {
            startActivity(Intent(requireContext(), VolumeActivity::class.java))
        }
        view.findViewById<View>(R.id.cardCapture).setOnClickListener {
            startActivity(Intent(requireContext(), CaptureActivity::class.java))
        }
        view.findViewById<View>(R.id.cardSpeed).setOnClickListener {
            startActivity(Intent(requireContext(), SpeedMeterActivity::class.java))
        }
        view.findViewById<View>(R.id.cardQr).setOnClickListener {
            startActivity(Intent(requireContext(), QrScannerActivity::class.java))
        }
    }
}