package com.jxguo92.mykarooextension.extension

import com.jxguo92.mykarooextension.feature.datafield.rearcogteeth.KarooRearGearStream
import com.jxguo92.mykarooextension.feature.datafield.rearcogteeth.RearCogTeethDataField
import com.jxguo92.mykarooextension.core.karoo.KarooFlowAdapter
import com.jxguo92.mykarooextension.feature.datafield.climberfield1.ClimberField1DataField
import com.jxguo92.mykarooextension.feature.datafield.climberfield2.ClimberField2DataField
import io.hammerhead.karooext.KarooSystemService
import io.hammerhead.karooext.extension.DataTypeImpl

object DataFieldRegistry {
    val typeIds: List<String> = listOf(
        RearCogTeethDataField.TYPE_ID,
        ClimberField1DataField.TYPE_ID,
        ClimberField2DataField.TYPE_ID,
    )

    fun create(
        karooSystem: KarooSystemService,
        extensionId: String,
    ): List<DataTypeImpl> {
        val flows = KarooFlowAdapter(karooSystem)
        return listOf(
            RearCogTeethDataField(KarooRearGearStream(karooSystem), extensionId),
            ClimberField1DataField(flows, extensionId),
            ClimberField2DataField(flows, extensionId),
        )
    }
}
