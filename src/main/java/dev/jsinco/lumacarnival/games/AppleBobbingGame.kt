package dev.jsinco.lumacarnival.games

import dev.jsinco.lumacarnival.CarnivalMain
import dev.jsinco.lumacarnival.Util
import dev.jsinco.lumacarnival.obj.Cuboid
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Item
import org.bukkit.event.EventHandler
import org.bukkit.event.player.PlayerItemConsumeEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import java.util.concurrent.ConcurrentLinkedQueue

@TaskAttributes(taskTime = 5L, async = true)
class AppleBobbingGame : GameTask() {

    companion object {
        private val key = NamespacedKey(CarnivalMain.instance, "apple-bobbing")
        val regularApple = ItemStack(Material.APPLE).apply {
            itemMeta = itemMeta?.apply {
                addEnchant(Enchantment.ARROW_DAMAGE, 1, true)
                persistentDataContainer.set(key, PersistentDataType.BOOLEAN, true)
            }
        }
        val activeApples: ConcurrentLinkedQueue<Item> = ConcurrentLinkedQueue()
    }

    private val configSec = CarnivalMain.config.getConfigurationSection("apple-bobbing")
        ?: throw RuntimeException("Missing apple-bobbing section in config")
    private val area: Cuboid = configSec.getString("area")?.let { Util.getArea(it) }
        ?: throw RuntimeException("Invalid area in config")
    private val returnLocation = configSec.getString("return-spot")?.let { Util.getLocation(it) }
        ?: throw RuntimeException("Invalid return spot in config")
    private val maxTargets: Int = configSec.getInt("max-apples")

    fun spawnApple(location: Location): Item {
        val apple = area.world.dropItem(location, regularApple)
        apple.setWillAge(false)
        apple.isPersistent = false
        return apple
    }

    fun getWaterBlock(): Location {
        var loc = area.randomLocation
        var tries = 0
        while (loc.block.type != Material.WATER && tries < 10) {
            loc = area.randomLocation
            tries++
        }
        return loc
    }

    override fun initializeGame() {
        if (activeApples.isNotEmpty()) {
            activeApples.forEach { it.remove() }
            activeApples.clear()
        }
        val water = getWaterBlock()
        activeApples.add(spawnApple(water))
    }

    override fun tick() {

        for (apple in activeApples) {
            if (apple.isDead) {
                activeApples.remove(apple)
            }
        }

        if (activeApples.size < maxTargets) {
            val water = getWaterBlock()
            Bukkit.getScheduler().runTask(CarnivalMain.instance, Runnable {
                activeApples.add(spawnApple(water))
            })
        }

        val players = area.world.players
        for (player in players) {
            if (area.isIn(player)) {
                player.teleportAsync(returnLocation)
                player.sendMessage("no")
            }
        }
    }

    override fun enabled(): Boolean {
        return configSec.getBoolean("enabled")
    }

    @EventHandler
    fun onConsumeItem(event: PlayerItemConsumeEvent) {
        val item = event.item
        if (item.itemMeta?.persistentDataContainer?.has(key, PersistentDataType.BOOLEAN) == true) {
            event.player.sendMessage("You ate an apple!")
        }
    }
}