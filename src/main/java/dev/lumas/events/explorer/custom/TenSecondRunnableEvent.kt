package dev.lumas.events.explorer.custom

import dev.lumas.events.explorer.listener.AbstractRunnableTicker
import dev.lumas.lumacore.manager.modules.AutoRegister
import dev.lumas.lumacore.manager.modules.RegisterType
import org.bukkit.entity.Player

/**
 * Called every 10 seconds on the player's entity scheduler
 */
class TenSecondRunnableEvent(val player: Player)

@AutoRegister(RegisterType.SERVICE) // TODO
class TenSecondRunner : AbstractRunnableTicker(200) {
    override fun doRun(player: Player) {
        fire(TenSecondRunnableEvent(player), player, intentsOnly = true)
    }
}