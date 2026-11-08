package com.kafi.utilityhub.capture

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
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

class EditorActivity : AppCompatActivity() {
    private lateinit var imageView: ImageView
    private lateinit var drawingView: DrawingView
    private lateinit var btnDraw: Button
    private lateinit var btnColor: Button
    private var bitmap: Bitmap? = null
    private var lastSaved: Uri? = null
    private var colorIndex = 0

    private val colors = intArrayOf(
        Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW, Color.BLACK
    )

    private val pickImage =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let { loadImage(it) }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_editor)

        imageView = findViewById(R.id.ivEditor)
        drawingView = findViewById(R.id.drawingView)
        btnDraw = findViewById(R.id.btnEditorDraw)
        btnColor = findViewById(R.id.btnEditorColor)

        findViewById<Button>(R.id.btnEditorPick).setOnClickListener {
            pickImage.launch("image/*")
        }
        findViewById<Button>(R.id.btnEditorRotate).setOnClickListener { rotate() }
        findViewById<Button>(R.id.btnEditorUndo).setOnClickListener { drawingView.undo() }
        findViewById<Button>(R.id.btnEditorSave).setOnClickListener { save(false) }
        findViewById<Button>(R.id.btnEditorShare).setOnClickListener { save(true) }
        btnDraw.setOnClickListener {
            drawingView.drawingEnabled = !drawingView.drawingEnabled
            btnDraw.text = if (drawingView.drawingEnabled) "Draw: on" else "Draw: off"
        }
        btnColor.setOnClickListener {
            colorIndex = (colorIndex + 1) % colors.size
            drawingView.setStrokeColor(colors[colorIndex])
            btnColor.text = "Color ${colorIndex + 1}"
        }
    }

    private fun loadImage(uri: Uri) {
        lifecycleScope.launch {
            val decoded = withContext(Dispatchers.IO) { decode(uri) }
            if (decoded == null) {
                Toast.makeText(this@EditorActivity, "Could not open image", Toast.LENGTH_SHORT).show()
                return@launch
            }
            bitmap?.recycle()
            bitmap = decoded
            imageView.setImageBitmap(decoded)
            drawingView.clear()
            drawingView.drawingEnabled = false
            btnDraw.text = "Draw: off"
            lastSaved = null
        }
    }

    private fun decode(uri: Uri): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        while (bounds.outWidth / sample > 2400 || bounds.outHeight / sample > 2400) sample *= 2
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        return contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, opts)
        }
    }

    private fun rotate() {
        val source = bitmap ?: run {
            Toast.makeText(this, "Pick an image first", Toast.LENGTH_SHORT).show()
            return
        }
        val matrix = Matrix().apply { postRotate(90f) }
        val rotated = Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
        bitmap = rotated
        imageView.setImageBitmap(rotated)
        drawingView.clear()
        lastSaved = null
    }

    private fun save(share: Boolean) {
        val current = bitmap ?: run {
            Toast.makeText(this, "Pick an image first", Toast.LENGTH_SHORT).show()
            return
        }
        val width = drawingView.width
        val height = drawingView.height
        if (width <= 0 || height <= 0) return

        lifecycleScope.launch {
            val uri = withContext(Dispatchers.IO) {
                val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(output)
                canvas.drawColor(Color.WHITE)
                canvas.drawBitmap(current, imageView.imageMatrix, null)
                drawingView.draw(canvas)
                val repo = SettingsRepository(this@EditorActivity)
                val format = repo.screenshotFormat.first()
                val quality = repo.screenshotQuality.first()
                val saved = MediaStoreUtils.saveImage(this@EditorActivity, output, format, quality)
                output.recycle()
                saved
            }
            if (uri == null) {
                Toast.makeText(this@EditorActivity, "Save failed", Toast.LENGTH_SHORT).show()
            } else {
                lastSaved = uri
                if (share) shareImage(uri) else
                    Toast.makeText(this@EditorActivity, "Saved to Pictures/Quick Tool", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun shareImage(uri: Uri) {
        try {
            startActivity(
                Intent(Intent.ACTION_SEND)
                    .setType("image/*")
                    .putExtra(Intent.EXTRA_STREAM, uri)
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            )
        } catch (e: Exception) {
            Toast.makeText(this, "No app available to share", Toast.LENGTH_SHORT).show()
        }
    }
}