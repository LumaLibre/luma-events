package dev.lumas.events.explorer.custom

import dev.lumas.events.explorer.listener.AbstractRunnableTicker
import dev.lumas.lumacore.manager.modules.AutoRegister
import dev.lumas.lumacore.manager.modules.RegisterType
import org.bukkit.entity.Player

/**
 * Called quarter per second on the player's entity scheduler
 */
class QuarterSecondRunnableEvent(val player: Player)

@AutoRegister(RegisterType.SERVICE)
class QuarterSecondRunner : AbstractRunnableTicker(5) {
    override fun doRun(player: Player) {
        fire(QuarterSecondRunnableEvent(player), player, intentsOnly = true)
    }
}