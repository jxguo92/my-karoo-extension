package com.jxguo92.mykarooextension.feature.datafield.rearcogteeth

import io.hammerhead.karooext.internal.Emitter
import io.hammerhead.karooext.models.DataPoint
import io.hammerhead.karooext.models.DataType
import io.hammerhead.karooext.models.StreamState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class RearCogTeethDataFieldTest {
    @Test
    fun `stream output prefers reported teeth`() {
        val input = shiftingState(
            DataType.Field.SHIFTING_REAR_GEAR_TEETH to 24.0,
            DataType.Field.SHIFTING_REAR_GEAR to 5.0,
            DataType.Field.SHIFTING_REAR_GEAR_MAX to 13.0,
        )

        val output = RearCogTeethDataField.mapState(input, outputDataTypeId) as StreamState.Streaming

        assertEquals(outputDataTypeId, output.dataPoint.dataTypeId)
        assertEquals(24.0, output.dataPoint.singleValue)
    }

    @Test
    fun `stream output falls back to XG-1371 gear mapping`() {
        val input = shiftingState(
            DataType.Field.SHIFTING_REAR_GEAR to 6.0,
            DataType.Field.SHIFTING_REAR_GEAR_MAX to 13.0,
        )

        val output = RearCogTeethDataField.mapState(input, outputDataTypeId) as StreamState.Streaming

        assertEquals(21.0, output.dataPoint.singleValue)
    }

    @Test
    fun `stream output is unavailable when a reading cannot be resolved`() {
        assertSame(StreamState.NotAvailable, RearCogTeethDataField.mapState(shiftingState(), outputDataTypeId))
    }

    @Test
    fun `non-streaming states pass through unchanged`() {
        listOf(StreamState.Idle, StreamState.Searching, StreamState.NotAvailable).forEach { input ->
            assertSame(input, RearCogTeethDataField.mapState(input, outputDataTypeId))
        }
    }

    @Test
    fun `cancelling a field stream cancels its rear gear subscription`() {
        val rearGearStream = FakeRearGearStream()
        val emitter = FakeEmitter()
        val field = RearCogTeethDataField(rearGearStream, "my-karoo-extension")

        field.startStream(emitter)
        rearGearStream.emit(shiftingState(DataType.Field.SHIFTING_REAR_GEAR_TEETH to 24.0))
        emitter.cancel()

        assertEquals(24.0, (emitter.states.single() as StreamState.Streaming).dataPoint.singleValue)
        assertEquals(true, rearGearStream.cancelled)
    }

    private fun shiftingState(vararg values: Pair<String, Double>) = StreamState.Streaming(
        DataPoint(
            dataTypeId = DataType.Type.SHIFTING_REAR_GEAR,
            values = mapOf(*values),
        ),
    )

    private val outputDataTypeId = DataType.dataTypeId(
        extension = "my-karoo-extension",
        typeId = RearCogTeethDataField.TYPE_ID,
    )

    private class FakeRearGearStream : RearGearStream {
        private lateinit var listener: (StreamState) -> Unit
        var cancelled = false

        override fun subscribe(onState: (StreamState) -> Unit): () -> Unit {
            listener = onState
            return { cancelled = true }
        }

        fun emit(state: StreamState) = listener(state)
    }

    private class FakeEmitter : Emitter<StreamState> {
        val states = mutableListOf<StreamState>()
        private var cancellable: () -> Unit = {}

        override fun onNext(t: StreamState) {
            states += t
        }

        override fun onError(t: Throwable) = Unit

        override fun onComplete() = Unit

        override fun setCancellable(cancellable: () -> Unit) {
            this.cancellable = cancellable
        }

        override fun cancel() = cancellable()
    }
}
