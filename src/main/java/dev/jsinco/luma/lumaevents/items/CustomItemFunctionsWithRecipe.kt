package dev.jsinco.luma.lumaevents.items

import dev.lumas.lumaitems.model.CustomItemFunctions
import org.bukkit.NamespacedKey
import org.bukkit.inventory.Recipe

abstract class CustomItemFunctionsWithRecipe : CustomItemFunctions() {
    abstract fun recipe(): Pair<NamespacedKey, Recipe>
}
