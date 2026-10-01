package com.jxguo92.mykarooextension.core.karoo

import com.jxguo92.mykarooextension.core.dashboard.DashboardState
import io.hammerhead.karooext.models.DataType
import io.hammerhead.karooext.models.StreamState

/** SDK sample conversion stays outside the pure state reducer. */
internal fun DashboardState.onLocationSample(event: StreamState): DashboardState {
    // SDK 1.1.9 documents LOC_SPEED but omits its constant. The FIELD_LOC_*_ID
    // naming follows its other LOCATION keys; retain the literal alias for compatibility.
    // Confirm the speed key/unit on Karoo hardware before treating it as a verified contract.
    val speed = event.fieldValue("FIELD_LOC_SPEED_ID") ?: event.fieldValue("LOC_SPEED")
    return onLocation(event.fieldValue(DataType.Field.LOC_BEARING), speed?.times(3.6))
}
