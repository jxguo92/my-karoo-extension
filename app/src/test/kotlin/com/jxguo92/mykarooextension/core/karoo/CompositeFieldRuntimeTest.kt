package com.jxguo92.mykarooextension.core.karoo

import com.jxguo92.mykarooextension.core.ui.DisplayTriplet
import io.hammerhead.karooext.internal.Emitter
import io.hammerhead.karooext.models.DataPoint
import io.hammerhead.karooext.models.DataType
import io.hammerhead.karooext.models.StreamState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CompositeFieldRuntimeTest {
    @Test fun `live view draws placeholder immediately and cancellation stops collection`() = runTest {
        val source = MutableSharedFlow<DisplayTriplet>(extraBufferCapacity = 2)
        val rendered = mutableListOf<DisplayTriplet>()
        var stopped = false
        val viewScope = kotlinx.coroutines.CoroutineScope(backgroundScope.coroutineContext + kotlinx.coroutines.SupervisorJob())
        publishDisplay(viewScope, flow {
            try { source.collect { emit(it) } } finally { stopped = true }
        }, false, DisplayTriplet(center = "preview"), rendered::add)
        assertEquals(listOf(DisplayTriplet()), rendered)
        runCurrent()
        source.emit(DisplayTriplet(center = "one"))
        source.emit(DisplayTriplet(center = "two"))
        runCurrent()
        advanceTimeBy(1_000)
        runCurrent()
        assertEquals(listOf(DisplayTriplet(), DisplayTriplet(center = "two")), rendered)
        viewScope.cancel()
        runCurrent()
        assertEquals(true, stopped)
        source.emit(DisplayTriplet(center = "three"))
        advanceTimeBy(1_000)
        runCurrent()
        assertEquals(2, rendered.size)
    }

    @Test fun `preview draws only fixed values without subscribing`() = runTest {
        val rendered = mutableListOf<DisplayTriplet>()
        var subscribed = false
        val expected = DisplayTriplet(center = "87%")
        publishDisplay(backgroundScope, flow { subscribed = true }, true, expected, rendered::add)
        runCurrent()
        assertEquals(listOf(expected), rendered)
        assertEquals(false, subscribed)
    }

    @Test fun `rapid view states produce only the latest value each second`() = runTest {
        val source = MutableSharedFlow<DisplayTriplet>(extraBufferCapacity = 4)
        val rendered = mutableListOf<DisplayTriplet>()
        val job = backgroundScope.launch { source.sampledDisplay().collect { rendered += it } }
        runCurrent()
        source.emit(DisplayTriplet(center = "10%"))
        source.emit(DisplayTriplet(center = "20%"))
        runCurrent()
        advanceTimeBy(999)
        runCurrent()
        assertEquals(emptyList<DisplayTriplet>(), rendered)
        advanceTimeBy(1)
        runCurrent()
        assertEquals(listOf(DisplayTriplet(center = "20%")), rendered)
        job.cancel()
    }

    @Test fun `center stream maps valid value and propagates unavailable states then cancels`() = runBlocking {
        val source = MutableSharedFlow<StreamState>(extraBufferCapacity = 1)
        val emitter = FakeEmitter()
        startCenterStream(source, DataType.Field.CADENCE, "extension:center", emitter)
        delay(50)
        source.emit(StreamState.Streaming(DataPoint(DataType.Type.CADENCE, mapOf(DataType.Field.CADENCE to 0.0))))
        delay(50)
        assertEquals(0.0, (emitter.states.last() as StreamState.Streaming).dataPoint.singleValue)
        source.emit(StreamState.Searching)
        delay(50)
        assertSame(StreamState.Searching, emitter.states.last())
        emitter.cancel()
    }

    private class FakeEmitter : Emitter<StreamState> {
        val states = mutableListOf<StreamState>()
        private var cancellation: () -> Unit = {}
        override fun onNext(t: StreamState) { states += t }
        override fun onError(t: Throwable) = Unit
        override fun onComplete() = Unit
        override fun setCancellable(cancellable: () -> Unit) { cancellation = cancellable }
        override fun cancel() = cancellation()
    }
}
