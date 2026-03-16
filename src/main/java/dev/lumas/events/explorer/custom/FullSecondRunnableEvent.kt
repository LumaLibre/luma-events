package dev.lumas.events.explorer.custom

import dev.lumas.core.annotation.Autowire
import dev.lumas.core.annotation.Register
import dev.lumas.events.explorer.listener.AbstractRunnableTicker
import org.bukkit.entity.Player

class FullSecondRunnableEvent(val player: Player)


@Register(Autowire.SERVICE) // TODO
class FullSecondRunner : AbstractRunnableTicker(20) {
    override fun doRun(player: Player) {
        fire(FullSecondRunnableEvent(player), player, intentsOnly = true)
    }
}