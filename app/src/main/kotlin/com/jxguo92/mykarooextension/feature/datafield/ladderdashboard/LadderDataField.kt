package com.jxguo92.mykarooextension.feature.datafield.ladderdashboard

import android.content.Context
import android.os.SystemClock
import android.util.Log
import com.jxguo92.mykarooextension.core.dashboard.DashboardCalculator
import com.jxguo92.mykarooextension.core.dashboard.DashboardState
import com.jxguo92.mykarooextension.core.karoo.KarooFlows
import com.jxguo92.mykarooextension.core.karoo.subscribeDashboardReadings
import com.jxguo92.mykarooextension.core.weather.DashboardWeather
import com.jxguo92.mykarooextension.core.weather.GeoLocation
import com.jxguo92.mykarooextension.core.weather.WeatherProvider
import com.jxguo92.mykarooextension.core.weather.startDashboardWeatherUpdates
import io.hammerhead.karooext.extension.DataTypeImpl
import io.hammerhead.karooext.internal.ViewEmitter
import io.hammerhead.karooext.models.UpdateGraphicConfig
import io.hammerhead.karooext.models.ViewConfig
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow

class LadderDataField(
    private val karoo: KarooFlows,
    private val weatherProvider: WeatherProvider?,
    private val parent: Job,
    extensionId: String,
) : DataTypeImpl(extensionId, TYPE_ID) {
    override fun startView(context: Context, config: ViewConfig, emitter: ViewEmitter) {
        Log.d(TYPE_ID, "gridSize=${config.gridSize}, viewSize=${config.viewSize}, preview=${config.preview}")
        val scope = CoroutineScope(SupervisorJob(parent) + Dispatchers.Default)
        val state = MutableStateFlow(DashboardState())
        val location = MutableStateFlow<GeoLocation?>(null)
        val weather = DashboardWeather(weatherProvider, SystemClock::elapsedRealtime)
        val renderer = LadderRenderer(config.viewSize)
        emitter.setCancellable { scope.cancel() }
        emitter.onNext(UpdateGraphicConfig(showHeader = false))
        LadderRuntime(
            scope, config.gridSize, config.preview,
            latest = {
                val snapshot = state.value
                val conditions = weather.current()
                snapshot.ladderFrame(DashboardCalculator.wind(
                    snapshot.heading, conditions?.windDirectionDegrees, conditions?.windSpeedMetersPerSecond,
                ))
            },
            subscribe = {
                subscribeDashboardReadings(karoo, scope, state, location)
                startDashboardWeatherUpdates(scope, weather, location)
            },
            submit = { emitter.updateView(renderer.render(context, it)) },
            onFailure = { Log.w(TYPE_ID, "Unable to submit dashboard frame; will retry") },
        ).start()
    }

    companion object {
        const val TYPE_ID = "ladder-full"
    }
}
