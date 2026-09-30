package com.jxguo92.mykarooextension.feature.datafield.concentricdashboard

import android.content.Context
import android.content.res.Configuration
import android.os.SystemClock
import android.util.Log
import com.jxguo92.mykarooextension.core.karoo.KarooFlows
import com.jxguo92.mykarooextension.core.karoo.fieldValue
import com.jxguo92.mykarooextension.core.weather.GeoLocation
import com.jxguo92.mykarooextension.core.weather.WeatherProvider
import io.hammerhead.karooext.extension.DataTypeImpl
import io.hammerhead.karooext.internal.Emitter
import io.hammerhead.karooext.internal.ViewEmitter
import io.hammerhead.karooext.models.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class ConcentricDataField(
    private val karoo: KarooFlows,
    private val weatherProvider: WeatherProvider?,
    private val parent: Job,
    extensionId: String,
) : DataTypeImpl(extensionId, TYPE_ID) {
    // This graphical field deliberately owns no numeric/center subscription.
    override fun startStream(emitter: Emitter<StreamState>) {
        emitter.onNext(StreamState.NotAvailable)
        emitter.setCancellable { }
    }

    override fun startView(context: Context, config: ViewConfig, emitter: ViewEmitter) {
        val scope = CoroutineScope(SupervisorJob(parent) + Dispatchers.Default)
        val state = MutableStateFlow(ConcentricState())
        val location = MutableStateFlow<GeoLocation?>(null)
        val weather = ConcentricWeather(weatherProvider, SystemClock::elapsedRealtime)
        emitter.setCancellable { scope.cancel() }
        emitter.onNext(UpdateGraphicConfig(showHeader = false))
        ConcentricRuntime(
            scope = scope,
            fullPage = config.gridSize == (60 to 60),
            preview = config.preview,
            night = { context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES },
            latest = {
                val snapshot = state.value
                val conditions = weather.current()
                snapshot.display(ConcentricCalculator.wind(snapshot.heading, conditions?.windDirectionDegrees, conditions?.windSpeedMetersPerSecond))
            },
            subscribe = {
                subscribe(scope, state, location)
                scope.launch {
                    while (isActive) {
                        weather.refresh(location.value)
                        delay(1_000)
                    }
                }
            },
            submit = { emitter.updateView(ConcentricView.render(context, config.viewSize, it)) },
            onFailure = { Log.w(TYPE_ID, "Unable to submit dashboard frame; will retry") },
        ).start()
    }

    private fun subscribe(scope: CoroutineScope, state: MutableStateFlow<ConcentricState>, location: MutableStateFlow<GeoLocation?>) {
        SOURCES.forEach { source ->
            scope.launch {
                karoo.streamStates(source.type).collect { event ->
                    state.update { it.onValue(source.metric, event.fieldValue(source.field)?.times(source.factor)) }
                }
            }
        }
        scope.launch { karoo.userProfile().collect { profile ->
            state.update { it.copy(ftp = profile.ftp.toDouble(), maximumHr = profile.maxHr.toDouble(), restingHr = profile.restingHr.toDouble()) }
        } }
        scope.launch { karoo.rideState().collect { ride ->
            state.update { it.onRide(ride != RideState.Idle) }
        } }
        scope.launch { karoo.streamStates(DataType.Type.SHIFTING_REAR_GEAR).collect { event ->
            state.update { it.onGear(event.fieldValue(DataType.Field.SHIFTING_REAR_GEAR), event.fieldValue(DataType.Field.SHIFTING_REAR_GEAR_MAX)) }
        } }
        scope.launch { karoo.streamStates(DataType.Type.LOCATION).collect { event ->
            val latitude = event.fieldValue(DataType.Field.LOC_LATITUDE)
            val longitude = event.fieldValue(DataType.Field.LOC_LONGITUDE)
            location.value = if (latitude != null && longitude != null && latitude in -90.0..90.0 && longitude in -180.0..180.0)
                GeoLocation(latitude, longitude) else null
            // SDK 1.1.9 does not expose a LOC_SPEED constant; LOCATION uses this wire key (m/s).
            state.update { it.onLocation(event.fieldValue(DataType.Field.LOC_BEARING), event.fieldValue("LOC_SPEED")?.times(3.6)) }
        } }
    }

    private data class Source(val metric: Metric, val type: String, val field: String, val factor: Double = 1.0)

    companion object {
        const val TYPE_ID = "concentric-dashboard"
        private val SOURCES = listOf(
            Source(Metric.POWER, DataType.Type.SMOOTHED_3S_AVERAGE_POWER, DataType.Field.SMOOTHED_3S_AVERAGE_POWER),
            Source(Metric.CADENCE, DataType.Type.CADENCE, DataType.Field.CADENCE),
            Source(Metric.HEART_RATE, DataType.Type.HEART_RATE, DataType.Field.HEART_RATE),
            Source(Metric.SPEED, DataType.Type.SPEED, DataType.Field.SPEED, 3.6),
            Source(Metric.DISTANCE, DataType.Type.DISTANCE, DataType.Field.DISTANCE, .001),
            Source(Metric.GRADE, DataType.Type.ELEVATION_GRADE, DataType.Field.ELEVATION_GRADE),
            Source(Metric.AVG_SPEED, DataType.Type.AVERAGE_SPEED, DataType.Field.AVERAGE_SPEED, 3.6),
            Source(Metric.AVG_HR, DataType.Type.AVERAGE_HR, DataType.Field.AVG_HR),
            Source(Metric.AVG_POWER, DataType.Type.AVERAGE_POWER, DataType.Field.AVERAGE_POWER),
            Source(Metric.NORMALIZED_POWER, DataType.Type.NORMALIZED_POWER, DataType.Field.NORMALIZED_POWER),
        )
    }
}
