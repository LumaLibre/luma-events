package dev.jsinco.lumacarnival.games.impls

import dev.jsinco.lumacarnival.CarnivalMain
import dev.jsinco.lumacarnival.Util
import dev.jsinco.lumacarnival.games.GameTask
import dev.jsinco.lumacarnival.games.TaskAttributes
import dev.jsinco.lumacarnival.obj.Cuboid
import org.bukkit.Bukkit
import org.bukkit.DyeColor
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.TropicalFish
import org.bukkit.event.EventHandler
import org.bukkit.event.entity.CreatureSpawnEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerInteractEntityEvent
import java.util.concurrent.ConcurrentLinkedQueue

@TaskAttributes(taskTime = 40L, async = true)
class UnderwaterAnimalCatchingGame : GameTask() {

    companion object {

        val activeAnimals: ConcurrentLinkedQueue<LivingEntity> = ConcurrentLinkedQueue()
    }

    private val configSec = CarnivalMain.config.getConfigurationSection("underwater-animals")
        ?: throw RuntimeException("Missing target-practice section in config")
    private val area: Cuboid = configSec.getString("area")?.let { Util.getArea(it) }
        ?: throw RuntimeException("Invalid area in config")
    private val maxAnimals: Int = configSec.getInt("max")



    fun getSafeAreaLocation(): Location {
        var loc = area.randomLocation
        var tries = 0
        while (loc.block.type != Material.WATER && tries < 10) {
            loc = area.randomLocation
            tries++
        }
        return loc
    }


    fun spawnAnimal(): LivingEntity {
        // I'll just use tropical fish for now.
        val loc = getSafeAreaLocation()
        val fish = area.world.createEntity(loc, TropicalFish::class.java)
        fish.bodyColor = DyeColor.entries.random()
        fish.pattern = TropicalFish.Pattern.entries.random()
        fish.isPersistent = false
        fish.spawnAt(loc, CreatureSpawnEvent.SpawnReason.CUSTOM)
        return fish
    }

    override fun initializeGame() {
        activeAnimals.add(spawnAnimal())
    }

    override fun stopGame() {
        for (animal in activeAnimals) {
            animal.remove()
        }
        activeAnimals.clear()
    }

    override fun tick() {
        for (animal in activeAnimals) {
            if (animal.isDead) {
                activeAnimals.remove(animal)
            }
        }

        if (activeAnimals.size < maxAnimals) {
            Bukkit.getScheduler().runTask(CarnivalMain.instance, Runnable {
                activeAnimals.add(spawnAnimal())
            })
        }
    }


    override fun enabled(): Boolean {
        return configSec.getBoolean("enabled")
    }

    @EventHandler
    fun onPlayerDeath(event: PlayerDeathEvent) {
        val player = event.entity

        if (!area.isIn(player)) {
            return
        }

        event.keepInventory = true
        event.keepLevel = true
        event.drops.clear()
        event.droppedExp = 0
        event.deathMessage(Util.mm("${player.name} drowned in a giant fish tank"))
    }

    @EventHandler
    fun onEntityInteract(event: PlayerInteractEntityEvent) {
        if (!activeAnimals.contains(event.rightClicked)) {
            return
        }

        activeAnimals.remove(event.rightClicked)
        event.rightClicked.remove()
        event.player.sendMessage(Util.mm("<purple>bloop"))
    }
}