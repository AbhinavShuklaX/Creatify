package com.ab.creatify

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.PopupWindow

class BackgroundColorPopup(
    private val context: Context,
    private val onColorSelected: (Int) -> Unit,
    private val onImageSelected: () -> Unit
) {

    private lateinit var popupWindow: PopupWindow

    private var selectedBackgroundColor = Color.WHITE
    private val colorCircles = mutableListOf<Pair<View, Int>>()
    private val colors = listOf(
        Color.WHITE,
        Color.BLACK,
        Color.parseColor("#64B5F6"),
        Color.parseColor("#81C784"),
        Color.parseColor("#BA68C8")
    )

    fun show(anchorView: View) {
        val popupView = LayoutInflater
            .from(context)
            .inflate(
                R.layout.popup_background_colors,
                null
            )

        val palette =
            popupView.findViewById<LinearLayout>(
                R.id.colorPalette
            )


        val chooseImageLayout =
            popupView.findViewById<LinearLayout>(
                R.id.chooseImageLayout
            )

        chooseImageLayout.setOnClickListener {
            popupWindow.dismiss()
            onImageSelected()
        }

        colors.forEach { color ->

            val circle = View(context)
            val params =
                LinearLayout.LayoutParams(80, 80)
            params.setMargins(8, 8, 8, 8)
            circle.layoutParams = params


            updateCircleDrawable(circle, color)
            colorCircles.add(
                Pair(circle, color)
            )

            circle.setOnClickListener {
                selectedBackgroundColor = color
                onColorSelected(color)
                refreshCircles()
            }

            palette.addView(circle)
        }

        popupWindow = PopupWindow(
            popupView,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            true
        )

        popupWindow.elevation = 12f

        popupWindow.showAsDropDown(
            anchorView,
            -120,
            10
        )
    }

    private fun updateCircleDrawable(
        circle: View,
        color: Int
    ) {

        val drawable = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(color)
            if (color == Color.WHITE) {
                setStroke(
                    2,
                    Color.parseColor("#D0D0D0")
                )
            }
            if (color == selectedBackgroundColor) {
                setStroke(
                    6,
                    Color.parseColor("#5cbcd9")
                )
            }
        }
        circle.background = drawable
    }

    private fun refreshCircles() {

        for ((circle, color) in colorCircles) {

            updateCircleDrawable(
                circle,
                color
            )
        }
    }
}