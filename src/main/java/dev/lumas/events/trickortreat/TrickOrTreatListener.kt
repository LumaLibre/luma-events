package dev.lumas.events.trickortreat

import com.destroystokyo.paper.event.player.PlayerUseUnknownEntityEvent
import com.nexomc.nexo.api.NexoFurniture
import com.nexomc.nexo.api.events.furniture.NexoFurnitureBreakEvent
import com.nexomc.nexo.api.events.furniture.NexoFurnitureDamageEvent
import com.nexomc.nexo.api.events.furniture.NexoFurnitureInteractEvent
import it.ajneb97.api.ConversationStartEvent
import org.bukkit.entity.ItemDisplay
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class TrickOrTreatListener(private val knocker: DoorKnocker) : Listener {

    private val lastPunch = ConcurrentHashMap<UUID, Long>()

    @EventHandler(ignoreCancelled = false)
    fun onInteract(event: NexoFurnitureInteractEvent) {
        if (event.hand != EquipmentSlot.HAND || event.player.isOperatingDoors() || !knocker.knock(event.player, event.baseEntity)) return
        event.useFurniture = Event.Result.DENY
        event.isCancelled = true
    }

    // Timed breaks only fire this once when they start
    @EventHandler(ignoreCancelled = false)
    fun onDamage(event: NexoFurnitureDamageEvent) {
        if (punch(event.player, event.baseEntity)) event.isCancelled = true
    }

    // Instant breaks (creative or no hardness) skip the damage event entirely
    @EventHandler(ignoreCancelled = false)
    fun onBreak(event: NexoFurnitureBreakEvent) {
        if (punch(event.player, event.baseEntity)) event.isCancelled = true
    }

    // Nexo only fires the damage/break events for players who can break at the door

    // Packet hitboxes (interaction, shulker, ghast)
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    fun onPunchHitbox(event: PlayerUseUnknownEntityEvent) {
        if (!event.isAttack) return
        val door = NexoFurniture.baseEntity(event.entityId, event.player.world) ?: return
        punch(event.player, door)
    }

    // Barrier hitboxes
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    fun onPunchBarrier(event: PlayerInteractEvent) {
        if (event.action != Action.LEFT_CLICK_BLOCK || event.hand != EquipmentSlot.HAND) return
        val door = event.clickedBlock?.let(NexoFurniture::baseEntity)
            ?: event.interactionPoint?.let(NexoFurniture::baseEntity)
            ?: return
        punch(event.player, door)
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onConversationStart(event: ConversationStartEvent) {
        knocker.watch(event.player, event.playerConversation.conversationEntity.fileName)
    }

    private fun punch(player: Player, door: ItemDisplay): Boolean {
        if (player.isOperatingDoors()) return false
        val now = System.currentTimeMillis()
        val last = lastPunch.put(player.uniqueId, now)
        if (last != null && now - last < PUNCH_DEDUPE_MS) return knocker.isDoor(door)
        return knocker.knock(player, door)
    }

    // Sneaking admins use and break doors normally instead of knocking
    private fun Player.isOperatingDoors() = isSneaking && hasPermission(TrickOrTreat.ADMIN)

    companion object {
        private const val PUNCH_DEDUPE_MS = 40L
    }
}
