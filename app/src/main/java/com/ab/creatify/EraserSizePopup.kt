package com.ab.creatify

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.PopupWindow
import androidx.core.graphics.drawable.toDrawable
import androidx.core.graphics.toColorInt

class EraserSizePopup(
    private val context: Context,
    currentSize: Float,
    private val onSizeSelected: (Float) -> Unit
) {

    data class SizeOption(val size: Float, val dotDp: Int)

    private val sizeOptions = listOf(
        SizeOption(20f, 12),
        SizeOption(40f, 18),
        SizeOption(60f, 26),
        SizeOption(80f, 34)
    )

    private var selectedSize = currentSize
    private val dotFrames = mutableListOf<Pair<FrameLayout, SizeOption>>()
    private lateinit var popupWindow: PopupWindow

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

        for (option in sizeOptions) {
            val frame = FrameLayout(context).apply {
                layoutParams = LinearLayout.LayoutParams(dp(48), dp(48)).apply {
                    marginEnd = dp(8)
                }
            }

            val dot = View(context).apply {
                layoutParams = FrameLayout.LayoutParams(dp(option.dotDp), dp(option.dotDp)).apply {
                    gravity = Gravity.CENTER
                }
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor("#424242".toColorInt())
                }
            }

            frame.addView(dot)
            updateFrameBackground(frame, option.size == selectedSize)
            dotFrames.add(Pair(frame, option))

            frame.setOnClickListener {
                selectedSize = option.size
                onSizeSelected(option.size)
                refreshSelection()
            }

            container.addView(frame)
        }

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

    private fun updateFrameBackground(frame: FrameLayout, isSelected: Boolean) {
        frame.background = if (isSelected) {
            GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(context.getColor(R.color.accent))            }
        } else {
            null
        }
    }

    private fun refreshSelection() {
        for ((frame, option) in dotFrames) {
            updateFrameBackground(frame, option.size == selectedSize)
        }
    }

    private fun dp(value: Int): Int = (value * context.resources.displayMetrics.density).toInt()
}