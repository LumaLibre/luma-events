package dev.jsinco.luma.commands.subcommands

import dev.jsinco.luma.ThanksgivingEvent
import dev.jsinco.luma.Util
import dev.jsinco.luma.commands.Subcommand
import dev.jsinco.luma.shop.ShopManager
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class Shop : Subcommand {

    override fun execute(sender: CommandSender, args: Array<out String>, plugin: ThanksgivingEvent) {

        if (args.size == 1) {
            sender as Player
            ShopManager.openShop(sender)
            return
        } else if (args.size == 2) {
            val player = Bukkit.getPlayerExact(args[1]) ?: return
            ShopManager.openShop(player)
            return
        }

        sender as Player

        if (args[1] == "remove") {
            val itemSectionName = args[2]
            ShopManager.removeShopItem(itemSectionName)

        } else {
            val item = sender.inventory.itemInMainHand
            if (item.type.isAir) return

            val command = args[2].replace("-", " ")
            val price = args[3].toIntOrNull() ?: return
            val slot = args[4].toIntOrNull() ?: return

            ShopManager.addShopItem(item, command, price, slot)
        }
        Util.sendMsg(sender, "Shop item added/removed!")
        ThanksgivingEvent.getShopManager().reloadShop()
    }

    override fun tabComplete(sender: CommandSender, args: Array<out String>, plugin: ThanksgivingEvent): List<String>? {
        return when(args.size) {
            2 -> listOf("add", "remove")
            3 -> {
                if (args[1] == "remove") {
                    ShopManager.file.keys.toList()
                } else {
                    listOf("<command>")
                }
            }
            4 -> listOf("<price>")
            5 -> listOf("<slot>")
            else -> null
        }
    }


    override fun permission(): String {
        return "lumaevent.admin"
    }

    override fun isPlayerOnly(): Boolean {
        return false
    }
}
