package dev.jsinco.lumacarnival.games

import dev.jsinco.lumacarnival.CarnivalMain
import dev.jsinco.lumacarnival.Util
import dev.jsinco.lumacarnival.obj.Cuboid
import org.bukkit.Material
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.LivingEntity
import org.bukkit.event.entity.CreatureSpawnEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack

@TaskAttributes(taskTime = 1L, async = true)
class TargetPracticeGame : GameTask {

    companion object {
        val targetItemStack = ItemStack(Material.TARGET)
    }

    private val configSec = CarnivalMain.config.getConfigurationSection("target-practice")
        ?: throw RuntimeException("Missing target-practice section in config")

    private val area: Cuboid = configSec.getString("area")?.let { Util.getArea(it) }
        ?: throw RuntimeException("Invalid area in config")


    fun spawnTarget(): ArmorStand {
        // I'll just use armor stands for now.
        val armorstand = area.world.createEntity(area.randomLocation, ArmorStand::class.java)
        armorstand.isVisible = false
        armorstand.isPersistent = false
        armorstand.setItem(EquipmentSlot.HEAD, targetItemStack)
        armorstand.setCanTick(false)
        armorstand.spawnAt(area.randomLocation, CreatureSpawnEvent.SpawnReason.CUSTOM)
        return armorstand
    }


    override fun initializeGame() {
        TODO("Not yet implemented")
    }

    override fun tick() {
        TODO("Not yet implemented")
    }
}