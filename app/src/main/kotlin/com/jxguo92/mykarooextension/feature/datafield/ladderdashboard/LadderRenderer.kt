package com.jxguo92.mykarooextension.feature.datafield.ladderdashboard

import android.content.Context
import android.graphics.*
import android.widget.RemoteViews
import com.jxguo92.mykarooextension.R
import com.jxguo92.mykarooextension.core.ui.DashboardDirections
import com.jxguo92.mykarooextension.core.ui.DashboardPalette
import kotlin.math.*

/** Owned by one view, repainted only after synchronous updateView returns. */
class LadderRenderer(size: Pair<Int, Int>) {
    private val width = if (size.first > 0 && size.second > 0) size.first else 480
    private val height = if (size.first > 0 && size.second > 0) size.second else 638
    private val pixels by lazy { Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888) }

    fun render(context: Context, frame: LadderFrame?): RemoteViews =
        RemoteViews(context.packageName, R.layout.ladder_dashboard_view).apply {
            if (frame != null) setImageViewBitmap(R.id.ladder_dashboard_image, bitmap(frame))
        }

    fun bitmap(frame: LadderFrame): Bitmap {
        val canvas = Canvas(pixels)
        canvas.drawColor(Color.parseColor("#111b22"))
        val scale = min(width / 480f, height / 638f)
        canvas.translate((width - 480 * scale) / 2, (height - 638 * scale) / 2)
        canvas.scale(scale, scale)
        LadderPainter(canvas).draw(frame)
        return pixels
    }
}

/** Prototype D coordinates after upper -19px / lower -16px and field origin -81px. */
private class LadderPainter(private val canvas: Canvas) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val font = Typeface.create("sans-serif", Typeface.BOLD)
    private val palette = DashboardPalette(true)
    private val zones = palette.zones
    private val heartColors = listOf(0, 1, 2, 3, 5, 6)
    private val winds = palette.winds
    private val ink = palette.ink
    private val muted = palette.muted
    private val track = Color.parseColor("#435761")
    private val tick = Color.parseColor("#50606a")

    fun draw(frame: LadderFrame) {
        val d = frame.values
        for (i in 0..11) {
            paint.style = Paint.Style.FILL
            paint.color = palette.cadence[if (i < 6) 0 else if (i < 10) 1 else 2]
            paint.alpha = if (i < frame.cadenceLights) 255 else 46
            val x = 51 + i * (354f / 11)
            canvas.drawRoundRect(RectF(x, 19f, x + 24, 54f), 3f, 3f, paint)
        }
        paint.alpha = 255
        rail(true, frame.speedFraction, Color.parseColor("#7bdbce"))
        rail(false, frame.heartPosition?.fraction, frame.heartPosition?.zone?.let { zones[heartColors[it]] } ?: ink)
        arc(270f, track, 16f)
        d.powerPosition?.takeIf { it.fraction > 0 }?.let { arc((270 * it.fraction).toFloat(), zones[it.zone], 9.6f) }
        for (i in 0..7) {
            val angle = -225 + i * 270f / 7
            line(point(127f, angle), point(137f, angle), Color.parseColor("#c6d7d9"), 2f)
        }
        val directions = DashboardDirections(canvas)
        directions.wind(125f, 120f, d.wind, winds, ink)
        d.northAngle?.let { directions.compass(355f, 120f, it, Color.parseColor("#ef5b5b"), Color.parseColor("#7f929a"), ink) }
        text(if (d.grade == "--") "--" else "${d.grade}%", 240f, 132f, 30.25f, ink, 150f)
        text(d.gear, 240f, 230f, 30.25f, Color.parseColor("#9db9c0"), 180f)
        text(d.power, 240f, 292f, 70.4f, Color.parseColor("#f5faf8"), 208f, middle = true)
        text("W", 240f, 346f, 16.5f, Color.parseColor("#9ab2ba"), 100f)
        text(d.speed, 114f, 438f, 43.56f, ink, 160f)
        text("km/h", 114f, 457f, 13.2f, muted, 100f)
        text(d.heartRate, 366f, 438f, 43.56f, ink, 150f)
        text("bpm", 366f, 457f, 13.2f, muted, 100f)
        field("AVG PWR", d.averagePower, "W", 89f, 509f, 120f)
        field("AVG SPD", d.averageSpeed, "km/h", 240f, 509f, 120f)
        field("AVG HR", d.averageHr, "bpm", 391f, 509f, 110f)
        field("NP", d.normalizedPower, "W", 166f, 580f, 120f)
        field("DIST", d.distance, "km", 315f, 580f, 130f)
    }

    private fun rail(left: Boolean, fraction: Double?, color: Int) {
        val sign = if (left) -1 else 1
        val center = if (left) 68f else 412f
        fun x(y: Float) = center + sign * (y - 102) * .075f
        fun y(f: Double) = (427 - 325 * f).toFloat()
        for (offset in listOf(-18f, 18f)) {
            line(PointF(center + offset, 87f), PointF(x(427f) + offset, 441f), tick, 1f)
        }
        line(PointF(x(427f), 427f), PointF(center, 102f), track, 20f, round = true)
        fraction?.takeIf { it > 0 }?.let {
            val end = y(it)
            line(PointF(x(427f), 427f), PointF(x(end), end), color, 12f, round = true)
        }
        for (i in 1..5) {
            val yy = y(i / 6.0)
            line(PointF(x(yy) - 4.5f, yy), PointF(x(yy) + 4.5f, yy), tick, 3f, round = true)
        }
    }

    private fun point(radius: Float, angle: Float): PointF {
        val radians = Math.toRadians(angle.toDouble())
        return PointF(240 + radius * cos(radians).toFloat(), 286 + radius * sin(radians).toFloat())
    }

    private fun arc(sweep: Float, color: Int, width: Float) {
        paint.color = color
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = width
        paint.strokeCap = Paint.Cap.ROUND
        canvas.drawArc(RectF(125f, 171f, 355f, 401f), -225f, sweep, false, paint)
    }

    private fun line(a: PointF, b: PointF, color: Int, width: Float, round: Boolean = false) {
        paint.color = color
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = width
        paint.strokeCap = if (round) Paint.Cap.ROUND else Paint.Cap.BUTT
        canvas.drawLine(a.x, a.y, b.x, b.y, paint)
    }

    private fun text(value: String, x: Float, y: Float, size: Float, color: Int, maxWidth: Float, middle: Boolean = false) {
        paint.style = Paint.Style.FILL
        paint.typeface = font
        paint.color = color
        paint.textAlign = Paint.Align.CENTER
        paint.textSize = size
        val measured = paint.measureText(value)
        if (measured > maxWidth) paint.textSize = size * maxWidth / measured
        val baseline = if (middle) y - (paint.fontMetrics.ascent + paint.fontMetrics.descent) / 2 else y
        canvas.drawText(value, x, baseline, paint)
    }

    private fun field(label: String, value: String, unit: String, x: Float, y: Float, width: Float) {
        text(label, x, y, 14.64f, muted, width)
        paint.typeface = font
        paint.textSize = 13.31f
        val unitWidth = paint.measureText(unit)
        val available = min(width, 2 * (480 - x - 4 - unitWidth))
        text(value, x, y + 27, 33.28f, ink, available)
        val valueWidth = paint.measureText(value)
        paint.textSize = 13.31f
        paint.textAlign = Paint.Align.LEFT
        paint.color = muted
        canvas.drawText(unit, x + valueWidth / 2 + 4, y + 27, paint)
    }
}
