package dev.lumas.events.items

import dev.lumas.lumaitems.model.item.CustomItemFunctions
import dev.lumas.lumaitems.model.item.ItemFactory
import dev.lumas.lumaitems.util.Tier
import dev.lumas.lumaitems.util.extensions.isMatchingItem
import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.inventory.ItemStack

class WaxcapShroomItem : CustomItemFunctions() {

    override fun createItem(): Pair<String, ItemStack> {
        return ItemFactory.builder()
            .name("<b><gradient:#3f2615:#775d46:#f0af91:#fecdbb:#ead3c1>Waxcap Shroom</gradient></b>")
            .lore(
                "A gnarly little mush-",
                "room that has almost",
                "no texture to it.",
                "",
                "Craft <#ead3c1>4</#ead3c1> of these into",
                "a different item."
            )
            .material(Material.BROWN_MUSHROOM)
            .tier(Tier.WONDERLAND_2026)
            .vanillaEnchants(Enchantment.UNBREAKING to 10)
            .persistentData("waxcap-shroom")
            .buildPair()
    }

    override fun onPlaceBlock(player: Player, event: BlockPlaceEvent) {
        val item = event.itemInHand
        if (item.isMatchingItem("waxcap-shroom")) {
            event.isCancelled = true
        }
    }

}
