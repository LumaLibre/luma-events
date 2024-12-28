package dev.jsinco.luma.lumaevents.items

import dev.jsinco.luma.lumaitems.LumaItems
import dev.jsinco.luma.lumaitems.api.LumaItemsAPI
import dev.jsinco.luma.lumaitems.manager.CustomItemFunctions
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.inventory.ItemStack

class HolidayCandleItem : CustomItemFunctions() {
    override fun createItem(): Pair<String, ItemStack> {
        return LumaItemsAPI.getInstance().factory()
            .name("<b><red>Holiday Candle")
            .material(Material.RED_CANDLE)
            .lore("A festive candle for the holidays.")
            .persistentData("holiday_candle")
            .buildPair()
    }

    override fun onPlaceBlock(player: Player, event: BlockPlaceEvent) {
        event.isCancelled = true
    }
}