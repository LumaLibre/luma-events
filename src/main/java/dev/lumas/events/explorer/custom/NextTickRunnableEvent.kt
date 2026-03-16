package dev.lumas.events.explorer.custom

import dev.lumas.core.annotation.Autowire
import dev.lumas.core.annotation.Register
import dev.lumas.events.explorer.listener.AbstractRunnableTicker
import org.bukkit.entity.Player

/**
 * Called once per tick on the player's entity scheduler
 */
class NextTickRunnableEvent(val player: Player)

@Register(Autowire.SERVICE) // TODO
class NextTickRunner : AbstractRunnableTicker(1) {
    override fun doRun(player: Player) {
        fire(NextTickRunnableEvent(player), player, intentsOnly = true)
    }
}