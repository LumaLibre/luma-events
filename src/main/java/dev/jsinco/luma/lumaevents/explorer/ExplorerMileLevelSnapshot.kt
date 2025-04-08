package dev.jsinco.luma.lumaevents.explorer

data class ExplorerMileLevelSnapshot(
    var currentQuantity: Int,
    val quantity: Int,
    val levels: Int,
    val levelMultiplier: Double,
) {

    fun getCurrentLevelQuantity(): Int {
        if (currentQuantity == 0) {
            return quantity
        } else if (currentQuantity >= levels) {
            return -1
        }

        return (currentQuantity * quantity).times(levelMultiplier).toInt()
    }

    fun getNextLevelQuantity(): Int {
        if (currentQuantity == 0) {
            return quantity
        } else if (currentQuantity >= levels) {
            return -1
        }

        return (currentQuantity + 1 * quantity).times(levelMultiplier).toInt()
    }

    fun getCurrentLevel(): Int {
        if (currentQuantity == 0) {
            return 0
        }

        val level = currentQuantity / quantity

        return level.coerceIn(0, levels - 1)
    }


    fun getMaximumQuantity(): Int {
        return this.getQuantityForLevel(levels)
    }

    fun getQuantityForLevel(level: Int): Int {
        return (quantity * level).times(levelMultiplier).toInt()
    }
}