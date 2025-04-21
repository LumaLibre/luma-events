package dev.jsinco.luma.lumaevents.tokens

import dev.jsinco.luma.lumaevents.utility.Util
import dev.jsinco.luma.lumaitems.items.ItemFactory
import dev.jsinco.luma.lumaitems.manager.CustomItemFunctions
import dev.jsinco.luma.lumaitems.util.tiers.Tier
import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.inventory.ItemStack

class EasterBasketToken : CustomItemFunctions() {


    override fun createItem(): Pair<String, ItemStack> {
        return ItemFactory.builder()
            .name("<b><gradient:#d798d4:#f4d8ea:#94675d:#FCE78F:#80F3B2>Easter Basket</gradient></b>")
            .lore("<gray>Carefully gathered by", "<gray>hand, or paw...")
            .material(Material.PLAYER_HEAD)
            .b64PHead("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYmU2ZDhjNjk4OGNkMGQxOTk5NzAzMDZhNGQ3NTY0NmQ5NzczZDcxMGViMjE5MzVkYjc3M2ViMjEyMTY3NjAyYiJ9fX0=")
            .tier(Tier.EASTER_2025)
            .vanillaEnchants(Enchantment.UNBREAKING to 10)
            .persistentData("easter-basket-token")
            .buildPair()
    }

    override fun onPlaceBlock(player: Player, event: BlockPlaceEvent) {
        Util.sendMsg(player, "You can't build with that, silly wabbit!")
        event.isCancelled = true
    }

}
