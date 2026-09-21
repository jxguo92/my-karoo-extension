package com.jxguo92.mykarooextension.feature.datafield.rearcogteeth

import android.content.Context
import android.util.TypedValue
import android.view.Gravity
import android.widget.RemoteViews
import com.jxguo92.mykarooextension.R
import io.hammerhead.karooext.models.ViewConfig

object RearCogTeethView {
    fun render(
        context: Context,
        config: ViewConfig,
        text: String,
    ): RemoteViews = RemoteViews(context.packageName, R.layout.rear_cog_teeth_view).apply {
        setTextViewText(R.id.rear_cog_teeth_value, text)
        setContentDescription(R.id.rear_cog_teeth_value, text)
        setTextViewTextSize(
            R.id.rear_cog_teeth_value,
            TypedValue.COMPLEX_UNIT_SP,
            config.textSize.toFloat(),
        )
        setInt(
            R.id.rear_cog_teeth_value,
            "setGravity",
            Gravity.CENTER_VERTICAL or config.alignment.horizontalGravity,
        )
    }

    private val ViewConfig.Alignment.horizontalGravity: Int
        get() = when (this) {
            ViewConfig.Alignment.LEFT -> Gravity.START
            ViewConfig.Alignment.CENTER -> Gravity.CENTER_HORIZONTAL
            ViewConfig.Alignment.RIGHT -> Gravity.END
        }
}
