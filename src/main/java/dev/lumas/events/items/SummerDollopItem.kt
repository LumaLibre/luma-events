package dev.lumas.events.items

import dev.lumas.lumaitems.model.item.CustomItemFunctions
import dev.lumas.lumaitems.model.item.ItemFactory
import dev.lumas.lumaitems.util.Tier
import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.inventory.ItemStack

class SummerDollopItem : CustomItemFunctions() {

    override fun createItem(): Pair<String, ItemStack> {
        return ItemFactory.builder()
            .name("<b><gradient:#487bd0:#6decea:#edf2dd:#f682ca:#FFFE5E>Summer Dollop</gradient></b>")
            .lore(
                "A delightful soft lump",
                "of pineapple cream",
                "perfect for crafting",
                "summer deserts.",
                "",
                "<#FFFE5E>Trade</#FFFE5E> enough of these",
                "in for special rewards!"
            )
            .material(Material.YELLOW_DYE)
            .tier(Tier.LUMARINE_2026)
            .vanillaEnchants(Enchantment.UNBREAKING to 10)
            .persistentData("summer-dollop")
            .buildPair()
    }

}
