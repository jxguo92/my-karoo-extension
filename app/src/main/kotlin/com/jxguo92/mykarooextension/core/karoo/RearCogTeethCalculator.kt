package com.jxguo92.mykarooextension.core.karoo

import kotlin.math.roundToInt

data class RearCogTeethReading(
    val teeth: Int,
    val inferred: Boolean,
) {
    val displayText: String = "${if (inferred) "≈" else ""}${teeth}T"
}

object RearCogTeethCalculator {
    private val xg1371TeethByGear = listOf(46, 38, 32, 28, 24, 21, 19, 17, 15, 13, 12, 11, 10)

    fun resolve(
        reportedTeeth: Double?,
        rearGearIndex: Double?,
        rearGearCount: Double?,
    ): RearCogTeethReading? {
        reportedTeeth.asPositiveInteger()?.let { teeth ->
            return RearCogTeethReading(teeth = teeth, inferred = false)
        }

        val gearIndex = rearGearIndex.asPositiveInteger() ?: return null
        val gearCount = rearGearCount?.asPositiveInteger()
        if (rearGearCount != null && gearCount != xg1371TeethByGear.size) return null

        val teeth = xg1371TeethByGear.getOrNull(gearIndex - 1) ?: return null
        return RearCogTeethReading(teeth = teeth, inferred = true)
    }

    private fun Double?.asPositiveInteger(): Int? {
        if (this == null || !isFinite()) return null
        val rounded = roundToInt()
        return rounded.takeIf { it > 0 && this == rounded.toDouble() }
    }
}
