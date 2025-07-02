package dev.jsinco.luma.lumaevents.games.events;

import dev.jsinco.luma.lumaevents.EventPlayerManager;
import dev.jsinco.luma.lumaevents.games.interfaces.Minigame;
import dev.jsinco.luma.lumaevents.obj.EventPlayer;
import dev.jsinco.luma.lumaevents.obj.MinigameBoundingBox;
import dev.jsinco.luma.lumaevents.obj.WorldTiedBoundingBox;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

public class MinigameExitPreventionListener implements Listener {

    private final Minigame minigame;

    public MinigameExitPreventionListener(Minigame minigame) {
        this.minigame = minigame;
    }

    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        minigame.ensureNotIllegal(); // Ensure active. Should be unregistered if not active
        MinigameBoundingBox bb = minigame.getBoundingBox();
        if (!bb.contains(event.getFrom()) || bb.contains(event.getTo())) { // Ensure player is in minigame
            return;
        }

        EventPlayer eplayer = EventPlayerManager.getByUUID(event.getPlayer().getUniqueId());

        if (minigame.getParticipants().contains(eplayer)) { // Ensure the player is supposed to BE in the minigame
            event.setCancelled(true);
            eplayer.sendMessage("You can't leave this minigame while it's active!");
        }
    }

    @EventHandler
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        minigame.ensureNotIllegal();

        Player player = event.getPlayer();
        EventPlayer eventPlayer = EventPlayerManager.getByUUID(player.getUniqueId());
        if (minigame.getParticipants().contains(eventPlayer) && minigame.getBoundingBox().contains(player) && !player.hasPermission("lumaevents.bypass")) {
            event.setCancelled(true);
            eventPlayer.sendMessage("You can't use commands while participating in this minigame. Use /event quit to leave.");
        }
    }
}
