package com.jxguo92.mykarooextension.core.ui

import android.content.Context
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.RemoteViews
import com.jxguo92.mykarooextension.R
import io.hammerhead.karooext.models.ViewConfig

data class DisplayTriplet(val left: String = "--", val center: String = "--", val right: String = "--")

object CompositeFieldView {
    fun isWide(config: ViewConfig): Boolean = config.gridSize.first == 60

    fun render(context: Context, config: ViewConfig, values: DisplayTriplet): RemoteViews =
        RemoteViews(context.packageName, R.layout.composite_field_view).apply {
            val wide = isWide(config)
            setViewVisibility(R.id.composite_left, if (wide) View.VISIBLE else View.GONE)
            setViewVisibility(R.id.composite_right, if (wide) View.VISIBLE else View.GONE)
            listOf(
                Triple(R.id.composite_left, values.left, 0.80f),
                Triple(R.id.composite_center, values.center, 1.15f),
                Triple(R.id.composite_right, values.right, 0.80f),
            ).forEach { (id, text, scale) ->
                setTextViewText(id, text)
                setContentDescription(id, text)
                setTextViewTextSize(id, TypedValue.COMPLEX_UNIT_SP, config.textSize * scale)
                setInt(id, "setGravity", Gravity.CENTER)
            }
        }
}
