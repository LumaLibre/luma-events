package dev.jsinco.luma.lumaevents.items

import dev.jsinco.luma.lumaitems.items.ItemFactory
import dev.jsinco.luma.lumaitems.manager.CustomItemFunctions
import dev.jsinco.luma.lumaitems.util.tiers.Tier
import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.inventory.ItemStack

class SummerOpal : CustomItemFunctions() {

    override fun createItem(): Pair<String, ItemStack> {
        return ItemFactory.builder()
            .name("<b><gradient:#ff4e50:#fc913a:#f9d62e:#eae374:#97c753>Summer Opal</gradient></b>")
            .lore(
                "A valuable stone that",
                "gives off a mesmerizing",
                "iridescence.",
                "",
                "You can trade multiple of",
                "these in for rewards."
            )
            .material(Material.RESIN_CLUMP)
            .tier(Tier.SUMMER_2025)
            .vanillaEnchants(Enchantment.UNBREAKING to 10)
            .persistentData("summer-opal-token")
            .buildPair()
    }

    override fun onPlaceBlock(player: Player, event: BlockPlaceEvent) {
        event.isCancelled = true
    }
}
