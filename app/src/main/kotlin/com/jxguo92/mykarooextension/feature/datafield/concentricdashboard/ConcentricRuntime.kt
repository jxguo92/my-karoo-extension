package com.jxguo92.mykarooextension.feature.datafield.concentricdashboard

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class ConcentricFrame(val display: ConcentricDisplay?, val night: Boolean)

/** Presentation boundary: the only place that submits frames, including theme-only changes. */
class ConcentricRuntime(
    private val scope: CoroutineScope,
    private val fullPage: Boolean,
    private val preview: Boolean,
    private val night: () -> Boolean,
    private val latest: () -> ConcentricDisplay,
    private val subscribe: () -> Unit,
    private val submit: (ConcentricFrame) -> Unit,
    private val onFailure: () -> Unit,
) {
    fun start() {
        var submitted: ConcentricFrame? = null
        fun publish(frame: ConcentricFrame) {
            if (!scope.isActive || submitted == frame) return
            try {
                submit(frame)
                submitted = frame
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                onFailure()
            }
        }
        publish(ConcentricFrame(if (!fullPage) null else if (preview) ConcentricPreview.display else ConcentricDisplay(), night()))
        if (!fullPage) return
        if (!preview && scope.isActive) subscribe()
        scope.launch {
            while (isActive) {
                delay(1_000)
                publish(ConcentricFrame(if (preview) ConcentricPreview.display else latest(), night()))
            }
        }
    }
}
