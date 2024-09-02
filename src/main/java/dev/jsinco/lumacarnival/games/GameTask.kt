package dev.jsinco.lumacarnival.games

import org.bukkit.event.Listener

abstract class GameTask : Listener {

    var taskAttributes: TaskAttributes? = null

    open fun initializeGame() {}

    open fun tick() {}

    abstract fun enabled(): Boolean
}
