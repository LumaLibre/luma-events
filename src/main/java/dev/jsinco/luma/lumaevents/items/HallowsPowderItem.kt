package dev.jsinco.luma.lumaevents.items

import dev.jsinco.luma.lumaitems.items.ItemFactory
import dev.jsinco.luma.lumaitems.manager.CustomItemFunctions
import dev.jsinco.luma.lumaitems.util.tiers.Tier
import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.inventory.ItemStack

class HallowsPowderItem : CustomItemFunctions() {

    override fun createItem(): Pair<String, ItemStack> {
        return ItemFactory.builder()
            .name("<b><gradient:#602749:#b14623:#f6921d>Hallows Powder</gradient></b>")
            .lore(
                "Shimmering dust garnered",
                "from lanterns lit on",
                "Halloween.",
                "",
                "Trade enough of it for",
                "rare rewards."
            )
            .material(Material.GLOWSTONE_DUST)
            .tier(Tier.HALLOWEEN_2025)
            .vanillaEnchants(Enchantment.UNBREAKING to 10)
            .persistentData("hallows-powder")
            .buildPair()
    }

}
