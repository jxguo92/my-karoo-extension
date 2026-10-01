package com.jxguo92.mykarooextension.core.ui

import android.graphics.*
import com.jxguo92.mykarooextension.core.dashboard.WindIndicator
import kotlin.math.cos
import kotlin.math.sin

/** Shared wind and true-north symbols; callers own position and theme. */
class DashboardDirections(private val canvas: Canvas) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    fun wind(x: Float, y: Float, wind: WindIndicator, color: Int) {
        val angle = wind.angle - 90f
        val tail = point(x, y, 17f, angle + 180)
        val neck = point(x, y, 4f, angle)
        val tip = point(x, y, 19f, angle)
        paint.color = color
        paint.strokeWidth = 4.5f
        canvas.drawLine(tail.x, tail.y, neck.x, neck.y, paint)
        polygon(listOf(tip, point(neck.x, neck.y, 9f, angle + 90), point(neck.x, neck.y, 9f, angle - 90)), color)
    }

    fun compass(x: Float, y: Float, north: Int, red: Int, grey: Int, ink: Int) {
        val angle = north - 90f
        val a = point(x, y, 7f, angle + 90)
        val b = point(x, y, 7f, angle - 90)
        polygon(listOf(point(x, y, 20f, angle), a, b), red)
        polygon(listOf(point(x, y, 20f, angle + 180), a, b), grey)
        val label = point(x, y, 32f, angle)
        paint.color = ink
        paint.typeface = Typeface.create("sans-serif", Typeface.BOLD)
        paint.textAlign = Paint.Align.CENTER
        paint.textSize = 13f
        canvas.drawText("N", label.x, label.y + 4f, paint)
    }

    private fun point(x: Float, y: Float, radius: Float, degrees: Float): PointF {
        val radians = Math.toRadians(degrees.toDouble())
        return PointF(x + radius * cos(radians).toFloat(), y + radius * sin(radians).toFloat())
    }

    private fun polygon(points: List<PointF>, color: Int) {
        val path = Path().apply {
            moveTo(points.first().x, points.first().y)
            points.drop(1).forEach { lineTo(it.x, it.y) }
            close()
        }
        paint.style = Paint.Style.FILL
        paint.color = color
        canvas.drawPath(path, paint)
    }
}
