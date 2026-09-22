package com.jxguo92.mykarooextension.feature.datafield.climberfield1

import org.junit.Assert.assertEquals
import org.junit.Test

class ClimberField1StateTest {
    private val metric = MeasurementSystem.METRIC
    private val imperial = MeasurementSystem.IMPERIAL

    @Test fun `distance and elevation follow separate unit preferences and thresholds`() {
        assertEquals("999 m", ClimberField1Formatter.formatDistance(999.0, metric))
        assertEquals("1.0 km", ClimberField1Formatter.formatDistance(1000.0, metric))
        assertEquals("3280 ft", ClimberField1Formatter.formatDistance(999.7, imperial))
        assertEquals("1.0 mi", ClimberField1Formatter.formatDistance(1609.344, imperial))
        assertEquals("-1.0 km", ClimberField1Formatter.formatDistance(-1000.0, metric))
        assertEquals("-33 ft", ClimberField1Formatter.formatElevation(-10.0, imperial))
        assertEquals("-0 m", ClimberField1Formatter.formatDistance(-0.1, metric))
        assertEquals("-0 ft", ClimberField1Formatter.formatElevation(-0.1, imperial))
        assertEquals("186 m", ClimberField1Formatter.formatElevation(185.6, metric))
    }

    @Test fun `missing non finite and negative FTP values are unavailable while zero is valid`() {
        assertEquals("--", ClimberField1Formatter.formatDistance(20.0, null))
        assertEquals("--", ClimberField1Formatter.formatElevation(Double.NaN, metric))
        assertEquals("--", ClimberField1Formatter.formatPercent(Double.POSITIVE_INFINITY))
        assertEquals("--", ClimberField1Formatter.formatPercent(-1.0))
        assertEquals("0%", ClimberField1Formatter.formatPercent(0.0))
        assertEquals("88%", ClimberField1Formatter.formatPercent(87.5))
    }

    @Test fun `climb fields fail independently and FTP survives missing climb`() {
        val initial = ClimberField1State(distanceUnit = metric, elevationUnit = metric)
            .onFtp(87.0)
        val partial = initial.onClimb(2300.0, null)
        assertEquals("2.3 km", partial.display().left)
        assertEquals("87%", partial.display().center)
        assertEquals("--", partial.display().right)
        assertEquals("--", partial.onClimb(null, null).display().left)
        assertEquals("87%", partial.onClimb(null, null).display().center)
    }

    @Test fun `idle clears and blocks readings until recording resumes while paused preserves them`() {
        val reading = ClimberField1State(distanceUnit = metric, elevationUnit = metric)
            .onFtp(87.0)
        assertEquals("87%", reading.onRide(true).display().center)
        val idle = reading.onRide(false)
        assertEquals("--", idle.onFtp(50.0).display().center)
        assertEquals("50%", idle.onRide(true)
            .onFtp(50.0).display().center)
    }

}
