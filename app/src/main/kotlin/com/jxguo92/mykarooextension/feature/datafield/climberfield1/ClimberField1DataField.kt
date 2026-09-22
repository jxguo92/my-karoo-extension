package com.jxguo92.mykarooextension.feature.datafield.climberfield1

import android.content.Context
import com.jxguo92.mykarooextension.core.karoo.KarooFlows
import com.jxguo92.mykarooextension.core.karoo.startCenterStream
import com.jxguo92.mykarooextension.core.karoo.startCompositeView
import com.jxguo92.mykarooextension.core.karoo.fieldValue
import com.jxguo92.mykarooextension.core.ui.DisplayTriplet
import io.hammerhead.karooext.extension.DataTypeImpl
import io.hammerhead.karooext.internal.Emitter
import io.hammerhead.karooext.internal.ViewEmitter
import io.hammerhead.karooext.models.DataType
import io.hammerhead.karooext.models.StreamState
import io.hammerhead.karooext.models.ViewConfig
import io.hammerhead.karooext.models.RideState
import io.hammerhead.karooext.models.UserProfile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ClimberField1DataField(private val karoo: KarooFlows, extensionId: String) : DataTypeImpl(extensionId, TYPE_ID) {
    override fun startStream(emitter: Emitter<StreamState>) = startCenterStream(
        karoo.streamStates(DataType.Type.PERCENT_MAX_FTP), DataType.Field.PERCENT_MAX_FTP, dataTypeId, emitter,
    )

    override fun startView(context: Context, config: ViewConfig, emitter: ViewEmitter) {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val state = MutableStateFlow(ClimberField1State())
        if (!config.preview) {
            scope.launch { karoo.streamStates(DataType.Type.CLIMB).collect { event -> state.update {
                it.onClimb(event.fieldValue(DataType.Field.DISTANCE_TO_TOP), event.fieldValue(DataType.Field.ELEVATION_TO_TOP))
            } } }
            scope.launch { karoo.streamStates(DataType.Type.PERCENT_MAX_FTP).collect { event -> state.update {
                it.onFtp(event.fieldValue(DataType.Field.PERCENT_MAX_FTP))
            } } }
            scope.launch { karoo.userProfile().collect { profile -> state.update {
                it.onUnits(profile.preferredUnit.distance.toSystem(), profile.preferredUnit.elevation.toSystem())
            } } }
            scope.launch { karoo.rideState().collect { event -> state.update { it.onRide(event != RideState.Idle) } } }
        }
        startCompositeView(context, config, emitter, state.map { it.display() }, PREVIEW, scope)
    }

    companion object {
        const val TYPE_ID = "climber-field-1"
        val PREVIEW = DisplayTriplet("2.3 km", "87%", "186 m")
    }
}

private fun UserProfile.PreferredUnit.UnitType.toSystem(): MeasurementSystem = when (this) {
    UserProfile.PreferredUnit.UnitType.METRIC -> MeasurementSystem.METRIC
    UserProfile.PreferredUnit.UnitType.IMPERIAL -> MeasurementSystem.IMPERIAL
}
