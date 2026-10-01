package com.jxguo92.mykarooextension.feature.datafield.ladderdashboard

import org.junit.Assert.*
import org.junit.Test

class LadderCalculatorTest {
    @Test fun `cadence lights and speed cover their fixed metric ranges`() {
        for ((rpm, lights) in listOf(0.0 to 0, 50.0 to 0, 51.0 to 1, 55.0 to 1, 56.0 to 2, 88.0 to 8, 110.0 to 12, 130.0 to 12)) {
            assertEquals(lights, LadderCalculator.cadenceLights(rpm))
        }
        assertEquals(0, LadderCalculator.cadenceLights(null))
        assertEquals(0, LadderCalculator.cadenceLights(Double.NaN))
        assertEquals(0, LadderCalculator.cadenceLights(-1.0))
        assertEquals(0.0, LadderCalculator.speed(0.0)!!, 0.0)
        assertEquals(.4766667, LadderCalculator.speed(28.6)!!, .000001)
        assertEquals(1.0, LadderCalculator.speed(60.0)!!, 0.0)
        assertEquals(1.0, LadderCalculator.speed(75.0)!!, 0.0)
        assertNull(LadderCalculator.speed(Double.POSITIVE_INFINITY))
    }
    @Test fun `heart rail uses equal HRR zones and clamps only its position`() {
        listOf(120.0, 134.0, 148.0, 162.0, 176.0, 190.0).forEachIndexed { index, bpm ->
            val position = LadderCalculator.heart(bpm, 190.0, 50.0)!!
            assertEquals(index, position.zone)
            assertEquals((index + 1) / 6.0, position.fraction, .000001)
        }
        assertEquals(0.0, LadderCalculator.heart(100.0, 190.0, 50.0)!!.fraction, 0.00001)
        assertEquals(1.0 / 6, LadderCalculator.heart(120.0, 190.0, 50.0)!!.fraction, 0.00001)
        assertEquals(0.595238, LadderCalculator.heart(156.0, 190.0, 50.0)!!.fraction, 0.00001)
        assertEquals(1.0, LadderCalculator.heart(210.0, 190.0, 50.0)!!.fraction, 0.0)
        assertNull(LadderCalculator.heart(156.0, 50.0, 50.0))
        assertNull(LadderCalculator.heart(0.0, 190.0, 50.0))
        assertNull(LadderCalculator.heart(156.0, Double.NaN, 50.0))
        assertEquals(1, LadderCalculator.heart(120.01, 190.0, 50.0)!!.zone)
        assertEquals(0.0, LadderCalculator.heart(107.4, 190.0, 50.0)!!.fraction, .000001)
    }
}
