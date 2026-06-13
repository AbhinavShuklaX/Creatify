package com.ab.creatify

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import kotlin.math.hypot

enum class ShapeType {
    FREEHAND, LINE, RECTANGLE, CIRCLE
}

class DrawingView(context: Context, attrs: AttributeSet) : View(context,attrs){

    private var currentPath = Path()
    private var currentPaint = Paint()
    private var paths = mutableListOf<Pair<Path, Paint>>()
    private var undonePaths = mutableListOf<Pair<Path, Paint>>()

    var brushColor: Int = Color.BLACK
    var brushSize: Float = 10f
    var brushOpacity: Int = 255
    var isEraserMode: Boolean = false
    var eraserSize: Float = 30f
    var currentShape: ShapeType = ShapeType.FREEHAND
    private var startX: Float = 0f
    private var startY: Float = 0f


    init {
        setupPaint()
    }

    private fun setupPaint() {
        currentPaint = Paint().apply {
            color = brushColor
            style = Paint.Style.STROKE
            strokeJoin = Paint.Join.ROUND
            strokeCap = Paint.Cap.ROUND
            strokeWidth = if (isEraserMode) eraserSize else brushSize
            isAntiAlias = true
            alpha = brushOpacity

            if (isEraserMode) {
                xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
            } else {
                xfermode = null
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val saveCount = canvas.saveLayer(null, null)

        for ((path, paint) in paths) {
            canvas.drawPath(path, paint)
        }

        canvas.drawPath(currentPath, currentPaint)
        canvas.restoreToCount(saveCount)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                undonePaths.clear()
                setupPaint()

                startX = x
                startY = y
                currentPath = Path()

                if (currentShape == ShapeType.FREEHAND) {
                    currentPath.moveTo(x, y)
                }

            }

            MotionEvent.ACTION_MOVE -> {
                if (currentShape == ShapeType.FREEHAND) {
                    currentPath.lineTo(x, y)
                } else {
                    updateShapePath(x, y)
                }
            }

            MotionEvent.ACTION_UP -> {
                if (currentShape == ShapeType.FREEHAND) {
                    currentPath.lineTo(x, y)
                } else {
                    updateShapePath(x, y)
                }

                paths.add(
                    Pair(
                        Path(currentPath),
                        Paint(currentPaint)
                    )
                )
                currentPath = Path()
                performClick()
            }

        }
        invalidate()
        return true
    }

    private fun updateShapePath(endX: Float, endY: Float) {
        currentPath.reset()

        when (currentShape) {
            ShapeType.LINE -> {
                currentPath.moveTo(startX, startY)
                currentPath.lineTo(endX, endY)
            }
            ShapeType.RECTANGLE -> {
                val left = minOf(startX, endX)
                val top = minOf(startY, endY)
                val right = maxOf(startX, endX)
                val bottom = maxOf(startY, endY)
                currentPath.addRect(left, top, right, bottom, Path.Direction.CW)
            }
            ShapeType.CIRCLE -> {
                val radius = hypot((endX - startX), (endY - startY))
                currentPath.addCircle(startX, startY, radius, Path.Direction.CW)
            }
            ShapeType.FREEHAND -> {
                // not used here
            }
        }
    }

    override fun performClick(): Boolean {
        return super.performClick()
    }

        fun undo() {
            if(paths.isNotEmpty()) {
                undonePaths.add(paths.removeAt(paths.lastIndex))
                invalidate()
            }
        }

    fun redo() {
        if(undonePaths.isNotEmpty()) {
            paths.add(undonePaths.removeAt(undonePaths.lastIndex))
            invalidate()
        }
    }

    fun clearCanvas() {
        paths.clear()
        undonePaths.clear()
        invalidate()
    }


}