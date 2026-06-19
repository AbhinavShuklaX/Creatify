package com.ab.creatify

import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.Path

sealed class DrawingAction {
    data class StrokeAction(
        val path: Path,
        val paint: Paint
    ) : DrawingAction()

    data class FillAction(
        val bitmap: Bitmap
    ) : DrawingAction()
}