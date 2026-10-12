package dev.lumas.events.trickortreat

import com.nexomc.nexo.api.NexoFurniture
import com.nexomc.nexo.mechanics.furniture.door.FurnitureDoor
import dev.lumas.events.trickortreat.NpcHook.hide
import dev.lumas.events.trickortreat.NpcHook.place
import io.papermc.paper.threadedregions.scheduler.ScheduledTask
import net.citizensnpcs.api.npc.NPC
import org.bukkit.Location
import org.bukkit.entity.ItemDisplay
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin

class DoorVisit(
    private val plugin: Plugin,
    private val flow: Flow,
    private val door: ItemDisplay,
    private val conversation: String,
    participants: Collection<Player>,
    private val onFinish: () -> Unit,
) {

    private enum class Phase(val holdsDoorOpen: Boolean) {
        WAITING(false), ARRIVING(false), STEPPING_OUT(true), OUTSIDE(true), RETURNING(true), CLOSING(false), FINISHED(false)
    }

    private val participants = participants.toMutableList()
    private val keeper = flow.doorkeeper.takeIf { it.isComplete && flow.openAt.isNotEmpty() }
    private val npc = keeper?.let { keeper ->
        if (!TrickOrTreat.citizensEnabled(plugin)) {
            plugin.logger.warning("Trick or treat flow ${flow.name} has an NPC, but Citizens is not enabled")
            return@let null
        }
        NpcHook.get(keeper.npc!!)
            .also { if (it == null) plugin.logger.warning("Trick or treat flow ${flow.name} uses unknown NPC ${keeper.npc}") }
    }
    private var phase = Phase.WAITING
    private var task: ScheduledTask? = null

    fun start() {
        task = plugin.server.regionScheduler.runAtFixedRate(plugin, door.location, { tick() }, 1, 1)
    }

    fun abort() {
        task?.cancel()
        if (phase == Phase.WAITING || phase == Phase.FINISHED) return
        phase = Phase.FINISHED
        runCatching {
            npc?.rest()
            setDoorOpen(false)
        }
    }

    // Also keeps the door open against Nexo's automatic close while the NPC is out
    private fun tick() {
        participants.removeIf { !it.isOnline || InteractionsHook.conversationOf(it) != conversation }
        if (phase.holdsDoorOpen) setDoorOpen(true)
        when (phase) {
            Phase.WAITING -> when {
                npc != null && participants.any { InteractionsHook.pointOf(it) in flow.openAt } -> stepOut(npc)
                participants.isEmpty() -> finish()
            }
            Phase.OUTSIDE -> if (participants.isEmpty()) stepIn(npc!!)
            else -> Unit
        }
    }

    private fun stepOut(npc: NPC) {
        phase = Phase.ARRIVING
        npc.place(keeper!!.inside!!)
        later(TELEPORT_TICKS) {
            phase = Phase.STEPPING_OUT
            setDoorOpen(true)
            later(PAUSE_TICKS) {
                npc.walk(keeper.outside!!) {
                    phase = Phase.OUTSIDE
                    participants.minByOrNull { it.location.distanceSquared(keeper.outside) }?.let { npc.faceLocation(it.eyeLocation) }
                }
            }
        }
    }

    private fun stepIn(npc: NPC) {
        phase = Phase.RETURNING
        val keeper = keeper!!
        npc.face(keeper.inside!!)
        later(PAUSE_TICKS) {
            npc.walk(keeper.inside) {
                npc.face(keeper.outside!!)
                later(PAUSE_TICKS) {
                    phase = Phase.CLOSING
                    setDoorOpen(false)
                    later(PAUSE_TICKS) {
                        npc.rest()
                        finish()
                    }
                }
            }
        }
    }

    private fun finish() {
        phase = Phase.FINISHED
        task?.cancel()
        onFinish()
    }

    private fun NPC.face(location: Location) {
        if (isSpawned) faceLocation(location)
    }

    private fun NPC.rest() {
        if (keeper!!.hideWhileClosed) hide() else place(keeper.standby!!)
    }

    // Falls back to teleporting so a blocked path or a lost callback never leaves the NPC stranded outside
    private fun NPC.walk(target: Location, then: () -> Unit) {
        var arrived = false
        fun arrive(teleport: Boolean) {
            if (arrived || phase == Phase.FINISHED) return
            arrived = true
            if (teleport) place(target)
            then()
        }
        if (!isSpawned) return arrive(true)
        navigator.setTarget(target)
        if (!navigator.isNavigating) return arrive(true)
        navigator.localParameters
            .distanceMargin(ARRIVAL_MARGIN)
            .pathDistanceMargin(0.0)
            .addSingleUseCallback { reason ->
                later(1) { arrive(reason != null || !isSpawned || entity.location.distance(target) > ARRIVAL_MARGIN * 2) }
            }
        later(WALK_TIMEOUT_TICKS) {
            if (arrived) return@later
            navigator.cancelNavigation()
            arrive(true)
        }
    }

    private fun setDoorOpen(open: Boolean) {
        if (!door.isValid) return
        val mechanic = NexoFurniture.furnitureMechanic(door) ?: return
        val nexoDoor = mechanic.door ?: return
        if (FurnitureDoor.isOpen(door) != open) nexoDoor.toggleState(door, mechanic)
    }

    private fun later(ticks: Long, action: () -> Unit) {
        plugin.server.regionScheduler.runDelayed(plugin, door.location, { action() }, ticks)
    }

    companion object {
        private const val TELEPORT_TICKS = 20L
        private const val PAUSE_TICKS = 10L
        private const val ARRIVAL_MARGIN = 0.5
        private const val WALK_TIMEOUT_TICKS = 200L
    }
}
