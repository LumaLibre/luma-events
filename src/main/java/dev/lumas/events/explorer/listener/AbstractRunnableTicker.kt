package dev.lumas.events.explorer.listener

import dev.lumas.events.utility.Executors
import dev.lumas.events.utility.scheduler.GlobalRunnable
import dev.lumas.lumacore.manager.models.Service
import io.papermc.paper.threadedregions.scheduler.ScheduledTask
import org.bukkit.Bukkit
import org.bukkit.entity.Player

abstract class AbstractRunnableTicker(val period: Long) : GlobalRunnable(), Service, AbstractExplorerListener {

    override fun accept(task: ScheduledTask) {
        for (player in Bukkit.getOnlinePlayers()) {
            Executors.runSync(player) {
                doRun(player)
            }
        }
    }

    abstract fun doRun(player: Player)

    override fun register() {
        this.repeatingGlobal(period, period)
    }

    override fun unregister() {
        this.cancel()
    }

}