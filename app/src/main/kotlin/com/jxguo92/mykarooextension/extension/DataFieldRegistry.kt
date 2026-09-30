package com.jxguo92.mykarooextension.extension

import com.jxguo92.mykarooextension.feature.datafield.rearcogteeth.KarooRearGearStream
import com.jxguo92.mykarooextension.feature.datafield.rearcogteeth.RearCogTeethDataField
import com.jxguo92.mykarooextension.core.karoo.KarooFlowAdapter
import com.jxguo92.mykarooextension.feature.datafield.climberfield1.ClimberField1DataField
import com.jxguo92.mykarooextension.feature.datafield.climberfield2.ClimberField2DataField
import io.hammerhead.karooext.KarooSystemService
import io.hammerhead.karooext.extension.DataTypeImpl

import android.content.Context
import com.jxguo92.mykarooextension.core.http.ConnectivityWifiStatus
import com.jxguo92.mykarooextension.core.http.NetworkAwareHttpGet
import com.jxguo92.mykarooextension.core.http.UrlConnectionHttpGet
import com.jxguo92.mykarooextension.core.karoo.KarooHttpGet
import com.jxguo92.mykarooextension.core.karoo.KarooSystemHttpChannel
import com.jxguo92.mykarooextension.core.weather.qweather.QWeatherConfig
import com.jxguo92.mykarooextension.core.weather.qweather.QWeatherProvider
import com.jxguo92.mykarooextension.feature.datafield.concentricdashboard.ConcentricDataField
import kotlinx.coroutines.Job

object DataFieldRegistry {
    val typeIds: List<String> = listOf(
        RearCogTeethDataField.TYPE_ID,
        ClimberField1DataField.TYPE_ID,
        ClimberField2DataField.TYPE_ID,
        ConcentricDataField.TYPE_ID,
    )

    fun create(
        karooSystem: KarooSystemService,
        extensionId: String,
        context: Context,
        parent: Job,
    ): List<DataTypeImpl> {
        val flows = KarooFlowAdapter(karooSystem)
        val weather = QWeatherConfig.fromBuildConfig()?.let { config ->
            QWeatherProvider(config, NetworkAwareHttpGet(
                ConnectivityWifiStatus(context), UrlConnectionHttpGet(),
                KarooHttpGet(KarooSystemHttpChannel(karooSystem)),
            ))
        }
        return listOf(
            RearCogTeethDataField(KarooRearGearStream(karooSystem), extensionId),
            ClimberField1DataField(flows, extensionId),
            ClimberField2DataField(flows, extensionId),
            ConcentricDataField(flows, weather, parent, extensionId),
        )
    }
}
