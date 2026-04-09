package dev.lumas.events.explorer.listener

import dev.lumas.events.explorer.intention.ExplorerIntentRegistry
import dev.lumas.events.manager.EventPlayerManager
import dev.lumas.events.obj.EventPlayer
import dev.lumas.events.utility.Executors
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.event.Listener
import java.util.concurrent.TimeUnit

interface AbstractExplorerListener : Listener {
    fun fire(event: Any, entity: Entity, ignoreMiles: Boolean = false) {
        if (entity is Player) {
            val eventPlayer: EventPlayer = EventPlayerManager.getByUUID(entity.uniqueId)
            if (eventPlayer.isSuspended) {
                eventPlayer.fireForAsides(entity.world, event)
            }
        }

        ExplorerIntentRegistry.unifiedValues().forEach { intent ->
            intent.tryApply(entity.world, event)
        }

        if (entity is Player && !ignoreMiles) {
            Executors.runAsync { _ ->
                val eventPlayer: EventPlayer = EventPlayerManager.getByUUID(entity.uniqueId)
                if (!eventPlayer.isSuspended) {
                    eventPlayer.fireForExplorerMiles(event)
                }
            }
        }
    }

    fun fireLater(event: Any, entity: Entity, delay: Long, ignoreMiles: Boolean = false) {

        if (entity is Player) {
            val eventPlayer: EventPlayer = EventPlayerManager.getByUUID(entity.uniqueId)

            Executors.delayedSync(entity, delay) {
                if (eventPlayer.isOnline && eventPlayer.isSuspended) {
                    eventPlayer.fireForAsides(entity.world, event)
                }
            }
        }

        Executors.delayedGlobal(delay) {
            Executors.runSync(entity) {
                ExplorerIntentRegistry.unifiedValues().forEach { intent ->
                    intent.tryApply(entity.world, event)
                }
            }
        }

        if (entity is Player && !ignoreMiles) {
            Executors.runDelayedAsync(TimeUnit.MILLISECONDS, delay * 50L) {
                val eventPlayer: EventPlayer = EventPlayerManager.getByUUID(entity.uniqueId)
                if (eventPlayer.isOnline && !eventPlayer.isSuspended) {
                    eventPlayer.fireForExplorerMiles(event)
                }
            }
        }
    }
}