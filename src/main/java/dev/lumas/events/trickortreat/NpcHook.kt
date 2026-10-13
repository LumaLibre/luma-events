package dev.lumas.events.trickortreat

import net.citizensnpcs.api.CitizensAPI
import net.citizensnpcs.api.event.DespawnReason
import net.citizensnpcs.api.npc.NPC
import net.citizensnpcs.api.trait.trait.Spawned
import org.bukkit.Location
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause
import org.bukkit.plugin.Plugin

object NpcHook {

    fun get(id: Int): NPC? = CitizensAPI.getNPCRegistry().getById(id)

    fun NPC.place(location: Location) {
        if (isSpawned) teleport(location, TeleportCause.PLUGIN) else spawn(location)
    }

    // A plain despawn would be undone by Citizens on the next chunk load or restart
    fun NPC.hide() {
        getOrAddTrait(Spawned::class.java).setSpawned(false)
        if (isSpawned) despawn(DespawnReason.PLUGIN)
    }

    fun rest(plugin: Plugin, keeper: Doorkeeper) {
        val npc = get(keeper.npc ?: return) ?: return
        val apply = {
            if (keeper.hideWhileClosed) npc.hide() else if (!npc.isSpawned) npc.spawn(keeper.standby!!)
        }
        val entity = npc.entity
        if (entity != null) entity.scheduler.run(plugin, { apply() }, null)
        else plugin.server.regionScheduler.run(plugin, keeper.standby!!) { apply() }
    }
}
