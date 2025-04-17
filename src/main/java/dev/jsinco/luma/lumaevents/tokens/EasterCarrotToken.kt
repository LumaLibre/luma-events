package dev.jsinco.luma.lumaevents.tokens

import dev.jsinco.luma.lumaevents.utility.Util
import dev.jsinco.luma.lumaitems.api.LumaItemsAPI
import dev.jsinco.luma.lumaitems.items.ItemFactory
import dev.jsinco.luma.lumaitems.manager.CustomItemFunctions
import dev.jsinco.luma.lumaitems.util.tiers.Tier
import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerItemConsumeEvent
import org.bukkit.inventory.ItemStack

class EasterCarrotToken : CustomItemFunctions() {


    override fun createItem(): Pair<String, ItemStack> {
        return ItemFactory.builder()
            .name("<b><gradient:#e48717:#FFC243:#C4D155:#348309>Easter Carrot</gradient></b>")
            .lore("<gray>This item needs a", "<gray>description.")
            .material(Material.GOLDEN_CARROT)
            .tier(Tier.EASTER_2025)
            .vanillaEnchants(Enchantment.KNOCKBACK to 8)
            .persistentData("easter-carrot-token")
            .buildPair()
    }

    override fun onConsumeItem(player: Player, event: PlayerItemConsumeEvent) {
        Util.sendMsg(player, "You can't eat that, silly wabbit!")
        event.isCancelled = true
    }
}
