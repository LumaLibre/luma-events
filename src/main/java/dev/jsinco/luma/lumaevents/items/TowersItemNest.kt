package dev.jsinco.luma.lumaevents.items

import com.destroystokyo.paper.event.entity.EntityKnockbackByEntityEvent
import dev.jsinco.luma.lumaevents.utility.Util
import dev.jsinco.luma.lumaitems.items.ItemFactory
import dev.jsinco.luma.lumaitems.manager.CustomItemFunctions
import dev.jsinco.luma.lumaitems.util.Executors
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.EnderSignal
import org.bukkit.entity.Entity
import org.bukkit.entity.Fireball
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.block.Action
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.ProjectileHitEvent
import org.bukkit.event.entity.ProjectileLaunchEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType


abstract class TowersItemNestItem : CustomItemFunctions() {

}

class TowersKnockbackStickItem : TowersItemNestItem() {
    override fun createItem(): Pair<String, ItemStack> {
        return ItemFactory.builder()
            .name("<yellow><b>Knockback Stick")
            .material(Material.STICK)
            .vanillaEnchants(Enchantment.KNOCKBACK to 10)
            .lore(
                "You've done well to read the lore",
                "of this stick. You may be thinking,",
                "\"What could stop me now?\"",
                "",
                "This stick has a 25% chance to",
                "reverse all knockback effects",
                "upon the user of it."
            )
            .persistentData("towers-knockback-stick")
            .buildPair()
    }

    override fun onPlayerKnockbackEntity(player: Player, event: EntityKnockbackByEntityEvent) {
        if (random().nextInt(100) >= 25) return
        val otherPlayer = event.entity as? Player ?: return

        event.isCancelled = true
        player.knockback(event.knockbackStrength.toDouble(), otherPlayer.location.x - player.location.x, otherPlayer.location.z - player.location.z)

    }
}


class TowersFireballItem : TowersItemNestItem() {
    override fun createItem(): Pair<String, ItemStack> {
        return ItemFactory.builder()
            .name("<yellow><b>Fireball")
            .material(Material.FIRE_CHARGE)
            .lore(
                "Right-click to fire",
                "a ghast fire ball towards",
                "the direction you are",
                "facing."
            )
            .vanillaEnchants(Enchantment.UNBREAKING to 1)
            .hideEnchants(true)
            .persistentData("towers-fireball")
            .buildPair()
    }

    override fun onRightClick(player: Player, event: PlayerInteractEvent) {
        if (event.action != Action.RIGHT_CLICK_AIR && event.action != Action.RIGHT_CLICK_BLOCK) return

        player.launchProjectile(Fireball::class.java)
        event.isCancelled = true
        event.item?.amount -= 1
    }
}


class TowersSatchelItem : TowersItemNestItem() {
    override fun createItem(): Pair<String, ItemStack> {
        return ItemFactory.builder()
            .name("<yellow><b>Satchel")
            .material(Material.SNOWBALL)
            .lore(
                // TODO: Lore
            )
            .persistentData("towers-satchel")
            .buildPair()
    }


    override fun onProjectileLaunch(player: Player, event: ProjectileLaunchEvent) {
        val entity = event.entity
        Util.setPersistentKey(entity, "towers-satchel", PersistentDataType.SHORT, 1)

        if (player.fallDistance < 0.1 || player.isFlying) return
        Executors.syncDelayed(5) {
            if (!entity.isDead) {
                detonate(entity)
            }
        }
    }

    override fun onProjectileLand(player: Player, event: ProjectileHitEvent) {
        detonate(event.entity)
    }

    private fun detonate(entity: Entity) {
        for (nearby in entity.getNearbyEntities(3.0, 6.5, 3.0)) {
            val directionAway = nearby.location.toVector()
                .subtract(entity.location.toVector())
                .normalize()
            directionAway.y += 1.0

            nearby.velocity = directionAway.multiply(2.0)
        }
        val world = entity.world
        world.playSound(entity.location, Sound.ENTITY_ALLAY_AMBIENT_WITH_ITEM, 1f, 1f)
        world.spawnParticle(Particle.GLOW, entity.location, 4)
        entity.remove()
    }
}

class TowersNearSighterItem : TowersItemNestItem() {

    companion object {
        private val DARKNESS = PotionEffect(PotionEffectType.DARKNESS, 100, 0)
        private val POISON = PotionEffect(PotionEffectType.POISON, 100, 1)
    }

    override fun createItem(): Pair<String, ItemStack> {
        return ItemFactory.builder()
            .name("<yellow><b>Near Sighter")
            .material(Material.SNOWBALL)
            .lore(
                // TODO: Lore
            )
            .persistentData("towers-near-sighter")
            .buildPair()
    }

    override fun onProjectileLaunch(player: Player, event: ProjectileLaunchEvent) {
        event.isCancelled = true
        val enderSignal = player.world.spawn(player.eyeLocation.add(0.0, 2.0, 0.0), EnderSignal::class.java) {
            it.targetLocation = event.entity.location
        }
        Executors.syncTimer(0, 1) {
            if (enderSignal.isDead) {
                it.cancel()
                return@syncTimer
            }
            for (entity in enderSignal.getNearbyEntities(50.0, 50.0, 50.0)) {
                if (entity !is LivingEntity) continue
                entity.addPotionEffect(DARKNESS)
                entity.addPotionEffect(POISON)
            }
        }
    }
}

class TowersProjectileResistantItem : TowersItemNestItem() {
    override fun createItem(): Pair<String, ItemStack> {
        return ItemFactory.builder()
            .name("<yellow><b>Projectile Resistant Helmet")
            .material(Material.LEATHER_HELMET)
            .persistentData("towers-projectile-resistant-helmet")
            .lore(
                // TODO: Lore
            )
            .buildPair()
    }

    override fun onPlayerDamaged(player: Player, event: EntityDamageEvent) {
        if (event.cause != EntityDamageEvent.DamageCause.PROJECTILE) return
    }

}

class TowersThornBootsItem : TowersItemNestItem() {
    override fun createItem(): Pair<String, ItemStack> {
        return ItemFactory.builder()
            .name("<aqua>Iron Boots")
            .material(Material.IRON_BOOTS)
            .persistentData("towers-thorn-boots")
            .vanillaEnchants(Enchantment.THORNS to 6)
            .buildPair()
    }
}