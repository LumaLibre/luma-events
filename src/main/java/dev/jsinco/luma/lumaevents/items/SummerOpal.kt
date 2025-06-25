package dev.jsinco.luma.lumaevents.items

import dev.jsinco.luma.lumaitems.items.ItemFactory
import dev.jsinco.luma.lumaitems.manager.CustomItemFunctions
import dev.jsinco.luma.lumaitems.util.tiers.Tier
import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.inventory.ItemStack

class SummerOpal : CustomItemFunctions() {

    override fun createItem(): Pair<String, ItemStack> {
        return ItemFactory.builder()
            .name("<b><gradient:#DC8BE5:#F6FF5C>Summer Opal</gradient></b>")
            .lore(
                "<gray>A valuable stone that",
                "<gray>gives off a mesmerizing",
                "<gray>iridescence.",
                "",
                "<gray>You can trade multiple of",
                "<gray>these in for special rewards."
            )
            .material(Material.RESIN_CLUMP)
            .tier(Tier.SUMMER_2025)
            .vanillaEnchants(Enchantment.UNBREAKING to 10)
            .persistentData("summer-opal-token")
            .buildPair()
    }
}
