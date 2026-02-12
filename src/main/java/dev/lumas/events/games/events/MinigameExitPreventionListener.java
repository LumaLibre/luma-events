package dev.lumas.events.games.events;

import dev.lumas.events.EventMain;
import dev.lumas.events.EventPlayerManager;
import dev.lumas.events.games.interfaces.Minigame;
import dev.lumas.events.obj.EventPlayer;
import dev.lumas.events.obj.MinigameBoundingBox;
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

    //@EventHandler too much work to deal with right now
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        minigame.ensureNotIllegal(); // Ensure active. Should be unregistered if not active
        MinigameBoundingBox bb = minigame.getBoundingBox();
        if (!bb.contains(event.getFrom()) || !bb.contains(event.getTo()) || event.getCause() == PlayerTeleportEvent.TeleportCause.ENDER_PEARL) { // Ensure player is in minigame
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

        if (EventMain.getOkaeriConfig().getCommandWhitelist()
                .contains(event.getMessage().split(" ")[0].substring(1).toLowerCase())) return;

        Player player = event.getPlayer();
        EventPlayer eventPlayer = EventPlayerManager.getByUUID(player.getUniqueId());
        if (minigame.getParticipants().contains(eventPlayer)
                && !player.hasPermission("lumaevents.bypass")
                && !event.getMessage().contains("quit") // super lazy check to allow quitting
        ) {
            event.setCancelled(true);
            eventPlayer.sendMessage("You can't use this command while participating in a minigame. Use /event quit to leave.");
        }
    }
}
