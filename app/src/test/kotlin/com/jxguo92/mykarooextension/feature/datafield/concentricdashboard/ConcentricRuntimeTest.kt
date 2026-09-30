package com.jxguo92.mykarooextension.feature.datafield.concentricdashboard

import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ConcentricRuntimeTest {
    @Test fun `preview has no subscriptions and still responds to theme without sensor changes`() = runTest {
        var subscriptions = 0
        var night = false
        val frames = mutableListOf<ConcentricFrame>()
        val runtime = ConcentricRuntime(backgroundScope, true, true, { night }, { ConcentricDisplay() },
            { subscriptions++ }, frames::add, {})
        runtime.start()
        assertEquals(ConcentricPreview.display, frames.single().display)
        night = true
        advanceTimeBy(999); runCurrent(); assertEquals(1, frames.size)
        advanceTimeBy(1); runCurrent(); assertTrue(frames.last().night)
        advanceTimeBy(1000); runCurrent(); assertEquals(2, frames.size)
        assertEquals(0, subscriptions)
    }

    @Test fun `unsupported size is blank and subscribes to nothing`() = runTest {
        var subscriptions = 0
        val frames = mutableListOf<ConcentricFrame>()
        ConcentricRuntime(backgroundScope, false, false, { false }, { error("read") },
            { subscriptions++ }, frames::add, {}).start()
        advanceTimeBy(2000); runCurrent()
        assertEquals(0, subscriptions)
        assertEquals(listOf(ConcentricFrame(null, false)), frames)
    }

    @Test fun `live placeholder is immediate updates limited to one Hz and failed submissions retry`() = runTest {
        var current = ConcentricDisplay(power = "1")
        var subscriptions = 0
        var failures = 0
        val frames = mutableListOf<ConcentricFrame>()
        val scope = CoroutineScope(backgroundScope.coroutineContext + Job())
        ConcentricRuntime(scope, true, false, { false }, { current }, { subscriptions++ }, {
            if (it.display?.power == "2" && failures == 0) { failures++; error("submit") }
            frames += it
        }, {}).start()
        assertEquals("--", frames.single().display!!.power)
        current = current.copy(power = "2")
        advanceTimeBy(1000); runCurrent(); assertEquals(1, frames.size)
        advanceTimeBy(1000); runCurrent(); assertEquals("2", frames.last().display!!.power)
        assertEquals(1, subscriptions)
        scope.cancel()
        current = current.copy(power = "3")
        advanceTimeBy(3000); runCurrent(); assertEquals(2, frames.size)
    }
}
