package com.ab.creatify

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View

class DrawingView(context: Context, attrs: AttributeSet) : View(context,attrs){

    private var currentPath = Path()
    private var currentPaint = Paint()
    private var paths = mutableListOf<Pair<Path, Paint>>()

    var brushColor: Int = Color.BLACK
    var brushSize: Float = 10f
    var brushOpacity: Int = 255

    init {
        setupPaint()
    }

    private fun setupPaint() {
        currentPaint = Paint().apply {
            color = brushColor
            style = Paint.Style.STROKE
            strokeJoin = Paint.Join.ROUND
            strokeCap = Paint.Cap.ROUND
            strokeWidth = brushSize
            isAntiAlias = true
            alpha = brushOpacity
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        for((path, paint) in paths) {
            canvas.drawPath(path, paint)
        }
        canvas.drawPath(currentPath, currentPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                currentPath = Path()
                setupPaint()
                currentPath.moveTo(x, y)
            }
            MotionEvent.ACTION_MOVE -> {
                currentPath.lineTo(x, y)
            }
            MotionEvent.ACTION_UP -> {
                currentPath.lineTo(x, y)
                paths.add(Pair(currentPath, currentPaint))
                currentPath = Path()
                performClick()
            }
        }
        invalidate()
        return true
    }

    override fun performClick(): Boolean {
        return super.performClick()
    }

    fun undo() {
        if (paths.isNotEmpty()) {
            paths.removeAt(paths.size - 1)
            invalidate()
        }
    }

    fun clearCanvas() {
        paths.clear()
        invalidate()
    }

}