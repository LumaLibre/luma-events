package dev.lumas.events.items

import dev.lumas.lumaitems.model.item.CustomItemFunctions
import org.bukkit.NamespacedKey
import org.bukkit.inventory.Recipe

abstract class CustomItemFunctionsWithRecipe : CustomItemFunctions() {
    abstract fun recipe(): Pair<NamespacedKey, Recipe>
}
