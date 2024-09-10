package dev.jsinco.lumacarnival.games.impls

import dev.jsinco.lumacarnival.CarnivalMain
import dev.jsinco.lumacarnival.Util
import dev.jsinco.lumacarnival.games.GameCommandExecutedEvent
import dev.jsinco.lumacarnival.games.GameSubCommand
import dev.jsinco.lumacarnival.games.GameTask
import dev.jsinco.lumacarnival.games.TaskAttributes
import dev.jsinco.lumacarnival.obj.Cuboid
import dev.jsinco.lumacarnival.obj.Sphere
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
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentLinkedQueue

@TaskAttributes(taskTime = 100L, async = true)
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
    private val sphere: Sphere = configSec.getString("sphere")?.let { Sphere(Util.getLocation(it), 10.0, 50.0) }
        ?: throw RuntimeException("Invalid sphere in config")
    private val maxTargets: Int = configSec.getInt("max")

    override fun enabled(): Boolean {
        return configSec.getBoolean("enabled")
    }

    fun getSafeAreaLocation(): CompletableFuture<Location> {
        return CompletableFuture.supplyAsync {
            val start = System.currentTimeMillis()
            val players = area.players
            var loc = area.randomLocation

            if (players.isNotEmpty()) {
                while (!players.any { it.hasLineOfSight(loc) } || sphere.isInSphere(loc) || !loc.block.isEmpty) {
                    loc = area.randomLocation
                }
            } else {
                while (sphere.isInSphere(loc) || !loc.block.isEmpty) {
                    loc = area.randomLocation
                }
            }


            CarnivalMain.instance.logger.info("Took ${System.currentTimeMillis() - start}ms to find a safe location for a target")
            loc
        }
    }


    fun spawnTarget() {
        // I'll just use armor stands for now.
        getSafeAreaLocation().thenAccept { loc ->
            Bukkit.getScheduler().runTask(CarnivalMain.instance, Runnable {
                val armorstand = area.world.createEntity(loc, ArmorStand::class.java)
                armorstand.isVisible = false
                armorstand.isPersistent = false
                armorstand.isInvulnerable = false
                armorstand.setItem(EquipmentSlot.HEAD, targetItemStack)
                armorstand.setCanTick(false)
                armorstand.spawnAt(loc, CreatureSpawnEvent.SpawnReason.CUSTOM)
                activeTargets.add(armorstand)
            })
        }
    }


    override fun initializeGame() {
        spawnTarget()

        val file = CarnivalMain.saves
        val list = file.getStringList("TargetPracticeEarner") ?: return
        list.forEach {
            totalTargetEarners.add(TargetPracticeEarner.deserialize(it))
        }
    }


    override fun save() {
        val file = CarnivalMain.saves
        file.set("TargetPracticeEarner", totalTargetEarners.map { it.serialize() })
        file.save()
    }

    override fun stopGame() {
        for (target in activeTargets) {
            target.remove()
        }
        activeTargets.clear()
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

        if (activeTargets.size < maxTargets && area.players.isNotEmpty()) {
            for (i in 0 until maxTargets - activeTargets.size) {
                spawnTarget()
            }
        }

        for (targetEarner in totalTargetEarners) {
            if (targetEarner.queuedAmount <= 0) {
                continue
            }
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

    @GameSubCommand("targetpractice-cashin", "lumacarnival.player", true)
    fun targetPracticeEarnerCashInCommand(event: GameCommandExecutedEvent) {
        val player = event.commandSender as Player
        val targetEarner = totalTargetEarners.find { it.playerUUID == player.uniqueId } ?: TargetPracticeEarner(player.uniqueId, 0).also { totalTargetEarners.add(it) }
        targetEarner.cashIn(player)
        Util.msg(player, "<green>You have cashed in your targets!")
    }

    @GameSubCommand("targetpractice-upgrade", "lumacarnival.player", true)
    fun upgradeBow(event: GameCommandExecutedEvent) {
        val player = event.commandSender as Player

        val bow = player.inventory.itemInMainHand
        if (!LumaItemsAPI.getInstance().isCustomItem(bow, "carnivaltargetpracticebow")) {
            Util.msg(player, "<red>You need a special bow to upgrade!")
            return
        }

        val effLevel = bow.enchantments[Enchantment.QUICK_CHARGE] ?: 0

        val targetEarner = totalTargetEarners.find { it.playerUUID == player.uniqueId } ?: TargetPracticeEarner(player.uniqueId, 0).also { totalTargetEarners.add(it) }
        val cost = 50 * (effLevel + 1)

        if (targetEarner.permanentAmount < cost) {
            Util.msg(player, "<red>You need $cost targets hit to upgrade your bow to the next level! <dark_gray>You have ${targetEarner.permanentAmount} targets hit.")
            return
        } else if (effLevel >= 4) {
            Util.msg(player, "<red>Your bow is already maxed out!")
            return
        }

        Bukkit.getScheduler().runTask(CarnivalMain.instance, Runnable {
            bow.addEnchantment(Enchantment.QUICK_CHARGE, effLevel + 1)
            Util.msg(player, "<green>Your bow has been upgraded to level ${effLevel + 1}!")
        })
    }
}