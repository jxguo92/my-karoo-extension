package com.jxguo92.mykarooextension.core.karoo

import android.content.Context
import com.jxguo92.mykarooextension.core.ui.CompositeFieldView
import com.jxguo92.mykarooextension.core.ui.DisplayTriplet
import io.hammerhead.karooext.internal.Emitter
import io.hammerhead.karooext.internal.ViewEmitter
import io.hammerhead.karooext.models.DataPoint
import io.hammerhead.karooext.models.DataType
import io.hammerhead.karooext.models.StreamState
import io.hammerhead.karooext.models.UpdateGraphicConfig
import io.hammerhead.karooext.models.ViewConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.launch

fun startCenterStream(
    source: Flow<StreamState>,
    field: String,
    outputTypeId: String,
    emitter: Emitter<StreamState>,
) {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    scope.launch {
        source.collect { state ->
            val value = state.fieldValue(field)
            emitter.onNext(if (state is StreamState.Streaming) {
                if (value == null) StreamState.NotAvailable else StreamState.Streaming(
                    DataPoint(outputTypeId, mapOf(DataType.Field.SINGLE to value)),
                )
            } else state)
        }
    }
    emitter.setCancellable { scope.cancel() }
}

@OptIn(FlowPreview::class)
fun Flow<DisplayTriplet>.sampledDisplay(): Flow<DisplayTriplet> = distinctUntilChanged().sample(1_000)

@OptIn(FlowPreview::class)
fun startCompositeView(
    context: Context,
    config: ViewConfig,
    emitter: ViewEmitter,
    values: Flow<DisplayTriplet>,
    preview: DisplayTriplet,
    scope: CoroutineScope,
) {
    emitter.onNext(UpdateGraphicConfig(showHeader = false))
    publishDisplay(scope, values, config.preview, preview) {
        emitter.updateView(CompositeFieldView.render(context, config, it))
    }
    emitter.setCancellable { scope.cancel() }
}

fun publishDisplay(
    scope: CoroutineScope,
    values: Flow<DisplayTriplet>,
    preview: Boolean,
    previewValues: DisplayTriplet,
    emit: (DisplayTriplet) -> Unit,
) {
    emit(if (preview) previewValues else DisplayTriplet())
    if (!preview) scope.launch { values.sampledDisplay().collect(emit) }
}
