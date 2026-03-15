package dev.lumas.events.explorer.custom

import dev.lumas.events.explorer.listener.AbstractRunnableTicker
import dev.lumas.lumacore.manager.modules.AutoRegister
import dev.lumas.lumacore.manager.modules.RegisterType
import org.bukkit.entity.Player

class FullSecondRunnableEvent(val player: Player)


@AutoRegister(RegisterType.SERVICE) // TODO
class FullSecondRunner : AbstractRunnableTicker(20) {
    override fun doRun(player: Player) {
        fire(FullSecondRunnableEvent(player), player, intentsOnly = true)
    }
}