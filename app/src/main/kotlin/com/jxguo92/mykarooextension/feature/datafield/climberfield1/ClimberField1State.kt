package com.jxguo92.mykarooextension.feature.datafield.climberfield1

import com.jxguo92.mykarooextension.core.ui.DisplayTriplet
import java.util.Locale
import kotlin.math.roundToInt

enum class MeasurementSystem { METRIC, IMPERIAL }

data class ClimberField1State(
    val distanceMeters: Double? = null,
    val percentFtp: Double? = null,
    val elevationMeters: Double? = null,
    val distanceUnit: MeasurementSystem? = null,
    val elevationUnit: MeasurementSystem? = null,
    val rideActive: Boolean? = null,
) {
    fun onClimb(distance: Double?, elevation: Double?): ClimberField1State = if (rideActive == false) this else copy(
        distanceMeters = distance.validFinite(),
        elevationMeters = elevation.validFinite(),
    )

    fun onFtp(value: Double?): ClimberField1State = if (rideActive == false) this else copy(
        percentFtp = value.validFinite(),
    )

    fun onUnits(distance: MeasurementSystem, elevation: MeasurementSystem): ClimberField1State = copy(
        distanceUnit = distance,
        elevationUnit = elevation,
    )

    fun onRide(active: Boolean): ClimberField1State = if (active) copy(rideActive = true) else copy(
        distanceMeters = null, percentFtp = null, elevationMeters = null, rideActive = false,
    )

    fun display(): DisplayTriplet = DisplayTriplet(
        left = ClimberField1Formatter.formatDistance(distanceMeters, distanceUnit),
        center = ClimberField1Formatter.formatPercent(percentFtp),
        right = ClimberField1Formatter.formatElevation(elevationMeters, elevationUnit),
    )
}

private fun Double?.validFinite(): Double? = this?.takeIf { it.isFinite() }

object ClimberField1Formatter {
    fun formatPercent(value: Double?): String = value?.takeIf { it.isFinite() && it >= 0 }
        ?.let { "${it.roundToInt()}%" } ?: "--"

    fun formatDistance(meters: Double?, unit: MeasurementSystem?): String {
        if (meters == null || !meters.isFinite() || unit == null) return "--"
        val converted = if (unit == MeasurementSystem.IMPERIAL) meters / 1609.344 else meters / 1000
        val small = if (unit == MeasurementSystem.IMPERIAL) meters * 3.280839895 else meters
        return if (kotlin.math.abs(converted) < 1) {
            "${roundedWithSign(small)} ${if (unit == MeasurementSystem.IMPERIAL) "ft" else "m"}"
        } else {
            String.format(Locale.US, "%.1f %s", converted, if (unit == MeasurementSystem.IMPERIAL) "mi" else "km")
        }
    }

    fun formatElevation(meters: Double?, unit: MeasurementSystem?): String =
        if (meters == null || !meters.isFinite() || unit == null) "--" else {
            val converted = if (unit == MeasurementSystem.IMPERIAL) meters * 3.280839895 else meters
            "${roundedWithSign(converted)} ${if (unit == MeasurementSystem.IMPERIAL) "ft" else "m"}"
        }

    private fun roundedWithSign(value: Double): String = value.roundToInt().let {
        if (value < 0 && it == 0) "-0" else it.toString()
    }
}
