package dev.jsinco.lumacarnival.games

import dev.jsinco.lumacarnival.CarnivalMain
import dev.jsinco.lumacarnival.Util
import dev.jsinco.lumacarnival.obj.Cuboid
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.entity.CreatureSpawnEvent
import org.bukkit.event.entity.ProjectileHitEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import java.util.concurrent.ConcurrentLinkedQueue

@TaskAttributes(taskTime = 50L, async = true)
class TargetPracticeGame : GameTask() {

    companion object {
        val targetItemStack = ItemStack(Material.TARGET).apply {
            itemMeta = itemMeta?.apply {
                addEnchant(Enchantment.ARROW_DAMAGE, 1, true)
            }
        }
        val activeTargets: ConcurrentLinkedQueue<LivingEntity> = ConcurrentLinkedQueue()
    }

    private val configSec = CarnivalMain.config.getConfigurationSection("target-practice")
        ?: throw RuntimeException("Missing target-practice section in config")
    private val area: Cuboid = configSec.getString("area")?.let { Util.getArea(it) }
        ?: throw RuntimeException("Invalid area in config")
    private val maxTargets: Int = configSec.getInt("max-targets")


    fun spawnTarget(): LivingEntity {
        // I'll just use armor stands for now.
        val armorstand = area.world.createEntity(area.randomLocation, ArmorStand::class.java)
        armorstand.isVisible = false
        armorstand.isPersistent = false
        armorstand.isInvulnerable = false
        armorstand.setItem(EquipmentSlot.HEAD, targetItemStack)
        armorstand.setCanTick(false)
        armorstand.spawnAt(area.randomLocation, CreatureSpawnEvent.SpawnReason.CUSTOM)
        return armorstand
    }


    override fun initializeGame() {
        if (activeTargets.isNotEmpty()) {
            activeTargets.forEach { it.remove() }
            activeTargets.clear()
        }
        activeTargets.add(spawnTarget())
    }

    override fun tick() {
        // shift each target's location
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
    }

    override fun enabled(): Boolean {
        return configSec.getBoolean("enabled")
    }


    @EventHandler
    fun onProjectileHit(event: ProjectileHitEvent) {
        if (!activeTargets.contains(event.hitEntity ?: return)) {
            return
        }

        val target = event.hitEntity as LivingEntity
        target.remove()
        activeTargets.remove(target)
        (event.entity.shooter as Player).sendMessage("You hit the target!")
        event.entity.remove()
    }
}