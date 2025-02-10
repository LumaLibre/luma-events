package dev.jsinco.luma.lumaevents.events;

import dev.jsinco.luma.lumaevents.EventPlayerManager;
import dev.jsinco.luma.lumaevents.games.exceptions.GameComponentIllegallyActive;
import dev.jsinco.luma.lumaevents.games.logic.Minigame;
import dev.jsinco.luma.lumaevents.obj.EventPlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTeleportEvent;

public class MinigameExitPrevention implements Listener {

    private final Minigame minigame;

    public MinigameExitPrevention(Minigame minigame) {
        this.minigame = minigame;
    }

    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        if (!minigame.isActive()) { // Ensure active. Should be unregistered if not active
            throw new GameComponentIllegallyActive("Minigame is not active");
        } else if (!minigame.getBoundingBox().contains(event.getFrom())) { // Ensure player is in minigame
            return;
        }

        EventPlayer eplayer = EventPlayerManager.getByUUID(event.getPlayer().getUniqueId());

        if (minigame.getParticipants().contains(eplayer)) { // Ensure the player is supposed to BE in the minigame
            event.setCancelled(true);
            eplayer.sendMessage("You can't leave this minigame while it's active!");
        }
    }
}
