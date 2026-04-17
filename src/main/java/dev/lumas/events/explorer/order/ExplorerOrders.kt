package dev.lumas.events.explorer.order

import dev.lumas.core.annotation.Autowire
import dev.lumas.core.annotation.Register
import dev.lumas.events.EventMain
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Tag
import org.bukkit.entity.EntityType
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerAttemptPickupItemEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType

@Register(Autowire.SERVICE)
object ExplorerOrders : ExplorerOrderContainer() {

    private val WORLDS = { EventMain.getOkaeriConfig().explorer.suspendedWorlds }
    private val ORDER_KEY = NamespacedKey(EventMain.getInstance(), "explorer_order")

    fun ItemStack.flag() {
        val meta = this.itemMeta ?: return
        meta.persistentDataContainer.set(ORDER_KEY, PersistentDataType.BOOLEAN, true)
        this.itemMeta = meta
    }

    fun ItemStack.isFlagged(): Boolean {
        val meta = this.itemMeta ?: return false
        return meta.persistentDataContainer.has(ORDER_KEY)
    }

    // Wardens have 50% more health than normal and also deal 25% more damage.
    // Wardens are hard to find and are only underground, so other mobs will be hell to deal with.
    // I don't know if this is possible to beat.
    val SH3LL = ExplorerOrder(
        name = "#0 shell",
        objective = "Kill a Warden.",
        quantity = 1,
        souls = 50,
        world = WORLDS,
        eventClass = EntityDeathEvent::class,
        icon = Material.WARDEN_SPAWN_EGG,
        biome = PaleSideBiome.of("sh3ll")
            .foliageColor("#F8DAF7")
            .grassColor("#FFF9FF")
    ) { event, completion ->
        if (event.entityType == EntityType.WARDEN) {
            completion.progress()
        }
    }

    // Creepers are always charged and duplicate when they explode.
    // Probably possible to beat.
    val NIHIL1ST = ExplorerOrder(
        name = "#1 nihilist",
        objective = "Kill 100 Creepers.",
        quantity = 100,
        souls = 10,
        world = WORLDS,
        eventClass = EntityDeathEvent::class,
        icon = Material.CREEPER_SPAWN_EGG,
        biome = PaleSideBiome.of("nihil1st")
            .foliageColor("#DAF7F8")
            .grassColor("#F9FFFF")
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
        icon = Material.ELDER_GUARDIAN_SPAWN_EGG,
        biome = PaleSideBiome.of("br3athe")
            .foliageColor("#DAF8E1")
            .grassColor("#F9FFF9")
    ) { event, completion ->
        if (event.entityType == EntityType.ELDER_GUARDIAN) {
            completion.progress()
        }
    }

    // Mining Fatigue I + Darkness + Blindness when below Y level 4.
    // It's possible, but very time-consuming.
    val F4TIGUE = ExplorerOrder(
        name = "#3 fatigue",
        objective = "Break 250 Diamond ores.",
        quantity = 250,
        souls = 15,
        world = WORLDS,
        eventClass = BlockBreakEvent::class,
        icon = Material.DIAMOND_ORE,
        biome = PaleSideBiome.of("f4tigue")
            .foliageColor("#F6F8DA")
            .grassColor("#FEFFF9")
    ) { event, completion ->
        if (Tag.DIAMOND_ORES.isTagged(event.block.type)) {
            event.isDropItems = false
            event.block.world.dropItemNaturally(event.block.location.toCenterLocation(), ItemStack(Material.DIAMOND))
            completion.progress()
        }
    }

    // Possible.
    val P1TY = ExplorerOrder(
        name = "#4 pity",
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
        eventClass = BlockBreakEvent::class,
        icon = Material.ANCIENT_DEBRIS,
        biome = PaleSideBiome.of("regret")
            .foliageColor("#F8EADA")
            .grassColor("#FFFDF9")
    ) { event, completion ->
        if (event.block.type == Material.ANCIENT_DEBRIS) {
            event.isDropItems = false
            event.block.world.dropItemNaturally(event.block.location.toCenterLocation(), ItemStack(Material.NETHERITE_SCRAP))
            completion.progress()
        }
    }


    val CRUSH = ExplorerOrder(
        name = "#6 crush",
        objective = "Obtain 2 maces.",
        quantity = 2,
        souls = 20,
        world = WORLDS,
        eventClass = PlayerAttemptPickupItemEvent::class,
        icon = Material.MACE,
        biome = PaleSideBiome.of("crvsh")
            .foliageColor("#F8DADA")
            .grassColor("#FFF9F9")
    ) { event, completion ->
        val item = event.item.itemStack
        if (item.type == Material.MACE && !item.isFlagged()) {
            item.flag()
            completion.progress()
        }
    }

    val EXPENSE = ExplorerOrder(
        name = "#7 expense",
        objective = "Kill 250 endermen.",
        quantity = 250,
        souls = 30,
        world = WORLDS,
        eventClass = EntityDeathEvent::class,
        icon = Material.ENDER_PEARL,
        biome = PaleSideBiome.of("exp3nse")
            .foliageColor("#F8DAF1")
            .grassColor("#FFF9FE")
    ) { event, completion ->
        if (event.entityType == EntityType.ENDERMAN) {
            completion.progress()
        }
    }


    val MELTING_POINT = ExplorerOrder(
        name = "#8 melting point",
        objective = "Obtain 30 blaze rods.",
        quantity = 30,
        souls = 13,
        world = WORLDS,
        eventClass = PlayerAttemptPickupItemEvent::class,
        icon = Material.BLAZE_ROD,
        biome = PaleSideBiome.of("m3lting_p01nt")
            .foliageColor("#EDF8DA")
            .grassColor("#FEFFF9")
    ) { event, completion ->
        val item = event.item.itemStack
        if (item.type == Material.BLAZE_ROD && !item.isFlagged()) {
            item.flag()
            completion.progress(item.amount)
        }
    }

    val DESPAIR = ExplorerOrder(
        name = "#9 despair",
        objective = "Kill 30 Wither Skeletons.",
        quantity = 30,
        souls = 10,
        world = WORLDS,
        eventClass = EntityDeathEvent::class,
        icon = Material.WITHER_SKELETON_SKULL
    ) { event, completion ->
        if (event.entityType == EntityType.WITHER_SKELETON) {
            completion.progress()
        }
    }

    val ABYSS = ExplorerOrder(
        name = "#10 abyss",
        objective = "Kill the Ender Dragon.",
        quantity = 1,
        souls = 20,
        world = WORLDS,
        eventClass = EntityDeathEvent::class,
        icon = Material.DRAGON_HEAD
    ) { event, completion ->
        if (event.entityType == EntityType.ENDER_DRAGON) {
            completion.progress()
        }
    }

}