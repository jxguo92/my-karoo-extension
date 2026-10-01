package com.jxguo92.mykarooextension.feature.datafield.ladderdashboard

import kotlinx.coroutines.*

/** Reads a complete frame at most once per second; failed submissions remain retryable. */
class LadderRuntime(
    private val scope: CoroutineScope,
    private val gridSize: Pair<Int, Int>,
    private val preview: Boolean,
    private val latest: () -> LadderFrame,
    private val subscribe: () -> Unit,
    private val submit: (LadderFrame?) -> Unit,
    private val onFailure: () -> Unit,
) {
    fun start() {
        var submitted: LadderFrame? = null
        var hasSubmitted = false
        fun publish(frame: LadderFrame?) {
            if (!scope.isActive || (hasSubmitted && submitted == frame)) return
            try {
                submit(frame)
                submitted = frame
                hasSubmitted = true
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                onFailure()
            }
        }
        val fullPage = isFullPage(gridSize)
        val initial = if (!fullPage) null else if (preview) LadderPreview.frame else LadderFrame()
        publish(initial)
        if (hasSubmitted && (!fullPage || preview)) return
        if (fullPage && !preview && scope.isActive) subscribe()
        scope.launch {
            while (isActive) {
                delay(1_000)
                publish(if (fullPage && !preview) latest() else initial)
                if (hasSubmitted && (!fullPage || preview)) break
            }
        }
    }

    companion object {
        fun isFullPage(gridSize: Pair<Int, Int>): Boolean = gridSize == (60 to 60)
    }
}
