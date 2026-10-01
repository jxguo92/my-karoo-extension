package com.jxguo92.mykarooextension.feature.datafield.ladderdashboard

import com.jxguo92.mykarooextension.core.dashboard.DashboardDisplay
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LadderRuntimeTest {
    @Test fun `live first frame immediate subsequent changes sampled and failures retried until cancellation`() = runTest {
        var latest = LadderFrame(DashboardDisplay(power = "1"))
        var subscriptions = 0
        var failure = true
        val frames = mutableListOf<LadderFrame?>()
        val scope = CoroutineScope(backgroundScope.coroutineContext + Job(backgroundScope.coroutineContext[Job]))
        LadderRuntime(scope, 60 to 60, false, { latest }, { subscriptions++ }, {
            if (it?.values?.power == "3" && failure) { failure = false; error("binder") }
            frames += it
        }, {}).start()
        assertEquals(listOf(LadderFrame()), frames)
        assertEquals(1, subscriptions)
        latest = LadderFrame(DashboardDisplay(power = "2"))
        latest = LadderFrame(DashboardDisplay(power = "3"))
        advanceTimeBy(999); runCurrent(); assertEquals(1, frames.size)
        advanceTimeBy(1); runCurrent(); assertEquals(1, frames.size)
        advanceTimeBy(1000); runCurrent(); assertEquals("3", frames.last()!!.values.power)
        advanceTimeBy(1000); runCurrent(); assertEquals(2, frames.size)
        scope.cancel()
        latest = LadderFrame(DashboardDisplay(power = "4"))
        advanceTimeBy(2000); runCurrent(); assertEquals(2, frames.size)
    }

    @Test fun `only full page subscribes while preview and other sizes emit once`() = runTest {
        for (size in listOf(60 to 48, 30 to 60, 60 to 15)) {
            val frames = mutableListOf<LadderFrame?>()
            assertFalse(LadderRuntime.isFullPage(size))
            LadderRuntime(backgroundScope, size, false, { error("read") }, { error("subscribe") }, frames::add, {}).start()
            assertEquals(listOf<LadderFrame?>(null), frames)
        }
        val frames = mutableListOf<LadderFrame?>()
        assertTrue(LadderRuntime.isFullPage(60 to 60))
        LadderRuntime(backgroundScope, 60 to 60, true, { error("read") }, { error("subscribe") }, frames::add, {}).start()
        advanceTimeBy(5000); runCurrent()
        assertEquals(listOf(LadderPreview.frame), frames)
    }
}
