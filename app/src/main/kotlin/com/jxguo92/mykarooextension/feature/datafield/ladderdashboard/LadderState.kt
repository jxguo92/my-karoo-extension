package com.jxguo92.mykarooextension.feature.datafield.ladderdashboard

import com.jxguo92.mykarooextension.core.dashboard.DashboardDisplay
import com.jxguo92.mykarooextension.core.dashboard.DashboardState
import com.jxguo92.mykarooextension.core.dashboard.Metric
import com.jxguo92.mykarooextension.core.dashboard.WindDisplay

data class LadderFrame(
    val values: DashboardDisplay = DashboardDisplay(),
    val cadenceLights: Int = 0,
    val speedFraction: Double? = null,
    val heartPosition: LadderPosition? = null,
)

fun DashboardState.ladderFrame(wind: WindDisplay = WindDisplay.Unavailable): LadderFrame {
    val values = display(wind)
    return LadderFrame(
        values = values.copy(grade = values.grade.removePrefix("+")),
        cadenceLights = LadderCalculator.cadenceLights(readings[Metric.CADENCE]),
        speedFraction = LadderCalculator.speed(readings[Metric.SPEED]),
        heartPosition = LadderCalculator.heart(readings[Metric.HEART_RATE], maximumHr, restingHr),
    )
}
