package com.jxguo92.mykarooextension.feature.datafield.rearcogteeth

import com.jxguo92.mykarooextension.core.karoo.RearCogTeethCalculator
import com.jxguo92.mykarooextension.core.karoo.RearCogTeethReading

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RearCogTeethCalculatorTest {
    @Test
    fun `uses reported tooth count without inferred marker`() {
        val reading = RearCogTeethCalculator.resolve(
            reportedTeeth = 24.0,
            rearGearIndex = 5.0,
            rearGearCount = 13.0,
        )

        assertEquals(RearCogTeethReading(teeth = 24, inferred = false), reading)
        assertEquals("24T", reading?.displayText)
    }

    @Test
    fun `infers XG-1371 teeth from one-based largest-to-smallest gear index`() {
        val expectedTeeth = listOf(46, 38, 32, 28, 24, 21, 19, 17, 15, 13, 12, 11, 10)

        expectedTeeth.forEachIndexed { zeroBasedIndex, teeth ->
            val reading = RearCogTeethCalculator.resolve(
                reportedTeeth = null,
                rearGearIndex = (zeroBasedIndex + 1).toDouble(),
                rearGearCount = 13.0,
            )

            assertEquals(RearCogTeethReading(teeth = teeth, inferred = true), reading)
            assertEquals("≈${teeth}T", reading?.displayText)
        }
    }

    @Test
    fun `infers when gear count is omitted`() {
        assertEquals(
            RearCogTeethReading(teeth = 21, inferred = true),
            RearCogTeethCalculator.resolve(
                reportedTeeth = null,
                rearGearIndex = 6.0,
                rearGearCount = null,
            ),
        )
    }

    @Test
    fun `does not infer for a different cassette size`() {
        assertNull(
            RearCogTeethCalculator.resolve(
                reportedTeeth = null,
                rearGearIndex = 6.0,
                rearGearCount = 12.0,
            ),
        )
    }

    @Test
    fun `rejects non-integral non-finite and out-of-range values`() {
        val invalidInputs = listOf(
            Triple(null, 0.0, 13.0),
            Triple(null, 14.0, 13.0),
            Triple(null, 1.5, 13.0),
            Triple(null, Double.NaN, 13.0),
            Triple(Double.POSITIVE_INFINITY, null, null),
            Triple(-1.0, null, null),
        )

        invalidInputs.forEach { (teeth, index, count) ->
            assertNull(RearCogTeethCalculator.resolve(teeth, index, count))
        }
    }
}
