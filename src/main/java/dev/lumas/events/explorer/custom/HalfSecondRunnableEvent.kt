package dev.lumas.events.explorer.custom

import dev.lumas.core.annotation.Autowire
import dev.lumas.core.annotation.Register
import dev.lumas.events.explorer.listener.AbstractRunnableTicker
import org.bukkit.entity.Player

/**
 * Called half per second on the player's entity scheduler
 */
class HalfSecondRunnableEvent(val player: Player)

@Register(Autowire.SERVICE) // TODO
class HalfSecondRunner : AbstractRunnableTicker(10) {
    override fun doRun(player: Player) {
        fire(HalfSecondRunnableEvent(player), player, intentsOnly = true)
    }
}