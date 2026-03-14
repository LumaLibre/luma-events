package dev.lumas.events.explorer.mile

/**
 * A snapshot of an [ExplorerMile]'s level progress for a player.
 * This is used to track the player's progress towards completing an [ExplorerMile] and to determine when to progress the level of the mile.
 * Depending on the context, this object may or may not mutate the [ExplorerMile] itself.
 */
class ExplorerMileLevelSnapshot(
    val maxQuantity: Int,
    var currentQuantity: Int,
    val maxLevels: Int,
    var currentLevel: Int,
    val levelMultiplier: Double,
) {

    /**
     * Determine if this level snapshot is completed.
     * @return true if the level snapshot is completed, false otherwise.
     */
    fun isCompleted(): Boolean {
        return currentLevel >= maxLevels //&& currentQuantity >= this.getMaxQuantityForLevel(currentLevel)
    }

    /**
     * Attempt to progress the level of this mile's level snapshot.
     * @return true if the level was progressed, false otherwise.
     */
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

    /**
     * Get the maximum [ExplorerMile.quantity] for the current level ([currentLevel]).
     * @return the maximum [ExplorerMile.quantity] for the current level.
     */
    fun getMaxQuantityForCurrentLevel(): Int {
        return getMaxQuantityForLevel(currentLevel)
    }

    /**
     * Get the maximum [ExplorerMile.quantity] for a given level.
     * @param level the level to get the maximum quantity for.
     * @return the maximum [ExplorerMile.quantity] for the given level.
     */
    fun getMaxQuantityForLevel(level: Int): Int {
        if (level < 1) {
            return maxQuantity
        }
        return (maxQuantity * level * levelMultiplier).toInt()
    }

    /**
     * Get the next level after the current level.
     * @return the next level after the current level.
     */
    fun getNextLevel(): Int {
        return currentLevel + 1
    }

    /**
     * Get the number of levels until completion.
     * @return the number of levels until completion.
     */
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