package com.jxguo92.mykarooextension.feature.datafield.ladderdashboard

import com.jxguo92.mykarooextension.core.dashboard.*
import org.junit.Assert.*
import org.junit.Test

class LadderStateTest {
    @Test fun `missing values clear independently and zero readings remain visible`() {
        val zero = LadderPreview.state.onValue(Metric.POWER, 0.0).onValue(Metric.SPEED, 0.0)
            .onValue(Metric.CADENCE, 0.0).onValue(Metric.HEART_RATE, 0.0).onGear(6.0, null).ladderFrame()
        assertEquals("0", zero.values.power)
        assertEquals("0.0", zero.values.speed)
        assertEquals("--", zero.values.heartRate)
        assertEquals("6", zero.values.gear)
        assertEquals(0, zero.cadenceLights)
        assertEquals(0.0, zero.values.powerPosition!!.fraction, 0.0)
        assertNull(zero.heartPosition)
        for (metric in Metric.entries) {
            for (missing in listOf(null, Double.NaN, Double.POSITIVE_INFINITY)) {
                val state = LadderPreview.state.onValue(metric, missing)
                assertFalse(state.readings.containsKey(metric))
                assertEquals(9, state.readings.size)
            }
        }
        val invalidProfile = LadderPreview.state.copy(ftp = 0.0, maximumHr = 50.0).ladderFrame()
        assertEquals("245", invalidProfile.values.power)
        assertEquals("156", invalidProfile.values.heartRate)
        assertNull(invalidProfile.values.powerPosition)
        assertNull(invalidProfile.heartPosition)
        assertEquals("-3.6", LadderPreview.state.onValue(Metric.GRADE, -3.6).ladderFrame().values.grade)
    }

    @Test fun `idle and expired weather mark wind unavailable while valid calm stays blank`() {
        val calm = LadderPreview.state.ladderFrame(DashboardCalculator.wind(315.0, 270.0, .29))
        assertEquals(WindDisplay.Calm, calm.values.wind)
        assertEquals(50, calm.values.northAngle)
        val expired = LadderPreview.state.ladderFrame(DashboardCalculator.wind(315.0, null, null))
        assertEquals(WindDisplay.Unavailable, expired.values.wind)
        assertEquals(50, expired.values.northAngle)
        val idle = LadderPreview.state.onRide(false).onValue(Metric.POWER, 245.0).onLocation(90.0, 30.0)
        assertEquals(LadderFrame(), idle.ladderFrame(LadderPreview.wind))
        val resumed = idle.onRide(true).onValue(Metric.POWER, 245.0).ladderFrame(LadderPreview.wind)
        assertEquals("245", resumed.values.power)
        assertEquals(WindDisplay.Unavailable, resumed.values.wind)
        assertNull(resumed.values.northAngle)
        assertEquals(LadderPreview.frame, LadderPreview.state.onRide(true).ladderFrame(LadderPreview.wind))
    }

    @Test fun `power core preview binds speed left heart right and all five auxiliary fields`() {
        val frame = LadderPreview.state.ladderFrame(LadderPreview.wind)
        assertEquals("245", frame.values.power)
        assertEquals("28.6", frame.values.speed)
        assertEquals("156", frame.values.heartRate)
        assertEquals("3.6", frame.values.grade)
        assertEquals("6/13", frame.values.gear)
        assertEquals(listOf("198", "27.4", "148", "214", "42.7"), listOf(frame.values.averagePower,
            frame.values.averageSpeed, frame.values.averageHr, frame.values.normalizedPower, frame.values.distance))
        assertEquals(8, frame.cadenceLights)
        assertEquals(.4766667, frame.speedFraction!!, .000001)
        assertEquals(.595238, frame.heartPosition!!.fraction, .000001)
        assertEquals(5, frame.values.powerPosition!!.zone)
        assertEquals(WindIndicator(140, 1), frame.values.wind)
        assertEquals(50, frame.values.northAngle)
    }
}
