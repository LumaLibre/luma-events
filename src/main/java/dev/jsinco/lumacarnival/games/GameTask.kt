package dev.jsinco.lumacarnival.games

import org.bukkit.event.Listener

abstract class GameTask : Listener {

    var taskAttributes: TaskAttributes? = null

    abstract fun enabled(): Boolean

    open fun initializeGame() {}

    open fun stopGame() {}

    open fun tick() {}

}
