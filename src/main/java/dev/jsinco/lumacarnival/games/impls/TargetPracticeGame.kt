package dev.jsinco.lumacarnival.games.impls

import dev.jsinco.lumacarnival.CarnivalMain
import dev.jsinco.lumacarnival.Util
import dev.jsinco.lumacarnival.games.GameTask
import dev.jsinco.lumacarnival.games.TaskAttributes
import dev.jsinco.lumacarnival.obj.Cuboid
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.entity.CreatureSpawnEvent
import org.bukkit.event.entity.ProjectileHitEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import java.util.concurrent.ConcurrentLinkedQueue

@TaskAttributes(taskTime = 50L, async = true)
class TargetPracticeGame : GameTask() {

    companion object {
        val targetItemStack = ItemStack(Material.TARGET).apply {
            itemMeta = itemMeta?.apply {
                addEnchant(Enchantment.ARROW_DAMAGE, 1, true)
            }
        }
        val targetToken = ItemStack(Material.TARGET).apply {
            itemMeta = itemMeta?.apply {
                displayName(Util.mm("<b><gradient:#fdee21:#ed1e26>Flyi</gradient><gradient:#ed1e26:#22a9e1>ng Ta</gradient><gradient:#22a9e1:#ffffff>rget</gradient></b>"))
                lore(Util.mml("<gray>A target that flies through the air"))
                addEnchant(Enchantment.DURABILITY, 10, true)
                persistentDataContainer.set(NamespacedKey(CarnivalMain.instance, "target-practice"), PersistentDataType.BOOLEAN, true)
            }
        }
        val activeTargets: ConcurrentLinkedQueue<LivingEntity> = ConcurrentLinkedQueue()
        val queuedEarners: MutableMap<Player, Int> = mutableMapOf()
    }

    private val configSec = CarnivalMain.config.getConfigurationSection("target-practice")
        ?: throw RuntimeException("Missing target-practice section in config")
    private val area: Cuboid = configSec.getString("area")?.let { Util.getArea(it) }
        ?: throw RuntimeException("Invalid area in config")
    private val maxTargets: Int = configSec.getInt("max")

    fun getSafeAreaLocation(): Location {
        var loc = area.randomLocation
        var tries = 0
        while (!loc.block.isEmpty && tries < 10) {
            loc = area.randomLocation
            tries++
        }
        return loc
    }


    fun spawnTarget(): LivingEntity {
        // I'll just use armor stands for now.
        val loc = getSafeAreaLocation()
        val armorstand = area.world.createEntity(loc, ArmorStand::class.java)
        armorstand.isVisible = false
        armorstand.isPersistent = false
        armorstand.isInvulnerable = false
        armorstand.setItem(EquipmentSlot.HEAD, targetItemStack)
        armorstand.setCanTick(false)
        armorstand.spawnAt(loc, CreatureSpawnEvent.SpawnReason.CUSTOM)
        return armorstand
    }


    override fun initializeGame() {
        activeTargets.add(spawnTarget())
    }


    override fun stopGame() {
        if (activeTargets.isNotEmpty()) {
            activeTargets.forEach { it.remove() }
            activeTargets.clear()
        }
    }

    override fun tick() {
        for (target in activeTargets) {
            if (target.isDead) {
                activeTargets.remove(target)
            } else if (target.ticksLived > 30000) {
                Bukkit.getScheduler().runTask(CarnivalMain.instance, Runnable {
                    target.remove()
                })
                activeTargets.remove(target)
            }
        }

        if (activeTargets.size < maxTargets) {
            Bukkit.getScheduler().runTask(CarnivalMain.instance, Runnable {
                activeTargets.add(spawnTarget())
            })
        }

        for (key in queuedEarners) {
            val player = key.key
            val amount = key.value
            Util.giveItem(player, targetToken.asQuantity(amount))
            Util.msg(player, "<yellow>+${amount}</yellow> targets hit!")
        }
    }

    override fun enabled(): Boolean {
        return configSec.getBoolean("enabled")
    }


    @EventHandler
    fun onProjectileHit(event: ProjectileHitEvent) {
        val player = event.entity.shooter as? Player ?: return
        if (!activeTargets.contains(event.hitEntity ?: return)) {
            return
        }

        if (!event.entity.hasMetadata("carnival_target_practice_bow")) {
            Util.msg(player, "You must use a special bow for these targets!")
        } else {
            val target = event.hitEntity as LivingEntity
            target.remove()
            activeTargets.remove(target)
            queuedEarners[player] = queuedEarners.getOrDefault(player, 0) + 1
        }

        event.entity.remove()
    }
}