package dev.jsinco.luma.lumaevents.archives.guis

import dev.jsinco.luma.lumacore.manager.guis.AbstractGui
import dev.jsinco.luma.lumacore.utility.Logging
import dev.jsinco.luma.lumacore.utility.Text
import dev.jsinco.luma.lumaevents.EventMain
import dev.jsinco.luma.lumaevents.archives.ArchivePercentChance
import dev.jsinco.luma.lumaevents.utility.Util
import dev.jsinco.luma.lumaitems.LumaItems
import dev.jsinco.luma.lumaitems.api.LumaItemsAPI
import dev.jsinco.luma.lumaitems.items.misc.jobs.ArchiveOfAstralisItemNest
import net.milkbowl.vault.economy.Economy
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.HumanEntity
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType

// re-added from winter 2024
class ArchiveReRollGui : AbstractGui() {


    companion object {
        private val plugin: EventMain = EventMain.getInstance()
        private const val BASE_COST = 3_000_000.0
        private const val COST_AMPLIFIER = 1_000_000.0
        private const val MAX_COST = 5_000_000.0
        private const val INFO_BOOK_SLOT = 11
        private const val ARCHIVE_SLOT = 13
        private const val CONFIRM_BUTTON_SLOT = 14

        private val GUI_ITEM_KEY = NamespacedKey(plugin, "gui-item")
        private val CONFIRM_KEY = NamespacedKey(plugin, "confirm")
        private val ARCHIVE_ROLLS_KEY = NamespacedKey(LumaItems.getInstance(), "archive-rolls")

        private val BORDER: ItemStack = Util.createBasicItem(Material.BLUE_STAINED_GLASS_PANE,"", false, listOf(), listOf("gui-item"))
        private val CONFIRM_BUTTON: ItemStack = Util.createBasicItem(Material.LIME_STAINED_GLASS_PANE,"<green><b>Confirm", true, listOf(), listOf("gui-item", "confirm"))
        private val INFO_BOOK = Util.createItem(Material.BOOK) { meta ->
            meta.displayName(Text.mmNoItalic("<b><#b986f9>Archive Re-Rolling Station Info"))
            meta.lore(Text.mmlNoItalic(
                "<gray>Use this station to re-roll",
                "<gray>the level of your Archive of Astralis.",
                "",
                "<gold>Cost to Re-Roll:",
                "<gold>Base Cost: <yellow>${BASE_COST.formatCost()}",
                "<gold>Cost Increase per Roll: <yellow>${COST_AMPLIFIER.formatCost()}",
                "<gold>Maximum Cost: <yellow>${MAX_COST.formatCost()}",
                "",
                "<aqua>Place your Archive in the center",
                "<aqua>slot and click the Confirm button",
                "<aqua>to re-roll its level."
            ))
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS)
            meta.addEnchant(Enchantment.UNBREAKING, 1, true)
        }
    }

    val gui: Inventory = Bukkit.createInventory(this, 27, Util.color("<b><#b986f9>Archive Re-Rolling Station"))
    val economy: Economy = Bukkit.getServicesManager().getRegistration(Economy::class.java)?.provider ?: throw IllegalStateException("Economy service not found. Is Vault installed?")

    init {
        for (slot in gui.contents.indices) {
            if (slot != ARCHIVE_SLOT) {
                gui.setItem(slot, BORDER)
            }
        }
        gui.setItem(INFO_BOOK_SLOT, INFO_BOOK)
        gui.setItem(CONFIRM_BUTTON_SLOT, CONFIRM_BUTTON)
    }


    override fun onInventoryClick(event: InventoryClickEvent) {

        val clickedItem = event.currentItem ?: return
        if (clickedItem.itemMeta?.persistentDataContainer?.has(GUI_ITEM_KEY, PersistentDataType.SHORT) == true) {
            event.isCancelled = true
        }

        if (clickedItem.itemMeta?.persistentDataContainer?.has(CONFIRM_KEY, PersistentDataType.SHORT) != true) {
            return
        }

        val inv = event.inventory
        val player = event.whoClicked as Player

        val archive = inv.getItem(ARCHIVE_SLOT) ?: return
        val rolls = archive.getArchiveRolls()
        val cost = (BASE_COST + (rolls * COST_AMPLIFIER)).coerceAtMost(MAX_COST)

        if (economy.getBalance(player) < cost) {
            Util.sendMsg(player, "You need <red>${cost.formatCost()}</red> to re-roll this Archive.")
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
            Util.sendMsg(player, "You must have a valid <red>Archive</red> in the left slot.")
            return
        }

        val level = archive.getArchiveLevel(jobType)
        if (level >= 5) {
            Util.sendMsg(player, "You cannot re-roll an <red>Archive</red> that is already at level 5%.")
            return
        }

        val customItem = LumaItemsAPI.getInstance().getCustomItem(jobType.key) as? ArchiveOfAstralisItemNest ?: return

        val response = economy.withdrawPlayer(player, cost)
        if (!response.transactionSuccess()) {
            Logging.log("Failed to withdraw money from ${player.name} for Archive re-roll: ${response.errorMessage}")
            return
        }

        val percent = ArchivePercentChance.randomByTotalWeight().actualPercent

        if (percent > level) {
            val newArchive = customItem.createItem(percent).second
            newArchive.incrementArchiveRolls(1)
            inv.setItem(ARCHIVE_SLOT, newArchive)
        } else {
            archive.incrementArchiveRolls(1)
            Util.sendMsg(player, "Your Archive re-roll did not yield a higher level. Better luck next time!")
        }

        Util.sendMsg(player, "You have re-rolled your Archive. The new cost to re-roll this Archive is <green>${(BASE_COST + ((rolls + 1) * COST_AMPLIFIER)).formatCost()}</green>.")
    }

    override fun onInventoryClose(event: InventoryCloseEvent) {
        val item = event.inventory.getItem(ARCHIVE_SLOT) ?: return
        Util.giveItem(event.player as Player, item)
    }

    override fun getInventory(): Inventory {
        return gui
    }

    override fun open(player: HumanEntity) {
        player.openInventory(gui)
    }


    private fun ItemStack.getArchiveLevel(jobType: ArchiveOfAstralisItemNest.JobType): Int {
        val level = this.itemMeta?.persistentDataContainer?.get(NamespacedKey(LumaItems.getInstance(), jobType.key), PersistentDataType.SHORT)
        return level?.toInt() ?: 0
    }

    private fun ItemStack.incrementArchiveRolls(value: Int) {
        val meta = this.itemMeta ?: return
        val container = meta.persistentDataContainer
        val currentRolls = container.get(ARCHIVE_ROLLS_KEY, PersistentDataType.SHORT) ?: 0
        container.set(ARCHIVE_ROLLS_KEY, PersistentDataType.SHORT, (currentRolls + value).toShort())
        this.itemMeta = meta
    }

    private fun ItemStack.getArchiveRolls(): Int {
        val meta = this.itemMeta ?: return 0
        val container = meta.persistentDataContainer
        val currentRolls = container.get(ARCHIVE_ROLLS_KEY, PersistentDataType.SHORT) ?: 0
        return currentRolls.toInt()
    }

}

private fun Double.formatCost(): String {
    return "$${String.format("%,.2f", this)}"
}

private fun Int.formatCost(): String {
    return "$${String.format("%,.2f", this.toDouble())}"
}
