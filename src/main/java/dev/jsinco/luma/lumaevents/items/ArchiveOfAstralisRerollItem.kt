package dev.jsinco.luma.lumaevents.items

import dev.jsinco.luma.lumaevents.guis.ArchiveReRollGui
import dev.jsinco.luma.lumaevents.utility.Util
import dev.jsinco.luma.lumaitems.api.LumaItemsAPI
import dev.jsinco.luma.lumaitems.items.nests.ArchiveOfAstralisItemNest
import dev.jsinco.luma.lumaitems.manager.CustomItemFunctions
import dev.jsinco.luma.lumaitems.util.tiers.Tier
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import java.util.UUID

class ArchiveOfAstralisRerollItem : CustomItemFunctions() {

    companion object {
        private val confirmedReRoll: MutableList<UUID> = mutableListOf()
    }

    override fun createItem(): Pair<String, ItemStack> {
        return LumaItemsAPI.getInstance().factory()
            .name("<b><#f498f6>Archive</#f498f6></b> <!b><#F7FFC9>of Astralis</#F7FFC9></!b> <gold><b>Re-roll</b></gold>")
            .lore(
                "<gray>Changes the Job and % of a",
                "<gray>single <#F7FFC9>Archive of Astralis</#F7FFC9>.",
                "",
                "<gray>Right click to use."
            )
            .material(Material.WRITABLE_BOOK)
            .persistentData("archivereroll")
            .tier(Tier.WINTER_2024)
            .vanillaEnchants(Enchantment.UNBREAKING to 5)
            .hideEnchants(true)
            .addSpace(false)
            .buildPair()
    }

    override fun onRightClick(player: Player, event: PlayerInteractEvent) {
        val archiveReRollGui = ArchiveReRollGui()
        archiveReRollGui.open(player)
        event.isCancelled = true
    }
}