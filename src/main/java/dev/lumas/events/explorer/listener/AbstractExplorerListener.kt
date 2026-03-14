package dev.lumas.events.explorer.listener

import dev.lumas.events.EventPlayerManager
import dev.lumas.events.explorer.intention.ExplorerIntentRegistry
import dev.lumas.events.obj.EventPlayer
import dev.lumas.events.utility.Executors
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.event.Listener
import java.util.concurrent.TimeUnit

abstract class AbstractExplorerListener : Listener {
    fun fire(event: Any, entity: Entity) {
        ExplorerIntentRegistry.unifiedValues().forEach { intent ->
            intent.tryApply(entity.world, event)
        }

        if (entity is Player) {
            Executors.runAsync { _ ->
                val eventPlayer: EventPlayer = EventPlayerManager.getByUUID(entity.uniqueId)
                eventPlayer.fireForExplorerMiles(event)
            }
        }
    }

    fun fireLater(event: Any, entity: Entity, delay: Long) {
        Executors.delayedGlobal(delay) {
            Executors.runSync(entity) {
                ExplorerIntentRegistry.unifiedValues().forEach { intent ->
                    intent.tryApply(entity.world, event)
                }
            }
        }

        if (entity is Player) {
            Executors.runDelayedAsync(TimeUnit.MILLISECONDS, delay * 50L) {
                val eventPlayer: EventPlayer = EventPlayerManager.getByUUID(entity.uniqueId)
                if (eventPlayer.isOnline()) {
                    eventPlayer.fireForExplorerMiles(event)
                }
            }
        }
    }
}