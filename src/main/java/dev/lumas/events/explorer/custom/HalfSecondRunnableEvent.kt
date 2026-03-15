package dev.lumas.events.explorer.custom

import dev.lumas.events.explorer.listener.AbstractRunnableTicker
import dev.lumas.lumacore.manager.modules.AutoRegister
import dev.lumas.lumacore.manager.modules.RegisterType
import org.bukkit.entity.Player

/**
 * Called half per second on the player's entity scheduler
 */
class HalfSecondRunnableEvent(val player: Player)

@AutoRegister(RegisterType.SERVICE) // TODO
class HalfSecondRunner : AbstractRunnableTicker(10) {
    override fun doRun(player: Player) {
        fire(HalfSecondRunnableEvent(player), player, intentsOnly = true)
    }
}