package com.kabox.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.view.View
import java.util.Calendar
import java.util.Locale
import kotlin.math.max

class RingProgressView(
    context: Context,
    private val progressColor: Int,
    private val trackColor: Int
) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var progress = 0f
    private val bounds = RectF()

    fun setProgress(value: Float) {
        progress = value.coerceIn(0f, 1f)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val stroke = 13f * resources.displayMetrics.density
        val side = minOf(width, height).toFloat()
        val inset = stroke / 2f + 3f
        bounds.set((width - side) / 2f + inset, (height - side) / 2f + inset,
            (width + side) / 2f - inset, (height + side) / 2f - inset)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = stroke
        paint.strokeCap = Paint.Cap.ROUND
        paint.color = trackColor
        canvas.drawArc(bounds, 0f, 360f, false, paint)
        paint.color = progressColor
        canvas.drawArc(bounds, -90f, 360f * progress, false, paint)
    }
}

class WeeklyFocusChart(
    context: Context,
    private val barColor: Int,
    private val guideColor: Int,
    private val values: List<Int>
) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val labels = (6 downTo 0).map { offset ->
        Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -offset) }
            .getDisplayName(Calendar.DAY_OF_WEEK, Calendar.SHORT, Locale.getDefault())?.take(1) ?: "·"
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (width <= 0 || height <= 0) return
        val density = resources.displayMetrics.density
        val left = 8f * density
        val right = width - 8f * density
        val top = 8f * density
        val bottom = height - 22f * density
        val chartHeight = bottom - top
        val maxValue = max(60, (values.maxOrNull() ?: 0))
        val gap = 13f * density
        val slot = (right - left) / 7f
        val barWidth = minOf(19f * density, slot - gap)

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f * density
        paint.color = guideColor
        for (i in 0..2) {
            val y = top + chartHeight * i / 2f
            canvas.drawLine(left, y, right, y, paint)
        }

        values.take(7).forEachIndexed { index, value ->
            val xCenter = left + slot * index + slot / 2f
            val barHeight = chartHeight * (value.toFloat() / maxValue.toFloat()).coerceIn(0.03f, 1f)
            val rect = RectF(xCenter - barWidth / 2f, bottom - barHeight, xCenter + barWidth / 2f, bottom)
            paint.style = Paint.Style.FILL
            paint.color = if (index == 6) barColor else android.graphics.Color.argb(120, 18, 122, 82)
            canvas.drawRoundRect(rect, 6f * density, 6f * density, paint)

            paint.color = android.graphics.Color.rgb(112, 128, 119)
            paint.textSize = 9f * density
            paint.textAlign = Paint.Align.CENTER
            canvas.drawText(labels[index], xCenter, height - 5f * density, paint)
        }
    }
}
