package com.kafi.utilityhub.capture

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

class DrawingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private class Stroke(val path: Path, val paint: Paint)

    private val strokes = mutableListOf<Stroke>()
    private val redo = mutableListOf<Stroke>()
    private var current: Path? = null
    private val paint = Paint().apply {
        isAntiAlias = true
        color = Color.RED
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        strokeWidth = 8f
    }

    var drawingEnabled = false

    fun setStrokeColor(color: Int) {
        paint.color = color
    }

    fun setStrokeWidth(width: Float) {
        paint.strokeWidth = width
    }

    fun undo() {
        if (strokes.isNotEmpty()) {
            redo.add(strokes.removeAt(strokes.lastIndex))
            invalidate()
        }
    }

    fun clear() {
        redo.clear()
        strokes.clear()
        invalidate()
    }

    fun hasDrawing(): Boolean = strokes.isNotEmpty()

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!drawingEnabled) return false
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                val path = Path().apply { moveTo(event.x, event.y) }
                current = path
                strokes.add(Stroke(path, Paint(paint)))
                redo.clear()
            }
            MotionEvent.ACTION_MOVE -> {
                current?.lineTo(event.x, event.y)
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                current = null
            }
        }
        invalidate()
        return true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        for (stroke in strokes) canvas.drawPath(stroke.path, stroke.paint)
    }
}