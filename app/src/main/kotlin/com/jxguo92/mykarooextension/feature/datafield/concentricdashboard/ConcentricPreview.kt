package com.jxguo92.mykarooextension.feature.datafield.concentricdashboard

object ConcentricPreview {
    val state = ConcentricState(
        readings = mapOf(
            Metric.POWER to 245.0, Metric.CADENCE to 88.0, Metric.HEART_RATE to 156.0,
            Metric.SPEED to 28.6, Metric.DISTANCE to 42.7, Metric.GRADE to 4.8,
            Metric.AVG_SPEED to 27.4, Metric.AVG_HR to 148.0,
            Metric.AVG_POWER to 198.0, Metric.NORMALIZED_POWER to 214.0,
        ),
        gear = "6/13", heading = 315.0, ftp = 200.0, maximumHr = 190.0, restingHr = 50.0,
    )
    val display = state.display(ConcentricCalculator.wind(315.0, 270.0, 4.0))
}
