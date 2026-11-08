package com.kafi.utilityhub.capture

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.kafi.utilityhub.R
import com.kafi.utilityhub.data.SettingsRepository
import com.kafi.utilityhub.util.MediaStoreUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class StitchActivity : AppCompatActivity() {
    private lateinit var imageView: ImageView
    private lateinit var tvCount: TextView
    private var uris: List<Uri> = emptyList()
    private var stitched: Bitmap? = null

    private val pickImages =
        registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { result ->
            uris = result
            stitched?.recycle()
            stitched = null
            imageView.setImageDrawable(null)
            tvCount.text = "${result.size} image(s) selected"
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stitch)

        imageView = findViewById(R.id.ivStitch)
        tvCount = findViewById(R.id.tvStitchCount)

        findViewById<Button>(R.id.btnStitchPick).setOnClickListener {
            pickImages.launch("image/*")
        }
        findViewById<Button>(R.id.btnStitchRun).setOnClickListener { stitch() }
        findViewById<Button>(R.id.btnStitchSave).setOnClickListener { save() }
    }

    private fun stitch() {
        if (uris.isEmpty()) {
            Toast.makeText(this, "Pick at least one image", Toast.LENGTH_SHORT).show()
            return
        }
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                val width = 1080
                val gap = 12
                val scaled = uris.mapNotNull { uri -> decode(uri, width) }
                if (scaled.isEmpty()) return@withContext null
                var total = 0
                for (b in scaled) total += b.height + gap
                total = total.coerceAtMost(16000)
                val out = Bitmap.createBitmap(width, total, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(out)
                canvas.drawColor(Color.WHITE)
                var y = 0
                for (b in scaled) {
                    if (y + b.height > total) break
                    canvas.drawBitmap(b, 0f, y.toFloat(), null)
                    y += b.height + gap
                    b.recycle()
                }
                out
            }
            stitched?.recycle()
            stitched = result
            imageView.setImageBitmap(result)
            if (result == null) {
                Toast.makeText(this@StitchActivity, "Stitch failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun decode(uri: Uri, targetWidth: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0) return null
        var sample = 1
        while (bounds.outWidth / sample > targetWidth) sample *= 2
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        val bmp = contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, opts)
        } ?: return null
        if (bmp.width == targetWidth) return bmp
        val scaled = Bitmap.createScaledBitmap(
            bmp,
            targetWidth,
            (bmp.height * targetWidth / bmp.width).coerceAtLeast(1),
            true
        )
        if (scaled !== bmp) bmp.recycle()
        return scaled
    }

    private fun save() {
        val bmp = stitched ?: run {
            Toast.makeText(this, "Stitch the images first", Toast.LENGTH_SHORT).show()
            return
        }
        lifecycleScope.launch {
            val uri = withContext(Dispatchers.IO) {
                val repo = SettingsRepository(this@StitchActivity)
                val format = repo.screenshotFormat.first()
                val quality = repo.screenshotQuality.first()
                MediaStoreUtils.saveImage(this@StitchActivity, bmp, format, quality)
            }
            val message = if (uri != null) "Saved to Pictures/Quick Tool" else "Save failed"
            Toast.makeText(this@StitchActivity, message, Toast.LENGTH_SHORT).show()
        }
    }
}