package com.jxguo92.mykarooextension.feature.datafield.concentricdashboard

import com.jxguo92.mykarooextension.core.dashboard.*

import org.junit.Assert.*
import org.junit.Test

class ConcentricCalculatorTest {
    @Test fun `power uses equal zones and continuous lower-owned boundaries`() {
        val bounds = listOf(110.0, 150.0, 180.0, 210.0, 240.0, 300.0, 400.0)
        bounds.forEachIndexed { index, power ->
            assertEquals(index, DashboardCalculator.power(power, 200.0)!!.zone)
            assertEquals((index + 1) / 7.0, DashboardCalculator.power(power, 200.0)!!.fraction, 1e-9)
        }
        assertEquals(1, DashboardCalculator.power(110.01, 200.0)!!.zone)
        assertEquals(0.5 / 7, DashboardCalculator.power(96.0, 200.0)!!.fraction, 1e-9)
        assertEquals(0.0, DashboardCalculator.power(0.0, 200.0)!!.fraction, 0.0)
        assertEquals(1.0, DashboardCalculator.power(999.0, 200.0)!!.fraction, 0.0)
        assertNull(DashboardCalculator.power(245.0, 0.0))
        assertNull(DashboardCalculator.power(Double.NaN, 200.0))
    }

    @Test fun `heart rate uses HRR bounds and cadence fills continuously`() {
        listOf(120.0, 134.0, 148.0, 162.0, 176.0, 190.0).forEachIndexed { zone, bpm ->
            assertEquals(zone, DashboardCalculator.heartZone(bpm, 190.0, 50.0))
        }
        assertNull(DashboardCalculator.heartZone(107.39, 190.0, 50.0))
        assertEquals(0, DashboardCalculator.heartZone(107.4, 190.0, 50.0))
        assertEquals(1, DashboardCalculator.heartZone(120.01, 190.0, 50.0))
        assertEquals(5, DashboardCalculator.heartZone(250.0, 190.0, 50.0))
        assertNull(DashboardCalculator.heartZone(150.0, 50.0, 50.0))
        assertNull(DashboardCalculator.heartZone(Double.POSITIVE_INFINITY, 190.0, 50.0))
        assertEquals(0.0, DashboardCalculator.cadenceFraction(50.0)!!, 0.0)
        assertEquals(1.0 / 60, DashboardCalculator.cadenceFraction(51.0)!!, 1e-9)
        assertEquals(1.0, DashboardCalculator.cadenceFraction(110.0)!!, 0.0)
        assertEquals(1.0, DashboardCalculator.cadenceFraction(200.0)!!, 0.0)
        assertNull(DashboardCalculator.cadenceFraction(-1.0))
        assertEquals(listOf(0, 1, 1, 2), listOf(80.0, 81.0, 100.0, 101.0).map(DashboardCalculator::cadenceColor))
    }

    @Test fun `wind points toward rider for headwind and away for tailwind with four color levels`() {
        assertEquals(WindIndicator(180, 1), DashboardCalculator.wind(90.0, 90.0, 4.0))
        assertEquals(WindIndicator(0, 1), DashboardCalculator.wind(90.0, 270.0, 4.0))
        assertEquals(listOf(0, 0, 1, 1, 2, 2, 3),
            listOf(.3, 3.399, 3.4, 5.499, 5.5, 7.999, 8.0).map { (DashboardCalculator.wind(0.0, 0.0, it) as WindIndicator).level })
        assertEquals(WindDisplay.Calm, DashboardCalculator.wind(0.0, 0.0, .299))
        assertEquals(WindDisplay.Unavailable, DashboardCalculator.wind(null, 0.0, 4.0))
        assertEquals(WindDisplay.Unavailable, DashboardCalculator.wind(0.0, -1.0, 4.0))
        assertEquals(WindDisplay.Unavailable, DashboardCalculator.wind(0.0, 0.0, Double.NaN))
        assertEquals(0, DashboardCalculator.angle(355.0))
        assertEquals(350, DashboardCalculator.angle(354.9))
    }

    @Test fun `calm requires valid heading direction and speed`() {
        assertEquals(WindDisplay.Calm, DashboardCalculator.wind(0.0, 360.0, 0.0))
        for (invalid in listOf(null, -1.0, 361.0, Double.NaN, Double.POSITIVE_INFINITY)) {
            assertEquals(WindDisplay.Unavailable, DashboardCalculator.wind(invalid, 0.0, .1))
            assertEquals(WindDisplay.Unavailable, DashboardCalculator.wind(0.0, invalid, .1))
        }
        for (invalid in listOf(null, -1.0, Double.NaN, Double.POSITIVE_INFINITY)) {
            assertEquals(WindDisplay.Unavailable, DashboardCalculator.wind(0.0, 0.0, invalid))
        }
        assertEquals(WindIndicator(180, 0), DashboardCalculator.wind(0.0, 0.0, .3))
    }
}
