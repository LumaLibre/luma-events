package dev.jsinco.lumacarnival.games.impls

import dev.jsinco.lumacarnival.CarnivalMain
import dev.jsinco.lumacarnival.Util
import dev.jsinco.lumacarnival.games.GameCommandExecutedEvent
import dev.jsinco.lumacarnival.games.GameSubCommand
import dev.jsinco.lumacarnival.games.GameTask
import dev.jsinco.lumacarnival.obj.Cuboid
import dev.jsinco.lumacarnival.obj.PrisonMineEarner
import dev.jsinco.lumaitems.api.LumaItemsAPI
import org.bukkit.Bukkit
import org.bukkit.Material
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

    override fun stopGame() {
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
        val prisonMineEarner = prisonMineEarners.find { it.playerUUID == player.uniqueId } ?: PrisonMineEarner(player.uniqueId, 0).also { prisonMineEarners.add(it) }
        prisonMineEarner.increaseAmount(1)
    }


    @GameSubCommand("prisonmine-cashin", "lumacarnival.prisonmine", true)
    fun prisonMineEarnerCashInCommand(event: GameCommandExecutedEvent) {
        val player = event.commandSender as Player
        val prisonMineEarner = prisonMineEarners.find { it.playerUUID == player.uniqueId } ?: return
        prisonMineEarner.cashIn(player)
        Util.msg(player, "<green>You have cashed in your mined blocks!")
    }

    @GameSubCommand("prisonmine-upgrade", "lumacarnival.prisonmine", true)
    fun upgradePickaxe(event: GameCommandExecutedEvent) {
        val player = event.commandSender as Player

        val pickaxe = player.inventory.itemInMainHand
        if (!LumaItemsAPI.getInstance().isCustomItem(pickaxe, "carnivalminingpickaxe")) {
            Util.msg(player, "<red>You need a special pickaxe to upgrade!")
            return
        }

        val effLevel = pickaxe.enchantments[Enchantment.DIG_SPEED] ?: 0

        val prisonMineEarner = prisonMineEarners.find { it.playerUUID == player.uniqueId } ?: PrisonMineEarner(player.uniqueId, 0).also { prisonMineEarners.add(it) }
        val cost = 1000 * (effLevel + 1)

        if (prisonMineEarner.permanentAmount < cost) {
            Util.msg(player, "<red>You need $cost mined blocks to upgrade your pickaxe to the next level! <dark_gray>You have ${prisonMineEarner.permanentAmount} mined blocks.")
            return
        }


        if (effLevel >= 3 && pickaxe.type != Material.NETHERITE_PICKAXE) {
            pickaxe.type = Material.NETHERITE_PICKAXE
        } else if (effLevel >= 8) {
            Util.msg(player, "<red>Your pickaxe is already maxed out!")
            return
        }

        pickaxe.addUnsafeEnchantment(Enchantment.DIG_SPEED, effLevel + 1)
        Util.msg(player, "<green>Your pickaxe has been upgraded to level <yellow>${effLevel + 1}</yellow>!")
    }
}