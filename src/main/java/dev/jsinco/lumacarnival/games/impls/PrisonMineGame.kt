package dev.jsinco.lumacarnival.games.impls

import dev.jsinco.lumacarnival.CarnivalMain
import dev.jsinco.lumacarnival.Util
import dev.jsinco.lumacarnival.games.GameCommandExecutedEvent
import dev.jsinco.lumacarnival.games.GameSubCommand
import dev.jsinco.lumacarnival.games.GameTask
import dev.jsinco.lumacarnival.obj.Cuboid
import dev.jsinco.lumacarnival.obj.earners.PrisonMineEarner
import dev.jsinco.lumaitems.api.LumaItemsAPI
import org.bukkit.Bukkit
import org.bukkit.Color
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.block.BlockBreakEvent

class PrisonMineGame : GameTask() {

    companion object {
        val prisonMineEarners: MutableList<PrisonMineEarner> = mutableListOf()
    }

    private val enabled = Bukkit.getPluginManager().isPluginEnabled("JetsPrisonMines")

    private val configSec = CarnivalMain.config.getConfigurationSection("prison-mine") ?: throw RuntimeException("Missing prison-mine section in config")
    private val area: Cuboid = configSec.getString("area")?.let { Util.getArea(it) } ?: throw RuntimeException("Invalid area in config")

    override fun enabled(): Boolean {
        return enabled
    }


    override fun initializeGame() {
        val file = CarnivalMain.saves
        val list = file.getStringList("PrisonMineEarner") ?: return
        list.forEach {
            prisonMineEarners.add(PrisonMineEarner.deserialize(it))
        }
    }

    override fun save() {
        val file = CarnivalMain.saves
        file.set("PrisonMineEarner", prisonMineEarners.map { it.serialize() })
        file.save()
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onPlayerBreakBlock(event: BlockBreakEvent) {
        if (!area.isIn(event.block.location)) {
            return
        }
        event.isDropItems = false
        event.expToDrop = 0
        val player = event.player

        if (!LumaItemsAPI.getInstance().isCustomItem(player.inventory.itemInMainHand, "carnivalminingpickaxe")) {
            Util.msg(player, "<red>You need a special pickaxe to mine here!")
            event.isCancelled = true
            return
        }

        event.isCancelled = false
        val prisonMineEarner = prisonMineEarners.find { it.playerUUID == player.uniqueId } ?: PrisonMineEarner(
            player.uniqueId,
            0
        ).also { prisonMineEarners.add(it) }
        prisonMineEarner.increaseAmount(1)

        // effects
        val block = event.block
        val colorRaw = dev.jsinco.lumaitems.util.Util.getColor(block)
        Bukkit.getScheduler().runTaskAsynchronously(CarnivalMain.instance, Runnable {
            player.spawnParticle(Particle.REDSTONE, block.location.toCenterLocation(), 5, 0.2, 0.2, 0.2,
                Particle.DustOptions(Color.fromRGB(colorRaw.red, colorRaw.blue, colorRaw.green), 1.0f))
            player.sendActionBar(Util.mm("<gradient:#8ec4f7:#ff9ccb>Blo</gradient><gradient:#ff9ccb:#d7f58d>ck</gradient><gradient:#d7f58d:#fffe8a>s: </gradient><gradient:#fffe8a:#ffd365>${prisonMineEarner.amount}</gradient>"))
        })
        player.playSound(block.location, Sound.ENTITY_ITEM_PICKUP, 0.3f, 1.4f)
    }


    @GameSubCommand("prisonmine-cashin", "lumacarnival.player", true)
    fun prisonMineEarnerCashInCommand(event: GameCommandExecutedEvent) {
        val player = event.commandSender as Player
        val prisonMineEarner = prisonMineEarners.find { it.playerUUID == player.uniqueId } ?: return
        prisonMineEarner.cashIn(player)
        Util.msg(player, "<green>You have cashed in your mined blocks!")
    }

    @GameSubCommand("prisonmine-upgrade", "lumacarnival.player", true)
    fun upgradePickaxe(event: GameCommandExecutedEvent) {
        val player = event.commandSender as Player

        val pickaxe = player.inventory.itemInMainHand
        if (!LumaItemsAPI.getInstance().isCustomItem(pickaxe, "carnivalminingpickaxe")) {
            Util.msg(player, "<red>You need a special pickaxe to upgrade!")
            return
        }

        val effLevel = pickaxe.enchantments[Enchantment.DIG_SPEED] ?: 0

        val prisonMineEarner = prisonMineEarners.find { it.playerUUID == player.uniqueId } ?: PrisonMineEarner(
            player.uniqueId,
            0
        ).also { prisonMineEarners.add(it) }
        val cost = 1300 * (effLevel + 1)

        if (prisonMineEarner.permanentAmount < cost) {
            Util.msg(player, "<red>You need $cost mined blocks to upgrade your pickaxe to the next level! <dark_gray>You have ${prisonMineEarner.permanentAmount} mined blocks.")
            return
        }


        Bukkit.getScheduler().runTask(CarnivalMain.instance, Runnable {
            if (effLevel >= 3 && pickaxe.type != Material.NETHERITE_PICKAXE) {
                pickaxe.type = Material.NETHERITE_PICKAXE
            } else if (effLevel >= 8) {
                Util.msg(player, "<red>Your pickaxe is already maxed out!")
                return@Runnable
            }

            pickaxe.addUnsafeEnchantment(Enchantment.DIG_SPEED, effLevel + 1)
            Util.msg(player, "<green>Your pickaxe has been upgraded to level <yellow>${effLevel + 1}</yellow>!")
        })
    }
}