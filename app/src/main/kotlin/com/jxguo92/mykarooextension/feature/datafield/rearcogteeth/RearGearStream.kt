package com.jxguo92.mykarooextension.feature.datafield.rearcogteeth

import io.hammerhead.karooext.KarooSystemService
import io.hammerhead.karooext.models.DataType
import io.hammerhead.karooext.models.OnStreamState
import io.hammerhead.karooext.models.StreamState

fun interface RearGearStream {
    fun subscribe(onState: (StreamState) -> Unit): () -> Unit
}

class KarooRearGearStream(
    private val karooSystem: KarooSystemService,
) : RearGearStream {
    override fun subscribe(onState: (StreamState) -> Unit): () -> Unit {
        val consumerId = karooSystem.addConsumer<OnStreamState>(
            params = OnStreamState.StartStreaming(DataType.Type.SHIFTING_REAR_GEAR),
            onError = { onState(StreamState.NotAvailable) },
            onComplete = { onState(StreamState.NotAvailable) },
            onEvent = { event -> onState(event.state) },
        )
        return { karooSystem.removeConsumer(consumerId) }
    }
}
