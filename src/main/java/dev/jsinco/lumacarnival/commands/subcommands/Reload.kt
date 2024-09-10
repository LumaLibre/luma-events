package dev.jsinco.lumacarnival.commands.subcommands

import dev.jsinco.lumacarnival.CarnivalMain
import dev.jsinco.lumacarnival.Util
import dev.jsinco.lumacarnival.commands.SubCommand
import org.bukkit.command.CommandSender

class Reload : SubCommand {
    override fun execute(plugin: CarnivalMain, sender: CommandSender, args: Array<out String>) {
        CarnivalMain.reload()
        Util.msg(sender, "Reloaded the plugin")
    }

    override fun tabComplete(plugin: CarnivalMain, sender: CommandSender, args: Array<out String>): List<String>? {
        return null
    }

    override fun permission(): String {
        return "lumacarnival.admin"
    }

    override fun playerOnly(): Boolean {
        return false
    }
}