package com.jxguo92.mykarooextension.feature.datafield.rearcogteeth

import android.content.Context
import com.jxguo92.mykarooextension.core.karoo.rearCogReading
import io.hammerhead.karooext.extension.DataTypeImpl
import io.hammerhead.karooext.internal.Emitter
import io.hammerhead.karooext.internal.ViewEmitter
import io.hammerhead.karooext.models.DataPoint
import io.hammerhead.karooext.models.DataType
import io.hammerhead.karooext.models.StreamState
import io.hammerhead.karooext.models.UpdateGraphicConfig
import io.hammerhead.karooext.models.ViewConfig

class RearCogTeethDataField(
    private val rearGearStream: RearGearStream,
    extensionId: String,
) : DataTypeImpl(extensionId, TYPE_ID) {
    override fun startStream(emitter: Emitter<StreamState>) {
        val cancel = rearGearStream.subscribe { state ->
            emitter.onNext(mapState(state, dataTypeId))
        }
        emitter.setCancellable(cancel)
    }

    override fun startView(context: Context, config: ViewConfig, emitter: ViewEmitter) {
        emitter.onNext(UpdateGraphicConfig(showHeader = true))

        if (config.preview) {
            emitter.updateView(RearCogTeethView.render(context, config, RearCogTeethPreview.reading.displayText))
            emitter.setCancellable {}
            return
        }

        fun render(state: StreamState) {
            val reading = state.rearCogReading()
            emitter.updateView(RearCogTeethView.render(context, config, reading?.displayText ?: "--"))
        }

        val cancel = rearGearStream.subscribe(::render)
        emitter.setCancellable(cancel)
    }

    companion object {
        const val TYPE_ID = "rear-cog-teeth"

        fun mapState(state: StreamState, outputDataTypeId: String): StreamState {
            if (state !is StreamState.Streaming) return state
            val reading = state.rearCogReading() ?: return StreamState.NotAvailable
            return StreamState.Streaming(
                DataPoint(
                    dataTypeId = outputDataTypeId,
                    values = mapOf(DataType.Field.SINGLE to reading.teeth.toDouble()),
                ),
            )
        }

    }
}
