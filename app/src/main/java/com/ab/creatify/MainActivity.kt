package com.ab.creatify

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.MotionEvent
import android.view.View
import android.widget.ImageButton
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var drawingView: DrawingView
    private lateinit var toolButtons: List<ImageButton>

    @SuppressLint("DiscouragedPrivateApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.statusBarColor = getColor(R.color.toolbar_bg)
        WindowInsetsControllerCompat(window, window.decorView)
            .isAppearanceLightStatusBars = false
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(0, 0, 0, systemBars.bottom)
            insets
        }

        val topToolbar = findViewById<View>(R.id.topToolbar)
        ViewCompat.setOnApplyWindowInsetsListener(topToolbar) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(v.paddingLeft, systemBars.top, v.paddingRight, v.paddingBottom)
            insets
        }

        drawingView =  findViewById(R.id.drawingView)

        val btnUndo = findViewById< ImageButton>(R.id.btnUndo)
        val btnClear = findViewById<ImageButton>(R.id.btnClear)
        val btnBrush = findViewById<ImageButton>(R.id.btnBrush)
        val btnRedo = findViewById<ImageButton>(R.id.btnRedo)
        val btnEraser = findViewById<ImageButton>(R.id.btnEraser)
        val btnShapes = findViewById<ImageButton>(R.id.btnShapes)
        val btnFill = findViewById<ImageButton>(R.id.btnFill)
        val btnMenu = findViewById<ImageButton>(R.id.btnMenu)
        val btnBackground = findViewById<ImageButton>(R.id.btnBackground)

        toolButtons = listOf(btnBrush, btnEraser, btnShapes, btnFill)

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
            drawingView.isFillMode = false
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
                onShapeSelected = { shapeType, _ ->
                    drawingView.isFillMode = false
                    drawingView.isEraserMode = false
                    drawingView.currentShape = shapeType
                    setActiveTool(btnShapes)
                },
                onColorSelected = { color ->
                    drawingView.brushColor = color
                }
            ).show(btnShapes)
        }

        btnFill.setOnClickListener {
            drawingView.isEraserMode = false
            drawingView.isFillMode = true
            drawingView.currentShape = ShapeType.FREEHAND
            setActiveTool(btnFill)
        }

        btnBrush.setOnClickListener {
            drawingView.isEraserMode = false
            drawingView.isFillMode = false
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


        listOf(
            btnUndo, btnRedo, btnBrush, btnEraser,
            btnShapes, btnClear, btnFill,
            btnBackground, btnMenu
        ).forEach { addPressAnimation(it) }

        btnMenu.setOnClickListener {

            val popup = PopupMenu(this, btnMenu)

            popup.menuInflater.inflate(
                R.menu.top_menu,
                popup.menu
            )

            try {
                val field = popup.javaClass.getDeclaredField("mPopup")
                field.isAccessible = true

                val menuPopupHelper = field.get(popup)

                menuPopupHelper.javaClass
                    .getDeclaredMethod(
                        "setForceShowIcon",
                        Boolean::class.java
                    )
                    .invoke(menuPopupHelper, true)

            } catch (e: Exception) {
                e.printStackTrace()
            }

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
                currentColor = drawingView.canvasBackgroundColor,
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
            btn.setBackgroundColor(getColor(R.color.toolbar_bg))
        }
        val drawable = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            cornerRadius = 16f
            setColor(getColor(R.color.accent))
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

        lifecycleScope.launch {
            try {
                val bitmap = withContext(Dispatchers.Default) {
                    drawingView.getBitmap()
                }

                withContext(Dispatchers.IO) {

                    val filename =
                        "Creatify_${System.currentTimeMillis()}.png"

                    val fos =
                        if (android.os.Build.VERSION.SDK_INT >=
                            android.os.Build.VERSION_CODES.Q
                        ) {
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

                            val uri =
                                contentResolver.insert(
                                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
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
                }

                Toast.makeText(
                    this@MainActivity,
                    "Drawing Saved",
                    Toast.LENGTH_SHORT
                ).show()
            } catch (e: Exception) {
                Toast.makeText(
                    this@MainActivity,
                    "Failed to Save",
                    Toast.LENGTH_SHORT
                ).show()
                e.printStackTrace()
            }
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
    @SuppressLint("ClickableViewAccessibility")
    private fun addPressAnimation(view: View) {
        view.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    v.animate().scaleX(0.65f).scaleY(0.65f).setDuration(30).start()
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    v.animate().scaleX(1f).scaleY(1f).setDuration(150).start()
                }
            }
            false
        }
    }

}