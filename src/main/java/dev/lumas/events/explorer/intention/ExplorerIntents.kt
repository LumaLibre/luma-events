package dev.lumas.events.explorer.intention

import dev.lumas.core.annotation.Autowire
import dev.lumas.core.annotation.Register
import dev.lumas.events.EventMain
import dev.lumas.events.explorer.custom.FullSecondRunnableEvent
import dev.lumas.events.explorer.custom.HalfSecondRunnableEvent
import dev.lumas.events.explorer.custom.TenSecondRunnableEvent
import dev.lumas.events.manager.EventPlayerManager
import dev.lumas.events.utility.Executors
import dev.lumas.events.utility.Util
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Sound
import org.bukkit.Tag
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
import org.bukkit.entity.Golem
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Mob
import org.bukkit.entity.Player
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

    private val WORLD = EventMain.getOkaeriConfig().suspendedWorlds // TODO: Change me
    private val TIMED_EXPLOSION = fun (loc: Location, delay: Long, player: Player) {
        loc.world.playSound(loc, Sound.ENTITY_CREEPER_PRIMED, 1f, 1f)
        Executors.delayedSync(loc, delay) {
            loc.world.createExplosion(loc, 25.0f)

            Executors.delayedSync(loc, 1) {
                for (player in loc.getNearbyPlayers(15.0)) {
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
    private val MINING_FATIGUE = PotionEffect(PotionEffectType.MINING_FATIGUE, 250, 0)
    private val BLINDNESS = PotionEffect(PotionEffectType.BLINDNESS, 250, 0)
    private val DARKNESS = PotionEffect(PotionEffectType.DARKNESS, 250, 0)
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
        EntityType.PUFFERFISH,
        EntityType.GUARDIAN,
        EntityType.ELDER_GUARDIAN,
        EntityType.ZOMBIE_NAUTILUS
    )
    private val IGNORE_WATER_DAMAGE_PREDICATE = { entity: Entity ->
        val type = entity.type
        Tag.ENTITY_TYPES_UNDEAD.isTagged(type) || IGNORED_AQUATIC_DAMAGES.contains(type)
    }
    private val DEATH_MESSAGE_COOLDOWN: Queue<UUID> = ConcurrentLinkedQueue()

    val DEATH_DELETE_INV_AND_UNSUSPEND = ExplorerIntent<PlayerDeathEvent>(
        title = "Death Deletes Inventory & Unsuspends",
        desc = "On a player's death, their inventory is deleted and they are unsuspended.",
        world = WORLD,
        eventClass = PlayerDeathEvent::class,
    ) { event ->
        val player = event.player
        event.isCancelled = true
        event.drops.clear()
        event.droppedExp = 0
        event.itemsToKeep.clear()
        player.inventory.clear()
        player.exp = 0f
        val eventPlayer = EventPlayerManager.getByUUIDOrNull(player.uniqueId)
        if (eventPlayer != null && eventPlayer.isSuspended) {
            eventPlayer.unsuspend()
        }

        if (!DEATH_MESSAGE_COOLDOWN.contains(player.uniqueId)) {
            Util.broadcast(event.deathMessage())
            DEATH_MESSAGE_COOLDOWN.add(player.uniqueId)
            Executors.delayedGlobal(10) {
                DEATH_MESSAGE_COOLDOWN.remove(player.uniqueId)
            }
        }
    }

    val CONTAINERS_BLOW_UP_WHEN_USED = ExplorerIntent<InventoryOpenEvent>(
        title = "Containers Explode When Opened",
        desc = "Real containers of any kind explode when opened.",
        world = WORLD,
        eventClass = InventoryOpenEvent::class,
    ) { event ->
        val type = event.inventory.type
        if (!VALID_CONTAINERS.contains(type)) {
            val holder = event.inventory.holder ?: return@ExplorerIntent
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
        eventClass = VillagerReplenishTradeEvent::class
    ) { event ->
        event.isCancelled = true
    }

    val WATER_DEALS_DAMAGE = ExplorerIntent<HalfSecondRunnableEvent>(
        title = "Standing in Water Deals Damage",
        desc = "Players standing in the water or rain take damage every half of a second.",
        world = WORLD,
        eventClass = HalfSecondRunnableEvent::class
    ) { event ->
        val player = event.player
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
        title = "Increase Mob Health & Damage",
        desc = "Mobs are significantly stronger than normal.",
        world = WORLD,
        eventClass = EntitySpawnEvent::class
    ) { event ->
        val entity = event.entity as? LivingEntity ?: return@ExplorerIntent
        entity.getAttribute(Attribute.MAX_HEALTH)?.let {
            if (it.getModifier(NAMESPACED_KEY) == null) {
                val value = it.value * 2.0
                it.addModifier(AttributeModifier(NAMESPACED_KEY, value, AttributeModifier.Operation.ADD_NUMBER))
                entity.health = it.value
            }
        }
        entity.getAttribute(Attribute.ATTACK_DAMAGE)?.let {
            if (it.getModifier(NAMESPACED_KEY) == null) {
                val value = it.value * 1.2
                it.addModifier(AttributeModifier(NAMESPACED_KEY, value, AttributeModifier.Operation.ADD_NUMBER))
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
        eventClass = EntityExplodeEvent::class
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
        eventClass = EntityDamageEvent::class
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
        eventClass = EntityDeathEvent::class
    ) { event ->
        val entity = event.entity
        if (entity is Animals) {
            event.drops.clear()
        }
    }

    val MINERS_HELL = ExplorerIntent<TenSecondRunnableEvent>(
        title = "Miner's Hell",
        desc = "Digging at or below Y level 4 grants mining fatigue and blindness.",
        world = WORLD,
        eventClass = TenSecondRunnableEvent::class
    ) { event ->
        val player = event.player
        if (player.location.blockY <= 4) {
            player.addPotionEffect(MINING_FATIGUE)
            player.addPotionEffect(BLINDNESS)
            player.addPotionEffect(DARKNESS)
        }
    }

    val FIRE_KILLS_WHEN_TOUCHED = ExplorerIntent<EntityDamageEvent>(
        title = "Fire & Lava Instantly Kills",
        desc = "Fire and lava deal massive damage.",
        world = WORLD,
        eventClass = EntityDamageEvent::class
    ) { event ->
        val player = event.entity as? Player ?: return@ExplorerIntent
        if (event.cause == EntityDamageEvent.DamageCause.FIRE || event.cause == EntityDamageEvent.DamageCause.LAVA) {
            player.health = 0.0
        }
    }

//    val NO_BENEFICIAL_POTION_EFFECTS = ExplorerIntent<EntityPotionEffectEvent>(
//        title = "No Beneficial Potion Effects",
//        desc = "Players do not receive any potion effects.",
//        world = WORLD,
//        eventClass = EntityPotionEffectEvent::class
//    ) { event ->
//        val player = event.entity as? Player ?: return@ExplorerIntent
//        val potionEffect = event.newEffect ?: return@ExplorerIntent
//        val type = potionEffect.type
//        if (type.category == PotionEffectTypeCategory.BENEFICIAL || type.effectCategory == PotionEffectType.Category.BENEFICIAL) {
//            event.isCancelled = true
//        }
//    }


    val ALWAYS_HOSTILE = ExplorerIntent<FullSecondRunnableEvent>(
        title = "Always Hostile",
        desc = "Neutral enemies are always hostile, hostile enemies will be able to target players from further away.",
        world = WORLD,
        eventClass = FullSecondRunnableEvent::class,
    ) { event ->
        val player = event.player
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
        desc = "Logs may not drop when broken.",
        world = WORLD,
        eventClass = BlockBreakEvent::class,
    ) { event ->
        val block = event.block
        if (Tag.LOGS.isTagged(block.type) && Random.nextBoolean()) {
            event.isDropItems = false
            block.world.playSound(block.location, Sound.ENTITY_ALLAY_ITEM_TAKEN, 1f, 1f)
        }
    }


    val ENTITIES_BLOW_UP_WHEN_TAMED = ExplorerIntent<EntityTameEvent>(
        title = "Taming Explosions",
        desc = "Tamed entities explode when tamed.",
        world = WORLD,
        eventClass = EntityTameEvent::class,
    ) { event ->
        TIMED_EXPLOSION(event.entity.location, 10L, event.owner as? Player ?: return@ExplorerIntent)
    }


    val RANDOM_LIGHTNING = ExplorerIntent<TenSecondRunnableEvent>(
        title = "Random Lightning",
        desc = "Lightning strikes randomly.",
        world = WORLD,
        eventClass = TenSecondRunnableEvent::class,
    ) { event ->
        val player = event.player
        // get a random location at least 60 blocks away from the player
        fun randomSign() = if (Random.nextBoolean()) 1.0 else -1.0

        val randomLocation = player.location.clone().add(
            Random.nextDouble(1.0, 100.0) * randomSign(),
            0.0,
            Random.nextDouble(1.0, 100.0) * randomSign()
        )
        player.world.strikeLightning(randomLocation)
    }
}