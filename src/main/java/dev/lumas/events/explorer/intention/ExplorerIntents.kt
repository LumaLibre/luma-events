package dev.lumas.events.explorer.intention

import dev.lumas.core.annotation.Autowire
import dev.lumas.core.annotation.Register
import dev.lumas.events.EventMain
import dev.lumas.events.explorer.custom.FullSecondRunnableEvent
import dev.lumas.events.explorer.custom.HalfSecondRunnableEvent
import dev.lumas.events.explorer.custom.TenSecondRunnableEvent
import dev.lumas.events.explorer.gui.ExplorerGui
import dev.lumas.events.manager.EventPlayerManager
import dev.lumas.events.utility.Executors
import dev.lumas.events.utility.Util
import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Sound
import org.bukkit.Tag
import org.bukkit.World
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.damage.DamageType
import org.bukkit.entity.Animals
import org.bukkit.entity.Bee
import org.bukkit.entity.Creeper
import org.bukkit.entity.Enderman
import org.bukkit.entity.Enemy
import org.bukkit.entity.Entity
import org.bukkit.entity.EntityType
import org.bukkit.entity.Fish
import org.bukkit.entity.Golem
import org.bukkit.entity.Mob
import org.bukkit.entity.Player
import org.bukkit.entity.Squid
import org.bukkit.entity.WitherSkeleton
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.event.entity.EntityExplodeEvent
import org.bukkit.event.entity.EntitySpawnEvent
import org.bukkit.event.entity.EntityTameEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.entity.VillagerReplenishTradeEvent
import org.bukkit.event.inventory.InventoryOpenEvent
import org.bukkit.event.inventory.InventoryType
import org.bukkit.inventory.BlockInventoryHolder
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import java.util.Queue
import java.util.UUID
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.random.Random

@Register(Autowire.SERVICE)
object ExplorerIntents : ExplorerIntentContainer() {

    private val WORLD = { EventMain.getOkaeriConfig().explorer.suspendedWorlds }
    private val TIMED_EXPLOSION = fun (loc: Location, delay: Long, player: Player) {
        loc.world.playSound(loc, Sound.ENTITY_CREEPER_PRIMED, 1f, 1f)
        Executors.delayedSync(loc, delay) {
            if (!WORLD.invoke().contains(loc.world.name) || !player.isOnline) {
                EventMain.getInstance().logger.warning("Timed explosion cancelled due to world no longer being suspended or player going offline or bad world.")
                return@delayedSync
            }

            loc.world.createExplosion(loc, 20.0f)

            Executors.delayedSync(loc, 1) {
                for (player in loc.getNearbyPlayers(10.0)) {
                    if (!player.isValid) {
                        player.damage(1000.0)
                    }
                }

                if (!loc.block.type.isAir) {
                    loc.block.type = Material.AIR
                }
            }
        }
    }
    private val NAMESPACED_KEY = NamespacedKey(EventMain.getInstance(), "explorer_intents")
    private val MINING_FATIGUE = PotionEffect(PotionEffectType.MINING_FATIGUE, 400, 0)
    //private val BLINDNESS = PotionEffect(PotionEffectType.BLINDNESS, 400, 0)
    private val DARKNESS = PotionEffect(PotionEffectType.DARKNESS, 400, 0)
    private val SLOWNESS_1 = PotionEffect(PotionEffectType.SLOWNESS, 400, 0)
    private val SLOWNESS_2 = PotionEffect(PotionEffectType.SLOWNESS, 400, 1)
    private val VALID_CONTAINERS = listOf(
        InventoryType.BLAST_FURNACE,
        InventoryType.BEACON,
        InventoryType.BREWING,
        InventoryType.FURNACE,
        InventoryType.PLAYER,
        InventoryType.WORKBENCH,
        InventoryType.CRAFTING,
        InventoryType.MERCHANT,
        InventoryType.ANVIL,
        InventoryType.COMPOSTER,
        InventoryType.SMITHING,
        InventoryType.LOOM
    )
    private val IGNORED_AQUATIC_DAMAGES = listOf(
        EntityType.DROWNED,
        EntityType.GUARDIAN,
        EntityType.ELDER_GUARDIAN,
        EntityType.ZOMBIE_NAUTILUS
    )
    private val IGNORE_WATER_DAMAGE_PREDICATE = { entity: Entity ->
        val type = entity.type
        Tag.ENTITY_TYPES_UNDEAD.isTagged(type) || Tag.ENTITY_TYPES_AQUATIC.isTagged(type) || IGNORED_AQUATIC_DAMAGES.contains(type)
    }
    private val DEATH_MESSAGE_COOLDOWN: Queue<UUID> = ConcurrentLinkedQueue()

    val DEATH_DELETE_INV_AND_UNSUSPEND = ExplorerIntent<PlayerDeathEvent>(
        title = "Death Unsuspends",
        desc = "On a player's death, they will be unsuspended and removed from the world.",
        world = WORLD,
        eventClass = PlayerDeathEvent::class,
        icon = Material.WEATHERED_COPPER_CHEST
    ) { event ->
        val player = event.player
        for (item in player.inventory) {
            if (item != null) {
                player.world.dropItemNaturally(player.location, item)
            }
        }
        event.isCancelled = true
        player.inventory.clear()
        player.exp = 0f
        event.drops.clear()
        event.droppedExp = 0
        event.itemsToKeep.clear()
        if (player.isInsideVehicle) {
            player.leaveVehicle()
        }
        val eventPlayer = EventPlayerManager.getByUUIDOrNull(player.uniqueId)
        Executors.delayedSync(eventPlayer, 1) {
            if (eventPlayer != null && eventPlayer.isSuspended) {
                eventPlayer.unsuspend(true)
                eventPlayer.lives -= 1
                eventPlayer.`paleSide$deaths`++
                EventPlayerManager.save(eventPlayer)
            }
        }

        if (!DEATH_MESSAGE_COOLDOWN.contains(player.uniqueId)) {
            Util.broadcast(event.deathMessage(), "lumaevents.default") // TODO Change
            DEATH_MESSAGE_COOLDOWN.add(player.uniqueId)
            Executors.delayedGlobal(6000) {
                DEATH_MESSAGE_COOLDOWN.remove(player.uniqueId)
            }
        }
    }

    val CONTAINERS_BLOW_UP_WHEN_USED = ExplorerIntent<InventoryOpenEvent>(
        title = "Containers Explode When Opened",
        desc = "Real containers of any kind explode when opened.",
        world = WORLD,
        eventClass = InventoryOpenEvent::class,
        icon = Material.CHEST
    ) { event ->
        val type = event.inventory.type
        if (!VALID_CONTAINERS.contains(type)) {
            val holder = event.inventory.holder ?: return@ExplorerIntent
            if (holder is ExplorerGui) {
                return@ExplorerIntent // safe gui
            }

            val loc =
                when (holder) {
                    is BlockInventoryHolder -> holder.block.location
                    is Entity -> holder.location
                    else -> event.player.location
                }
            TIMED_EXPLOSION(loc, 3L, event.player as Player)
        }
    }

    val VILLAGERS_NEVER_RESTOCK = ExplorerIntent<VillagerReplenishTradeEvent>(
        title = "Villagers Never Restock",
        desc = "Villagers never restock their trades.",
        world = WORLD,
        eventClass = VillagerReplenishTradeEvent::class,
        icon = Material.VILLAGER_SPAWN_EGG
    ) { event ->
        event.isCancelled = true
    }

    val WATER_DEALS_DAMAGE = ExplorerIntent<HalfSecondRunnableEvent>(
        title = "Standing in Water Deals Damage",
        desc = "Players standing in the water or rain take damage every half of a second.",
        world = WORLD,
        eventClass = HalfSecondRunnableEvent::class,
        icon = Material.PUFFERFISH_BUCKET
    ) { event ->
        val player = event.player
        if (player.gameMode != GameMode.SURVIVAL) {
            return@ExplorerIntent
        }
        if (player.isInRain || player.isInWater) {
            player.damage(0.5)
            player.world.playSound(player.location, Sound.ENCHANT_THORNS_HIT, 1f, 1f)
        }

        val nearbyEntities = player.location.getNearbyLivingEntities(65.0)
        nearbyEntities.filterNot(IGNORE_WATER_DAMAGE_PREDICATE).forEach {
            if (it.isInRain || it.isInWater) {
                it.damage(0.5)
                it.world.playSound(it.location, Sound.ENCHANT_THORNS_HIT, 1f, 1f)
            }
        }
    }

    val MOBS_ARE_STRONGER = ExplorerIntent<EntitySpawnEvent>(
        title = "Stronger Enemies",
        desc = "All enemies have 40% extra health and creepers spawn charged.",
        world = WORLD,
        eventClass = EntitySpawnEvent::class,
        icon = Material.DRAGON_BREATH
    ) { event ->
        val entity = event.entity as? Enemy ?: return@ExplorerIntent

        entity.getAttribute(Attribute.MAX_HEALTH)?.let {
            if (it.getModifier(NAMESPACED_KEY) == null) {
                val value = it.value * 1.4
                it.addModifier(AttributeModifier(NAMESPACED_KEY, value, AttributeModifier.Operation.ADD_NUMBER))
                entity.health = it.value
            }
        }

        if (entity is Creeper) {
            entity.isPowered = true
        }
    }


    val CREEPER_DUPLICATION = ExplorerIntent<EntityExplodeEvent>(
        title = "Duplicating Creepers",
        desc = "Creepers will duplicate when exploding.",
        world = WORLD,
        eventClass = EntityExplodeEvent::class,
        icon = Material.CREEPER_SPAWN_EGG
    ) { event ->
        val entity = event.entity as? Creeper ?: return@ExplorerIntent
        // Folia regions should be able to handle this no problem.
        // Let's just put a hard cap at 50 in a 150-block radius.
        val nearbyCreepers = entity.location.getNearbyEntitiesByType(Creeper::class.java, 150.0)

        if (nearbyCreepers.size >= 150) {
            return@ExplorerIntent
        }

        // Spawn new creepers
        entity.world.spawn(entity.location, Creeper::class.java)
        entity.world.spawn(entity.location, Creeper::class.java)
    }

    val CREEPERS_IMMUNE_TO_EXPLOSIONS = ExplorerIntent<EntityDamageEvent>(
        title = "Creepers are Immune to Explosions",
        desc = "Creepers are immune to explosions.",
        world = WORLD,
        eventClass = EntityDamageEvent::class,
        icon = Material.CREEPER_BANNER_PATTERN
    ) { event ->
        if (event.entity !is Creeper) return@ExplorerIntent

        val type = event.damageSource.damageType
        if (type == DamageType.EXPLOSION || type == DamageType.PLAYER_EXPLOSION) {
            event.damage = 0.0
        }
    }


    val ANIMALS_YIELD_NOTHING = ExplorerIntent<EntityDeathEvent>(
        title = "Animals Yield Nothing",
        desc = "Animals yield nothing when killed.",
        world = WORLD,
        eventClass = EntityDeathEvent::class,
        icon = Material.PORKCHOP
    ) { event ->
        val entity = event.entity
        if (entity is Animals || entity is Fish || entity is Squid) {
            event.drops.clear()
        }
    }

    val MINERS_HELL = ExplorerIntent<TenSecondRunnableEvent>(
        title = "Nuisance",
        desc = "Slowness I, Mining Fatigue I (y < 12 in overworld), and Blindness I (y < 12 in overworld).",
        world = WORLD,
        eventClass = TenSecondRunnableEvent::class,
        icon = Material.DIAMOND_PICKAXE
    ) { event ->
        val player = event.player
        val y = player.location.blockY
        val env = player.world.environment
        if (env == World.Environment.NORMAL) {
            if (y <= 11) {
                player.addPotionEffect(MINING_FATIGUE)
                //player.addPotionEffect(BLINDNESS)
                player.addPotionEffect(DARKNESS)
            }
        } else {
            player.addPotionEffect(MINING_FATIGUE)
        }

        player.addPotionEffect(SLOWNESS_1)
    }

    val FIRE_KILLS_WHEN_TOUCHED = ExplorerIntent<EntityDamageEvent>(
        title = "Fire & Lava Instantly Kills",
        desc = "Fire and lava will set your health to 0.",
        world = WORLD,
        eventClass = EntityDamageEvent::class,
        icon = Material.BLAZE_POWDER
    ) { event ->
        val player = event.entity as? Player ?: return@ExplorerIntent
        val eventPlayer = EventPlayerManager.getByUUID(player.uniqueId)
        if (event.cause == EntityDamageEvent.DamageCause.FIRE || event.cause == EntityDamageEvent.DamageCause.LAVA) {
            player.health = 0.0
        }
    }


    val ALWAYS_HOSTILE = ExplorerIntent<FullSecondRunnableEvent>(
        title = "Always Hostile",
        desc = "Neutral enemies are always hostile and all enemies will be able to target players from further away (excluding endermen).",
        world = WORLD,
        eventClass = FullSecondRunnableEvent::class,
        icon = Material.ENDER_EYE
    ) { event ->
        val player = event.player
        if (player.gameMode != GameMode.SURVIVAL) {
            return@ExplorerIntent
        }

        val nearbyEnemies = player.location.getNearbyLivingEntities(65.0)
            .filter { ((it is Enemy || it is Golem || it is Bee) && it !is Enderman) }
            .map { it as Mob }

        nearbyEnemies.forEach {
            if (it.target == null) {
                it.target = player
            }
        }
    }

    val LOGS_MAY_NOT_DROP = ExplorerIntent<BlockBreakEvent>(
        title = "Logs May Not Drop",
        desc = "Logs may not drop when broken. (90% chance to drop nothing)",
        world = WORLD,
        eventClass = BlockBreakEvent::class,
        icon = Material.OAK_LOG
    ) { event ->
        val block = event.block
        if (Tag.LOGS.isTagged(block.type) && Random.nextDouble() > 0.1) {
            event.isDropItems = false
            block.world.playSound(block.location, Sound.ENTITY_ALLAY_ITEM_TAKEN, 1f, 1f)
        }
    }


    val ENTITIES_BLOW_UP_WHEN_TAMED = ExplorerIntent<EntityTameEvent>(
        title = "Taming Explosions",
        desc = "Tamed entities explode when tamed.",
        world = WORLD,
        eventClass = EntityTameEvent::class,
        icon = Material.BONE
    ) { event ->
        TIMED_EXPLOSION(event.entity.location, 10L, event.owner as? Player ?: return@ExplorerIntent)
    }



    val ENDERMEN_INSTA_KILL = ExplorerIntent<EntityDamageEvent>(
        title = "Endermen Instantly Kill",
        desc = "Endermen instantly kill players.",
        world = WORLD,
        eventClass = EntityDamageEvent::class,
        icon = Material.ENDER_PEARL
    ) { event ->
        val damaged = event.entity as? Player ?: return@ExplorerIntent

        if (event.damageSource.directEntity is Enderman) {
            damaged.health = 0.0
            event.isCancelled = true
        }
    }

    val WITHER_SKELETON_EXTREMA = ExplorerIntent<EntityDamageEvent>(
        title = "Wither Skeleton Extrema",
        desc = "Wither skeletons are stronger.",
        world = WORLD,
        eventClass = EntityDamageEvent::class,
        icon = Material.WITHER_SKELETON_SKULL
    ) { event ->
        val damaged = event.entity as? Player ?: return@ExplorerIntent
        val causer = event.damageSource.causingEntity as? WitherSkeleton ?: return@ExplorerIntent

        // knockback away from skeleton
        val skeletonLoc = causer.location
        damaged.knockback(20.0, damaged.location.x - skeletonLoc.x, damaged.location.z - skeletonLoc.z)

        // 50% extra damage
        event.damage *= 1.5
    }
}