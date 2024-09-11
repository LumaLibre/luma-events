package dev.jsinco.lumacarnival.shop

import dev.jsinco.lumacarnival.CarnivalMain
import dev.jsinco.lumacarnival.CarnivalToken
import dev.jsinco.lumacarnival.Util
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
                Util.msg(player, "No more pages!")
            }
            return
        }


        val shopItem: ShopItem = ShopManager.getShopItemFromDisplayItem(item) ?: return
        val purchaseAmount = CarnivalMain.shopManager.getPurchaseAmount(player, shopItem)
        if (purchaseAmount >= 2) {
            Util.msg(player, "You have already purchased this item!")
            return
        }

        val command = shopItem.command
        val tokenPrice = shopItem.tokenPrice

        if (CarnivalToken.take(player, tokenPrice)) {
            if (command.equals("empty", true)) {
                Util.giveItem(player, shopItem.item)
            } else {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.replace("%player%", player.name))
            }
            CarnivalMain.shopManager.setItemPurchased(player, shopItem, purchaseAmount + 1)
        } else {
            Util.msg(player, "You don't have enough tokens!")
        }
    }
}