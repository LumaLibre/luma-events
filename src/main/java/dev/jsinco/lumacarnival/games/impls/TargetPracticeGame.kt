package dev.jsinco.lumacarnival.games.impls

import dev.jsinco.lumacarnival.CarnivalMain
import dev.jsinco.lumacarnival.Util
import dev.jsinco.lumacarnival.games.GameCommandExecutedEvent
import dev.jsinco.lumacarnival.games.GameSubCommand
import dev.jsinco.lumacarnival.games.GameTask
import dev.jsinco.lumacarnival.games.TaskAttributes
import dev.jsinco.lumacarnival.obj.Cuboid
import dev.jsinco.lumacarnival.obj.TargetPracticeEarner
import dev.jsinco.lumaitems.api.LumaItemsAPI
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.entity.CreatureSpawnEvent
import org.bukkit.event.entity.ProjectileHitEvent
import org.bukkit.event.entity.ProjectileLaunchEvent
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
        val totalTargetEarners: ConcurrentLinkedQueue<TargetPracticeEarner> = ConcurrentLinkedQueue()
    }

    private val configSec = CarnivalMain.config.getConfigurationSection("target-practice")
        ?: throw RuntimeException("Missing target-practice section in config")
    private val area: Cuboid = configSec.getString("area")?.let { Util.getArea(it) }
        ?: throw RuntimeException("Invalid area in config")
    private val maxTargets: Int = configSec.getInt("max")

    override fun enabled(): Boolean {
        return configSec.getBoolean("enabled")
    }

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

        val file = CarnivalMain.saves
        val list = file.getStringList("TargetPracticeEarner") ?: return
        list.forEach {
            totalTargetEarners.add(TargetPracticeEarner.deserialize(it))
        }
    }


    override fun stopGame() {
        val file = CarnivalMain.saves
        file.set("TargetPracticeEarner", totalTargetEarners.map { it.serialize() })
        file.save()

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

        for (targetEarner in totalTargetEarners) {
            Util.msg(targetEarner.player, "<b><gold>+${targetEarner.queuedAmount}</gold></b> targets hit! <dark_gray>(Total: ${targetEarner.amount})")
            targetEarner.queuedAmount = 0
        }
    }



    @EventHandler(priority = EventPriority.HIGHEST)
    fun onProjectileLaunch(event: ProjectileLaunchEvent) {
        if (area.isIn(event.entity.location)) {
            event.isCancelled = false
        }
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

            val targetEarner = totalTargetEarners.find { it.playerUUID == player.uniqueId } ?: TargetPracticeEarner(player.uniqueId, 0).also { totalTargetEarners.add(it) }
            targetEarner.increaseAmount(1)
            targetEarner.increaseQueuedAmount(1)
        }

        event.entity.remove()
    }

    @GameSubCommand("targetpractice-cashin", "lumacarnival.targetpractice", true)
    fun targetPracticeEarnerCashInCommand(event: GameCommandExecutedEvent) {
        val player = event.commandSender as Player
        val targetEarner = totalTargetEarners.find { it.playerUUID == player.uniqueId } ?: return
        targetEarner.cashIn(player)
        Util.msg(player, "<green>You have cashed in your targets!")
    }

    @GameSubCommand("targetpractice-upgrade", "lumacarnival.targetpractice", true)
    fun upgradeBow(event: GameCommandExecutedEvent) {
        val player = event.commandSender as Player

        val bow = player.inventory.itemInMainHand
        if (!LumaItemsAPI.getInstance().isCustomItem(bow, "carnivaltargetpracticebow")) {
            Util.msg(player, "<red>You need a special bow to upgrade!")
            return
        }

        val effLevel = bow.enchantments[Enchantment.QUICK_CHARGE] ?: 0

        val targetEarner = totalTargetEarners.find { it.playerUUID == player.uniqueId } ?: TargetPracticeEarner(player.uniqueId, 0).also { totalTargetEarners.add(it) }
        val cost = 1000 * (effLevel + 1)

        if (targetEarner.permanentAmount < cost) {
            Util.msg(player, "<red>You need $cost targets hit to upgrade your bow to the next level! <dark_gray>You have ${targetEarner.permanentAmount} targets hit.")
            return
        } else if (effLevel >= 4) {
            Util.msg(player, "<red>Your bow is already maxed out!")
            return
        }

        bow.addEnchantment(Enchantment.QUICK_CHARGE, effLevel + 1)
        Util.msg(player, "<green>Your bow has been upgraded to level ${effLevel + 1}!")
    }
}