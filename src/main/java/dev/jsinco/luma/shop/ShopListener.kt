package dev.jsinco.luma.shop

import dev.jsinco.luma.PrisonMinePlayer
import dev.jsinco.luma.PrisonMinePlayerManager
import dev.jsinco.luma.Util
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemStack

class ShopListener : Listener {

    @EventHandler
    fun onInventoryClick(event : InventoryClickEvent) {
        if (event.inventory.getHolder(false) !is ShopManager) return

        event.isCancelled = true
        val inv = event.inventory
        val item: ItemStack = event.currentItem ?: return
        val player = event.whoClicked as Player


        if (GuiUtil.isGuiButton(item)) {
            val direction = GuiUtil.getGuiButtonDirection(item) ?: return
            val shopManager = inv.holder as ShopManager
            val newPage = if (direction) {
                shopManager.getNextPage(inv)
            } else {
                shopManager.getPreviousPage(inv)
            }

            if (newPage != null) {
                player.openInventory(newPage)
            } else {
                Util.sendMsg(player, "No more pages to show.")
            }
            return
        }


        val shopItem: ShopItem = ShopManager.getShopItemFromDisplayItem(item) ?: return
        val command = shopItem.command
        val tokenPrice = shopItem.tokenPrice
        val prisonMinePlayer: PrisonMinePlayer = PrisonMinePlayerManager.getByUUID(player.uniqueId)

        if (prisonMinePlayer.points >= tokenPrice) {
            prisonMinePlayer.points -= tokenPrice
            PrisonMinePlayerManager.save(prisonMinePlayer)
            if (command.equals("empty", true)) {
                Util.giveItem(player, shopItem.item)
            } else {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.replace("%player%", player.name))
            }
        } else {
            Util.sendMsg(player, "You don't have enough tokens!")
        }
    }
}