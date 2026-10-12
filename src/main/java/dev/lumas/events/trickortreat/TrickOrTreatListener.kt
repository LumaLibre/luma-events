package dev.lumas.events.trickortreat

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
import org.bukkit.inventory.EquipmentSlot

class TrickOrTreatListener(private val knocker: DoorKnocker) : Listener {

    @EventHandler(ignoreCancelled = true)
    fun onInteract(event: NexoFurnitureInteractEvent) {
        if (event.hand != EquipmentSlot.HAND || event.player.isOperatingDoors() || !knocker.knock(event.player, event.baseEntity)) return
        event.useFurniture = Event.Result.DENY
        event.isCancelled = true
    }

    // Timed breaks only fire this once when they start
    @EventHandler(ignoreCancelled = true)
    fun onDamage(event: NexoFurnitureDamageEvent) {
        if (punch(event.player, event.baseEntity)) event.isCancelled = true
    }

    // Instant breaks (creative or no hardness) skip the damage event entirely
    @EventHandler(ignoreCancelled = true)
    fun onBreak(event: NexoFurnitureBreakEvent) {
        if (punch(event.player, event.baseEntity)) event.isCancelled = true
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onConversationStart(event: ConversationStartEvent) {
        knocker.watch(event.player, event.playerConversation.conversationEntity.fileName)
    }

    private fun punch(player: Player, door: ItemDisplay) = !player.isOperatingDoors() && knocker.knock(player, door)

    // Sneaking admins use and break doors normally instead of knocking
    private fun Player.isOperatingDoors() = isSneaking && hasPermission(TrickOrTreat.ADMIN)
}
