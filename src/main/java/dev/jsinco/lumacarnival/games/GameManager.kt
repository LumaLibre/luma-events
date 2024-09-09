package dev.jsinco.lumacarnival.games

import dev.jsinco.lumacarnival.CarnivalMain
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabExecutor
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.scheduler.BukkitTask
import java.lang.reflect.Method

class GameManager : TabExecutor {

    val activeGames: MutableList<GameTask> = mutableListOf()
    val tasks: MutableList<BukkitTask> = mutableListOf()

    val activeCommands: MutableMap<GameSubCommand, Method> = mutableMapOf()

    fun registerGame(game: GameTask): GameManager {
        if (!game.enabled()) {
            return this
        }

        val taskattr: TaskAttributes? = game.javaClass.getAnnotation(TaskAttributes::class.java)

        game.taskAttributes = taskattr
        game.initializeGame()
        activeGames.add(game)

        val commands = getAllCommands(game)
        if (commands.isNotEmpty()) {
            activeCommands.putAll(commands)
        }
        return this
    }


    fun startGameTicker(): GameManager {
        for (game in activeGames) {
            if (!game.enabled()) {
                continue
            }

            Bukkit.getPluginManager().registerEvents(game, CarnivalMain.instance)

            val taskAttributes = game.taskAttributes ?: continue

            val task = if (!taskAttributes.async) {
                object : BukkitRunnable() {
                    override fun run() {
                        game.tick()
                    }
                }.runTaskTimer(CarnivalMain.instance, 0L, taskAttributes.taskTime)
            } else {
                object : BukkitRunnable() {
                    override fun run() {
                        game.tick()
                    }
                }.runTaskTimerAsynchronously(CarnivalMain.instance, 0L, taskAttributes.taskTime)
            }
            tasks.add(task)
        }
        return this
    }

    fun stop() {
        for (task in tasks) {
            task.cancel()
        }
        tasks.clear()
        for (game in activeGames) {
            game.stopGame()
        }
        activeGames.clear()
    }

    fun saveAll() {
        for (game in activeGames) {
            game.save()
        }
    }


    private fun getAllCommands(clazz: GameTask): Map<GameSubCommand, Method> {
        val commands = mutableMapOf<GameSubCommand, Method>()
        for (method in clazz.javaClass.declaredMethods) {
            val annotation = method.getAnnotation(GameSubCommand::class.java) ?: continue
            commands[annotation] = method
        }
        return commands
    }

    override fun onTabComplete(sender: CommandSender, command: Command, label: String, args: Array<out String>): MutableList<String> {
        return activeCommands.map { it.key.name }.toMutableList()
    }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (args.isEmpty()) {
            return false
        }
        val subCommand = args[0]
        val method = activeCommands.keys.find { it.name == subCommand } ?: return false
        if (method.playerOnly && sender !is Player) {
            return false
        }
        val methodToInvoke = activeCommands[method] ?: return false
        methodToInvoke.invoke(activeGames.find { it.javaClass == methodToInvoke.declaringClass } ?: return false, GameCommandExecutedEvent(sender, args.drop(1).toList()))
        return true
    }
}