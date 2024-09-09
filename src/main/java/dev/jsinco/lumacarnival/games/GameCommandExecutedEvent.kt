package dev.jsinco.lumacarnival.games

import org.bukkit.command.CommandSender

data class GameCommandExecutedEvent(
    val commandSender: CommandSender,
    val args: List<String>
)