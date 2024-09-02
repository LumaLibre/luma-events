package dev.jsinco.lumacarnival.games

import dev.jsinco.lumacarnival.CarnivalMain
import dev.jsinco.lumacarnival.Util
import dev.jsinco.lumacarnival.obj.Cuboid
import dev.jsinco.lumaitems.api.LumaItemsAPI
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Item
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.player.PlayerFishEvent
import org.bukkit.event.player.PlayerItemConsumeEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType


class AppleBobbingGame : GameTask() {

    companion object {
        private val key = NamespacedKey(CarnivalMain.instance, "apple-bobbing")
        val regularApple = ItemStack(Material.APPLE).apply {
            itemMeta = itemMeta?.apply {
                addEnchant(Enchantment.ARROW_DAMAGE, 1, true)
                persistentDataContainer.set(key, PersistentDataType.BOOLEAN, true)
            }
        }
    }

    private val configSec = CarnivalMain.config.getConfigurationSection("apple-bobbing")
        ?: throw RuntimeException("Missing apple-bobbing section in config")
    private val area: Cuboid = configSec.getString("area")?.let { Util.getArea(it) }
        ?: throw RuntimeException("Invalid area in config")


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

    @EventHandler(priority = EventPriority.LOW)
    fun playerFishApple(event: PlayerFishEvent) {
        // Check if the player is in the apple bobbing area
        if (!area.isInWithMarge(event.hook.location, 2.0)) {
            return
        }

        var fishingRod = event.player.inventory.itemInMainHand
        if (fishingRod.type != Material.FISHING_ROD) {
            fishingRod = event.player.inventory.itemInOffHand
        }
        if (fishingRod.type != Material.FISHING_ROD) {
            return
        }

        if (!LumaItemsAPI().isCustomItem(fishingRod, "carnivalfishingrod")) {
            event.isCancelled = true
            event.player.sendMessage("You need a special fishing rod to catch apples!")
        }

        val item = event.caught as? Item ?: return
        item.itemStack = regularApple
    }

/*
    @EventHandler
    fun playerPickUpApple(event: EntityPickupItemEvent) {
        if (!activeApples.contains(event.item)) {
            return
        }

        val player = event.entity as? Player ?: return

        if (player.isInWater || area.isInWithMarge(player.location, 2.0)) {
            event.isCancelled = true
        }
    }
 */
}