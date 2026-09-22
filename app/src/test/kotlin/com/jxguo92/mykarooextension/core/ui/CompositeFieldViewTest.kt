package com.jxguo92.mykarooextension.core.ui

import com.jxguo92.mykarooextension.feature.datafield.climberfield1.ClimberField1DataField
import com.jxguo92.mykarooextension.feature.datafield.climberfield2.ClimberField2DataField
import io.hammerhead.karooext.models.ViewConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CompositeFieldViewTest {
    @Test fun `only width sixty uses three columns`() {
        assertTrue(CompositeFieldView.isWide(config(60)))
        assertFalse(CompositeFieldView.isWide(config(30)))
        assertFalse(CompositeFieldView.isWide(config(61)))
    }

    @Test fun `previews contain fixed center readings`() {
        assertEquals(DisplayTriplet("2.3 km", "87%", "186 m"), ClimberField1DataField.PREVIEW)
        assertEquals(DisplayTriplet("156 bpm", "92 rpm", "≈21T"), ClimberField2DataField.PREVIEW)
    }

    private fun config(width: Int) = ViewConfig(
        gridSize = width to 20,
        viewSize = 300 to 100,
        textSize = 20,
        alignment = ViewConfig.Alignment.LEFT,
        boundariesEnabled = false,
        preview = true,
    )
}
