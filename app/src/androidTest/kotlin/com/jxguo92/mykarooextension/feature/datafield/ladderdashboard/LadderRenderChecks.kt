package com.jxguo92.mykarooextension.feature.datafield.ladderdashboard

import android.content.Context
import android.graphics.Bitmap
import android.widget.ImageView
import com.jxguo92.mykarooextension.R
import com.jxguo92.mykarooextension.core.dashboard.WindDisplay
import java.io.File

/** Runs with the repository's existing Canvas instrumentation runner. */
object LadderRenderChecks {
    fun run(context: Context) {
        val output = File(context.filesDir, "ladder-previews").apply { mkdirs() }
        val normal = LadderPreview.frame
        val long = normal.copy(values = normal.values.copy(
            power = "123456789", speed = "12345.6", heartRate = "123456", gear = "123/456",
            grade = "-12345.6", averagePower = "123456789", averageSpeed = "12345.6",
            averageHr = "123456", normalizedPower = "123456789", distance = "123456.7",
        ))
        val renderer = LadderRenderer(480 to 638)
        var reused: Bitmap? = null
        for ((name, frame) in listOf("preview" to normal, "missing" to LadderFrame(),
            "calm" to normal.copy(values = normal.values.copy(wind = WindDisplay.Calm)), "long" to long)) {
            val bitmap = renderer.bitmap(frame)
            check(bitmap.width == 480 && bitmap.height == 638)
            if (name == "missing") {
                val calm = LadderRenderer(480 to 638).bitmap(frame.copy(values = frame.values.copy(wind = WindDisplay.Calm)))
                check((106..144).any { x -> (103..137).any { y -> bitmap.getPixel(x, y) != calm.getPixel(x, y) } })
                check(bitmap.getPixel(355, 120) == calm.getPixel(355, 120))
            }
            if (reused != null) check(bitmap === reused)
            reused = bitmap
            File(output, "$name.png").outputStream().use {
                check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it))
            }
            val view = renderer.render(context, frame).apply(context, null)
            check(view.findViewById<ImageView>(R.id.ladder_dashboard_image).drawable != null)
        }
        val blank = renderer.render(context, null).apply(context, null)
        check(blank.findViewById<ImageView>(R.id.ladder_dashboard_image).drawable == null)
        for (size in listOf(960 to 1276, 240 to 319, 480 to 100, 0 to 0, -1 to 5, 600 to 638)) {
            val bitmap = LadderRenderer(size).bitmap(normal)
            val expected = if (size.first > 0 && size.second > 0) size else (480 to 638)
            check(bitmap.width == expected.first && bitmap.height == expected.second)
            check(bitmap.getPixel(0, 0) == android.graphics.Color.parseColor("#111b22"))
        }
    }
}
