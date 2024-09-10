package dev.jsinco.lumacarnival.commands

import dev.jsinco.lumacarnival.CarnivalMain
import dev.jsinco.lumacarnival.commands.subcommands.GiveToken
import dev.jsinco.lumacarnival.commands.subcommands.Reload
import dev.jsinco.lumacarnival.commands.subcommands.Shop
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabExecutor
import org.bukkit.entity.Player

class CommandManager(private val plugin: CarnivalMain) : TabExecutor {

    private val commands: Map<String, SubCommand> = mapOf(
        "reload" to Reload(),
        "token" to GiveToken(),
        "shop" to Shop()
    )

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (args.isEmpty()) return false

        val subCommand = commands[args[0]] ?: return false

        if (subCommand.playerOnly() && sender !is Player) return false
        else if (subCommand.permission() != null && !sender.hasPermission(subCommand.permission()!!)) return false

        subCommand.execute(plugin, sender, args)
        return true
    }

    override fun onTabComplete(sender: CommandSender, command: Command, label: String, args: Array<out String>): List<String>? {
        if (args.size == 1) return commands.keys.toList()

        val subCommand = commands[args[0]] ?: return null

        if (subCommand.playerOnly() && sender !is Player) return null
        else if (subCommand.permission() != null && !sender.hasPermission(subCommand.permission()!!)) return null

        return subCommand.tabComplete(plugin, sender, args)
    }
}