package com.ab.creatify

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.PopupWindow
import com.skydoves.colorpickerview.ColorPickerDialog
import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener
import androidx.core.graphics.toColorInt

class BackgroundColorPopup(
    private val context: Context,
    currentColor: Int,
    private val onColorSelected: (Int) -> Unit,
    private val onImageSelected: () -> Unit
) {

    private lateinit var popupWindow: PopupWindow
    private var selectedBackgroundColor = currentColor
    private val colorCircles = mutableListOf<Pair<View, Int>>()

    private val row1Colors = listOf(
        Color.WHITE,
        Color.BLACK,
        "#F44336".toColorInt(),
        "#FF9800".toColorInt(),
        "#FFEB3B".toColorInt(),
        "#4CAF50".toColorInt()
    )

    private val row2Colors = listOf(
        "#2196F3".toColorInt(),
        "#9C27B0".toColorInt(),
        "#00BCD4".toColorInt(),
        "#795548".toColorInt(),
        "#FF4081".toColorInt(),
        "#607D8B".toColorInt()
    )

    @SuppressLint("InflateParams")
    fun show(anchorView: View) {
        val popupView = LayoutInflater.from(context)
            .inflate(R.layout.popup_background_colors, null)

        val row1 = popupView.findViewById<LinearLayout>(R.id.colorRow1)
        val row2 = popupView.findViewById<LinearLayout>(R.id.colorRow2)

        // Add colors to row 1
        row1Colors.forEach { color ->
            val circle = createColorCircle(color)
            colorCircles.add(Pair(circle, color))
            row1.addView(circle)
        }

        row2Colors.forEach { color ->
            val circle = createColorCircle(color)
            colorCircles.add(Pair(circle, color))
            row2.addView(circle)
        }

        val rainbowCircle = popupView.findViewById<View>(R.id.rainbowCircle)
        val rainbowDrawable = GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(
                Color.RED, Color.YELLOW,
                Color.GREEN, Color.CYAN,
                Color.BLUE, Color.MAGENTA
            )
        )
        rainbowDrawable.shape = GradientDrawable.OVAL
        rainbowCircle.background = rainbowDrawable

        val customColorLayout = popupView.findViewById<LinearLayout>(R.id.customColorLayout)
        customColorLayout.setOnClickListener {
            openCustomColorPicker()
        }

        val chooseImageLayout = popupView.findViewById<LinearLayout>(R.id.chooseImageLayout)
        chooseImageLayout.setOnClickListener {
            popupWindow.dismiss()
            onImageSelected()
        }

        popupWindow = PopupWindow(
            popupView,
            dpToPx(290),
            LinearLayout.LayoutParams.WRAP_CONTENT,
            true
        ).apply {
            elevation = 16f
            isOutsideTouchable = true
        }

        popupWindow.showAsDropDown(anchorView, -80, 10)
    }

    private fun createColorCircle(color: Int): View {
        val circle = View(context)
        val size = dpToPx(36)
        val params = LinearLayout.LayoutParams(size, size)
        params.setMargins(dpToPx(4), dpToPx(4), dpToPx(4), dpToPx(4))
        circle.layoutParams = params
        updateCircleDrawable(circle, color)

        circle.setOnClickListener {
            selectedBackgroundColor = color
            onColorSelected(color)
            refreshCircles()
        }

        return circle
    }

    private fun openCustomColorPicker() {
        ColorPickerDialog.Builder(context)
            .setTitle("Pick a Color")
            .attachAlphaSlideBar(false)
            .attachBrightnessSlideBar(true)
            .setPositiveButton("Select", ColorEnvelopeListener { envelope, _ ->
                selectedBackgroundColor = envelope.color
                onColorSelected(envelope.color)
                refreshCircles()
                popupWindow.dismiss()
            })
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun updateCircleDrawable(circle: View, color: Int) {
        val drawable = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(color)
            if (color == Color.WHITE) {
                setStroke(dpToPx(1), "#D0D0D0".toColorInt())
            }
            if (color == selectedBackgroundColor) {
                setStroke(dpToPx(3), "#0D1B2A".toColorInt())
            }
        }
        circle.background = drawable
    }

    private fun refreshCircles() {
        for ((circle, color) in colorCircles) {
            updateCircleDrawable(circle, color)
            if (color == selectedBackgroundColor) {
                circle.scaleX = 1.15f
                circle.scaleY = 1.15f
            } else {
                circle.scaleX = 1f
                circle.scaleY = 1f
            }
        }
    }

    private fun dpToPx(dp: Int): Int =
        (dp * context.resources.displayMetrics.density).toInt()
}