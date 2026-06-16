package com.ab.creatify

import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.ImageDecoder
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.ImageButton
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.WindowInsetsControllerCompat

class MainActivity : AppCompatActivity() {

    private lateinit var drawingView: DrawingView
    private lateinit var toolButtons: List<ImageButton>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.statusBarColor =
            getColor(R.color.toolbar_bg)
        WindowInsetsControllerCompat(
            window,
            window.decorView
        ).isAppearanceLightStatusBars = true
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
            drawingView.currentShape = ShapeType.FREEHAND
            setActiveTool(btnEraser)

            EraserSizePopup(this, drawingView.eraserSize) { size ->
                drawingView.eraserSize = size
            }.show(btnEraser)
        }

        btnShapes.setOnClickListener {
            drawingView.isEraserMode = false
            ShapeSelectorPopup(
                context = this,
                currentShape = drawingView.currentShape,
                currentColor = drawingView.brushColor,
                onShapeSelected = { shapeType, iconRes ->
                    drawingView.currentShape = shapeType
                    btnShapes.setImageResource(iconRes)
                    setActiveTool(btnShapes)
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

        val btnMenu = findViewById<ImageButton>(R.id.btnMenu)
        val btnBackground = findViewById<ImageButton>(R.id.btnBackground)

        btnMenu.setOnClickListener {
            val popup = PopupMenu(this, btnMenu)
            popup.menuInflater.inflate(R.menu.top_menu, popup.menu)
            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.action_save -> {
                        saveDrawing()
                        true
                    }
                    R.id.action_share -> {
                        shareDrawing()
                        true
                    }
                    else -> false
                }
            }
            popup.show()
        }

        btnBackground.setOnClickListener {
            BackgroundColorPopup(
                context = this,
                onColorSelected = { color ->
                    drawingView.setCanvasBackground(color)
                },
                onImageSelected = {
                    imagePickerLauncher.launch("image/*")
                }
            ).show(btnBackground)
        }
    }
    private fun setActiveTool(activeBtn: ImageButton) {
        toolButtons.forEach { btn ->
            btn.setBackgroundColor(Color.WHITE)
        }
        val drawable = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            cornerRadius = 16f
            setColor(Color.parseColor("#5cbcd9"))
        }
        activeBtn.background = drawable
    }

    private val imagePickerLauncher =
        registerForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri ->
            uri?.let {
                val bitmap = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                    val source = ImageDecoder.createSource(
                        contentResolver,
                        it
                    )
                    ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                        decoder.isMutableRequired = true
                        decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                    }
                } else {
                    MediaStore.Images.Media.getBitmap(
                        contentResolver,
                        it
                    )
                }
                drawingView.setBackgroundImage(bitmap)
            }
        }

    private fun saveDrawing() {
        val bitmap = drawingView.getBitmap()
        val filename =
            "Creatify_${System.currentTimeMillis()}.png"
        try {
            val fos = if (
                android.os.Build.VERSION.SDK_INT >=
                android.os.Build.VERSION_CODES.Q
            ) {
                val values = android.content.ContentValues().apply {
                    put(
                        android.provider.MediaStore.Images.Media.DISPLAY_NAME,
                        filename
                    )
                    put(
                        android.provider.MediaStore.Images.Media.MIME_TYPE,
                        "image/png"
                    )
                    put(
                        android.provider.MediaStore.Images.Media.RELATIVE_PATH,
                        "Pictures/Creatify"
                    )
                }

                val uri =
                    contentResolver.insert(
                        android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        values
                    )
                contentResolver.openOutputStream(uri!!)
            } else {
                null
            }
            fos?.use {
                bitmap.compress(
                    Bitmap.CompressFormat.PNG,
                    100,
                    it
                )
            }
            Toast.makeText(
                this,
                "Drawing Saved",
                Toast.LENGTH_SHORT
            ).show()
        } catch (e: Exception) {
            Toast.makeText(
                this,
                "Failed to Save",
                Toast.LENGTH_SHORT
            ).show()
            e.printStackTrace()
        }
    }

    private fun shareDrawing() {
        val bitmap = drawingView.getBitmap()
        val imageUri = getImageUri(bitmap)

        if (imageUri != null) {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {

                type = "image/png"

                putExtra(
                    Intent.EXTRA_STREAM,
                    imageUri
                )

                addFlags(
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            startActivity(
                Intent.createChooser(
                    shareIntent,
                    "Share Drawing"
                )
            )
        }
    }

    private fun getImageUri(bitmap: Bitmap): Uri? {
        val filename = "Creatify_Share_${System.currentTimeMillis()}.png"

        val values = ContentValues().apply {
            put(
                MediaStore.Images.Media.DISPLAY_NAME,
                filename
            )
            put(
                MediaStore.Images.Media.MIME_TYPE,
                "image/png"
            )
            put(
                MediaStore.Images.Media.RELATIVE_PATH,
                "Pictures/Creatify"
            )
        }

        val uri = contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            values
        )
        uri?.let {
            contentResolver.openOutputStream(it)?.use { stream ->

                bitmap.compress(
                    Bitmap.CompressFormat.PNG,
                    100,
                    stream
                )
            }
        }
        return uri
    }

}