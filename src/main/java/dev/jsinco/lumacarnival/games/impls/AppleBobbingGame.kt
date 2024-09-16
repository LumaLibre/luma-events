package dev.jsinco.lumacarnival.games.impls

import com.oheers.fish.api.EMFFishEvent
import dev.jsinco.lumacarnival.CarnivalMain
import dev.jsinco.lumacarnival.Util
import dev.jsinco.lumacarnival.games.GameCommandExecutedEvent
import dev.jsinco.lumacarnival.games.GameSubCommand
import dev.jsinco.lumacarnival.games.GameTask
import dev.jsinco.lumacarnival.obj.earners.AppleBobberEarner
import dev.jsinco.lumacarnival.obj.Cuboid
import dev.jsinco.lumaitems.api.LumaItemsAPI
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.World
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Item
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityPickupItemEvent
import org.bukkit.event.player.PlayerFishEvent
import org.bukkit.event.player.PlayerItemConsumeEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType


class AppleBobbingGame : GameTask() {

    class AppleBobbingEMFBlocker(private val world: World) : Listener {
        @EventHandler(priority = EventPriority.LOWEST)
        fun onEMFFish(event: EMFFishEvent) {
            if (event.player.world == world) {
                event.isCancelled = true
            }
        }
    }

    companion object {
        private val key = NamespacedKey(CarnivalMain.instance, "apple-bobbing")
        private val regularApple = ItemStack(Material.APPLE).apply {
            itemMeta = itemMeta?.apply {
                //displayName(Util.mm("<b><gradient:#569BDD:#DD3535>Soaked</gradient><gradient:#DD3535:#DD3535> Apple</gradient></b>"))
                //lore(Util.mml("<light_gray>A delicious apple soaked in water"))
                addEnchant(Enchantment.DURABILITY, 10, true)
                persistentDataContainer.set(key, PersistentDataType.BOOLEAN, true)
            }
        }

        private val appleBobberEarners: MutableList<AppleBobberEarner> = mutableListOf()
    }

    private val configSec = CarnivalMain.config.getConfigurationSection("apple-bobbing")
        ?: throw RuntimeException("Missing apple-bobbing section in config")
    private val area: Cuboid = configSec.getString("area")?.let { Util.getArea(it) }
        ?: throw RuntimeException("Invalid area in config")


    override fun enabled(): Boolean {
        return configSec.getBoolean("enabled")
    }

    override fun initializeGame() {
        val file = CarnivalMain.saves
        val list = file.getStringList("AppleBobberEarner") ?: return
        list.forEach {
            appleBobberEarners.add(AppleBobberEarner.deserialize(it))
        }

        // block emf
        if (Bukkit.getPluginManager().isPluginEnabled("EvenMoreFish")) {
            Bukkit.getPluginManager().registerEvents(AppleBobbingEMFBlocker(area.world), CarnivalMain.instance)
        }
    }


    override fun save() {
        val file = CarnivalMain.saves
        file.set("AppleBobberEarner", appleBobberEarners.map { it.serialize() })
        file.save()
    }


    @EventHandler
    fun onConsumeItem(event: PlayerItemConsumeEvent) {
        val item = event.item
        if (item.itemMeta?.persistentDataContainer?.has(key, PersistentDataType.BOOLEAN) == true) {
            Util.msg(event.player, "Delicious.")
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    fun playerFishApple(event: PlayerFishEvent) {
        // Check if the player is in the apple bobbing area
        if (!area.isInWithMarge(event.hook.location, 2.0)) {
            return
        } else if (event.state != PlayerFishEvent.State.CAUGHT_FISH) {
            return
        }

        val player = event.player

        var fishingRod = player.inventory.itemInMainHand
        if (fishingRod.type != Material.FISHING_ROD) {
            fishingRod = player.inventory.itemInOffHand
        }
        if (fishingRod.type != Material.FISHING_ROD) {
            return
        }

        if (!LumaItemsAPI.getInstance().isCustomItem(fishingRod, "carnivalfishingrod")) {
            event.isCancelled = true
            Util.msg(player, "You can only fish with the Carnival Fishing Rod!")
        }

        val item = event.caught as? Item ?: return
        item.itemStack.amount = 0
        val earner = appleBobberEarners.find { it.playerUUID == player.uniqueId } ?: AppleBobberEarner(
            player.uniqueId,
            0
        ).also { appleBobberEarners.add(it) }
        earner.increaseAmount(1)
    }


    fun onPlayerPickupApple(event: EntityPickupItemEvent) {
        val player = event.entity as? Player ?: return
        val item = event.item
        if (item.itemStack.itemMeta?.persistentDataContainer?.has(key, PersistentDataType.BOOLEAN) == true) {
            event.isCancelled = true
            item.remove()

            val earner = appleBobberEarners.find { it.playerUUID == player.uniqueId } ?: AppleBobberEarner(
                player.uniqueId,
                0
            ).also { appleBobberEarners.add(it) }
            earner.increaseAmount(1)
            Util.msg(player, "You picked up a soaked apple!")
        }
    }

    @GameSubCommand("applebobber-cashin", "lumacarnival.player", true)
    fun appleBobberCashInCommand(event: GameCommandExecutedEvent) {
        val player = event.commandSender as Player
        val earner = appleBobberEarners.find { it.playerUUID == player.uniqueId } ?: AppleBobberEarner(
            player.uniqueId,
            0
        ).also { appleBobberEarners.add(it) }
        earner.cashIn(player)
        Util.msg(player, "You have cashed in your soaked apples!")
    }

    @GameSubCommand("applebobber-upgrade", "lumacarnival.player", true)
    fun upgradeFishingRod(event: GameCommandExecutedEvent) {
        val player = event.commandSender as Player

        val fishingRod = player.inventory.itemInMainHand
        if (!LumaItemsAPI.getInstance().isCustomItem(fishingRod, "carnivalfishingrod")) {
            Util.msg(player, "You need a special fishing rod to upgrade!")
            return
        }

        val effLevel = fishingRod.enchantments[Enchantment.LURE] ?: 0

        val earner = appleBobberEarners.find { it.playerUUID == player.uniqueId } ?: AppleBobberEarner(
            player.uniqueId,
            0
        ).also { appleBobberEarners.add(it) }
        val cost = 50 * (effLevel + 1)

        if (earner.permanentAmount < cost) {
            Util.msg(player, "You need $cost soaked apples to upgrade your fishing rod to the next level! You have ${earner.permanentAmount} soaked apples.")
            return
        }


        if (effLevel >= 6) {
            Util.msg(player, "Your fishing rod is already maxed out!")
            return
        }

        Bukkit.getScheduler().runTask(CarnivalMain.instance, Runnable {
            fishingRod.addUnsafeEnchantment(Enchantment.LURE, effLevel + 1)
            Util.msg(player, "<green>Your fishing rod has been upgraded to level ${effLevel + 1}!")
        })
    }
}