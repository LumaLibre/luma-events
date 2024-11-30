package dev.jsinco.luma.shop

import dev.jsinco.luma.Util
import org.bukkit.inventory.ItemStack

class ShopItem (
    val item: ItemStack,
    val command: String,
    val tokenPrice: Int,
    val slot: Int
) {

    val displayItem: ItemStack = item.clone()

    init {
        val itemMeta = displayItem.itemMeta
        val lore = itemMeta.lore() ?: mutableListOf()
        lore.addAll(
            Util.color(
                listOf(
                    "",
                    "<#645b82>• Click to exchange for",
                    "<#645b82>• <#cc7870>${String.format("%,d", tokenPrice)} <#645b82>tokens."
                )
            )
        )
        itemMeta.lore(lore)
        displayItem.itemMeta = itemMeta
    }
}