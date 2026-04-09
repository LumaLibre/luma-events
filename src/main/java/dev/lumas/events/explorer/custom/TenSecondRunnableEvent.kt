package dev.lumas.events.explorer.custom

import dev.lumas.core.annotation.Autowire
import dev.lumas.core.annotation.Register
import dev.lumas.events.explorer.listener.AbstractRunnableTicker
import org.bukkit.entity.Player

/**
 * Called every 10 seconds on the player's entity scheduler
 */
class TenSecondRunnableEvent(val player: Player)

@Register(Autowire.SERVICE) // TODO
class TenSecondRunner : AbstractRunnableTicker(200) {
    override fun doRun(player: Player) {
        fire(TenSecondRunnableEvent(player), player, ignoreMiles = true)
    }
}