package com.jxguo92.mykarooextension.core.weather

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.StateFlow

/** Polling belongs to the view's scope; cancelling it also cancels an in-flight request. */
fun startDashboardWeatherUpdates(
    scope: CoroutineScope,
    weather: DashboardWeather,
    location: StateFlow<GeoLocation?>,
): Job = scope.launch {
    while (isActive) {
        weather.refresh(location.value)
        delay(1_000)
    }
}
