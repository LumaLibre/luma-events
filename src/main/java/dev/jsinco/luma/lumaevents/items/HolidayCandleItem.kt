package dev.jsinco.luma.lumaevents.items

import dev.jsinco.luma.lumaitems.api.LumaItemsAPI
import dev.jsinco.luma.lumaitems.manager.CustomItemFunctions
import dev.jsinco.luma.lumaitems.util.tiers.Tier
import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.inventory.ItemStack

class HolidayCandleItem : CustomItemFunctions() {
    override fun createItem(): Pair<String, ItemStack> {
        return LumaItemsAPI.getInstance().factory()
            .name("<b><#F14452>H<#E95257>o<#E2605C>l<#DA6E61>i<#D27D66>d<#CB8B6B>a<#C39970>y <#B3B57B>C<#ACC380>a<#A4D285>n<#9CE08A>d<#95EE8F>l<#8DFC94>e")
            .material(Material.RED_CANDLE)
            .lore("<gray>A festive candle", "<gray>for the holidays.")
            .persistentData("holiday_candle")
            .vanillaEnchants(Enchantment.UNBREAKING to 10)
            .hideEnchants(true)
            .tier(Tier.WINTER_2024)
            .buildPair()
    }

    override fun onPlaceBlock(player: Player, event: BlockPlaceEvent) {
        event.isCancelled = true
    }
}