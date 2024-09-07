package dev.jsinco.lumacarnival.games.impls

import dev.jsinco.lumacarnival.CarnivalMain
import dev.jsinco.lumacarnival.CarnivalToken
import dev.jsinco.lumacarnival.Util
import dev.jsinco.lumacarnival.games.GameTask
import dev.jsinco.lumacarnival.obj.Cuboid
import dev.jsinco.lumaitems.api.LumaItemsAPI
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.block.BlockBreakEvent
import kotlin.random.Random

class PrisonMineGame : GameTask() {

    private val enabled = Bukkit.getPluginManager().isPluginEnabled("JetsPrisonMines")

    private val configSec = CarnivalMain.config.getConfigurationSection("prison-mine") ?: throw RuntimeException("Missing prison-mine section in config")
    private val area: Cuboid = configSec.getString("area")?.let { Util.getArea(it) } ?: throw RuntimeException("Invalid area in config")

    override fun enabled(): Boolean {
        return enabled
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
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
        } else if (Random.nextInt(0, 100) < 5) {
            CarnivalToken.give(player, 1)
        }
    }
}