package dev.jsinco.luma.lumaevents.guis

import dev.jsinco.luma.lumacore.manager.guis.AbstractGui
import dev.jsinco.luma.lumaevents.EventMain
import dev.jsinco.luma.lumaevents.utility.Util
import dev.jsinco.luma.lumaitems.LumaItems
import dev.jsinco.luma.lumaitems.api.LumaItemsAPI
import dev.jsinco.luma.lumaitems.items.nests.ArchiveOfAstralisItemNest
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.HumanEntity
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType

class ArchiveReRollGui : AbstractGui {


    companion object {
        private val plugin: EventMain = EventMain.getInstance()
        private val EMPTY_SLOTS: List<Int> = listOf(11, 13, 15)
        private val BORDER: ItemStack = Util.createBasicItem(Material.LIGHT_BLUE_STAINED_GLASS_PANE,"", false, listOf(), listOf("gui-item"))
        private val CONFIRM_BUTTON: ItemStack = Util.createBasicItem(Material.LIME_STAINED_GLASS_PANE,"<green><b>Confirm", true, listOf(), listOf("gui-item", "confirm"))
    }

    val gui: Inventory = Bukkit.createInventory(this, 27, Util.color("<b><#f498f6>Archive</#f498f6></b> <!b><#F7FFC9>of Astralis</#F7FFC9></!b> <gold><b>Re-roll</b></gold>"))

    init {
        for (slot in gui.contents.indices) {
            if (!EMPTY_SLOTS.contains(slot)) {
                gui.setItem(slot, BORDER)
            }
        }
        gui.setItem(16, CONFIRM_BUTTON)
    }


    override fun onInventoryClick(event: InventoryClickEvent) {

        val clickedItem = event.currentItem ?: return
        if (clickedItem.itemMeta?.persistentDataContainer?.has(NamespacedKey(plugin, "gui-item"), PersistentDataType.SHORT) == true) {
            event.isCancelled = true
        }
        val i = event.inventory
        val p = event.whoClicked as Player

        val archive = i.getItem(11) ?: return

        val reRollItem = i.getItem(13) ?: return
        if (!isReRollItem(reRollItem)) {
            Util.sendMsg(p, "You must have a <red>Re-roll</red> item in the middle slot.")
            return
        }

        var jobType: ArchiveOfAstralisItemNest.JobType? = null
        for (job in ArchiveOfAstralisItemNest.JobType.entries) {
            if (archive.itemMeta?.persistentDataContainer?.has(NamespacedKey(LumaItems.getInstance(), job.key), PersistentDataType.SHORT) == true) {
                jobType = job
                break
            }
        }
        if (jobType == null) {
            // not a valid archive
            Util.sendMsg(p, "You must have a valid <red>Archive</red> in the left slot.")
            return
        }

//        val level = archive.itemMeta?.persistentDataContainer?.get(NamespacedKey(LumaItems.getInstance(), jobType.key), PersistentDataType.SHORT) ?: 2
//        if (level >= 5) {
//            Util.sendMsg(p, "You cannot re-roll an <red>Archive</red> that is already at level 5.")
//            return
//        }

        val customItem = LumaItemsAPI.getInstance().getCustomItem(ArchiveOfAstralisItemNest.JobType.entries.random().key) ?: return
        val newArchive = customItem.createItem().second

        reRollItem.amount -= 1
        i.setItem(15, newArchive)
        i.setItem(11, null)

        Util.sendMsg(p, "You have re-rolled your <red>Archive</red>.")
    }

    override fun onInventoryClose(event: InventoryCloseEvent) {
        for (emptySlot in EMPTY_SLOTS) {
            if (event.inventory.getItem(emptySlot) != null) {
                Util.giveItem(event.player as Player, event.inventory.getItem(emptySlot)!!)
            }
        }
    }

    override fun getInventory(): Inventory {
        return gui
    }

    override fun open(player: HumanEntity) {
        player.openInventory(gui)
    }

    private fun isReRollItem(item: ItemStack): Boolean {
        return item.itemMeta?.persistentDataContainer?.has(NamespacedKey(LumaItems.getInstance(), "archivereroll")) ?: false
    }
}
