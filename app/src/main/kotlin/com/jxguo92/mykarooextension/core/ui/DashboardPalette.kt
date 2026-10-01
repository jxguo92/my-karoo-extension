package com.jxguo92.mykarooextension.core.ui

import android.graphics.Color

class DashboardPalette(val night: Boolean) {
    fun color(dark: String, light: String) = Color.parseColor(if (night) dark else light)
    val background = color("#111b22", "#e8edef")
    val ink = color("#eef5f5", "#10181c")
    val muted = color("#94aeb6", "#5b6f78")
    val gear = color("#d7e6e8", "#3a4a52")
    val power = color("#f7fbfa", "#0b1216")
    val zones = (if (night) listOf("#55c99c", "#5cbce5", "#4280ea", "#ebcb52", "#ff8a5c", "#ed547a", "#a774f6")
        else listOf("#16a076", "#1790c8", "#2c63d6", "#c29100", "#e45f1f", "#d4305c", "#8450e0")).map(Color::parseColor)
    val cadence = (if (night) listOf("#edf2ed", "#69d697", "#f36b65") else listOf("#2c363d", "#1c9a55", "#d93a36")).map(Color::parseColor)
    val winds = (if (night) listOf("#edf2ed", "#4fd1b5", "#ebcb52", "#f36b65") else listOf("#5b6f78", "#0f9a83", "#a67c00", "#d93a36")).map(Color::parseColor)
    val idle = color("#27313a", "#d5dce0")
    val tick = color("#7f8e96", "#6c7b83")
    val activeTick = color("#f4f8f8", "#10181c")
    val track = color("#3b4249", "#d2d9dd")
    val cadenceTick = color("#eef3f4", "#44525a")
    val needle = color("#ffffff", "#0b1216")
}
