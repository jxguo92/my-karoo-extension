package com.jxguo92.mykarooextension.feature.datafield.concentricdashboard

import android.content.Context
import android.graphics.*
import android.widget.RemoteViews
import com.jxguo92.mykarooextension.R
import kotlin.math.*

object ConcentricView {
    fun render(context: Context, size: Pair<Int, Int>, frame: ConcentricFrame): RemoteViews =
        RemoteViews(context.packageName, R.layout.concentric_dashboard_view).apply {
            setImageViewBitmap(R.id.concentric_dashboard_image, bitmap(size, frame))
        }

    fun bitmap(size: Pair<Int, Int>, frame: ConcentricFrame): Bitmap {
        val validSize = if (size.first > 0 && size.second > 0) size else (480 to 638)
        val scale = min(1f, min(validSize.first / 480f, validSize.second / 638f))
        val width = max(1, (480 * scale).roundToInt())
        val height = max(1, (638 * scale).roundToInt())
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        if (frame.display == null) return bitmap
        val canvas = Canvas(bitmap)
        canvas.scale(width / 480f, height / 638f)
        DashboardPainter(canvas, Palette(frame.night)).draw(frame.display)
        return bitmap
    }
}

private class Palette(val night: Boolean) {
    fun color(dark: String, light: String) = Color.parseColor(if (night) dark else light)
    val background = color("#111b22", "#e8edef")
    val ink = color("#eef5f5", "#10181c")
    val muted = color("#94aeb6", "#5b6f78")
    val gear = color("#d7e6e8", "#3a4a52")
    val power = color("#f7fbfa", "#0b1216")
    val zones = (if (night) listOf("#55c99c", "#5cbce5", "#4280ea", "#ebcb52", "#ff8a5c", "#ed547a", "#a774f6")
        else listOf("#16a076", "#1790c8", "#2c63d6", "#c29100", "#e45f1f", "#d4305c", "#8450e0")).map(Color::parseColor)
    val cadence = (if (night) listOf("#edf2ed", "#69d697", "#f36b65") else listOf("#2c363d", "#1c9a55", "#d93a36")).map(Color::parseColor)
    val winds = (if (night) listOf("#edf2ed", "#4fd1b5", "#ebcb52", "#f36b65") else listOf("#5b6f78", "#0f9a83", "#a67c00", "#d93a36")).map(Color::parseColor)
    val idle = color("#27313a", "#d5dce0")
    val tick = color("#7f8e96", "#6c7b83")
    val activeTick = color("#f4f8f8", "#10181c")
    val track = color("#3b4249", "#d2d9dd")
    val cadenceTick = color("#eef3f4", "#44525a")
    val needle = color("#ffffff", "#0b1216")
}

/** Coordinates are the prototype's middle field, translated upward by its 81px header. */
private class DashboardPainter(private val canvas: Canvas, private val theme: Palette) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val condensed = Typeface.create("sans-serif-condensed", Typeface.BOLD)
    private val sans = Typeface.create("sans-serif", Typeface.BOLD)
    private val cx = 240f
    private val cy = 241f

    fun draw(d: ConcentricDisplay) {
        canvas.drawColor(theme.background)
        paint.style = Paint.Style.FILL
        paint.shader = RadialGradient(cx, cy, 226f,
            intArrayOf(theme.color("#000000", "#ffffff"), theme.color("#030607", "#fbfcfd"), theme.background),
            floatArrayOf(0f, .78f, 1f), Shader.TileMode.CLAMP)
        canvas.drawCircle(cx, cy, 226f, paint)
        paint.shader = null
        val active = d.powerPosition?.zone
        for (zone in 0..6) {
            val color = if (active == zone) theme.zones[zone] else theme.idle
            paint.shader = RadialGradient(cx, cy, 207f,
                intArrayOf(withAlpha(color, 128), color), floatArrayOf(191f / 207, 1f), Shader.TileMode.CLAMP)
            arc(199f, 130f + zone * 40, 40f, color, 16f, shader = true)
            paint.shader = null
            glow(active == zone, theme.zones[zone]) {
                arc(205f, 130f + zone * 40, 40f, theme.zones[zone], 4f)
            }
        }
        for (tick in 0..28) {
            val major = tick % 4 == 0
            val lit = active != null && tick >= active * 4 && tick <= (active + 1) * 4
            radial(if (major) 191f else 198.2f, 207f, tick / 28f, if (lit) theme.activeTick else theme.tick, if (major) 2.5f else 1.5f)
        }
        arc(134f, 130f, 280f, theme.track, 10f, round = true)
        d.cadenceFraction?.takeIf { it > 0 }?.let { fraction ->
            glow(true, theme.cadence[d.cadenceColor]) {
                arc(134f, 130f, (280 * fraction).toFloat(), theme.cadence[d.cadenceColor], 10f, round = true)
            }
        }
        for (i in 0..6) radial(120f, 126f, i / 6f, theme.cadenceTick, 2f)
        d.powerPosition?.let { position -> glow(true, theme.needle) {
            radial(146f, 205.5f, position.fraction.toFloat(), theme.needle, 3f)
        } }
        d.wind?.let { wind -> wind(wind) }
        d.northAngle?.let(::compass)
        text(d.gear, 240f, 177f, 30f, theme.gear, 190f, condensed)
        text(d.power, 240f, 283f, 96f, theme.power, 220f, condensed)
        stacked(d.speed, "km/h", 240f, 389f, 56f, theme.ink, 210f, condensed)
        field("DIST", d.distance, "km", 100f, 477f, 40f, 15f, 184f, 519f)
        field("GRADE", d.grade, "%", 380f, 477f, 40f, 15f, 184f, 519f)
        val hrColor = d.heartZone?.let { theme.zones[listOf(0, 1, 2, 3, 5, 6)[it]] } ?: theme.ink
        stacked(d.heartRate, "bpm", 240f, 481f, 56f, hrColor, 104f)
        field("AVG SPD", d.averageSpeed, "km/h", 64f, 565f, 33f, 13.3f, 116f)
        field("AVG HR", d.averageHr, "bpm", 182f, 565f, 33f, 13.3f, 112f)
        field("AVG PWR", d.averagePower, "W", 298f, 565f, 33f, 13.3f, 112f)
        field("NOR PWR", d.normalizedPower, "W", 416f, 565f, 33f, 13.3f, 116f)
    }

    private fun withAlpha(color: Int, alpha: Int) = (color and 0x00ffffff) or (alpha shl 24)
    private fun point(x: Float, y: Float, radius: Float, degrees: Float): PointF {
        val radians = Math.toRadians(degrees.toDouble())
        return PointF(x + radius * cos(radians).toFloat(), y + radius * sin(radians).toFloat())
    }
    private fun line(a: PointF, b: PointF, color: Int, width: Float) {
        paint.color = color; paint.strokeWidth = width; paint.style = Paint.Style.STROKE
        paint.strokeCap = Paint.Cap.BUTT
        canvas.drawLine(a.x, a.y, b.x, b.y, paint)
    }
    private fun radial(from: Float, to: Float, fraction: Float, color: Int, width: Float) {
        val angle = 130 + 280 * fraction
        line(point(cx, cy, from, angle), point(cx, cy, to, angle), color, width)
    }
    private fun arc(radius: Float, start: Float, sweep: Float, color: Int, width: Float, round: Boolean = false, shader: Boolean = false) {
        if (!shader) paint.shader = null
        paint.color = color; paint.style = Paint.Style.STROKE; paint.strokeWidth = width
        paint.strokeCap = if (round) Paint.Cap.ROUND else Paint.Cap.BUTT
        canvas.drawArc(RectF(cx - radius, cy - radius, cx + radius, cy + radius), start, sweep, false, paint)
    }
    private fun glow(enabled: Boolean, color: Int, draw: () -> Unit) {
        if (enabled) paint.setShadowLayer(if (theme.night) 3f else 1.2f, 0f, if (theme.night) 0f else 1f,
            if (theme.night) color else 0x590b1216)
        draw()
        paint.clearShadowLayer()
    }
    private fun text(value: String, x: Float, y: Float, size: Float, color: Int, maxWidth: Float, font: Typeface = sans) {
        paint.style = Paint.Style.FILL; paint.shader = null; paint.color = color
        paint.typeface = font; paint.textAlign = Paint.Align.CENTER; paint.textSize = size
        val width = paint.measureText(value)
        if (width > maxWidth) paint.textSize = size * maxWidth / width
        canvas.drawText(value, x, y, paint)
    }
    private fun stacked(value: String, unit: String, x: Float, y: Float, size: Float, color: Int, width: Float, font: Typeface = sans) {
        text(value, x, y, size, color, width, font)
        text(unit, x, y + 21.6f, 21.8f, theme.muted, width, font)
    }
    private fun field(label: String, value: String, unit: String, x: Float, labelY: Float, size: Float, unitSize: Float, width: Float, valueY: Float = labelY + size + 2) {
        text(label, x, labelY, 14.6f, theme.muted, width)
        paint.typeface = sans; paint.textSize = size
        val rawValueWidth = paint.measureText(value)
        paint.textSize = unitSize
        val unitWidth = paint.measureText(unit)
        val valueWidth = min(rawValueWidth, width - unitWidth - 4)
        val left = x - (valueWidth + 4 + unitWidth) / 2
        text(value, left + valueWidth / 2, valueY, size, theme.ink, valueWidth)
        text(unit, left + valueWidth + 4 + unitWidth / 2, valueY, unitSize, theme.muted, unitWidth)
    }
    private fun polygon(points: List<PointF>, color: Int) {
        val path = Path().apply {
            moveTo(points.first().x, points.first().y)
            points.drop(1).forEach { lineTo(it.x, it.y) }
            close()
        }
        paint.style = Paint.Style.FILL; paint.color = color
        canvas.drawPath(path, paint)
    }
    private fun wind(wind: WindIndicator) {
        val angle = wind.angle - 90f
        val tail = point(46f, 47f, 17f, angle + 180)
        val neck = point(46f, 47f, 4f, angle)
        val tip = point(46f, 47f, 19f, angle)
        val color = theme.winds[wind.level]
        line(tail, neck, color, 4.5f)
        polygon(listOf(tip, point(neck.x, neck.y, 9f, angle + 90), point(neck.x, neck.y, 9f, angle - 90)), color)
    }
    private fun compass(north: Int) {
        val angle = north - 90f
        val a = point(430f, 51f, 7f, angle + 90)
        val b = point(430f, 51f, 7f, angle - 90)
        polygon(listOf(point(430f, 51f, 20f, angle), a, b), theme.color("#ef5b5b", "#d93434"))
        polygon(listOf(point(430f, 51f, 20f, angle + 180), a, b), theme.color("#7f929a", "#a3b0b6"))
        val label = point(430f, 51f, 32f, angle)
        text("N", label.x, label.y + 4f, 13f, theme.color("#dfe9ea", "#2f3d44"), 18f)
    }
}
