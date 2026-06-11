package com.ab.creatify

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.Toast
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class BrushBottomSheet(
    private val currentColor: Int,
    private val currentSize: Float,
    private val currentOpacity: Int,
    private val listener: BrushSettingsListener
) : BottomSheetDialogFragment() {

    interface BrushSettingsListener {
        fun onColorChanged(color: Int)
        fun onSizeChanged(size: Float)
        fun onOpacityChanged(opacity: Int)
    }

        private val colors = listOf(
            Color.BLACK,
            Color.WHITE,
            Color.RED,
            Color.BLUE,
            Color.GREEN,
            Color.CYAN,
            Color.MAGENTA
    )


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.bottom_sheet_brush, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupColors(view)
        setupSizeSlider(view)
        setupOpacitySlider(view)
    }

    private var selectedColor = currentColor
    private val colorCircles = mutableListOf<Pair<View, Int>>()

    private fun setupColors(view: View) {
        val colorPalette = view.findViewById<LinearLayout>(R.id.colorPalette)

        for (color in colors) {
            val circle = View(requireContext())
            val size = 130
            val params = LinearLayout.LayoutParams(size, size)
            params.setMargins(8, 8, 8, 8)
            circle.layoutParams = params

            colorCircles.add(Pair(circle, color))
            updateCircleDrawable(circle, color, color == selectedColor)

            circle.setOnClickListener {
                selectedColor = color
                listener.onColorChanged(color)
                refreshCircles()
            }

            colorPalette.addView(circle)
        }

        addPickerCircle(colorPalette)

    }

    private fun addPickerCircle(colorPalette: LinearLayout) {

        val pickerCircle = View(requireContext())

        val size = 130
        val params = LinearLayout.LayoutParams(size, size)

        params.setMargins(8, 8, 8, 8)

        pickerCircle.layoutParams = params

        val drawable = GradientDrawable()

        drawable.shape = GradientDrawable.OVAL

        drawable.setColor(Color.LTGRAY)

        drawable.setStroke(4, Color.DKGRAY)

        pickerCircle.background = drawable

        pickerCircle.setOnClickListener {

            Toast.makeText(
                requireContext(),
                "Advanced Color Picker Coming Soon",
                android.widget.Toast.LENGTH_SHORT
            ).show()

        }

        colorPalette.addView(pickerCircle)
    }

    private fun updateCircleDrawable(circle: View, color: Int, isSelected: Boolean) {
        val drawable = GradientDrawable()
        drawable.shape = GradientDrawable.OVAL
        drawable.setColor(color)
        if (isSelected) {
            drawable.setStroke(6, Color.GRAY)
        }
        circle.background = drawable
    }

    private fun refreshCircles() {
        for ((circle, color) in colorCircles) {
            updateCircleDrawable(circle, color, color == selectedColor)
        }
    }

    private fun setupSizeSlider(view: View) {
        val seekBarSize = view.findViewById<SeekBar>(R.id.seekBarSize)
        seekBarSize.progress = currentSize.toInt()

        seekBarSize.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                listener.onSizeChanged(progress.toFloat())
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun setupOpacitySlider(view: View) {
        val seekBarOpacity = view.findViewById<SeekBar>(R.id.seekBarOpacity)
        seekBarOpacity.progress = currentOpacity

        seekBarOpacity.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                listener.onOpacityChanged(progress)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }
}