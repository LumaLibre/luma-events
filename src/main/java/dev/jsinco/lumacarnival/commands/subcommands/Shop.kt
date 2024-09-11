package dev.jsinco.lumacarnival.commands.subcommands

import dev.jsinco.lumacarnival.CarnivalMain
import dev.jsinco.lumacarnival.Util
import dev.jsinco.lumacarnival.commands.SubCommand
import dev.jsinco.lumacarnival.shop.ShopManager
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class Shop : SubCommand {
    override fun execute(plugin: CarnivalMain, sender: CommandSender, args: Array<out String>) {

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
        Util.msg(sender, "Shop item added/removed!")
        CarnivalMain.shopManager.reloadShop()
    }

    override fun tabComplete(plugin: CarnivalMain, sender: CommandSender, args: Array<out String>): List<String>? {
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
        return "lumacarnival.admin"
    }

    override fun playerOnly(): Boolean {
        return false
    }
}