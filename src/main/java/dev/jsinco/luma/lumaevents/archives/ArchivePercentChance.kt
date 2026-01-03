package dev.jsinco.luma.lumaevents.archives

import dev.lumas.lumacore.utility.Logging

// TODO: Move to LumaItems?
enum class ArchivePercentChance(val weight: Int, val actualPercent: Int) {

    FIVE_PERCENT(weight = 15, actualPercent = 5),
    FOUR_PERCENT(weight = 30, actualPercent = 4),
    THREE_PERCENT(weight = 45, actualPercent = 3),
    TWO_PERCENT(weight = 10, actualPercent = 2);


    companion object {
        val TOTAL_WEIGHT = entries.toTypedArray().sumOf { it.weight }

        fun randomByTotalWeight(): ArchivePercentChance {
            val randomValue = (1..TOTAL_WEIGHT).random()
            var cumulativeWeight = 0

            for (percentChance in entries) {
                cumulativeWeight += percentChance.weight
                if (randomValue <= cumulativeWeight) {
                    return percentChance
                }
            }
            Logging.errorLog("Failed to select ArchivePercentChance by weight")
            return THREE_PERCENT // Fallback, should not reach here
        }
    }
}