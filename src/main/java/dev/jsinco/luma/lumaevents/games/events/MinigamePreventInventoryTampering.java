package dev.jsinco.luma.lumaevents.games.events;

import dev.jsinco.luma.lumaevents.EventPlayerManager;
import dev.jsinco.luma.lumaevents.games.logic.Minigame;
import dev.jsinco.luma.lumaevents.obj.EventPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerCommandSendEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;

public class MinigamePreventInventoryTampering implements Listener {

    private final Minigame minigame;

    public MinigamePreventInventoryTampering(Minigame minigame) {
        this.minigame = minigame;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        minigame.ensureNotIllegal();

        Player player = (Player) event.getWhoClicked();
        EventPlayer eventPlayer = EventPlayerManager.getByUUID(player.getUniqueId());
        if (minigame.getParticipants().contains(eventPlayer) && minigame.getBoundingBox().contains(player)) {
            event.setCancelled(true);
            eventPlayer.sendMessage("You can't change your inventory while participating in this minigame.");
        }
    }

    @EventHandler
    public void onPlayerSwapHotBarItems(PlayerItemHeldEvent event) {
        minigame.ensureNotIllegal();

        Player player = event.getPlayer();
        EventPlayer eventPlayer = EventPlayerManager.getByUUID(player.getUniqueId());
        if (minigame.getParticipants().contains(eventPlayer) && minigame.getBoundingBox().contains(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        minigame.ensureNotIllegal();

        Player player = event.getPlayer();
        EventPlayer eventPlayer = EventPlayerManager.getByUUID(player.getUniqueId());
        if (minigame.getParticipants().contains(eventPlayer) && minigame.getBoundingBox().contains(player)) {
            event.setCancelled(true);
            eventPlayer.sendMessage("You can't drop items while participating in this minigame.");
        }
    }

//    @EventHandler
//    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
//        minigame.ensureNotIllegal();
//
//        Player player = event.getPlayer();
//        EventPlayer eventPlayer = EventPlayerManager.getByUUID(player.getUniqueId());
//        if (minigame.getParticipants().contains(eventPlayer) && minigame.getBoundingBox().contains(player)) {
//            event.setCancelled(true);
//            eventPlayer.sendMessage("You can't use commands while participating in this minigame. Use /easter quit to leave.");
//        }
//    }
}
