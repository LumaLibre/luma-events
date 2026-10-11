package dev.lumas.events.games.events;

import dev.lumas.core.annotation.Autowire;
import dev.lumas.core.annotation.Register;
import dev.lumas.events.EventMain;
import dev.lumas.events.manager.EventPlayerManager;
import dev.lumas.events.model.EventPlayer;
import io.papermc.paper.event.player.AsyncPlayerSpawnLocationEvent;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.UUID;

@Register(Autowire.LISTENER)
public class MinigameCleanupListener implements Listener {

    // TODO: Should this be separated between two listeners?

    @EventHandler
    public void onPlayerAsyncSpawn(AsyncPlayerSpawnLocationEvent event) {
        UUID uuid = event.getConnection().getProfile().getId();

        if (uuid == null) {
            return;
        }

        // Preload data so /event join isn't a blocking file read on a region tick
        EventPlayerManager.warmCache(uuid);

        EventPlayer eventPlayer = EventPlayerManager.getByUUIDOrNull(uuid);

        if (eventPlayer != null && eventPlayer.minigameDirty()) {
            Location loc = EventMain.getConfiguration().getGameDropOffLocation();
            if (loc != null) {
                event.setSpawnLocation(loc);
            }
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        EventPlayerManager.warmCache(player.getUniqueId());
        EventPlayer eventPlayer = EventPlayerManager.getByUUIDOrNull(player.getUniqueId());

        if (eventPlayer != null && eventPlayer.minigameDirty()) {
            if (player.getGameMode() != GameMode.SURVIVAL && !player.isOp()) {
                player.setGameMode(GameMode.SURVIVAL);
            }
            eventPlayer.minigameDirty(false);
        }
    }
}
