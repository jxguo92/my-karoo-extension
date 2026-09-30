package com.jxguo92.mykarooextension.feature.datafield.concentricdashboard

import android.app.Instrumentation
import android.app.Activity
import android.graphics.Bitmap
import android.os.Bundle
import java.io.File

/** Runs the production Android Canvas renderer and exports deterministic visual fixtures. */
class ConcentricRenderInstrumentation : Instrumentation() {
    override fun onCreate(arguments: Bundle?) {
        super.onCreate(arguments)
        start()
    }

    override fun onStart() {
        try {
            val output = File(targetContext.filesDir, "concentric-previews").apply { mkdirs() }
            val normal = ConcentricPreview.display
            val long = normal.copy(power = "123456789", speed = "12345.6", heartRate = "123456", gear = "123/456",
                distance = "123456.7", grade = "+12345.6", averageSpeed = "12345.6", averageHr = "123456",
                averagePower = "123456789", normalizedPower = "123456789")
            for ((name, display) in listOf("preview" to normal, "missing" to ConcentricDisplay(), "long" to long)) {
                for (night in listOf(false, true)) {
                    val frame = ConcentricFrame(display, night)
                    val bitmap = ConcentricView.bitmap(480 to 638, frame)
                    check(bitmap.width == 480 && bitmap.height == 638)
                    File(output, "$name-${if (night) "night" else "day"}.png").outputStream().use {
                        check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it))
                    }
                    // RemoteViews must inflate with the real framework and each call owns its pixels.
                    ConcentricView.render(targetContext, 480 to 638, frame).apply(targetContext, null)
                    val firstPixel = bitmap.getPixel(0, 0)
                    ConcentricView.bitmap(480 to 638, frame.copy(night = !night))
                    check(bitmap.getPixel(0, 0) == firstPixel)
                }
            }
            for (size in listOf(960 to 1276, 240 to 319, 480 to 100, 0 to 0, -1 to 5)) {
                val bitmap = ConcentricView.bitmap(size, ConcentricFrame(normal, false))
                check(bitmap.width <= 480 && bitmap.height <= 638)
                if (size.first > 0 && size.second > 0) check(bitmap.width <= size.first && bitmap.height <= size.second)
            }
            val blank = ConcentricView.bitmap(480 to 638, ConcentricFrame(null, false))
            check(blank.getPixel(240, 319) == 0)
            finish(Activity.RESULT_OK, Bundle().apply { putString("stream", "Canvas, RemoteViews, sizes and independent bitmaps passed; previews: $output\n") })
        } catch (failure: Throwable) {
            finish(Activity.RESULT_CANCELED, Bundle().apply { putString("stream", failure.stackTraceToString()) })
        }
    }
}
