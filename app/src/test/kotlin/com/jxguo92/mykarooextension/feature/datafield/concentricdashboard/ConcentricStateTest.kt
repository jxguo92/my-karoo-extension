package com.jxguo92.mykarooextension.feature.datafield.concentricdashboard

import org.junit.Assert.*
import org.junit.Test

class ConcentricStateTest {
    @Test fun `idle clears readings and heading and ignores readings until resumed`() {
        val active = ConcentricState().onValue(Metric.POWER, 245.0).onValue(Metric.SPEED, 5.0)
            .onLocation(315.0, null)
        assertEquals("245", active.display().power)
        val idle = active.onRide(false).onValue(Metric.POWER, 200.0).onLocation(90.0, 20.0)
        assertEquals("--", idle.display().power)
        assertNull(idle.heading)
        val resumed = idle.onRide(true).onValue(Metric.POWER, 0.0)
        assertEquals("0", resumed.display().power)
        assertNull(resumed.heading)
    }

    @Test fun `heading prefers sample speed then last valid speed and resets at idle`() {
        val state = ConcentricState().onValue(Metric.SPEED, 20.0).onValue(Metric.SPEED, null)
        val moving = state.onLocation(355.0, null)
        assertEquals(355.0, moving.heading!!, 0.0)
        assertEquals(10, moving.display().northAngle)
        assertEquals(355.0, moving.onLocation(90.0, 4.99).heading!!, 0.0)
        assertEquals(90.0, moving.onLocation(90.0, 5.0).heading!!, 0.0)
        assertNull(moving.onRide(false).onRide(true).onLocation(90.0, null).heading)
    }

    @Test fun `unavailable readings degrade independently and invalid profile preserves real values`() {
        val state = ConcentricPreview.state.onValue(Metric.CADENCE, Double.NaN)
            .onValue(Metric.HEART_RATE, 0.0).onValue(Metric.GRADE, -4.8)
            .copy(ftp = -1.0)
        val d = state.display()
        assertEquals("245", d.power)
        assertEquals("28.6", d.speed)
        assertEquals("--", d.heartRate)
        assertEquals("-4.8", d.grade)
        assertNull(d.cadenceFraction)
        assertNull(d.powerPosition)
        assertEquals("+4.8", ConcentricPreview.display.grade)
        assertEquals("0.0", state.onValue(Metric.SPEED, 0.0).display().speed)
        assertEquals("6", state.onGear(6.0, null).display().gear)
        assertEquals("6/13", state.onGear(6.0, 13.0).display().gear)
        assertEquals("--", state.onGear(14.0, 13.0).display().gear)
        assertEquals("--", state.onGear(1.5, 13.0).display().gear)
        assertEquals("123456789", state.onValue(Metric.POWER, 123456789.0).display().power)
        assertEquals("156", ConcentricPreview.state.onRide(true).display().heartRate)
    }
}
