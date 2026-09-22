package com.jxguo92.mykarooextension.feature.datafield.climberfield2

import com.jxguo92.mykarooextension.core.karoo.RearCogTeethReading
import com.jxguo92.mykarooextension.core.ui.DisplayTriplet
import kotlin.math.roundToInt

data class ClimberField2State(
    val heartRate: Double? = null,
    val cadence: Double? = null,
    val rearCog: RearCogTeethReading? = null,
    val rideActive: Boolean? = null,
) {
    fun onHeartRate(value: Double?): ClimberField2State = if (rideActive == false) this else copy(heartRate = value.validFinite())
    fun onCadence(value: Double?): ClimberField2State = if (rideActive == false) this else copy(cadence = value.validFinite())
    fun onGear(value: RearCogTeethReading?): ClimberField2State = if (rideActive == false) this else copy(rearCog = value)

    fun onRide(active: Boolean): ClimberField2State = if (active) copy(rideActive = true) else copy(
        heartRate = null, cadence = null, rearCog = null, rideActive = false,
    )

    fun display(): DisplayTriplet = DisplayTriplet(
        left = ClimberField2Formatter.formatHeartRate(heartRate),
        center = ClimberField2Formatter.formatCadence(cadence),
        right = rearCog?.displayText ?: "--",
    )
}

private fun Double?.validFinite(): Double? = this?.takeIf { it.isFinite() }

object ClimberField2Formatter {
    fun formatHeartRate(value: Double?): String = value?.takeIf { it.isFinite() && it > 0 }
        ?.let { "${it.roundToInt()} bpm" } ?: "--"

    fun formatCadence(value: Double?): String = value?.takeIf { it.isFinite() && it >= 0 }
        ?.let { "${it.roundToInt()} rpm" } ?: "--"
}
