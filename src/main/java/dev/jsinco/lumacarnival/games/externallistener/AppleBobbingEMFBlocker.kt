package dev.jsinco.lumacarnival.games.externallistener

import com.oheers.fish.api.EMFFishEvent
import org.bukkit.World
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener

class AppleBobbingEMFBlocker(private val world: World) : Listener {

    @EventHandler(priority = EventPriority.LOWEST)
    fun onEMFFish(event: EMFFishEvent) {
        if (event.player.world == world) {
            event.isCancelled = true
        }
    }
}