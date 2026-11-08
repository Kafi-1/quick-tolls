package com.kafi.utilityhub.qr

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.ScaleGestureDetector
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.kafi.utilityhub.R
import com.kafi.utilityhub.data.HistoryRepository
import kotlinx.coroutines.launch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class QrScannerActivity : AppCompatActivity() {
    private lateinit var previewView: PreviewView
    private lateinit var tvHint: TextView
    private lateinit var btnTorch: Button
    private lateinit var cameraExecutor: ExecutorService
    private var camera: Camera? = null
    private var analyzer: BarcodeAnalyzer? = null
    private var scanHandled = false
    private var torchOn = false

    private val cameraPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) startCamera() else
                Toast.makeText(this, "Camera permission is required to scan", Toast.LENGTH_LONG).show()
        }

    private val pickImage =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let { scanFromGallery(it) }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_qr_scanner)

        previewView = findViewById(R.id.previewView)
        tvHint = findViewById(R.id.tvQrHint)
        btnTorch = findViewById(R.id.btnQrTorch)
        cameraExecutor = Executors.newSingleThreadExecutor()

        btnTorch.setOnClickListener { toggleTorch() }
        findViewById<Button>(R.id.btnQrGallery).setOnClickListener { pickImage.launch("image/*") }

        val pinch = ScaleGestureDetector(this, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                val cam = camera ?: return false
                val zoomState = cam.cameraInfo.zoomState.value ?: return false
                val ratio = (zoomState.zoomRatio * detector.scaleFactor)
                    .coerceIn(zoomState.minZoomRatio, zoomState.maxZoomRatio)
                cam.cameraControl.setZoomRatio(ratio)
                return true
            }
        })
        previewView.setOnTouchListener { _, event ->
            pinch.onTouchEvent(event)
            true
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            startCamera()
        } else {
            cameraPermission.launch(Manifest.permission.CAMERA)
        }
    }

    private fun startCamera() {
        val providerFuture = ProcessCameraProvider.getInstance(this)
        providerFuture.addListener({
            try {
                val provider = providerFuture.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                val localAnalyzer = BarcodeAnalyzer(
                    onBarcode = { barcode -> onBarcode(barcode) },
                    onError = { }
                )
                analyzer = localAnalyzer
                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also { it.setAnalyzer(cameraExecutor, localAnalyzer) }

                provider.unbindAll()
                camera = provider.bindToLifecycle(
                    this,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    analysis
                )
                val hasFlash = camera?.cameraInfo?.hasFlashUnit() == true
                btnTorch.isEnabled = hasFlash
                tvHint.text = if (hasFlash) "Scanning… pinch to zoom" else "Scanning… pinch to zoom (no flash)"
            } catch (e: Exception) {
                Toast.makeText(this, "Camera failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun toggleTorch() {
        val cam = camera ?: return
        if (cam.cameraInfo.hasFlashUnit() != true) return
        torchOn = !torchOn
        cam.cameraControl.enableTorch(torchOn)
        btnTorch.text = if (torchOn) "Flash: on" else "Flash: off"
    }

    private fun onBarcode(barcode: Barcode) {
        val value = barcode.rawValue ?: barcode.displayValue ?: return
        if (scanHandled) return
        scanHandled = true
        runOnUiThread { showResult(value) }
    }

    private fun scanFromGallery(uri: Uri) {
        val image = try {
            InputImage.fromFilePath(this, uri)
        } catch (e: Exception) {
            Toast.makeText(this, "Could not read image", Toast.LENGTH_SHORT).show()
            return
        }
        BarcodeScanning.getClient().process(image)
            .addOnSuccessListener { codes ->
                val value = codes.firstOrNull()?.rawValue ?: codes.firstOrNull()?.displayValue
                if (value != null) {
                    scanHandled = true
                    showResult(value)
                } else {
                    Toast.makeText(this, "No code found in image", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Scan failed", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showResult(value: String) {
        lifecycleScope.launch {
            HistoryRepository(this@QrScannerActivity)
                .add("qr", "Scanned code", value)
        }
        MaterialAlertDialogBuilder(this)
            .setTitle("Scan result")
            .setMessage(value)
            .setPositiveButton("Copy") { _, _ ->
                val clipboard = getSystemService(ClipboardManager::class.java)
                clipboard.setPrimaryClip(ClipData.newPlainText("Scanned code", value))
                Toast.makeText(this, "Copied", Toast.LENGTH_SHORT).show()
                scanHandled = false
            }
            .setNeutralButton("Open") { _, _ ->
                openValue(value)
                scanHandled = false
            }
            .setNegativeButton("Close") { _, _ -> scanHandled = false }
            .setOnCancelListener { scanHandled = false }
            .show()
    }

    private fun openValue(value: String) {
        try {
            val uri = if (value.startsWith("http://") || value.startsWith("https://")) {
                Uri.parse(value)
            } else {
                Uri.parse("https://www.google.com/search?q=${Uri.encode(value)}")
            }
            startActivity(Intent(Intent.ACTION_VIEW, uri))
        } catch (e: Exception) {
            Toast.makeText(this, "No app available to open this", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        analyzer?.close()
        cameraExecutor.shutdown()
        super.onDestroy()
    }
}