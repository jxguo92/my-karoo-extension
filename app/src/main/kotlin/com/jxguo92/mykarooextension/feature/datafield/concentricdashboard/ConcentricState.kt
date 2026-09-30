package com.jxguo92.mykarooextension.feature.datafield.concentricdashboard

import java.util.Locale

/** Values at this boundary are metric: km/h, km, watts, rpm, bpm and percent. */
enum class Metric { POWER, CADENCE, HEART_RATE, SPEED, DISTANCE, GRADE, AVG_SPEED, AVG_HR, AVG_POWER, NORMALIZED_POWER }

data class ConcentricState(
    val acceptingReadings: Boolean = true,
    val readings: Map<Metric, Double> = emptyMap(),
    val gear: String = "--",
    val heading: Double? = null,
    val lastValidSpeed: Double? = null,
    val ftp: Double? = null,
    val maximumHr: Double? = null,
    val restingHr: Double? = null,
) {
    fun onValue(metric: Metric, value: Double?): ConcentricState {
        if (!acceptingReadings) return this
        val valid = when (metric) {
            Metric.GRADE -> value?.takeIf { it.isFinite() }
            Metric.HEART_RATE, Metric.AVG_HR -> ConcentricCalculator.positive(value)
            else -> ConcentricCalculator.nonNegative(value)
        }
        return copy(
            readings = if (valid == null) readings - metric else readings + (metric to valid),
            lastValidSpeed = if (metric == Metric.SPEED && valid != null) valid else lastValidSpeed,
        )
    }

    fun onGear(index: Double?, total: Double?): ConcentricState {
        if (!acceptingReadings) return this
        fun integer(value: Double?) = value?.takeIf { it.isFinite() && it > 0 && it <= Int.MAX_VALUE && it % 1 == 0.0 }?.toInt()
        val current = integer(index)
        val count = integer(total)
        return copy(gear = when {
            current == null || (count != null && current > count) -> "--"
            count == null -> "$current"
            else -> "$current/$count"
        })
    }

    fun onLocation(bearing: Double?, sampleSpeedKmh: Double?): ConcentricState =
        if (!acceptingReadings) this else copy(heading = ConcentricCalculator.heading(
            heading, bearing, ConcentricCalculator.nonNegative(sampleSpeedKmh) ?: lastValidSpeed,
        ))

    fun onRide(active: Boolean): ConcentricState = if (active) copy(acceptingReadings = true)
        else copy(acceptingReadings = false, readings = emptyMap(), gear = "--", heading = null, lastValidSpeed = null)

    fun display(wind: WindIndicator? = null): ConcentricDisplay {
        fun integer(metric: Metric) = readings[metric]?.let { String.format(Locale.US, "%.0f", it) } ?: "--"
        fun decimal(metric: Metric) = readings[metric]?.let {
            String.format(Locale.US, if (metric == Metric.GRADE && it > 0) "+%.1f" else "%.1f", it)
        } ?: "--"
        return ConcentricDisplay(
            power = integer(Metric.POWER), speed = decimal(Metric.SPEED), heartRate = integer(Metric.HEART_RATE),
            gear = gear, distance = decimal(Metric.DISTANCE), grade = decimal(Metric.GRADE),
            averageSpeed = decimal(Metric.AVG_SPEED), averageHr = integer(Metric.AVG_HR),
            averagePower = integer(Metric.AVG_POWER), normalizedPower = integer(Metric.NORMALIZED_POWER),
            powerPosition = ConcentricCalculator.power(readings[Metric.POWER], ftp),
            heartZone = ConcentricCalculator.heartZone(readings[Metric.HEART_RATE], maximumHr, restingHr),
            cadenceFraction = ConcentricCalculator.cadenceFraction(readings[Metric.CADENCE]),
            cadenceColor = readings[Metric.CADENCE]?.let(ConcentricCalculator::cadenceColor) ?: 0,
            northAngle = heading?.let { ConcentricCalculator.angle(-it) },
            wind = if (acceptingReadings && heading != null) wind else null,
        )
    }
}

data class ConcentricDisplay(
    val power: String = "--", val speed: String = "--", val heartRate: String = "--",
    val gear: String = "--", val distance: String = "--", val grade: String = "--",
    val averageSpeed: String = "--", val averageHr: String = "--",
    val averagePower: String = "--", val normalizedPower: String = "--",
    val powerPosition: PowerPosition? = null, val heartZone: Int? = null,
    val cadenceFraction: Double? = null, val cadenceColor: Int = 0,
    val northAngle: Int? = null, val wind: WindIndicator? = null,
)
