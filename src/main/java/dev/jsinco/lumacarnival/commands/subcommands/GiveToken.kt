package dev.jsinco.lumacarnival.commands.subcommands

import dev.jsinco.lumacarnival.CarnivalMain
import dev.jsinco.lumacarnival.CarnivalToken
import dev.jsinco.lumacarnival.commands.SubCommand
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class GiveToken : SubCommand {
    override fun execute(plugin: CarnivalMain, sender: CommandSender, args: Array<out String>) {
        val player = plugin.server.getPlayer(args[1]) ?: sender as? Player ?: return
        val amount = args[2].toIntOrNull() ?: return

        CarnivalToken.give(player, amount)
    }

    override fun tabComplete(plugin: CarnivalMain, sender: CommandSender, args: Array<out String>): List<String> {
        return listOf("<player>", "<amount>")
    }

    override fun permission(): String {
        return "lumacarnival.admin"
    }

    override fun playerOnly(): Boolean {
        return false
    }
}