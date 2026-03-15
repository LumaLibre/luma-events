package dev.lumas.events.explorer.custom

import dev.lumas.events.explorer.listener.AbstractRunnableTicker
import dev.lumas.lumacore.manager.modules.AutoRegister
import dev.lumas.lumacore.manager.modules.RegisterType
import org.bukkit.entity.Player

/**
 * Called once per tick on the player's entity scheduler
 */
class NextTickRunnableEvent(val player: Player)

@AutoRegister(RegisterType.SERVICE) // TODO
class NextTickRunner : AbstractRunnableTicker(1) {
    override fun doRun(player: Player) {
        fire(NextTickRunnableEvent(player), player, intentsOnly = true)
    }
}