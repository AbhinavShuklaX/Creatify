package com.ab.creatify

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import com.skydoves.colorpickerview.ColorPickerDialog
import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener
import androidx.core.graphics.toColorInt
import androidx.core.graphics.drawable.toDrawable

class ShapeSelectorPopup(
    private val context: Context,
    currentShape: ShapeType,
    private val currentColor: Int,
    private val onShapeSelected: (ShapeType, Int) -> Unit,
    private val onColorSelected: (Int) -> Unit
) {

    data class ShapeOption(val type: ShapeType, val iconRes: Int)

    private val shapeOptions = listOf(
        ShapeOption(ShapeType.LINE, R.drawable.ic_line),
        ShapeOption(ShapeType.RECTANGLE, R.drawable.ic_rectangle),
        ShapeOption(ShapeType.CIRCLE, R.drawable.ic_circle),
        ShapeOption(ShapeType.TRIANGLE, R.drawable.ic_triangle),
        ShapeOption(ShapeType.ARROW, R.drawable.ic_arrow)
    )

    private var selectedShape = currentShape
    private val frameViews = mutableListOf<Pair<FrameLayout, ShapeOption>>()
    private lateinit var popupWindow: PopupWindow
    private lateinit var colorCircle: View

    fun show(anchor: View) {
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(dp(12), dp(12), dp(12), dp(12))
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(16).toFloat()
                setColor(Color.WHITE)
            }
        }

        for (option in shapeOptions) {
            val frame = FrameLayout(context).apply {
                layoutParams = LinearLayout.LayoutParams(dp(48), dp(48)).apply {
                    marginEnd = dp(4)
                }
            }

            val icon = ImageView(context).apply {
                layoutParams = FrameLayout.LayoutParams(dp(28), dp(28)).apply {
                    gravity = Gravity.CENTER
                }
                setImageResource(option.iconRes)
            }

            frame.addView(icon)
            updateFrameBackground(frame, option.type == selectedShape)
            frameViews.add(Pair(frame, option))

            frame.setOnClickListener {
                selectedShape = option.type
                onShapeSelected(option.type, option.iconRes)
                refreshSelection()
            }

            container.addView(frame)
        }

        val divider = View(context).apply {
            layoutParams = LinearLayout.LayoutParams(dp(1), dp(40)).apply {
                marginEnd = dp(8)
                marginStart = dp(4)
            }
            setBackgroundColor("#DDDDDD".toColorInt())
        }
        container.addView(divider)

        val colorFrame = FrameLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(dp(48), dp(48))
        }

        colorCircle = View(context).apply {
            layoutParams = FrameLayout.LayoutParams(dp(28), dp(28)).apply {
                gravity = Gravity.CENTER
            }
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(currentColor)
                setStroke(dp(1), "#BBBBBB".toColorInt())
            }
        }

        colorFrame.addView(colorCircle)
        colorFrame.setOnClickListener {
            openColorPicker()
        }
        container.addView(colorFrame)

        popupWindow = PopupWindow(
            container,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        ).apply {
            isOutsideTouchable = true
            isFocusable = true
            setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
        }

        container.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
        val popupHeight = container.measuredHeight

        popupWindow.showAsDropDown(anchor, 0, -(anchor.height + popupHeight), Gravity.NO_GRAVITY)
    }

    private fun openColorPicker() {
        ColorPickerDialog.Builder(context)
            .setTitle("Pick a Color")
            .attachAlphaSlideBar(false)
            .attachBrightnessSlideBar(true)
            .setPositiveButton("Select", ColorEnvelopeListener { envelope, _ ->
                (colorCircle.background as GradientDrawable).setColor(envelope.color)
                onColorSelected(envelope.color)
            })
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun updateFrameBackground(frame: FrameLayout, isSelected: Boolean) {
        frame.background = if (isSelected) {
            GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(context.getColor(R.color.accent))
            }
        } else {
            null
        }
    }

    private fun refreshSelection() {
        for ((frame, option) in frameViews) {
            updateFrameBackground(frame, option.type == selectedShape)
        }
    }

    private fun dp(value: Int): Int = (value * context.resources.displayMetrics.density).toInt()
}