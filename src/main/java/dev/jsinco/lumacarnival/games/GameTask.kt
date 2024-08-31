package dev.jsinco.lumacarnival.games

import org.bukkit.event.Listener

abstract class GameTask : Listener {

    lateinit var taskAttributes: TaskAttributes

    abstract fun initializeGame()

    abstract fun tick()

    abstract fun enabled(): Boolean
}
