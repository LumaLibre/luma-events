package dev.jsinco.luma.lumaevents.items

import dev.jsinco.luma.lumaitems.items.ItemFactory
import dev.jsinco.luma.lumaitems.manager.CustomItemFunctions
import dev.jsinco.luma.lumaitems.util.tiers.Tier
import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.inventory.ItemStack

class WinterStampItem : CustomItemFunctions() {

    override fun createItem(): Pair<String, ItemStack> {
        return ItemFactory.builder()
            .name("<b><gradient:#1d7240:#5e9f52:#e4ba58:#f2b054:#f06f3f:#ee4631:#e4352b:#a61e20>Winter Stamp</gradient></b>")
            .lore(
                "A neat little stamp",
                "to mark your holiday",
                "packages with!",
                "",
                "Trade enough of these",
                "in for special rewards."
            )
            .material(Material.RED_DYE)
            .tier(Tier.CHRISTMAS_2025)
            .vanillaEnchants(Enchantment.UNBREAKING to 10)
            .persistentData("winter-stamp")
            .buildPair()
    }

}
