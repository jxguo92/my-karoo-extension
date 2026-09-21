package com.jxguo92.mykarooextension.extension

import com.jxguo92.mykarooextension.feature.datafield.rearcogteeth.KarooRearGearStream
import com.jxguo92.mykarooextension.feature.datafield.rearcogteeth.RearCogTeethDataField
import io.hammerhead.karooext.KarooSystemService
import io.hammerhead.karooext.extension.DataTypeImpl

object DataFieldRegistry {
    val typeIds: List<String> = listOf(RearCogTeethDataField.TYPE_ID)

    fun create(
        karooSystem: KarooSystemService,
        extensionId: String,
    ): List<DataTypeImpl> = listOf(
        RearCogTeethDataField(KarooRearGearStream(karooSystem), extensionId),
    )
}
