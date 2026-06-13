package com.ab.creatify

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.widget.ImageButton
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {

    private lateinit var drawingView: DrawingView
    private lateinit var toolButtons: List<ImageButton>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        drawingView =  findViewById(R.id.drawingView)

        val btnUndo = findViewById< ImageButton>(R.id.btnUndo)
        val btnClear = findViewById<ImageButton>(R.id.btnClear)
        val btnBrush = findViewById<ImageButton>(R.id.btnBrush)
        val btnRedo = findViewById<ImageButton>(R.id.btnRedo)
        val btnEraser = findViewById<ImageButton>(R.id.btnEraser)
        val btnShapes = findViewById<ImageButton>(R.id.btnShapes)

        toolButtons = listOf(btnBrush, btnEraser, btnShapes)

        btnUndo.setOnClickListener {
            drawingView.undo()
        }

        btnRedo.setOnClickListener {
            drawingView.redo()
        }

        btnClear.setOnClickListener {
            drawingView.clearCanvas()
        }

        btnEraser.setOnClickListener {
            drawingView.isEraserMode = true
            setActiveTool(btnEraser)

            EraserSizePopup(this, drawingView.eraserSize) { size ->
                drawingView.eraserSize = size
            }.show(btnEraser)
        }

        btnShapes.setOnClickListener {
            drawingView.isEraserMode = false
            setActiveTool(btnShapes)

            ShapeSelectorPopup(
                context = this,
                currentShape = drawingView.currentShape,
                currentColor = drawingView.brushColor,
                onShapeSelected = { shapeType, iconRes ->
                    drawingView.currentShape = shapeType
                    btnShapes.setImageResource(iconRes)
                },
                onColorSelected = { color ->
                    drawingView.brushColor = color
                }
            ).show(btnShapes)
        }

        btnBrush.setOnClickListener {
            drawingView.isEraserMode = false
            drawingView.currentShape = ShapeType.FREEHAND
            setActiveTool(btnBrush)
            val brushSheet = BrushBottomSheet(
                currentColor = drawingView.brushColor,
                currentSize = drawingView.brushSize,
                currentOpacity = drawingView.brushOpacity,
                listener = object : BrushBottomSheet.BrushSettingsListener {
                    override fun onColorChanged(color: Int) {
                        drawingView.brushColor = color
                    }
                    override fun onSizeChanged(size: Float) {
                        drawingView.brushSize = size
                    }
                    override fun onOpacityChanged(opacity: Int) {
                        drawingView.brushOpacity = opacity
                    }
                }
            )
            brushSheet.show(supportFragmentManager, "BrushBottomSheet")
        }
        setActiveTool(btnBrush)
    }
    private fun setActiveTool(activeBtn: ImageButton) {
        toolButtons.forEach { btn ->
            btn.setBackgroundColor(Color.WHITE)
        }
        val drawable = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 16f
            setColor(Color.parseColor("#5cbcd9"))
        }
        activeBtn.background = drawable
    }
}