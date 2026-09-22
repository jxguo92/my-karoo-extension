package com.jxguo92.mykarooextension.feature.datafield.climberfield2

import com.jxguo92.mykarooextension.core.karoo.RearCogTeethReading
import org.junit.Assert.assertEquals
import org.junit.Test

class ClimberField2StateTest {
    @Test fun `heart rate zero is unavailable but zero cadence is valid`() {
        assertEquals("--", ClimberField2Formatter.formatHeartRate(0.0))
        assertEquals("--", ClimberField2Formatter.formatHeartRate(-1.0))
        assertEquals("--", ClimberField2Formatter.formatHeartRate(Double.NaN))
        assertEquals("156 bpm", ClimberField2Formatter.formatHeartRate(155.5))
        assertEquals("0 rpm", ClimberField2Formatter.formatCadence(0.0))
        assertEquals("--", ClimberField2Formatter.formatCadence(-1.0))
    }

    @Test fun `gear loss clears only gear and paused retains sensor data`() {
        val state = ClimberField2State()
            .onHeartRate(156.0)
            .onCadence(92.0)
            .onGear(RearCogTeethReading(21, true))
        assertEquals("≈21T", state.display().right)
        val lost = state.onRide(true).onGear(null).display()
        assertEquals("156 bpm", lost.left)
        assertEquals("92 rpm", lost.center)
        assertEquals("--", lost.right)
        assertEquals("--", state.onRide(false).onCadence(92.0).display().center)
    }

}
