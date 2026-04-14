package dev.lumas.events.explorer.order

import dev.lumas.core.annotation.Autowire
import dev.lumas.core.annotation.Register
import dev.lumas.events.EventMain
import dev.lumas.events.explorer.custom.BlockBrokenExplorerEvent
import org.bukkit.Material
import org.bukkit.Tag
import org.bukkit.entity.EntityType
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.event.entity.PlayerDeathEvent

@Register(Autowire.SERVICE)
object ExplorerOrders : ExplorerOrderContainer() {

    private val WORLDS = EventMain.getOkaeriConfig().explorer.suspendedWorlds

    // Wardens have 50% more health than normal and also deal 25% more damage.
    // Wardens are hard to find and are only underground, so other mobs will be hell to deal with.
    // I don't know if this is possible to beat.
    val SH3LL = ExplorerOrder(
        name = "#0 sh3ll",
        objective = "Kill a Warden.",
        quantity = 1,
        souls = 40,
        world = WORLDS,
        eventClass = EntityDeathEvent::class,
        icon = Material.WARDEN_SPAWN_EGG
    ) { event, completion ->
        if (event.entityType == EntityType.WARDEN) {
            completion.progress()
        }
    }

    // Creepers are always charged and duplicate when they explode.
    // Probably possible to beat.
    val NIHIL1ST = ExplorerOrder(
        name = "#1 nihil1st",
        objective = "Kill 100 Creepers.",
        quantity = 100,
        souls = 10,
        world = WORLDS,
        eventClass = EntityDeathEvent::class,
        icon = Material.CREEPER_SPAWN_EGG
    ) { event, completion ->
        if (event.entityType == EntityType.CREEPER) {
            completion.progress()
        }
    }

    // Water damages you by 1/4 a heart every half a second. A golden apple or regeneration would heal the player faster than the tick damage.
    // Definitely possible to beat.
    val BREATHE = ExplorerOrder(
        name = "#2 breathe",
        objective = "Kill two Elder Guardians.",
        quantity = 2,
        souls = 10,
        world = WORLDS,
        eventClass = EntityDeathEvent::class,
        icon = Material.ELDER_GUARDIAN_SPAWN_EGG
    ) { event, completion ->
        if (event.entityType == EntityType.ELDER_GUARDIAN) {
            completion.progress()
        }
    }

    // Mining Fatigue I + Darkness + Blindness when below Y level 4.
    // It's possible, but very time-consuming.
    val F4TIGUE = ExplorerOrder(
        name = "#3 f4tigue",
        objective = "Break 250 Diamond ores.",
        quantity = 250,
        souls = 15,
        world = WORLDS,
        eventClass = BlockBrokenExplorerEvent::class,
        icon = Material.DIAMOND_ORE
    ) { event, completion ->
        if (Tag.DIAMOND_ORES.isTagged(event.type)) {
            completion.progress()
        }
    }

    // Possible.
    val P1TY = ExplorerOrder(
        name = "#4 p1ty",
        objective = "Die.",
        quantity = 1,
        souls = 1,
        world = WORLDS,
        eventClass = PlayerDeathEvent::class,
        icon = Material.AMETHYST_SHARD
    ) { _, completion ->
        completion.progress()
    }

    // All mobs near the player will always be angry at them (even if they can't see them).
    // Lava and fire kills players instantly.
    // Inventories are cleared completely when the player dies.
    // It's possible, but very time-consuming.
    val REGRET = ExplorerOrder(
        name = "#5 regret",
        objective = "Break 20 Ancient Debris.",
        quantity = 20,
        souls = 20,
        world = WORLDS,
        eventClass = BlockBrokenExplorerEvent::class,
        icon = Material.ANCIENT_DEBRIS
    ) { event, completion ->
        if (event.type == Material.ANCIENT_DEBRIS) {
            completion.progress()
        }
    }

}