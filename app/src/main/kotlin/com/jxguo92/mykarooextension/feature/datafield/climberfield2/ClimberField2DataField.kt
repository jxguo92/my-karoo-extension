package com.jxguo92.mykarooextension.feature.datafield.climberfield2

import android.content.Context
import com.jxguo92.mykarooextension.core.karoo.KarooFlows
import com.jxguo92.mykarooextension.core.karoo.startCenterStream
import com.jxguo92.mykarooextension.core.karoo.startCompositeView
import com.jxguo92.mykarooextension.core.karoo.fieldValue
import com.jxguo92.mykarooextension.core.karoo.rearCogReading
import com.jxguo92.mykarooextension.core.ui.DisplayTriplet
import io.hammerhead.karooext.extension.DataTypeImpl
import io.hammerhead.karooext.internal.Emitter
import io.hammerhead.karooext.internal.ViewEmitter
import io.hammerhead.karooext.models.DataType
import io.hammerhead.karooext.models.StreamState
import io.hammerhead.karooext.models.ViewConfig
import io.hammerhead.karooext.models.RideState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ClimberField2DataField(private val karoo: KarooFlows, extensionId: String) : DataTypeImpl(extensionId, TYPE_ID) {
    override fun startStream(emitter: Emitter<StreamState>) = startCenterStream(
        karoo.streamStates(DataType.Type.CADENCE), DataType.Field.CADENCE, dataTypeId, emitter,
    )

    override fun startView(context: Context, config: ViewConfig, emitter: ViewEmitter) {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val state = MutableStateFlow(ClimberField2State())
        if (!config.preview) {
            scope.launch { karoo.streamStates(DataType.Type.HEART_RATE).collect { event -> state.update {
                it.onHeartRate(event.fieldValue(DataType.Field.HEART_RATE))
            } } }
            scope.launch { karoo.streamStates(DataType.Type.CADENCE).collect { event -> state.update {
                it.onCadence(event.fieldValue(DataType.Field.CADENCE))
            } } }
            scope.launch { karoo.streamStates(DataType.Type.SHIFTING_REAR_GEAR).collect { event -> state.update {
                it.onGear(event.rearCogReading())
            } } }
            scope.launch { karoo.rideState().collect { event -> state.update { it.onRide(event != RideState.Idle) } } }
        }
        startCompositeView(context, config, emitter, state.map { it.display() }, PREVIEW, scope)
    }

    companion object {
        const val TYPE_ID = "climber-field-2"
        val PREVIEW = DisplayTriplet("156 bpm", "92 rpm", "≈21T")
    }
}
