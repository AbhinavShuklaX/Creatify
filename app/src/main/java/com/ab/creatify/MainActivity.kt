package com.ab.creatify

import android.os.Bundle
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {

    private lateinit var drawingView: DrawingView

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

        btnUndo.setOnClickListener {
            drawingView.undo()
        }

        btnClear.setOnClickListener {
            drawingView.clearCanvas()
        }

        btnBrush.setOnClickListener {
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
    }
}