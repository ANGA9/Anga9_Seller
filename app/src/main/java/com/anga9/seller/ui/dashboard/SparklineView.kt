package com.anga9.seller.ui.dashboard

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View

class SparklineView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 5f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        color = Color.parseColor("#1A6FD4")
    }

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val linePath = Path()
    private val fillPath = Path()

    private var dataPoints: List<Double> = emptyList()

    fun setData(points: List<Double>) {
        this.dataPoints = points
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0 || h <= 0) return

        if (dataPoints.isEmpty() || dataPoints.all { it == 0.0 }) {
            // Draw subtle flat baseline
            val baseLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = 3f
                color = Color.parseColor("#E5E7EB")
            }
            canvas.drawLine(10f, h * 0.75f, w - 10f, h * 0.75f, baseLinePaint)
            return
        }

        val padding = 12f
        val usableWidth = w - padding * 2
        val usableHeight = h - padding * 2

        val maxVal = dataPoints.maxOrNull()?.coerceAtLeast(1.0) ?: 1.0
        val minVal = (dataPoints.minOrNull() ?: 0.0).coerceAtMost(0.0)
        val range = (maxVal - minVal).coerceAtLeast(1.0)

        val stepX = usableWidth / (dataPoints.size - 1).coerceAtLeast(1)

        val coords = dataPoints.mapIndexed { index, value ->
            val x = padding + index * stepX
            val normalized = ((value - minVal) / range).toFloat()
            val y = h - padding - (normalized * usableHeight)
            Pair(x, y)
        }

        linePath.reset()
        fillPath.reset()

        if (coords.isNotEmpty()) {
            linePath.moveTo(coords[0].first, coords[0].second)
            fillPath.moveTo(coords[0].first, h)
            fillPath.lineTo(coords[0].first, coords[0].second)

            for (i in 1 until coords.size) {
                val prev = coords[i - 1]
                val curr = coords[i]
                val midX = (prev.first + curr.first) / 2f
                linePath.cubicTo(midX, prev.second, midX, curr.second, curr.first, curr.second)
                fillPath.cubicTo(midX, prev.second, midX, curr.second, curr.first, curr.second)
            }

            fillPath.lineTo(coords.last().first, h)
            fillPath.close()

            // Fill gradient
            fillPaint.shader = LinearGradient(
                0f, 0f, 0f, h,
                Color.parseColor("#331A6FD4"),
                Color.parseColor("#001A6FD4"),
                Shader.TileMode.CLAMP
            )

            canvas.drawPath(fillPath, fillPaint)
            canvas.drawPath(linePath, linePaint)
        }
    }
}
