package dev.lumas.events.games.events;

import dev.lumas.core.annotation.Autowire;
import dev.lumas.core.annotation.Register;
import dev.lumas.events.EventMain;
import dev.lumas.events.manager.EventPlayerManager;
import dev.lumas.events.model.EventPlayer;
import io.papermc.paper.event.player.AsyncPlayerSpawnLocationEvent;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.UUID;

@Register(Autowire.LISTENER)
public class MinigameTeleportAwayListener implements Listener {

    @EventHandler
    public void onPlayerAsyncSpawn(AsyncPlayerSpawnLocationEvent event) {
        UUID uuid = event.getConnection().getProfile().getId();

        if (uuid == null) {
            return;
        }

        // Preload data so /event join isn't a blocking file read on a region tick
        EventPlayerManager.warmCache(uuid);

        EventPlayer eventPlayer = EventPlayerManager.getByUUIDOrNull(uuid);

        if (eventPlayer != null && eventPlayer.wasDisconnectedFromMinigame()) {
            Location loc = EventMain.getOkaeriConfig().getGameDropOffLocation();
            if (loc != null) {
                event.setSpawnLocation(loc);
            }
            eventPlayer.resetDisconnectedFromMinigame();
        }
    }
}
