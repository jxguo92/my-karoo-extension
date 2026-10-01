package com.jxguo92.mykarooextension.feature.datafield.concentricdashboard

import com.jxguo92.mykarooextension.core.dashboard.*
import com.jxguo92.mykarooextension.core.karoo.onLocationSample

import io.hammerhead.karooext.models.DataPoint
import io.hammerhead.karooext.models.DataType
import io.hammerhead.karooext.models.StreamState
import org.junit.Assert.*
import org.junit.Test

class ConcentricLocationTest {
    @Test fun `same sample speed overrides previous speed using location wire ids`() {
        val moving = DashboardState().onValue(Metric.SPEED, 20.0).onLocation(315.0, null)
        val stopped = moving.onLocationSample(sample("FIELD_LOC_SPEED_ID" to 0.0))
        assertEquals(315.0, stopped.heading!!, 0.0)
        val slow = moving.onValue(Metric.SPEED, 0.0)
        assertEquals(90.0, slow.onLocationSample(sample("FIELD_LOC_SPEED_ID" to 2.0)).heading!!, 0.0)
    }

    @Test fun `legacy speed alias and missing speed retain heading policy`() {
        val moving = DashboardState().onValue(Metric.SPEED, 20.0).onLocation(315.0, null)
        assertEquals(315.0, moving.onLocationSample(sample("LOC_SPEED" to 0.0)).heading!!, 0.0)
        assertEquals(90.0, moving.onLocationSample(sample()).heading!!, 0.0)
        assertEquals(90.0, moving.onLocationSample(sample("FIELD_LOC_SPEED_ID" to Double.NaN)).heading!!, 0.0)
        assertEquals(315.0, moving.onLocationSample(StreamState.NotAvailable).heading!!, 0.0)
        assertNull(moving.onRide(false).onLocationSample(sample("FIELD_LOC_SPEED_ID" to 2.0)).heading)
        assertEquals(315.0, moving.onLocationSample(sample("FIELD_LOC_SPEED_ID" to 0.0, "LOC_SPEED" to 2.0)).heading!!, 0.0)
    }

    private fun sample(vararg values: Pair<String, Double>) = StreamState.Streaming(
        DataPoint(DataType.Type.LOCATION, mapOf(DataType.Field.LOC_BEARING to 90.0) + values),
    )
}
