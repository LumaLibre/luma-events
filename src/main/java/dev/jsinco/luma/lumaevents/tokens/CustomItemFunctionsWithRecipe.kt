package dev.jsinco.luma.lumaevents.tokens

import dev.jsinco.luma.lumaitems.manager.CustomItemFunctions
import org.bukkit.NamespacedKey
import org.bukkit.inventory.Recipe

abstract class CustomItemFunctionsWithRecipe : CustomItemFunctions() {
    abstract fun recipe(): Pair<NamespacedKey, Recipe>
}
