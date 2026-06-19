package com.ab.creatify

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.graphics.createBitmap
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.cos
import kotlin.math.sin

enum class ShapeType {
    FREEHAND, LINE, RECTANGLE, CIRCLE, TRIANGLE, ARROW
}

class DrawingView(context: Context, attrs: AttributeSet) : View(context, attrs) {

    private var currentPath = Path()
    private var currentPaint = Paint()

    private val actions = mutableListOf<DrawingAction>()
    private val undoneActions = mutableListOf<DrawingAction>()

    var brushColor: Int = Color.BLACK
    var canvasBackgroundColor = Color.WHITE
    private var backgroundBitmap: Bitmap? = null
    var brushSize: Float = 10f
    var brushOpacity: Int = 255
    var isEraserMode: Boolean = false
    var eraserSize: Float = 30f
    var currentShape: ShapeType = ShapeType.FREEHAND
    private var startX: Float = 0f
    private var startY: Float = 0f

    var isFillMode: Boolean = false
    var fillTolerance: Int = 60
    private var isFilling = false
    var onFillStart: (() -> Unit)? = null
    var onFillEnd: (() -> Unit)? = null

    private val viewScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    init {
        setupPaint()
    }

    private fun setupPaint() {
        currentPaint = Paint().apply {
            color = brushColor
            style = Paint.Style.STROKE
            strokeJoin = Paint.Join.MITER
            strokeMiter = 10f
            strokeCap = Paint.Cap.ROUND
            strokeWidth = if (isEraserMode) eraserSize else brushSize
            isAntiAlias = true
            alpha = brushOpacity
            xfermode = if (isEraserMode) PorterDuffXfermode(PorterDuff.Mode.CLEAR) else null
        }
    }

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Step 1 — Draw background FIRST (outside saveLayer)
        if (backgroundBitmap != null) {
            canvas.drawBitmap(
                backgroundBitmap!!,
                null,
                Rect(0, 0, width, height),
                null
            )
        } else {
            canvas.drawColor(canvasBackgroundColor)
        }

        val saveCount = canvas.saveLayer(null, null)

        for (action in actions) {
            when (action) {
                is DrawingAction.StrokeAction -> {
                    canvas.drawPath(action.path, action.paint)
                }
                is DrawingAction.FillAction -> {
                    canvas.drawBitmap(
                        action.bitmap,
                        null,
                        Rect(0, 0, width, height),
                        null
                    )
                }
            }
        }
        canvas.drawPath(currentPath, currentPaint)
        canvas.restoreToCount(saveCount)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (isFillMode) {
            if (event.action == MotionEvent.ACTION_DOWN) {
                performFloodFill(event.x.toInt(), event.y.toInt())
            }
            return true
        }

        val x = event.x
        val y = event.y

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                undoneActions.clear()
                setupPaint()
                startX = x
                startY = y
                currentPath = Path()
                if (isEraserMode || currentShape == ShapeType.FREEHAND) {
                    currentPath.moveTo(x, y)
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (isEraserMode || currentShape == ShapeType.FREEHAND) {
                    currentPath.lineTo(x, y)
                } else {
                    updateShapePath(x, y)
                }
            }
            MotionEvent.ACTION_UP -> {
                if (isEraserMode || currentShape == ShapeType.FREEHAND) {
                    currentPath.lineTo(x, y)
                } else {
                    updateShapePath(x, y)
                }
                actions.add(
                    DrawingAction.StrokeAction(
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
                currentPath.addRect(
                    minOf(startX, endX), minOf(startY, endY),
                    maxOf(startX, endX), maxOf(startY, endY),
                    Path.Direction.CW
                )
            }
            ShapeType.CIRCLE -> {
                val radius = hypot(endX - startX, endY - startY)
                currentPath.addCircle(startX, startY, radius, Path.Direction.CW)
            }
            ShapeType.TRIANGLE -> {
                val centerX = (startX + endX) / 2
                currentPath.moveTo(centerX, startY)
                currentPath.lineTo(startX, endY)
                currentPath.lineTo(endX, endY)
                currentPath.lineTo(centerX, startY)
                currentPath.close()
            }
            ShapeType.ARROW -> {
                currentPath.moveTo(startX, startY)
                currentPath.lineTo(endX, endY)
                val angle = atan2(
                    (endY - startY).toDouble(),
                    (endX - startX).toDouble()
                )
                val arrowLength = 40.0
                val x1 = endX - arrowLength * cos(angle - Math.PI / 6)
                val y1 = endY - arrowLength * sin(angle - Math.PI / 6)
                val x2 = endX - arrowLength * cos(angle + Math.PI / 6)
                val y2 = endY - arrowLength * sin(angle + Math.PI / 6)
                currentPath.moveTo(endX, endY)
                currentPath.lineTo(x1.toFloat(), y1.toFloat())
                currentPath.moveTo(endX, endY)
                currentPath.lineTo(x2.toFloat(), y2.toFloat())
            }
            ShapeType.FREEHAND -> {}
        }
    }

    fun undo() {
        if (actions.isNotEmpty()) {
            undoneActions.add(actions.removeAt(actions.lastIndex))
            invalidate()
        }
    }

    fun redo() {
        if (undoneActions.isNotEmpty()) {
            actions.add(undoneActions.removeAt(undoneActions.lastIndex))
            invalidate()
        }
    }

    fun clearCanvas() {
        actions.clear()
        undoneActions.clear()
        invalidate()
    }

    fun setBackgroundImage(bitmap: Bitmap) {
        backgroundBitmap = bitmap
        invalidate()
    }

    fun setCanvasBackground(color: Int) {
        backgroundBitmap = null
        canvasBackgroundColor = color
        invalidate()
    }

    fun getBitmap(): Bitmap {
        val bitmap = createBitmap(width, height)
        draw(Canvas(bitmap))
        return bitmap
    }

    private fun performFloodFill(x: Int, y: Int) {
        if (isFilling || width <= 0 || height <= 0) return
        if (x < 0 || y < 0 || x >= width || y >= height) return

        isFilling = true
        onFillStart?.invoke()

        val snapshot = createBitmap(width, height)
        draw(Canvas(snapshot))

        val targetFillColor = Color.argb(
            brushOpacity,
            Color.red(brushColor),
            Color.green(brushColor),
            Color.blue(brushColor)
        )

        viewScope.launch {
            val result = withContext(Dispatchers.Default) {
                floodFill(snapshot, x, y, targetFillColor, fillTolerance)
            }
            if (result != null) {
                undoneActions.clear()
                actions.add(DrawingAction.FillAction(result))
                invalidate()
            }
            isFilling = false
            onFillEnd?.invoke()
        }
    }

    private fun floodFill(
        source: Bitmap,
        startX: Int,
        startY: Int,
        fillColor: Int,
        tolerance: Int,
        growPixels: Int = 2
    ): Bitmap? {
        val w = source.width
        val h = source.height
        val pixels = IntArray(w * h)
        source.getPixels(pixels, 0, w, 0, 0, w, h)

        val startIndex = startY * w + startX
        val targetColor = pixels[startIndex]

        if (colorsMatch(targetColor, fillColor, 4)) return null

        val filled = BooleanArray(w * h)
        val visited = BooleanArray(w * h)
        val stack = IntArray(w * h)
        var sp = 0
        stack[sp++] = startIndex
        visited[startIndex] = true

        while (sp > 0) {
            val idx = stack[--sp]
            filled[idx] = true

            val px = idx % w
            val py = idx / w
            val hasLeft = px > 0
            val hasRight = px < w - 1
            val hasUp = py > 0
            val hasDown = py < h - 1

            if (hasLeft) {
                val n = idx - 1
                if (!visited[n] && colorsMatch(pixels[n], targetColor, tolerance)) { visited[n] = true; stack[sp++] = n }
            }
            if (hasRight) {
                val n = idx + 1
                if (!visited[n] && colorsMatch(pixels[n], targetColor, tolerance)) { visited[n] = true; stack[sp++] = n }
            }
            if (hasUp) {
                val n = idx - w
                if (!visited[n] && colorsMatch(pixels[n], targetColor, tolerance)) { visited[n] = true; stack[sp++] = n }
            }
            if (hasDown) {
                val n = idx + w
                if (!visited[n] && colorsMatch(pixels[n], targetColor, tolerance)) { visited[n] = true; stack[sp++] = n }
            }
            if (hasLeft && hasUp) {
                val n = idx - w - 1
                if (!visited[n] && colorsMatch(pixels[n], targetColor, tolerance)) { visited[n] = true; stack[sp++] = n }
            }
            if (hasRight && hasUp) {
                val n = idx - w + 1
                if (!visited[n] && colorsMatch(pixels[n], targetColor, tolerance)) { visited[n] = true; stack[sp++] = n }
            }
            if (hasLeft && hasDown) {
                val n = idx + w - 1
                if (!visited[n] && colorsMatch(pixels[n], targetColor, tolerance)) { visited[n] = true; stack[sp++] = n }
            }
            if (hasRight && hasDown) {
                val n = idx + w + 1
                if (!visited[n] && colorsMatch(pixels[n], targetColor, tolerance)) { visited[n] = true; stack[sp++] = n }
            }
        }

        var currentMask = filled
        repeat(growPixels) {
            val next = currentMask.copyOf()
            for (py in 0 until h) {
                for (px in 0 until w) {
                    val idx = py * w + px
                    if (currentMask[idx]) continue

                    val hasLeft = px > 0
                    val hasRight = px < w - 1
                    val hasUp = py > 0
                    val hasDown = py < h - 1

                    val touchesFilled =
                        (hasLeft && currentMask[idx - 1]) ||
                                (hasRight && currentMask[idx + 1]) ||
                                (hasUp && currentMask[idx - w]) ||
                                (hasDown && currentMask[idx + w])

                    if (touchesFilled) next[idx] = true
                }
            }
            currentMask = next
        }

        val resultPixels = IntArray(w * h) { i ->
            if (currentMask[i]) fillColor else Color.TRANSPARENT
        }

        return Bitmap.createBitmap(resultPixels, w, h, Bitmap.Config.ARGB_8888)
    }


    private fun colorsMatch(c1: Int, c2: Int, tolerance: Int): Boolean {
        return abs(Color.alpha(c1) - Color.alpha(c2)) <= tolerance &&
                abs(Color.red(c1) - Color.red(c2)) <= tolerance &&
                abs(Color.green(c1) - Color.green(c2)) <= tolerance &&
                abs(Color.blue(c1) - Color.blue(c2)) <= tolerance
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        viewScope.cancel()
    }
}