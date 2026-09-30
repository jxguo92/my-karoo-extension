package com.jxguo92.mykarooextension.feature.datafield.concentricdashboard

import kotlin.math.floor

/** Zero-based zone and equal-zone arc position. */
data class PowerPosition(val zone: Int, val fraction: Double)
data class WindIndicator(val angle: Int, val level: Int)

object ConcentricCalculator {
    fun power(watts: Double?, ftp: Double?): PowerPosition? {
        if (nonNegative(watts) == null || positive(ftp) == null) return null
        val ratio = (watts!! / ftp!!).coerceIn(.41, 2.0)
        val bounds = listOf(.41, .55, .75, .9, 1.05, 1.2, 1.5, 2.0)
        val zone = (0..6).first { ratio <= bounds[it + 1] }
        return PowerPosition(zone, (zone + (ratio - bounds[zone]) / (bounds[zone + 1] - bounds[zone])) / 7)
    }

    fun heartZone(bpm: Double?, maximum: Double?, resting: Double?): Int? {
        if (positive(bpm) == null || positive(maximum) == null || nonNegative(resting) == null || maximum!! <= resting!!) return null
        val ratio = (bpm!! - resting) / (maximum - resting)
        if (ratio < .41) return null
        return listOf(.5, .6, .7, .8, .9, 1.0).indexOfFirst { ratio <= it }.takeIf { it >= 0 } ?: 5
    }

    fun cadenceFraction(rpm: Double?): Double? = nonNegative(rpm)?.let { ((it - 50) / 60).coerceIn(0.0, 1.0) }
    fun cadenceColor(rpm: Double): Int = if (rpm <= 80) 0 else if (rpm <= 100) 1 else 2
    fun angle(degrees: Double): Int = ((floor(((degrees % 360 + 360) % 360) / 10 + .5).toInt() * 10) % 360)
    fun bearing(degrees: Double?): Double? = degrees?.takeIf { it.isFinite() && it in 0.0..360.0 }
    fun heading(previous: Double?, bearing: Double?, speedKmh: Double?): Double? =
        if (nonNegative(speedKmh) != null && speedKmh!! >= 5 && bearing(bearing) != null) bearing else previous

    fun wind(heading: Double?, from: Double?, speed: Double?): WindIndicator? {
        if (bearing(heading) == null || bearing(from) == null || nonNegative(speed) == null || speed!! < .3) return null
        val level = when { speed < 3.4 -> 0; speed < 5.5 -> 1; speed < 8 -> 2; else -> 3 }
        return WindIndicator(angle(from!! + 180 - heading!!), level)
    }

    fun nonNegative(value: Double?): Double? = value?.takeIf { it.isFinite() && it >= 0 }
    fun positive(value: Double?): Double? = value?.takeIf { it.isFinite() && it > 0 }
}
