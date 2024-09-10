package dev.jsinco.lumacarnival.commands

import dev.jsinco.lumacarnival.CarnivalMain
import org.bukkit.command.CommandSender

interface SubCommand {

    fun execute(plugin: CarnivalMain, sender: CommandSender, args: Array<out String>)

    fun tabComplete(plugin: CarnivalMain, sender: CommandSender, args: Array<out String>): List<String>?

    fun permission(): String?

    fun playerOnly(): Boolean
}