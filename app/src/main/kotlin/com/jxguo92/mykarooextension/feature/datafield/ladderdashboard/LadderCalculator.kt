package com.jxguo92.mykarooextension.feature.datafield.ladderdashboard

import kotlin.math.floor

data class LadderPosition(val zone: Int, val fraction: Double)

object LadderCalculator {
    fun cadenceLights(rpm: Double?): Int = when {
        rpm == null || !rpm.isFinite() || rpm < 51 -> 0
        rpm >= 110 -> 12
        else -> (floor((rpm - 51) / 5).toInt() + 1).coerceIn(1, 12)
    }

    fun speed(kmh: Double?): Double? = kmh?.takeIf { it.isFinite() && it >= 0 }?.let { (it / 60).coerceIn(0.0, 1.0) }

    fun heart(bpm: Double?, maximum: Double?, resting: Double?): LadderPosition? {
        if (bpm == null || !bpm.isFinite() || bpm <= 0 || maximum == null || !maximum.isFinite() ||
            resting == null || !resting.isFinite() || resting < 0 || maximum <= resting) return null
        val ratio = ((bpm - resting) / (maximum - resting)).coerceIn(.41, 1.0)
        val bounds = listOf(.41, .5, .6, .7, .8, .9, 1.0)
        val zone = (0..5).first { ratio <= bounds[it + 1] }
        return LadderPosition(zone, (zone + (ratio - bounds[zone]) / (bounds[zone + 1] - bounds[zone])) / 6)
    }
}
