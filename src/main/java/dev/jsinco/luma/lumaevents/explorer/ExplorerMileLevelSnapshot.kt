package dev.jsinco.luma.lumaevents.explorer

import dev.jsinco.luma.lumaevents.utility.Util

class ExplorerMileLevelSnapshot(
    val maxQuantity: Int,
    var currentQuantity: Int,
    val maxLevels: Int,
    var currentLevel: Int,
    val levelMultiplier: Double,
) {

    fun isCompleted(): Boolean {
        return currentLevel >= maxLevels //&& currentQuantity >= this.getMaxQuantityForLevel(currentLevel)
    }

    fun tryProgressLevel(): Boolean {
        if (currentLevel >= maxLevels) {
            return false
        }


        if (currentQuantity >= this.getMaxQuantityForLevel(currentLevel)) {
            //Util.log("Progressing level $currentLevel to ${currentLevel + 1} with quantity $currentQuantity")
            currentLevel++
            currentQuantity = 0
            return true
        }
        return false
    }

    fun getMaxQuantityForCurrentLevel(): Int {
        return getMaxQuantityForLevel(currentLevel)
    }

    fun getMaxQuantityForLevel(level: Int): Int {
        if (level < 1) {
            return maxQuantity
        }
        return (maxQuantity * level * levelMultiplier).toInt()
    }

    fun getNextLevel(): Int {
        return currentLevel + 1
    }

    fun getLevelsUntilCompletion(): Int {
        var totalLevelsUntilCompletion = 0
        for (i in 0 .. maxLevels) {
            if (i > currentLevel) {
                totalLevelsUntilCompletion++
            }
        }
        return totalLevelsUntilCompletion
    }
}