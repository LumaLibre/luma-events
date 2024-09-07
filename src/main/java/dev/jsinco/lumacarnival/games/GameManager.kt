package dev.jsinco.lumacarnival.games

import dev.jsinco.lumacarnival.CarnivalMain
import org.bukkit.Bukkit
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.scheduler.BukkitTask

class GameManager {

    val activeGames: MutableList<GameTask> = mutableListOf()
    val tasks: MutableList<BukkitTask> = mutableListOf()

    fun registerGame(game: GameTask): GameManager {
        if (!game.enabled()) {
            return this
        }

        val taskattr: TaskAttributes? = game.javaClass.getAnnotation(TaskAttributes::class.java)

        game.taskAttributes = taskattr
        game.initializeGame()
        activeGames.add(game)
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
}