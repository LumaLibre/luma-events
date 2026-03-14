package dev.lumas.events.explorer.intention

import dev.lumas.events.utility.Executors
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.entity.VillagerReplenishTradeEvent
import org.bukkit.event.inventory.InventoryOpenEvent
import org.bukkit.event.inventory.InventoryType

object ExplorerIntents : ExplorerIntentContainer() {

    private const val WORLD = "*"
    private val TIMED_EXPLOSION = fun (player: Player, delay: Long) {
        player.world.playSound(player.location, Sound.ENTITY_CREEPER_PRIMED, 1f, 1f)
        Executors.delayedSync(player, delay) {
            player.world.createExplosion(player.location, 10.0f)
        }
    }

    val DEATH_DELETE_INV = ExplorerIntent<PlayerDeathEvent>(
        title = "Death Deletes Inventory",
        desc = "On a player's death, their inventory is deleted.",
        world = WORLD,
        eventClass = PlayerDeathEvent::class,
        handler = { event ->
            event.drops.clear()
            event.droppedExp = 0
            event.itemsToKeep.clear()
            event.entity.inventory.clear()
            event.player.exp = 0f
        }
    )

    val CHESTS_BLOW_UP_WHEN_USED = ExplorerIntent<InventoryOpenEvent>(
        title = "Containers Explode When Opened",
        desc = "Real containers of any kind explode when opened.",
        world = WORLD,
        eventClass = InventoryOpenEvent::class,
    ) { event ->
        val type = event.inventory.type
        if (type != InventoryType.FURNACE && type != InventoryType.PLAYER && type != InventoryType.WORKBENCH && type != InventoryType.CRAFTING) {
            TIMED_EXPLOSION(event.player as Player, 60L)
        }
    }

    val VILLAGERS_NEVER_RESTOCK = ExplorerIntent<VillagerReplenishTradeEvent>(
        title = "Villagers Never Restock",
        desc = "Villagers never restock their trades.",
        world = WORLD,
        eventClass = VillagerReplenishTradeEvent::class
    ) { event ->
        event.isCancelled = true
    }


    init {
        ensureEnumerated()
    }
}