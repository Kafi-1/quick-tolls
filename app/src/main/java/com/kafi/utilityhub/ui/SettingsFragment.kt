package com.kafi.utilityhub.ui

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.RadioGroup
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.snackbar.Snackbar
import com.kafi.utilityhub.R
import com.kafi.utilityhub.data.HistoryRepository
import com.kafi.utilityhub.data.SettingsRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SettingsFragment : Fragment(R.layout.fragment_settings) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val repo = SettingsRepository(requireContext())
        val ui = lifecycleScope

        val rgTheme = view.findViewById<RadioGroup>(R.id.rgTheme)
        var themeReady = false
        rgTheme.setOnCheckedChangeListener { _, id ->
            if (!themeReady) return@setOnCheckedChangeListener
            val mode = when (id) {
                R.id.rbLight -> "light"
                R.id.rbDark -> "dark"
                else -> "system"
            }
            ui.launch { repo.setString(SettingsRepository.THEME_MODE, mode) }
            AppCompatDelegate.setDefaultNightMode(
                when (mode) {
                    "light" -> AppCompatDelegate.MODE_NIGHT_NO
                    "dark" -> AppCompatDelegate.MODE_NIGHT_YES
                    else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                }
            )
        }
        ui.launch {
            val mode = repo.themeMode.first()
            rgTheme.check(
                when (mode) {
                    "light" -> R.id.rbLight
                    "dark" -> R.id.rbDark
                    else -> R.id.rbSystem
                }
            )
            themeReady = true
        }

        val rgFormat = view.findViewById<RadioGroup>(R.id.rgFormat)
        var formatReady = false
        rgFormat.setOnCheckedChangeListener { _, id ->
            if (!formatReady) return@setOnCheckedChangeListener
            val fmt = if (id == R.id.rbJpeg) "jpeg" else "png"
            ui.launch { repo.setString(SettingsRepository.SCREENSHOT_FORMAT, fmt) }
        }
        ui.launch {
            val fmt = repo.screenshotFormat.first()
            rgFormat.check(if (fmt == "jpeg") R.id.rbJpeg else R.id.rbPng)
            formatReady = true
        }

        val seekQuality = view.findViewById<SeekBar>(R.id.seekQuality)
        ui.launch { seekQuality.progress = repo.screenshotQuality.first() }
        seekQuality.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, p: Int, fromUser: Boolean) {
                if (fromUser) ui.launch { repo.setInt(SettingsRepository.SCREENSHOT_QUALITY, p) }
            }

            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })

        val spinner = view.findViewById<Spinner>(R.id.spinnerResolution)
        val options = listOf("480p", "720p", "1080p")
        spinner.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, options)
        var spinnerReady = false
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                if (!spinnerReady) return
                ui.launch { repo.setString(SettingsRepository.RECORD_RESOLUTION, options[pos]) }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        ui.launch {
            val res = repo.recordResolution.first()
            spinner.setSelection(options.indexOf(res).coerceAtLeast(0))
            spinnerReady = true
        }

        val seekFps = view.findViewById<SeekBar>(R.id.seekFps)
        val tvFps = view.findViewById<TextView>(R.id.tvFps)
        ui.launch {
            seekFps.progress = repo.recordFps.first()
            tvFps.text = "${seekFps.progress} fps"
        }
        seekFps.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, p: Int, fromUser: Boolean) {
                tvFps.text = "$p fps"
                if (fromUser) ui.launch { repo.setInt(SettingsRepository.RECORD_FPS, p.coerceAtLeast(1)) }
            }

            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })

        val seekBitrate = view.findViewById<SeekBar>(R.id.seekBitrate)
        val tvBitrate = view.findViewById<TextView>(R.id.tvBitrate)
        fun bitrateKbps(p: Int) = 1000 + p * 100
        ui.launch {
            val kbps = repo.recordBitrateKbps.first()
            seekBitrate.progress = ((kbps - 1000) / 100).coerceIn(0, 110)
            tvBitrate.text = "${bitrateKbps(seekBitrate.progress)} kbps"
        }
        seekBitrate.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, p: Int, fromUser: Boolean) {
                tvBitrate.text = "${bitrateKbps(p)} kbps"
                if (fromUser) ui.launch { repo.setInt(SettingsRepository.RECORD_BITRATE_KBPS, bitrateKbps(p)) }
            }

            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })

        val swMic = view.findViewById<MaterialSwitch>(R.id.switchMic)
        var micReady = false
        ui.launch {
            swMic.isChecked = repo.recordMic.first()
            micReady = true
        }
        swMic.setOnCheckedChangeListener { _, on ->
            if (micReady) ui.launch { repo.setBoolean(SettingsRepository.RECORD_MIC, on) }
        }

        view.findViewById<Button>(R.id.btnClearHistory).setOnClickListener {
            ui.launch {
                HistoryRepository(requireContext()).clear()
                Snackbar.make(view, "History cleared", Snackbar.LENGTH_SHORT).show()
            }
        }

        view.findViewById<TextView>(R.id.tvVersion).text = "Quick Tool 1.0"
    }
}