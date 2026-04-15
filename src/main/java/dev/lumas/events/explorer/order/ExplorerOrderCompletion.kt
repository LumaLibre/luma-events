package dev.lumas.events.explorer.order

import dev.lumas.events.model.EventPlayer
import dev.lumas.events.utility.Executors
import org.bukkit.Sound

class ExplorerOrderCompletion(
    val explorerOrder: ExplorerOrder<*>,
    var currentQuantity: Int,
    val maxQuantity: Int
) {

    companion object {
        @JvmStatic
        fun empty(explorerOrder: ExplorerOrder<*>) = ExplorerOrderCompletion(explorerOrder, 0, explorerOrder.quantity)
    }

    fun isCompleted(): Boolean {
        return currentQuantity >= maxQuantity
    }

    fun progress() {
        currentQuantity++
    }

    fun progress(amount: Int) {
        currentQuantity += amount
    }

    fun completionEffects(eventPlayer: EventPlayer): Boolean {
        if (!isCompleted()) {
            return false
        }

        val name = explorerOrder.name
        eventPlayer.souls += explorerOrder.souls

        val ticksBetween = 3L // adjust for speed

        for (i in name.indices) {
            Executors.delayedSync(eventPlayer, (i * ticksBetween) + 30L) {
                eventPlayer.sendTitle("<obf>${name.take(i + 1)}</obf>", "")
                if (i == name.lastIndex) {
                    Executors.delayedSync(eventPlayer, 40L) {
                        eventPlayer.sendTitle(name, explorerOrder.objective)
                        eventPlayer.operatePlayer {
                            it.playSound(it.location, Sound.ENTITY_FIREWORK_ROCKET_BLAST_FAR, 0.5f, 1f)
                        }
                    }
                }
            }
        }
        return true
    }
}