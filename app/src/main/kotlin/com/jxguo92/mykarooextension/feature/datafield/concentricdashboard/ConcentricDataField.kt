package com.jxguo92.mykarooextension.feature.datafield.concentricdashboard

import com.jxguo92.mykarooextension.core.dashboard.*
import com.jxguo92.mykarooextension.core.weather.DashboardWeather
import com.jxguo92.mykarooextension.core.karoo.subscribeDashboardReadings

import android.content.Context
import android.content.res.Configuration
import android.os.SystemClock
import android.util.Log
import com.jxguo92.mykarooextension.core.karoo.KarooFlows
import com.jxguo92.mykarooextension.core.weather.GeoLocation
import com.jxguo92.mykarooextension.core.weather.WeatherProvider
import com.jxguo92.mykarooextension.core.weather.startDashboardWeatherUpdates
import io.hammerhead.karooext.extension.DataTypeImpl
import io.hammerhead.karooext.internal.Emitter
import io.hammerhead.karooext.internal.ViewEmitter
import io.hammerhead.karooext.models.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow

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
        val state = MutableStateFlow(DashboardState())
        val location = MutableStateFlow<GeoLocation?>(null)
        val weather = DashboardWeather(weatherProvider, SystemClock::elapsedRealtime)
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
                snapshot.display(DashboardCalculator.wind(snapshot.heading, conditions?.windDirectionDegrees, conditions?.windSpeedMetersPerSecond))
            },
            subscribe = {
                subscribeDashboardReadings(karoo, scope, state, location)
                startDashboardWeatherUpdates(scope, weather, location)
            },
            submit = { emitter.updateView(ConcentricView.render(context, config.viewSize, it)) },
            onFailure = { Log.w(TYPE_ID, "Unable to submit dashboard frame; will retry") },
        ).start()
    }

    companion object {
        const val TYPE_ID = "concentric-dashboard"
    }
}
