package dev.lumas.events.games.events;

import dev.lumas.events.EventPlayerManager;
import dev.lumas.events.games.obj.InventorySnapshot;
import dev.lumas.events.games.InventorySnapshotManager;
import dev.lumas.events.games.interfaces.Minigame;
import dev.lumas.events.obj.EventPlayer;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class MinigameInventoryRestoringQuitListener implements Listener {

    private final Minigame minigame;

    public MinigameInventoryRestoringQuitListener(Minigame minigame) {
        this.minigame = minigame;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        minigame.ensureNotIllegal();
        Player player = event.getPlayer();
        InventorySnapshot inventorySnapshot = InventorySnapshotManager.INSTANCE.getSnapshotByOwner(player.getUniqueId());
        if (inventorySnapshot == null) {
            return;
        }
        inventorySnapshot.restore(player);
        InventorySnapshotManager.INSTANCE.unregisterSnapshot(inventorySnapshot);
        EventPlayer eplayer = EventPlayerManager.getByUUID(player.getUniqueId());
        minigame.removeParticipant(eplayer);
        //minigame.sendAudienceMessage( eplayer.getName() + " has left the minigame.");

        Location gameDropOffLocation = minigame.getGameDropOffLocation();
        if (gameDropOffLocation != null) {
            player.teleport(gameDropOffLocation);
        }
    }
}
