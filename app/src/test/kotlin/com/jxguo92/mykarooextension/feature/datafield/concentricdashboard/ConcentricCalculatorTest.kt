package com.jxguo92.mykarooextension.feature.datafield.concentricdashboard

import org.junit.Assert.*
import org.junit.Test

class ConcentricCalculatorTest {
    @Test fun `power uses equal zones and continuous lower-owned boundaries`() {
        val bounds = listOf(110.0, 150.0, 180.0, 210.0, 240.0, 300.0, 400.0)
        bounds.forEachIndexed { index, power ->
            assertEquals(index, ConcentricCalculator.power(power, 200.0)!!.zone)
            assertEquals((index + 1) / 7.0, ConcentricCalculator.power(power, 200.0)!!.fraction, 1e-9)
        }
        assertEquals(1, ConcentricCalculator.power(110.01, 200.0)!!.zone)
        assertEquals(0.5 / 7, ConcentricCalculator.power(96.0, 200.0)!!.fraction, 1e-9)
        assertEquals(0.0, ConcentricCalculator.power(0.0, 200.0)!!.fraction, 0.0)
        assertEquals(1.0, ConcentricCalculator.power(999.0, 200.0)!!.fraction, 0.0)
        assertNull(ConcentricCalculator.power(245.0, 0.0))
        assertNull(ConcentricCalculator.power(Double.NaN, 200.0))
    }

    @Test fun `heart rate uses HRR bounds and cadence fills continuously`() {
        listOf(120.0, 134.0, 148.0, 162.0, 176.0, 190.0).forEachIndexed { zone, bpm ->
            assertEquals(zone, ConcentricCalculator.heartZone(bpm, 190.0, 50.0))
        }
        assertNull(ConcentricCalculator.heartZone(107.39, 190.0, 50.0))
        assertEquals(0, ConcentricCalculator.heartZone(107.4, 190.0, 50.0))
        assertEquals(1, ConcentricCalculator.heartZone(120.01, 190.0, 50.0))
        assertEquals(5, ConcentricCalculator.heartZone(250.0, 190.0, 50.0))
        assertNull(ConcentricCalculator.heartZone(150.0, 50.0, 50.0))
        assertNull(ConcentricCalculator.heartZone(Double.POSITIVE_INFINITY, 190.0, 50.0))
        assertEquals(0.0, ConcentricCalculator.cadenceFraction(50.0)!!, 0.0)
        assertEquals(1.0 / 60, ConcentricCalculator.cadenceFraction(51.0)!!, 1e-9)
        assertEquals(1.0, ConcentricCalculator.cadenceFraction(110.0)!!, 0.0)
        assertEquals(1.0, ConcentricCalculator.cadenceFraction(200.0)!!, 0.0)
        assertNull(ConcentricCalculator.cadenceFraction(-1.0))
        assertEquals(listOf(0, 1, 1, 2), listOf(80.0, 81.0, 100.0, 101.0).map(ConcentricCalculator::cadenceColor))
    }

    @Test fun `wind points toward rider for headwind and away for tailwind with four color levels`() {
        assertEquals(180, ConcentricCalculator.wind(90.0, 90.0, 4.0)!!.angle)
        assertEquals(0, ConcentricCalculator.wind(90.0, 270.0, 4.0)!!.angle)
        assertEquals(listOf(0, 0, 1, 1, 2, 2, 3),
            listOf(.3, 3.399, 3.4, 5.499, 5.5, 7.999, 8.0).map { ConcentricCalculator.wind(0.0, 0.0, it)!!.level })
        assertNull(ConcentricCalculator.wind(0.0, 0.0, .299))
        assertNull(ConcentricCalculator.wind(null, 0.0, 4.0))
        assertNull(ConcentricCalculator.wind(0.0, -1.0, 4.0))
        assertNull(ConcentricCalculator.wind(0.0, 0.0, Double.NaN))
        assertEquals(0, ConcentricCalculator.angle(355.0))
        assertEquals(350, ConcentricCalculator.angle(354.9))
    }
}
