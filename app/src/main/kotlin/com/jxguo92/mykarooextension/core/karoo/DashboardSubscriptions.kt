package com.jxguo92.mykarooextension.core.karoo

import com.jxguo92.mykarooextension.core.dashboard.DashboardState
import com.jxguo92.mykarooextension.core.dashboard.Metric
import com.jxguo92.mykarooextension.core.weather.GeoLocation
import io.hammerhead.karooext.models.DataType
import io.hammerhead.karooext.models.RideState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
/** The common metric/gear/profile/location streams; the caller owns all child jobs. */
fun subscribeDashboardReadings(
    karoo: KarooFlows,
    scope: CoroutineScope,
    state: MutableStateFlow<DashboardState>,
    location: MutableStateFlow<GeoLocation?>,
) {
    DASHBOARD_SOURCES.forEach { source ->
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
        state.update { it.onLocationSample(event) }
    } }
}

private data class Source(val metric: Metric, val type: String, val field: String, val factor: Double = 1.0)

private val DASHBOARD_SOURCES = listOf(
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
