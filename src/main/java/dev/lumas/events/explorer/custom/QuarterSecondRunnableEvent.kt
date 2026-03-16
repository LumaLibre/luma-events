package dev.lumas.events.explorer.custom

import dev.lumas.core.annotation.Autowire
import dev.lumas.core.annotation.Register
import dev.lumas.events.explorer.listener.AbstractRunnableTicker
import org.bukkit.entity.Player

/**
 * Called quarter per second on the player's entity scheduler
 */
class QuarterSecondRunnableEvent(val player: Player)

@Register(Autowire.SERVICE)
class QuarterSecondRunner : AbstractRunnableTicker(5) {
    override fun doRun(player: Player) {
        fire(QuarterSecondRunnableEvent(player), player, intentsOnly = true)
    }
}